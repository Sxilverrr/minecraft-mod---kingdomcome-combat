package com.kingdomcomecombat.client.collision;

import com.kingdomcomecombat.collision.AnimatedAttackHitboxLibrary;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.animation.GeckoLikeAnimationLibrary;
import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.CombatWeaponUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClientModelHurtboxCache {
    private static final double MODEL_UNIT_TO_BLOCK = 1.0 / 16.0;
    private static final double CLASSIC_MODEL_HEIGHT_BLOCKS = 32.0 * MODEL_UNIT_TO_BLOCK;
    private static final double MODEL_ORIGIN_Y = 24.0;
    private static final double TRACK_DISTANCE = 16.0;
    private static final double TRACK_DISTANCE_SQUARED = TRACK_DISTANCE * TRACK_DISTANCE;
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final Map<Integer, Entry> CACHE = new HashMap<>();

    private ClientModelHurtboxCache() {
    }

    public static void update(
            int entityId,
            ModelPart head,
            ModelPart body,
            ModelPart rightArm,
            ModelPart leftArm,
            ModelPart rightLeg,
            ModelPart leftLeg
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        if (!(client.world.getEntityById(entityId) instanceof LivingEntity entity)) {
            return;
        }

        if (!HumanoidHurtboxLibrary.isHumanoidTarget(entity)) {
            return;
        }
        if (!shouldTrack(entity)) {
            CACHE.remove(entityId);
            return;
        }

        long worldTime = client.world.getTime();
        float yaw = entity.getBodyYaw();
        double heightScale = Math.max(0.5, entity.getHeight() / CLASSIC_MODEL_HEIGHT_BLOCKS);
        CACHE.put(entityId, new Entry(
                worldTime,
                List.of(
                        partBox(entity, HumanoidHurtboxLibrary.Part.HEAD, yaw, heightScale, head,
                                new Vec3d(0.0, -4.0, 0.0), 8.0, 8.0, 8.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.BODY, yaw, heightScale, body,
                                new Vec3d(0.0, 6.0, 0.0), 8.0, 12.0, 4.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.SHOULDERS, yaw, heightScale, rightArm,
                                new Vec3d(0.0, 3.0, 0.0), 4.0, 6.0, 4.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.SHOULDERS, yaw, heightScale, leftArm,
                                new Vec3d(0.0, 3.0, 0.0), 4.0, 6.0, 4.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.RIGHT_ARM, yaw, heightScale, rightArm,
                                new Vec3d(0.0, 9.0, 0.0), 4.0, 6.0, 4.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.LEFT_ARM, yaw, heightScale, leftArm,
                                new Vec3d(0.0, 9.0, 0.0), 4.0, 6.0, 4.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.RIGHT_LEG, yaw, heightScale, rightLeg,
                                new Vec3d(0.0, 6.0, 0.0), 4.0, 12.0, 4.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.LEFT_LEG, yaw, heightScale, leftLeg,
                                new Vec3d(0.0, 6.0, 0.0), 4.0, 12.0, 4.0)
                )
        ));
    }

    public static void updateIllager(
            int entityId,
            ModelPart head,
            ModelPart rightArm,
            ModelPart leftArm,
            ModelPart rightLeg,
            ModelPart leftLeg
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        if (!(client.world.getEntityById(entityId) instanceof LivingEntity entity)) {
            return;
        }

        if (!HumanoidHurtboxLibrary.isHumanoidTarget(entity)) {
            return;
        }
        if (!shouldTrack(entity)) {
            CACHE.remove(entityId);
            return;
        }

        long worldTime = client.world.getTime();
        float yaw = entity.getBodyYaw();
        double heightScale = Math.max(0.5, entity.getHeight() / CLASSIC_MODEL_HEIGHT_BLOCKS);
        SimulatedPart body = SimulatedPart.defaults(0.0F, 0.0F, 0.0F);
        CACHE.put(entityId, new Entry(
                worldTime,
                List.of(
                        partBox(entity, HumanoidHurtboxLibrary.Part.HEAD, yaw, heightScale, head,
                                new Vec3d(0.0, -4.0, 0.0), 8.0, 8.0, 8.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.BODY, yaw, heightScale, body,
                                new Vec3d(0.0, 6.0, 0.0), 8.0, 12.0, 4.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.SHOULDERS, yaw, heightScale, rightArm,
                                new Vec3d(0.0, 3.0, 0.0), 4.0, 6.0, 4.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.SHOULDERS, yaw, heightScale, leftArm,
                                new Vec3d(0.0, 3.0, 0.0), 4.0, 6.0, 4.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.RIGHT_ARM, yaw, heightScale, rightArm,
                                new Vec3d(0.0, 9.0, 0.0), 4.0, 6.0, 4.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.LEFT_ARM, yaw, heightScale, leftArm,
                                new Vec3d(0.0, 9.0, 0.0), 4.0, 6.0, 4.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.RIGHT_LEG, yaw, heightScale, rightLeg,
                                new Vec3d(0.0, 6.0, 0.0), 4.0, 12.0, 4.0),
                        partBox(entity, HumanoidHurtboxLibrary.Part.LEFT_LEG, yaw, heightScale, leftLeg,
                                new Vec3d(0.0, 6.0, 0.0), 4.0, 12.0, 4.0)
                )
        ));
    }

    public static Optional<List<HumanoidHurtboxLibrary.PartBox>> get(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return Optional.empty();
        }

        if (entity == client.player) {
            return Optional.empty();
        }

        return getCachedModelPartHurtboxes(entity);
    }

    public static void updateGenericHead(int entityId, Vec3d min, Vec3d max) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null
                || !(client.world.getEntityById(entityId) instanceof LivingEntity entity)
                || HumanoidHurtboxLibrary.isHumanoidTarget(entity)
                || !shouldTrack(entity)) {
            return;
        }

        Vec3d size = max.subtract(min);
        if (size.x <= 0.001 || size.y <= 0.001 || size.z <= 0.001) {
            return;
        }

        AnimatedAttackHitboxLibrary.OrientedBox head = new AnimatedAttackHitboxLibrary.OrientedBox(
                min.add(max).multiply(0.5),
                size.multiply(0.5),
                new Vec3d(1.0, 0.0, 0.0),
                new Vec3d(0.0, 1.0, 0.0),
                new Vec3d(0.0, 0.0, 1.0)
        );
        List<HumanoidHurtboxLibrary.PartBox> boxes = new java.util.ArrayList<>(
                HumanoidHurtboxLibrary.getGenericHurtboxes(entity)
        );
        boxes.removeIf(box -> box.part() == HumanoidHurtboxLibrary.Part.HEAD);
        boxes.add(new HumanoidHurtboxLibrary.PartBox(HumanoidHurtboxLibrary.Part.HEAD, head));
        CACHE.put(entityId, new Entry(client.world.getTime(), List.copyOf(boxes)));
    }

    public static List<HumanoidHurtboxLibrary.PartBox> simulateLocalPlayer(LivingEntity entity) {
        if (!CombatAnimationClient.hasLocalPlayerCombatAnimation()) {
            return getCachedModelPartHurtboxes(entity)
                    .orElseGet(() -> HumanoidHurtboxLibrary.getHurtboxes(entity));
        }

        LocalAnimation animation = currentLocalAnimation();
        SimulatedPart body = SimulatedPart.defaults(0.0F, 0.0F, 0.0F);
        SimulatedPart head = SimulatedPart.defaults(0.0F, 0.0F, 0.0F);
        SimulatedPart rightArm = SimulatedPart.defaults(-5.0F, 2.0F, 0.0F);
        SimulatedPart leftArm = SimulatedPart.defaults(5.0F, 2.0F, 0.0F);
        SimulatedPart rightLeg = SimulatedPart.defaults(-1.9F, 12.0F, 0.0F);
        SimulatedPart leftLeg = SimulatedPart.defaults(1.9F, 12.0F, 0.0F);

        GeckoLikeAnimationLibrary.BoneTransform bodyTransform = sampleBone(animation, "body");
        applyRoot(body, bodyTransform, animation.weight());
        applyChild(head, sampleBone(animation, "head"), animation.weight(), bodyTransform);
        applyChild(rightArm, sampleBone(animation, "rightArm"), animation.weight(), bodyTransform);
        applyChild(leftArm, sampleBone(animation, "leftArm"), animation.weight(), bodyTransform);
        applyChild(rightLeg, sampleBone(animation, "rightLeg"), animation.weight(), bodyTransform);
        applyChild(leftLeg, sampleBone(animation, "leftLeg"), animation.weight(), bodyTransform);

        float yaw = entity.getBodyYaw();
        double heightScale = Math.max(0.5, entity.getHeight() / CLASSIC_MODEL_HEIGHT_BLOCKS);
        return List.of(
                partBox(entity, HumanoidHurtboxLibrary.Part.HEAD, yaw, heightScale, head,
                        new Vec3d(0.0, -4.0, 0.0), 8.0, 8.0, 8.0),
                partBox(entity, HumanoidHurtboxLibrary.Part.BODY, yaw, heightScale, body,
                        new Vec3d(0.0, 6.0, 0.0), 8.0, 12.0, 4.0),
                partBox(entity, HumanoidHurtboxLibrary.Part.SHOULDERS, yaw, heightScale, rightArm,
                        new Vec3d(0.0, 3.0, 0.0), 4.0, 6.0, 4.0),
                partBox(entity, HumanoidHurtboxLibrary.Part.SHOULDERS, yaw, heightScale, leftArm,
                        new Vec3d(0.0, 3.0, 0.0), 4.0, 6.0, 4.0),
                partBox(entity, HumanoidHurtboxLibrary.Part.RIGHT_ARM, yaw, heightScale, rightArm,
                        new Vec3d(0.0, 9.0, 0.0), 4.0, 6.0, 4.0),
                partBox(entity, HumanoidHurtboxLibrary.Part.LEFT_ARM, yaw, heightScale, leftArm,
                        new Vec3d(0.0, 9.0, 0.0), 4.0, 6.0, 4.0),
                partBox(entity, HumanoidHurtboxLibrary.Part.RIGHT_LEG, yaw, heightScale, rightLeg,
                        new Vec3d(0.0, 6.0, 0.0), 4.0, 12.0, 4.0),
                partBox(entity, HumanoidHurtboxLibrary.Part.LEFT_LEG, yaw, heightScale, leftLeg,
                        new Vec3d(0.0, 6.0, 0.0), 4.0, 12.0, 4.0)
        );
    }

    private static Optional<List<HumanoidHurtboxLibrary.PartBox>> getCachedModelPartHurtboxes(
            LivingEntity entity
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return Optional.empty();
        }

        Entry entry = CACHE.get(entity.getId());
        if (entry == null || client.world.getTime() - entry.worldTime() > 2L) {
            return Optional.empty();
        }

        return Optional.of(entry.boxes());
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
                        || !(client.world.getEntityById(entry.getKey()) instanceof LivingEntity living)
                        || !shouldTrack(living)
        );
    }

    private static boolean shouldTrack(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || entity == client.player) {
            return true;
        }

        return entity.squaredDistanceTo(client.player) <= TRACK_DISTANCE_SQUARED;
    }

    private static HumanoidHurtboxLibrary.PartBox partBox(
            LivingEntity entity,
            HumanoidHurtboxLibrary.Part part,
            float yaw,
            double heightScale,
            ModelPart modelPart,
            Vec3d centerOffset,
            double widthUnits,
            double heightUnits,
            double depthUnits
    ) {
        Quaternionf rotation = new Quaternionf().rotateZYX(
                modelPart.roll,
                modelPart.yaw,
                modelPart.pitch
        );
        Vector3f centerModel = rotation.transform(new Vector3f(
                (float) centerOffset.x,
                (float) centerOffset.y,
                (float) centerOffset.z
        ));
        centerModel.add(modelPart.originX, modelPart.originY, modelPart.originZ);

        Vec3d center = entity.getPos().add(modelPointToWorldOffset(centerModel, yaw, heightScale));
        Vec3d axisX = modelVectorToWorld(rotation.transform(new Vector3f(1.0F, 0.0F, 0.0F)), yaw, heightScale)
                .normalize();
        Vec3d axisY = modelVectorToWorld(rotation.transform(new Vector3f(0.0F, 1.0F, 0.0F)), yaw, heightScale)
                .normalize();
        Vec3d axisZ = modelVectorToWorld(rotation.transform(new Vector3f(0.0F, 0.0F, 1.0F)), yaw, heightScale)
                .normalize();

        return new HumanoidHurtboxLibrary.PartBox(
                part,
                new AnimatedAttackHitboxLibrary.OrientedBox(
                        center,
                        new Vec3d(
                                widthUnits * MODEL_UNIT_TO_BLOCK * 0.5,
                                heightUnits * MODEL_UNIT_TO_BLOCK * 0.5 * heightScale,
                                depthUnits * MODEL_UNIT_TO_BLOCK * 0.5
                        ),
                        axisX,
                        axisY,
                        axisZ
                )
        );
    }

    private static HumanoidHurtboxLibrary.PartBox partBox(
            LivingEntity entity,
            HumanoidHurtboxLibrary.Part part,
            float yaw,
            double heightScale,
            SimulatedPart modelPart,
            Vec3d centerOffset,
            double widthUnits,
            double heightUnits,
            double depthUnits
    ) {
        Quaternionf rotation = new Quaternionf().rotateZYX(
                modelPart.roll,
                modelPart.yaw,
                modelPart.pitch
        );
        Vector3f centerModel = rotation.transform(new Vector3f(
                (float) centerOffset.x,
                (float) centerOffset.y,
                (float) centerOffset.z
        ));
        centerModel.add(modelPart.positionX, modelPart.positionY, modelPart.positionZ);

        Vec3d center = entity.getPos().add(modelPointToWorldOffset(centerModel, yaw, heightScale));
        Vec3d axisX = modelVectorToWorld(rotation.transform(new Vector3f(1.0F, 0.0F, 0.0F)), yaw, heightScale)
                .normalize();
        Vec3d axisY = modelVectorToWorld(rotation.transform(new Vector3f(0.0F, 1.0F, 0.0F)), yaw, heightScale)
                .normalize();
        Vec3d axisZ = modelVectorToWorld(rotation.transform(new Vector3f(0.0F, 0.0F, 1.0F)), yaw, heightScale)
                .normalize();

        return new HumanoidHurtboxLibrary.PartBox(
                part,
                new AnimatedAttackHitboxLibrary.OrientedBox(
                        center,
                        new Vec3d(
                                widthUnits * MODEL_UNIT_TO_BLOCK * 0.5,
                                heightUnits * MODEL_UNIT_TO_BLOCK * 0.5 * heightScale,
                                depthUnits * MODEL_UNIT_TO_BLOCK * 0.5
                        ),
                        axisX,
                        axisY,
                        axisZ
                )
        );
    }

    private static LocalAnimation currentLocalAnimation() {
        if (CombatClientState.attacking && CombatClientState.lastAttackDirection != null) {
            return new LocalAnimation(
                    GeckoLikeAnimationLibrary.Kind.ATTACK,
                    CombatClientState.lastAttackDirection,
                    CombatClientState.currentAttackElapsedSeconds(),
                    CombatClientState.currentAttackAnimationName,
                    1.0F
            );
        }

        MinecraftClient client = MinecraftClient.getInstance();
        String stanceAnimationName = client.player == null
                ? null
                : CombatWeaponUtil.stanceAnimationName(client.player, CombatClientState.currentDirection);
        return new LocalAnimation(
                GeckoLikeAnimationLibrary.Kind.STANCE,
                CombatClientState.currentDirection,
                0.0F,
                stanceAnimationName == null || stanceAnimationName.isBlank() ? null : stanceAnimationName,
                1.0F
        );
    }

    private static GeckoLikeAnimationLibrary.BoneTransform sampleBone(
            LocalAnimation animation,
            String boneName
    ) {
        if (animation.customAnimationName() != null) {
            return GeckoLikeAnimationLibrary.sampleNamedBone(
                    animation.customAnimationName(),
                    boneName,
                    animation.elapsedSeconds()
            );
        }

        return GeckoLikeAnimationLibrary.sampleBone(
                animation.kind(),
                animation.direction(),
                boneName,
                animation.elapsedSeconds()
        );
    }

    private static void applyRoot(
            SimulatedPart part,
            GeckoLikeAnimationLibrary.BoneTransform own,
            float weight
    ) {
        own.rotation().ifPresent(pose -> {
            part.pitch = lerp(part.pitch, part.defaultPitch + pose.x() * DEG_TO_RAD, weight);
            part.yaw = lerp(part.yaw, part.defaultYaw + pose.y() * DEG_TO_RAD, weight);
            part.roll = lerp(part.roll, part.defaultRoll + pose.z() * DEG_TO_RAD, weight);
        });

        own.position().ifPresent(pose -> {
            part.positionX = lerp(part.positionX, part.defaultX + pose.x(), weight);
            part.positionY = lerp(part.positionY, part.defaultY + pose.y(), weight);
            part.positionZ = lerp(part.positionZ, part.defaultZ + pose.z(), weight);
        });
    }

    private static void applyChild(
            SimulatedPart part,
            GeckoLikeAnimationLibrary.BoneTransform own,
            float weight,
            GeckoLikeAnimationLibrary.BoneTransform parent
    ) {
        applyRoot(part, own, weight);
        applyParentTransform(part, parent, weight);
    }

    private static void applyParentTransform(
            SimulatedPart part,
            GeckoLikeAnimationLibrary.BoneTransform parent,
            float weight
    ) {
        if (parent.rotation().isEmpty() && parent.position().isEmpty()) {
            return;
        }

        Vector3f baseOffset = new Vector3f(
                part.positionX - part.defaultX,
                part.positionY - part.defaultY,
                part.positionZ - part.defaultZ
        );

        parent.position().ifPresent(pose -> baseOffset.add(pose.x(), pose.y(), pose.z()));

        if (parent.rotation().isPresent()) {
            GeckoLikeAnimationLibrary.BonePose pose = parent.rotation().get();
            Quaternionf parentRotation = new Quaternionf()
                    .rotateZYX(
                            pose.z() * DEG_TO_RAD,
                            pose.y() * DEG_TO_RAD,
                            pose.x() * DEG_TO_RAD
                    );

            Vector3f bindOffset = new Vector3f(
                    part.defaultX,
                    part.defaultY,
                    part.defaultZ
            );
            Vector3f rotatedOffset = parentRotation.transform(new Vector3f(bindOffset));
            rotatedOffset.sub(bindOffset);
            baseOffset.add(rotatedOffset);

            part.pitch = lerp(part.pitch, part.pitch + pose.x() * DEG_TO_RAD, weight);
            part.yaw = lerp(part.yaw, part.yaw + pose.y() * DEG_TO_RAD, weight);
            part.roll = lerp(part.roll, part.roll + pose.z() * DEG_TO_RAD, weight);
        }

        part.positionX = lerp(part.positionX, part.defaultX + baseOffset.x, weight);
        part.positionY = lerp(part.positionY, part.defaultY + baseOffset.y, weight);
        part.positionZ = lerp(part.positionZ, part.defaultZ + baseOffset.z, weight);
    }

    private static float lerp(float from, float to, float progress) {
        progress = Math.max(0.0F, Math.min(1.0F, progress));
        return from + (to - from) * progress;
    }

    private static Vec3d modelPointToWorldOffset(
            Vector3f point,
            float yaw,
            double heightScale
    ) {
        return AnimatedAttackHitboxLibrary.modelToWorld(
                new Vec3d(
                        point.x * MODEL_UNIT_TO_BLOCK,
                        (MODEL_ORIGIN_Y - point.y) * MODEL_UNIT_TO_BLOCK * heightScale,
                        point.z * MODEL_UNIT_TO_BLOCK
                ),
                yaw
        );
    }

    private static Vec3d modelVectorToWorld(
            Vector3f vector,
            float yaw,
            double heightScale
    ) {
        return AnimatedAttackHitboxLibrary.modelToWorld(
                new Vec3d(
                        vector.x * MODEL_UNIT_TO_BLOCK,
                        -vector.y * MODEL_UNIT_TO_BLOCK * heightScale,
                        vector.z * MODEL_UNIT_TO_BLOCK
                ),
                yaw
        );
    }

    private record Entry(long worldTime, List<HumanoidHurtboxLibrary.PartBox> boxes) {
    }

    private record LocalAnimation(
            GeckoLikeAnimationLibrary.Kind kind,
            CombatDirection direction,
            float elapsedSeconds,
            String customAnimationName,
            float weight
    ) {
    }

    private static class SimulatedPart {
        private final float defaultX;
        private final float defaultY;
        private final float defaultZ;
        private final float defaultPitch;
        private final float defaultYaw;
        private final float defaultRoll;
        private float positionX;
        private float positionY;
        private float positionZ;
        private float pitch;
        private float yaw;
        private float roll;

        private SimulatedPart(float originX, float originY, float originZ) {
            this.defaultX = originX;
            this.defaultY = originY;
            this.defaultZ = originZ;
            this.defaultPitch = 0.0F;
            this.defaultYaw = 0.0F;
            this.defaultRoll = 0.0F;
            this.positionX = originX;
            this.positionY = originY;
            this.positionZ = originZ;
        }

        private static SimulatedPart defaults(float originX, float originY, float originZ) {
            return new SimulatedPart(originX, originY, originZ);
        }
    }
}
