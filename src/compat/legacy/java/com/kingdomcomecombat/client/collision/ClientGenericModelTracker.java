package com.kingdomcomecombat.client.collision;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.LegacyBipedAnimationApplier;
import com.kingdomcomecombat.client.animation.LegacyMobStanceHeadTargeting;
import com.kingdomcomecombat.client.render.FirstPersonBodyRenderOffsetContext;
import com.kingdomcomecombat.collision.AnimatedAttackHitboxLibrary;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;

/** Reads final legacy-renderer matrices, equivalent to the modern vertex capture path. */
public final class ClientGenericModelTracker {
    private static final float UNIT = 1.0F / 16.0F;
    private static RenderContext context;
    private static PendingAnimation pendingAnimation;

    private ClientGenericModelTracker() {}

    public static void clearModelClassificationCache() {}

    /**
     * Defers the combat pose until the first actual ModelPart render. This is
     * later than renderer animation callbacks used by compatibility mods, so
     * they cannot overwrite KCC's final arm and leg rotations afterward.
     */
    public static void armFinalCombatAnimation(LivingEntity entity, EntityModel<?> model) {
        pendingAnimation = new PendingAnimation(entity, model);
    }

    public static void resetPendingAnimation() {
        pendingAnimation = null;
    }

    public static void applyPendingCombatAnimation() {
        PendingAnimation pending = pendingAnimation;
        pendingAnimation = null;
        if (pending == null
                || !ClientEntityGeckoAnimationState.hasActiveCombatLayer(pending.entity().getId())) return;

        if (pending.model() instanceof BipedEntityModel<?> biped) {
            float originalHeadYaw = biped.head.yaw;
            float originalHeadPitch = biped.head.pitch;
            LegacyBipedAnimationApplier.apply((BipedEntityModel) biped, pending.entity());
            if (ClientEntityGeckoAnimationState.isStanceOnly(pending.entity().getId())) {
                LegacyMobStanceHeadTargeting.apply(
                        pending.entity(),
                        (float) Math.toDegrees(originalHeadYaw),
                        (float) Math.toDegrees(originalHeadPitch),
                        biped.head,
                        biped.body
                );
            }
            return;
        }

        applyFinalStructuredCombatAnimation(pending.entity(), pending.model());
    }

    /** Legacy equivalent of the modern structural model animation path. */
    private static void applyFinalStructuredCombatAnimation(LivingEntity entity, EntityModel<?> model) {
        ModelPart head = findPart(model, "head", "head_parts", "headpart", "skull");
        ModelPart rightArm = findPart(model, "rightarm", "right_arm", "arm_right", "r_arm", "right_hand");
        ModelPart leftArm = findPart(model, "leftarm", "left_arm", "arm_left", "l_arm", "left_hand");
        ModelPart rightLeg = findPart(model, "rightleg", "right_leg", "leg_right", "r_leg", "right_foot");
        ModelPart leftLeg = findPart(model, "leftleg", "left_leg", "leg_left", "l_leg", "left_foot");
        ModelPart body = findPart(model, "body", "torso", "chest", "upper_body", "upperbody");
        if (body == null || head == null || rightArm == null || leftArm == null
                || rightLeg == null || leftLeg == null) return;
        LegacyBipedAnimationApplier.applyParts(
                entity, body, head, rightArm, leftArm, rightLeg, leftLeg);
    }

