package com.kingdomcomecombat.client.collision;

import com.kingdomcomecombat.collision.AnimatedAttackHitboxLibrary;
import com.kingdomcomecombat.client.compat.FirstPersonRenderCompat;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.client.render.FirstPersonBodyRenderOffsetContext;
import com.kingdomcomecombat.client.render.CollisionOnlyRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class ClientItemHitboxCache {
    private static final double MODEL_UNIT_TO_BLOCK = 1.0 / 16.0;
    private static final Map<Integer, Entry> CACHE = new HashMap<>();
    private static final ThreadLocal<Integer> CAPTURE_ENTITY_ID = new ThreadLocal<>();

    private ClientItemHitboxCache() {
    }

    public static void beginCapture(int entityId) {
        // First Person Model renders the local player a second time with a
        // camera-relative matrix.  That pass is visual only: letting it update
        // this cache makes combat collision disagree with third person.
        MinecraftClient client = MinecraftClient.getInstance();
        boolean localFirstPerson = client.player != null
                && entityId == client.player.getId()
                && client.options.getPerspective().isFirstPerson();
        if (!FirstPersonRenderCompat.isExternalBodyRenderOrPreparing()
                && (!localFirstPerson || CollisionOnlyRenderContext.isActive())
                && entityId >= 0 && shouldTrack(entityId)) {
            CAPTURE_ENTITY_ID.set(entityId);
        }
    }

    public static void endCapture() {
        CAPTURE_ENTITY_ID.remove();
    }

    public static boolean isHiddenLocalFirstPersonCapture() {
        Integer entityId = CAPTURE_ENTITY_ID.get();
        MinecraftClient client = MinecraftClient.getInstance();
        return entityId != null
                && client.player != null
                && entityId == client.player.getId()
                && client.options.getPerspective().isFirstPerson()
                && CollisionOnlyRenderContext.isActive();
    }

    public static void captureItemModel(MatrixStack matrices) {
        captureItemModel(matrices, null);
    }

    public static void captureItemModel(MatrixStack matrices, Supplier<Vector3f[]> verticesSupplier) {
        Integer entityId = CAPTURE_ENTITY_ID.get();
        if (entityId == null) {
            return;
        }

        Vec3d sizeUnits = getEntityRealHitboxSizeUnits(entityId);
        Vec3d offsetUnits = getEntityRealHitboxOffsetUnits(entityId);
        Vec3d rotationDegrees = getEntityRealHitboxRotationDegrees(entityId);
        Vector3f center = getModelCenter(verticesSupplier)
                .orElseGet(() -> getSpecialModelCenter(entityId));
        center.add(
                (float) (offsetUnits.x * MODEL_UNIT_TO_BLOCK),
                (float) (offsetUnits.y * MODEL_UNIT_TO_BLOCK),
                (float) (offsetUnits.z * MODEL_UNIT_TO_BLOCK)
        );
        put(entityId, matrices, center, new Vec3d(
                sizeUnits.x * 0.5 * MODEL_UNIT_TO_BLOCK,
                sizeUnits.z * 0.5 * MODEL_UNIT_TO_BLOCK,
                sizeUnits.y * 0.5 * MODEL_UNIT_TO_BLOCK
        ), rotationDegrees);
    }

    public static void update(int entityId, MatrixStack matrices) {
        Vec3d sizeUnits = getEntityRealHitboxSizeUnits(entityId);
        Vec3d rotationDegrees = getEntityRealHitboxRotationDegrees(entityId);
        double centerZ = -sizeUnits.z * 0.5 * MODEL_UNIT_TO_BLOCK;
        put(entityId, matrices, new Vector3f(0.0F, 0.0F, (float) centerZ), new Vec3d(
                sizeUnits.x * 0.5 * MODEL_UNIT_TO_BLOCK,
                sizeUnits.y * 0.5 * MODEL_UNIT_TO_BLOCK,
                sizeUnits.z * 0.5 * MODEL_UNIT_TO_BLOCK
        ), rotationDegrees);
    }

    private static void put(
            int entityId,
            MatrixStack matrices,
            Vector3f localCenter,
            Vec3d localHalfExtents,
            Vec3d rotationDegrees
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || entityId < 0) {
            return;
        }
        if (!shouldTrack(entityId)) {
            CACHE.remove(entityId);
            return;
        }

        Matrix4f matrix = new Matrix4f(matrices.peek().getPositionMatrix());
        Vec3d center = transformPosition(matrix, localCenter)
                .add(client.gameRenderer.getCamera().getPos())
                .subtract(FirstPersonBodyRenderOffsetContext.offsetFor(entityId));
        Vec3d rawAxisX = transformDirection(matrix, toVector3f(rotateLocalAxis(1.0, 0.0, 0.0, rotationDegrees)));
        Vec3d rawAxisY = transformDirection(matrix, toVector3f(rotateLocalAxis(0.0, 1.0, 0.0, rotationDegrees)));
        Vec3d rawAxisZ = transformDirection(matrix, toVector3f(rotateLocalAxis(0.0, 0.0, 1.0, rotationDegrees)));
        double scaleX = Math.max(rawAxisX.length(), 1.0E-6);
        double scaleY = Math.max(rawAxisY.length(), 1.0E-6);
        double scaleZ = Math.max(rawAxisZ.length(), 1.0E-6);
        Vec3d axisX = rawAxisX.multiply(1.0 / scaleX);
        Vec3d axisY = rawAxisY.multiply(1.0 / scaleY);
        Vec3d axisZ = rawAxisZ.multiply(1.0 / scaleZ);

        CACHE.put(entityId, new Entry(
                client.world.getTime(),
                new AnimatedAttackHitboxLibrary.OrientedBox(
                        center,
                        new Vec3d(
                                localHalfExtents.x * scaleX,
                                localHalfExtents.y * scaleY,
                                localHalfExtents.z * scaleZ
                        ),
                        axisX,
                        axisY,
                        axisZ
                )
        ));
    }

    private static Vec3d getEntityRealHitboxSizeUnits(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d fallback = AnimatedAttackHitboxLibrary.getRealHitboxSizeUnits();
        if (client.world == null || entityId < 0) {
            return fallback;
        }

        if (client.world.getEntityById(entityId) instanceof LivingEntity livingEntity) {
            return EquipmentCombatAttributesRegistry.realHitboxSizeUnits(
                    livingEntity,
                    livingEntity.getMainHandStack(),
                    fallback
            );
        }

        return fallback;
    }

    private static Vec3d getEntityRealHitboxOffsetUnits(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d fallback = AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits();
        if (client.world == null || entityId < 0) {
            return fallback;
        }

        if (client.world.getEntityById(entityId) instanceof LivingEntity livingEntity) {
            return EquipmentCombatAttributesRegistry.realHitboxOffsetUnits(
                    livingEntity.getMainHandStack(),
                    fallback
            );
        }

        return fallback;
    }

    private static Vec3d getEntityRealHitboxRotationDegrees(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d fallback = AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees();
        if (client.world == null || entityId < 0) {
            return fallback;
        }

        if (client.world.getEntityById(entityId) instanceof LivingEntity livingEntity) {
            return EquipmentCombatAttributesRegistry.realHitboxRotationDegrees(
                    livingEntity.getMainHandStack(),
                    fallback
            );
        }

        return fallback;
    }

    public static Optional<AnimatedAttackHitboxLibrary.OrientedBox> get(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return Optional.empty();
        }

        Entry entry = CACHE.get(entityId);
        if (entry == null || client.world.getTime() - entry.worldTime() > 3L) {
            return Optional.empty();
        }

        return Optional.of(entry.box());
    }

    public static void invalidate(int entityId) {
        CACHE.remove(entityId);
    }

    public static void clear() {
        CACHE.clear();
        CAPTURE_ENTITY_ID.remove();
    }

    public static void cleanup() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            CACHE.clear();
            return;
        }

        long worldTime = client.world.getTime();
        CACHE.entrySet().removeIf(entry ->
                worldTime - entry.getValue().worldTime() > 20L
                        || client.world.getEntityById(entry.getKey()) == null
                        || !shouldTrack(entry.getKey())
        );
    }

    private static boolean shouldTrack(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || entityId < 0) {
            return false;
        }
        if (!(client.world.getEntityById(entityId) instanceof LivingEntity entity)
                || client.player == null) {
            return false;
        }
        return ClientCollisionTrackingPolicy.shouldCaptureItemHitbox(entity);
    }

    private static Vec3d transformPosition(Matrix4f matrix, Vector3f position) {
        Vector3f result = matrix.transformPosition(position);
        return new Vec3d(result.x, result.y, result.z);
    }

    private static Vec3d transformDirection(Matrix4f matrix, Vector3f direction) {
        Vector3f result = matrix.transformDirection(direction);
        return new Vec3d(result.x, result.y, result.z);
    }

    private static Vector3f toVector3f(Vec3d vector) {
        return new Vector3f((float) vector.x, (float) vector.y, (float) vector.z);
    }

    private static Vec3d rotateLocalAxis(double x, double y, double z, Vec3d rotationDegrees) {
        double pitch = Math.toRadians(rotationDegrees.x);
        double yaw = Math.toRadians(rotationDegrees.y);
        double roll = Math.toRadians(rotationDegrees.z);

        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosRoll = Math.cos(roll);
        double sinRoll = Math.sin(roll);

        double rollX = x * cosRoll - y * sinRoll;
        double rollY = x * sinRoll + y * cosRoll;
        double rollZ = z;

        double pitchX = rollX;
        double pitchY = rollY * cosPitch - rollZ * sinPitch;
        double pitchZ = rollY * sinPitch + rollZ * cosPitch;

        double yawX = pitchX * cosYaw + pitchZ * sinYaw;
        double yawY = pitchY;
        double yawZ = -pitchX * sinYaw + pitchZ * cosYaw;

        return new Vec3d(yawX, yawY, yawZ);
    }

    private static Optional<Vector3f> getModelCenter(Supplier<Vector3f[]> verticesSupplier) {
        if (verticesSupplier == null) {
            return Optional.empty();
        }
        Vector3f[] vertices = verticesSupplier.get();
        if (vertices == null || vertices.length == 0) {
            return Optional.empty();
        }

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (Vector3f vertex : vertices) {
            if (vertex == null) {
                continue;
            }
            minX = Math.min(minX, vertex.x);
            minY = Math.min(minY, vertex.y);
            minZ = Math.min(minZ, vertex.z);
            maxX = Math.max(maxX, vertex.x);
            maxY = Math.max(maxY, vertex.y);
            maxZ = Math.max(maxZ, vertex.z);
        }
        if (!Float.isFinite(minX) || !Float.isFinite(minY) || !Float.isFinite(minZ)) {
            return Optional.empty();
        }
        return Optional.of(new Vector3f(
                (minX + maxX) * 0.5F,
                (minY + maxY) * 0.5F,
                (minZ + maxZ) * 0.5F
        ));
    }

    private static Vector3f getSpecialModelCenter(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null
                && client.world.getEntityById(entityId) instanceof LivingEntity livingEntity
                && livingEntity.getMainHandStack().isOf(Items.TRIDENT)) {
            // The trident is rendered through SpecialModelRenderer, so the layer's
            // regular vertex supplier is empty. Its model spans Y=-4..27 and the
            // renderer flips Y, placing the visual center at -11.5 model units.
            return new Vector3f(0.0F, (float) (-11.5 * MODEL_UNIT_TO_BLOCK), 0.0F);
        }
        return new Vector3f();
    }

    private record Entry(
            long worldTime,
            AnimatedAttackHitboxLibrary.OrientedBox box
    ) {
    }
}