    private static ModelPart findPart(EntityModel<?> model, String... aliases) {
        for (Class<?> type = model.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!ModelPart.class.isAssignableFrom(field.getType())) continue;
                String fieldName = field.getName().toLowerCase(java.util.Locale.ROOT);
                for (String alias : aliases) {
                    if (!fieldName.equals(alias.toLowerCase(java.util.Locale.ROOT))) continue;
                    try {
                        field.setAccessible(true);
                        return (ModelPart) field.get(model);
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        break;
                    }
                }
            }
        }
        return null;
    }

    public static boolean begin(int entityId, EntityModel<?> model, MatrixStack matrices) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null
                || !(client.world.getEntityById(entityId) instanceof LivingEntity entity)
                || !(model instanceof BipedEntityModel<?> biped)
                || !ClientCollisionTrackingPolicy.shouldCaptureHurtbox(entity)) {
            context = null;
            return false;
        }
        IdentityHashMap<ModelPart, List<PartSpec>> parts = new IdentityHashMap<>();
        add(parts, biped.head, HumanoidHurtboxLibrary.Part.HEAD, 0, -4, 0, 8, 8, 8);
        add(parts, biped.body, HumanoidHurtboxLibrary.Part.BODY, 0, 6, 0, 8, 12, 4);
        add(parts, biped.rightArm, HumanoidHurtboxLibrary.Part.SHOULDERS, 0, 3, 0, 4, 6, 4);
        add(parts, biped.rightArm, HumanoidHurtboxLibrary.Part.RIGHT_ARM, 0, 9, 0, 4, 6, 4);
        add(parts, biped.leftArm, HumanoidHurtboxLibrary.Part.SHOULDERS, 0, 3, 0, 4, 6, 4);
        add(parts, biped.leftArm, HumanoidHurtboxLibrary.Part.LEFT_ARM, 0, 9, 0, 4, 6, 4);
        add(parts, biped.rightLeg, HumanoidHurtboxLibrary.Part.RIGHT_LEG, 0, 6, 0, 4, 12, 4);
        add(parts, biped.leftLeg, HumanoidHurtboxLibrary.Part.LEFT_LEG, 0, 6, 0, 4, 12, 4);
        Vector3f renderOrigin = matrices.peek().getPositionMatrix().transformPosition(new Vector3f());
        context = new RenderContext(entityId, parts, new IdentityHashMap<>(),
                entity.getLerpedPos(client.getRenderTickCounter().getTickDelta(true)),
                new Vec3d(renderOrigin.x, renderOrigin.y, renderOrigin.z));
        return true;
    }

    public static void captureIfHead(ModelPart part, MatrixStack matrices) {
        applyPendingCombatAnimation();
        RenderContext current = context;
        if (current == null || !current.parts.containsKey(part)) return;
        matrices.push();
        part.rotate(matrices);
        Matrix4f transform = new Matrix4f(matrices.peek().getPositionMatrix());
        matrices.pop();
        List<HumanoidHurtboxLibrary.PartBox> boxes = new ArrayList<>();
        for (PartSpec spec : current.parts.get(part)) {
            Vector3f center = transform.transformPosition(new Vector3f(spec.x * UNIT, spec.y * UNIT, spec.z * UNIT));
            Vector3f x = transform.transformDirection(new Vector3f(1, 0, 0));
            Vector3f y = transform.transformDirection(new Vector3f(0, 1, 0));
            Vector3f z = transform.transformDirection(new Vector3f(0, 0, 1));
            double sx = x.length(), sy = y.length(), sz = z.length();
            x.normalize(); y.normalize(); z.normalize();
            Vec3d rendered = new Vec3d(center.x, center.y, center.z);
            Vec3d world = current.entityOrigin.add(rendered.subtract(current.renderOrigin))
                    .subtract(FirstPersonBodyRenderOffsetContext.offsetFor(current.entityId));
            boxes.add(new HumanoidHurtboxLibrary.PartBox(spec.part,
                    new AnimatedAttackHitboxLibrary.OrientedBox(world,
                            new Vec3d(spec.width * UNIT * 0.5 * sx, spec.height * UNIT * 0.5 * sy,
                                    spec.depth * UNIT * 0.5 * sz),
                            new Vec3d(x.x, x.y, x.z), new Vec3d(y.x, y.y, y.z),
                            new Vec3d(z.x, z.y, z.z))));
        }
        current.rendered.put(part, boxes);
    }

    public static void end(MatrixStack matrices) {
        pendingAnimation = null;
        RenderContext current = context;
        context = null;
        if (current == null || current.rendered.isEmpty()) return;
        ClientModelHurtboxCache.updateRendered(current.entityId,
                current.rendered.values().stream().flatMap(List::stream).toList());
    }

    private static void add(Map<ModelPart, List<PartSpec>> map, ModelPart modelPart,
                            HumanoidHurtboxLibrary.Part part, float x, float y, float z,
                            float width, float height, float depth) {
        map.computeIfAbsent(modelPart, ignored -> new ArrayList<>())
                .add(new PartSpec(part, x, y, z, width, height, depth));
    }

    private record PartSpec(HumanoidHurtboxLibrary.Part part, float x, float y, float z,
                            float width, float height, float depth) {}
    private record RenderContext(int entityId,
                                 IdentityHashMap<ModelPart, List<PartSpec>> parts,
                                 IdentityHashMap<ModelPart, List<HumanoidHurtboxLibrary.PartBox>> rendered,
                                 Vec3d entityOrigin, Vec3d renderOrigin) {}
    private record PendingAnimation(LivingEntity entity, EntityModel<?> model) {}
}
