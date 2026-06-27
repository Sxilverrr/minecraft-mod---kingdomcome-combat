package com.kingdomcomecombat.collision;

import com.kingdomcomecombat.compat.GuardVillagersCompat;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HumanoidHurtboxLibrary {
    private static final double MODEL_UNIT_TO_BLOCK = 1.0 / 16.0;
    private static final double EXTRA_MARGIN = 1.0 * MODEL_UNIT_TO_BLOCK;
    private static final double CLASSIC_MODEL_HEIGHT_BLOCKS = 32.0 * MODEL_UNIT_TO_BLOCK;

    private HumanoidHurtboxLibrary() {
    }

    public static List<PartBox> getHurtboxes(LivingEntity entity) {
        if (!isHumanoidTarget(entity)) {
            return getGenericHurtboxes(entity);
        }

        float yaw = entity.getBodyYaw();
        double heightScale = modelHeightScale(entity);
        List<PartBox> boxes = new ArrayList<>();

        boxes.add(createExact(entity, Part.HEAD, yaw, new Vec3d(0.0, 28.0, 0.0), 8.0, 8.0, 8.0, heightScale));
        boxes.add(createExact(entity, Part.SHOULDERS, yaw, new Vec3d(-6.0, 21.0, 0.0), 4.0, 6.0, 4.0, heightScale));
        boxes.add(createExact(entity, Part.SHOULDERS, yaw, new Vec3d(6.0, 21.0, 0.0), 4.0, 6.0, 4.0, heightScale));
        boxes.add(createExact(entity, Part.BODY, yaw, new Vec3d(0.0, 18.0, 0.0), 8.0, 12.0, 4.0, heightScale));
        boxes.add(createExact(entity, Part.RIGHT_ARM, yaw, new Vec3d(-6.0, 15.0, 0.0), 4.0, 6.0, 4.0, heightScale));
        boxes.add(createExact(entity, Part.LEFT_ARM, yaw, new Vec3d(6.0, 15.0, 0.0), 4.0, 6.0, 4.0, heightScale));
        boxes.add(createExact(entity, Part.RIGHT_LEG, yaw, new Vec3d(-2.0, 6.0, 0.0), 4.0, 12.0, 4.0, heightScale));
        boxes.add(createExact(entity, Part.LEFT_LEG, yaw, new Vec3d(2.0, 6.0, 0.0), 4.0, 12.0, 4.0, heightScale));

        return boxes;
    }

    public static Optional<HitResult> getHitResult(
            AnimatedAttackHitboxLibrary.OrientedBox attackBox,
            LivingEntity entity
    ) {
        return getHitResult(attackBox, entity, getHurtboxes(entity));
    }

    public static Optional<HitResult> getHitResult(
            AnimatedAttackHitboxLibrary.OrientedBox attackBox,
            LivingEntity entity,
            List<PartBox> hurtboxes
    ) {
        List<PartBox> hits = hurtboxes.stream()
                .filter(hurtbox -> attackBox.intersects(hurtbox.box()))
                .toList();
        if (hits.isEmpty()) {
            return Optional.empty();
        }

        PartBox selected = selectPreferredHit(entity, hits, attackBox);
        Vec3d position = closestPointOnBox(selected.box(), attackBox.center());
        return Optional.of(new HitResult(
                selected.part(),
                detailedPartForHit(entity, selected.part(), selected.box(), position),
                position,
                selected.box(),
                attackBox
        ));
    }

    public static Optional<HitResult> getAnimatedHitResult(
            AnimatedAttackHitboxLibrary.OrientedBox attackBox,
            LivingEntity entity,
            HumanoidAnimationPoseLibrary.Kind kind,
            com.kingdomcomecombat.combat.CombatDirection direction,
            float elapsed
    ) {
        return getHitResult(
                attackBox,
                entity,
                getAnimatedHurtboxes(entity, kind, direction, elapsed)
        );
    }

    public static Optional<HitResult> getStaticHitResult(
            AnimatedAttackHitboxLibrary.OrientedBox attackBox,
            LivingEntity entity
    ) {
        return getHitResult(attackBox, entity, getHurtboxes(entity));
    }

    public static HitResult reportedHitResult(LivingEntity entity, Part part) {
        return reportedHitResult(entity, part, null);
    }

    public static HitResult reportedHitResult(LivingEntity entity, Part part, Vec3d reportedPosition) {
        AnimatedAttackHitboxLibrary.OrientedBox box = getHurtboxes(entity).stream()
                .filter(hurtbox -> hurtbox.part() == part)
                .findFirst()
                .map(PartBox::box)
                .orElseGet(() -> new AnimatedAttackHitboxLibrary.OrientedBox(
                        entity.getPos().add(0.0, entity.getHeight() * 0.5, 0.0),
                        new Vec3d(
                                Math.max(0.12, entity.getWidth() * 0.5),
                                Math.max(0.12, entity.getHeight() * 0.5),
                                Math.max(0.12, entity.getWidth() * 0.5)
                        ),
                        new Vec3d(1.0, 0.0, 0.0),
                        new Vec3d(0.0, 1.0, 0.0),
                        new Vec3d(0.0, 0.0, 1.0)
                ));

        Vec3d position = reportedPosition == null
                ? box.center()
                : closestPointOnBox(box, reportedPosition);
        return new HitResult(
                part,
                detailedPartForHit(entity, part, box, position),
                position,
                box,
                null
        );
    }

    public static Optional<Part> getHitPart(
            AnimatedAttackHitboxLibrary.OrientedBox attackBox,
            LivingEntity entity
    ) {
        return getHitResult(attackBox, entity).map(HitResult::part);
    }

    public static boolean intersectsAny(
            AnimatedAttackHitboxLibrary.OrientedBox attackBox,
            LivingEntity entity
    ) {
        return getHitPart(attackBox, entity).isPresent();
    }

    public static boolean isHumanoidTarget(LivingEntity entity) {
        if (GuardVillagersCompat.isGuard(entity)) {
            return true;
        }
        if (entity instanceof PlayerEntity) {
            return true;
        }

        EntityType<?> type = entity.getType();
        return type == EntityType.SKELETON
                || type == EntityType.STRAY
                || type == EntityType.WITHER_SKELETON
                || type == EntityType.ZOMBIE
                || type == EntityType.HUSK
                || type == EntityType.DROWNED
                || type == EntityType.ZOMBIFIED_PIGLIN
                || type == EntityType.PIGLIN
                || type == EntityType.PIGLIN_BRUTE
                || type == EntityType.PILLAGER
                || type == EntityType.VINDICATOR
                || type == EntityType.EVOKER
                || type == EntityType.ILLUSIONER
                || type == EntityType.WITCH
                || type == EntityType.VILLAGER
                || type == EntityType.WANDERING_TRADER
                || type == EntityType.ZOMBIE_VILLAGER
                || type == EntityType.BOGGED
                || type == EntityType.GIANT
                || type == EntityType.ARMOR_STAND;
    }

    public static List<PartBox> getGenericHurtboxes(LivingEntity entity) {
        double width = Math.max(0.18, entity.getWidth());
        double height = Math.max(0.20, entity.getHeight());
        float yaw = entity.getBodyYaw();
        Vec3d axisX = AnimatedAttackHitboxLibrary.modelToWorld(
                new Vec3d(1.0, 0.0, 0.0),
                yaw
        ).normalize();
        Vec3d axisY = new Vec3d(0.0, 1.0, 0.0);
        Vec3d axisZ = AnimatedAttackHitboxLibrary.modelToWorld(
                new Vec3d(0.0, 0.0, 1.0),
                yaw
        ).normalize();
        Vec3d center = entity.getPos().add(0.0, height * 0.5, 0.0);
        AnimatedAttackHitboxLibrary.OrientedBox body = new AnimatedAttackHitboxLibrary.OrientedBox(
                center,
                new Vec3d(width * 0.5, height * 0.5, width * 0.5),
                axisX,
                axisY,
                axisZ
        );

        GenericHeadSpec headSpec = genericHeadSpec(entity.getType());
        double headWidth = clamp(width * headSpec.widthScale(), 0.16, Math.max(0.18, width * 0.92));
        double headHeight = clamp(height * headSpec.heightScale(), 0.14, Math.max(0.16, height * 0.42));
        double headDepth = clamp(width * headSpec.depthScale(), 0.16, Math.max(0.18, width * 0.92));
        Vec3d forward = axisZ.multiply(-1.0);
        Vec3d headCenter = entity.getPos()
                .add(forward.multiply(width * headSpec.forwardOffsetScale()))
                .add(0.0, height * headSpec.heightCenterScale(), 0.0);
        AnimatedAttackHitboxLibrary.OrientedBox head = new AnimatedAttackHitboxLibrary.OrientedBox(
                headCenter,
                new Vec3d(headWidth * 0.5, headHeight * 0.5, headDepth * 0.5),
                axisX,
                axisY,
                axisZ
        );

        return List.of(new PartBox(Part.BODY, body), new PartBox(Part.HEAD, head));
    }

    private static GenericHeadSpec genericHeadSpec(EntityType<?> type) {
        if (type == EntityType.CREEPER) {
            return new GenericHeadSpec(0.0, 0.78, 0.72, 0.32, 0.72);
        }

        if (type == EntityType.SPIDER || type == EntityType.CAVE_SPIDER) {
            return new GenericHeadSpec(0.42, 0.42, 0.46, 0.32, 0.38);
        }

        if (type == EntityType.CHICKEN || type == EntityType.PARROT) {
            return new GenericHeadSpec(0.24, 0.72, 0.42, 0.30, 0.34);
        }

        if (type == EntityType.WOLF
                || type == EntityType.CAT
                || type == EntityType.FOX
                || type == EntityType.OCELOT
                || type == EntityType.RABBIT) {
            return new GenericHeadSpec(0.44, 0.58, 0.46, 0.30, 0.40);
        }

        if (type == EntityType.HORSE
                || type == EntityType.SKELETON_HORSE
                || type == EntityType.ZOMBIE_HORSE
                || type == EntityType.DONKEY
                || type == EntityType.MULE
                || type == EntityType.CAMEL
                || type == EntityType.LLAMA
                || type == EntityType.TRADER_LLAMA) {
            return new GenericHeadSpec(0.46, 0.72, 0.42, 0.30, 0.48);
        }

        if (type == EntityType.GOAT
                || type == EntityType.SHEEP
                || type == EntityType.COW
                || type == EntityType.MOOSHROOM
                || type == EntityType.PIG
                || type == EntityType.POLAR_BEAR
                || type == EntityType.PANDA) {
            return new GenericHeadSpec(0.42, 0.68, 0.46, 0.30, 0.44);
        }

        return new GenericHeadSpec(0.18, 0.76, 0.58, 0.30, 0.52);
    }

    public static List<PartBox> getAnimatedHurtboxes(
            LivingEntity entity,
            HumanoidAnimationPoseLibrary.Kind kind,
            com.kingdomcomecombat.combat.CombatDirection direction,
            float elapsed
    ) {
        double heightScale = modelHeightScale(entity);
        float yaw = entity.getBodyYaw();
        HumanoidAnimationPoseLibrary.BonePose bodyPosition = samplePosition("body", kind, direction, elapsed);
        HumanoidAnimationPoseLibrary.BonePose bodyRotation = sampleRotation("body", kind, direction, elapsed);

        List<PartBox> boxes = new ArrayList<>();
        boxes.add(createAnimatedWithPivot(
                entity,
                Part.HEAD,
                yaw,
                new Vec3d(0.0, -4.0, 0.0),
                new Vec3d(0.0, 24.0, 0.0),
                8.0,
                8.0,
                8.0,
                heightScale,
                bodyPosition,
                bodyRotation,
                samplePosition("head", kind, direction, elapsed),
                sampleRotation("head", kind, direction, elapsed),
                true,
                0.0
        ));
        boxes.add(createAnimatedWithPivot(
                entity,
                Part.SHOULDERS,
                yaw,
                new Vec3d(0.0, 3.0, 0.0),
                new Vec3d(-5.0, 22.0, 0.0),
                4.0,
                6.0,
                4.0,
                heightScale,
                bodyPosition,
                bodyRotation,
                samplePosition("rightArm", kind, direction, elapsed),
                sampleRotation("rightArm", kind, direction, elapsed),
                true
        ));
        boxes.add(createAnimatedWithPivot(
                entity,
                Part.SHOULDERS,
                yaw,
                new Vec3d(0.0, 3.0, 0.0),
                new Vec3d(5.0, 22.0, 0.0),
                4.0,
                6.0,
                4.0,
                heightScale,
                bodyPosition,
                bodyRotation,
                samplePosition("leftArm", kind, direction, elapsed),
                sampleRotation("leftArm", kind, direction, elapsed),
                true
        ));
        boxes.add(createAnimatedWithPivot(
                entity,
                Part.BODY,
                yaw,
                new Vec3d(0.0, -6.0, 0.0),
                new Vec3d(0.0, 24.0, 0.0),
                8.0,
                12.0,
                4.0,
                heightScale,
                HumanoidAnimationPoseLibrary.BonePose.ZERO,
                HumanoidAnimationPoseLibrary.BonePose.ZERO,
                bodyPosition,
                bodyRotation,
                false,
                0.0
        ));
        boxes.add(createAnimatedWithPivot(
                entity,
                Part.RIGHT_ARM,
                yaw,
                new Vec3d(0.0, 9.0, 0.0),
                new Vec3d(-5.0, 22.0, 0.0),
                4.0,
                6.0,
                4.0,
                heightScale,
                bodyPosition,
                bodyRotation,
                samplePosition("rightArm", kind, direction, elapsed),
                sampleRotation("rightArm", kind, direction, elapsed),
                true
        ));
        boxes.add(createAnimatedWithPivot(
                entity,
                Part.LEFT_ARM,
                yaw,
                new Vec3d(0.0, 9.0, 0.0),
                new Vec3d(5.0, 22.0, 0.0),
                4.0,
                6.0,
                4.0,
                heightScale,
                bodyPosition,
                bodyRotation,
                samplePosition("leftArm", kind, direction, elapsed),
                sampleRotation("leftArm", kind, direction, elapsed),
                true
        ));
        boxes.add(createAnimatedWithPivot(
                entity,
                Part.RIGHT_LEG,
                yaw,
                new Vec3d(0.0, 6.0, 0.0),
                new Vec3d(-1.9, 12.0, 0.0),
                4.0,
                12.0,
                4.0,
                heightScale,
                bodyPosition,
                bodyRotation,
                HumanoidAnimationPoseLibrary.BonePose.ZERO,
                HumanoidAnimationPoseLibrary.BonePose.ZERO,
                true
        ));
        boxes.add(createAnimatedWithPivot(
                entity,
                Part.LEFT_LEG,
                yaw,
                new Vec3d(0.0, 6.0, 0.0),
                new Vec3d(1.9, 12.0, 0.0),
                4.0,
                12.0,
                4.0,
                heightScale,
                bodyPosition,
                bodyRotation,
                HumanoidAnimationPoseLibrary.BonePose.ZERO,
                HumanoidAnimationPoseLibrary.BonePose.ZERO,
                true
        ));

        return boxes;
    }

    private static HumanoidAnimationPoseLibrary.BonePose samplePosition(
            String boneName,
            HumanoidAnimationPoseLibrary.Kind kind,
            com.kingdomcomecombat.combat.CombatDirection direction,
            float elapsed
    ) {
        return HumanoidAnimationPoseLibrary.samplePosition(
                kind,
                direction,
                boneName,
                elapsed
        ).orElse(HumanoidAnimationPoseLibrary.BonePose.ZERO);
    }

    private static HumanoidAnimationPoseLibrary.BonePose sampleRotation(
            String boneName,
            HumanoidAnimationPoseLibrary.Kind kind,
            com.kingdomcomecombat.combat.CombatDirection direction,
            float elapsed
    ) {
        return HumanoidAnimationPoseLibrary.sampleRotation(
                kind,
                direction,
                boneName,
                elapsed
        ).orElse(HumanoidAnimationPoseLibrary.BonePose.ZERO);
    }

    private static PartBox create(
            LivingEntity entity,
            Part part,
            float yaw,
            Vec3d centerModelUnits,
            double widthUnits,
            double heightUnits,
            double depthUnits,
            double heightScale
    ) {
        return create(entity, part, yaw, centerModelUnits, widthUnits, heightUnits, depthUnits, heightScale, EXTRA_MARGIN);
    }

    private static PartBox createExact(
            LivingEntity entity,
            Part part,
            float yaw,
            Vec3d centerModelUnits,
            double widthUnits,
            double heightUnits,
            double depthUnits,
            double heightScale
    ) {
        return create(entity, part, yaw, centerModelUnits, widthUnits, heightUnits, depthUnits, heightScale, 0.0);
    }

    private static PartBox create(
            LivingEntity entity,
            Part part,
            float yaw,
            Vec3d centerModelUnits,
            double widthUnits,
            double heightUnits,
            double depthUnits,
            double heightScale,
            double extraMargin
    ) {
        Vec3d localCenter = new Vec3d(
                centerModelUnits.x * MODEL_UNIT_TO_BLOCK,
                centerModelUnits.y * MODEL_UNIT_TO_BLOCK * heightScale,
                centerModelUnits.z * MODEL_UNIT_TO_BLOCK
        );

        Vec3d center = entity.getPos().add(
                AnimatedAttackHitboxLibrary.modelToWorld(localCenter, yaw)
        );

        Vec3d axisX = AnimatedAttackHitboxLibrary.modelToWorld(
                new Vec3d(1.0, 0.0, 0.0),
                yaw
        ).normalize();
        Vec3d axisY = new Vec3d(0.0, 1.0, 0.0);
        Vec3d axisZ = AnimatedAttackHitboxLibrary.modelToWorld(
                new Vec3d(0.0, 0.0, 1.0),
                yaw
        ).normalize();

        Vec3d halfExtents = new Vec3d(
                widthUnits * MODEL_UNIT_TO_BLOCK * 0.5 + extraMargin,
                heightUnits * MODEL_UNIT_TO_BLOCK * 0.5 * heightScale + extraMargin,
                depthUnits * MODEL_UNIT_TO_BLOCK * 0.5 + extraMargin
        );

        return new PartBox(
                part,
                new AnimatedAttackHitboxLibrary.OrientedBox(
                        center,
                        halfExtents,
                        axisX,
                        axisY,
                        axisZ
                )
        );
    }

    private static PartBox createSized(
            LivingEntity entity,
            Part part,
            float yaw,
            double centerY,
            double halfWidth,
            double halfHeight,
            double halfDepth
    ) {
        Vec3d center = entity.getPos().add(0.0, centerY, 0.0);
        Vec3d axisX = AnimatedAttackHitboxLibrary.modelToWorld(
                new Vec3d(1.0, 0.0, 0.0),
                yaw
        ).normalize();
        Vec3d axisY = new Vec3d(0.0, 1.0, 0.0);
        Vec3d axisZ = AnimatedAttackHitboxLibrary.modelToWorld(
                new Vec3d(0.0, 0.0, 1.0),
                yaw
        ).normalize();

        return new PartBox(
                part,
                new AnimatedAttackHitboxLibrary.OrientedBox(
                        center,
                        new Vec3d(halfWidth, halfHeight, halfDepth),
                        axisX,
                        axisY,
                        axisZ
                )
        );
    }

    private static Vec3d closestPointOnBox(
            AnimatedAttackHitboxLibrary.OrientedBox box,
            Vec3d point
    ) {
        Vec3d delta = point.subtract(box.center());
        Vec3d result = box.center();
        result = result.add(box.axisX().multiply(clamp(
                delta.dotProduct(box.axisX()),
                -box.halfExtents().x,
                box.halfExtents().x
        )));
        result = result.add(box.axisY().multiply(clamp(
                delta.dotProduct(box.axisY()),
                -box.halfExtents().y,
                box.halfExtents().y
        )));
        result = result.add(box.axisZ().multiply(clamp(
                delta.dotProduct(box.axisZ()),
                -box.halfExtents().z,
                box.halfExtents().z
        )));

        return result;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static PartBox selectPreferredHit(
            LivingEntity entity,
            List<PartBox> hits,
            AnimatedAttackHitboxLibrary.OrientedBox attackBox
    ) {
        if (!isHumanoidTarget(entity)) {
            Optional<PartBox> head = hits.stream()
                    .filter(hit -> hit.part() == Part.HEAD)
                    .min(Comparator.comparingDouble(hit ->
                            hit.box().center().squaredDistanceTo(attackBox.center())
                    ));
            if (head.isPresent()) {
                return head.get();
            }
        }

        boolean hitShoulder = hits.stream().anyMatch(hit -> hit.part() == Part.SHOULDERS);
        boolean hitArm = hits.stream().anyMatch(hit ->
                hit.part() == Part.LEFT_ARM || hit.part() == Part.RIGHT_ARM
        );
        if (hitShoulder && hitArm) {
            return hits.stream()
                    .filter(hit -> hit.part() == Part.SHOULDERS)
                    .min(Comparator.comparingDouble(hit ->
                            hit.box().center().squaredDistanceTo(attackBox.center())
                    ))
                    .orElse(hits.getFirst());
        }

        return hits.stream()
                .min(Comparator.comparingDouble(hit ->
                        hit.box().center().squaredDistanceTo(attackBox.center())
                ))
                .orElse(hits.getFirst());
    }

    private static double modelHeightScale(LivingEntity entity) {
        return Math.max(0.5, entity.getHeight() / CLASSIC_MODEL_HEIGHT_BLOCKS);
    }

    private static String fallbackDetailedPart(Part part) {
        return switch (part) {
            case HEAD -> "side_head";
            case SHOULDERS -> "shoulder";
            case BODY -> "chest";
            case LOWER, LEFT_LEG, RIGHT_LEG -> "knee";
            case LEFT_ARM, RIGHT_ARM -> "arm";
        };
    }

    private static String fallbackDetailedPart(LivingEntity entity, Part part) {
        if (!isHumanoidTarget(entity)) {
            return part == Part.HEAD ? "crown" : "abdomen";
        }

        return fallbackDetailedPart(part);
    }

    private static String detailedPartForHit(
            LivingEntity entity,
            Part part,
            AnimatedAttackHitboxLibrary.OrientedBox box,
            Vec3d position
    ) {
        if (!isHumanoidTarget(entity) || part != Part.HEAD) {
            return fallbackDetailedPart(entity, part);
        }

        Vec3d local = position.subtract(box.center());
        double localX = normalizedAxisAmount(local, box.axisX(), box.halfExtents().x);
        double localY = normalizedAxisAmount(local, box.axisY(), box.halfExtents().y);
        double localZ = normalizedAxisAmount(local, box.axisZ(), box.halfExtents().z);

        if (localY >= 0.52) {
            return "crown";
        }

        if (localZ <= -0.28 && Math.abs(localX) <= 0.70) {
            return "face";
        }

        return "side_head";
    }

    private static double normalizedAxisAmount(Vec3d local, Vec3d axis, double halfExtent) {
        if (halfExtent <= 0.000001) {
            return 0.0;
        }

        return local.dotProduct(axis) / halfExtent;
    }

    private static PartBox createAnimated(
            LivingEntity entity,
            Part part,
            float yaw,
            Vec3d centerModelUnits,
            double widthUnits,
            double heightUnits,
            double depthUnits,
            double heightScale,
            HumanoidAnimationPoseLibrary.BonePose parentPosition,
            HumanoidAnimationPoseLibrary.BonePose parentRotation,
            HumanoidAnimationPoseLibrary.BonePose ownPosition,
            HumanoidAnimationPoseLibrary.BonePose ownRotation
    ) {
        return createAnimated(
                entity,
                part,
                yaw,
                centerModelUnits,
                widthUnits,
                heightUnits,
                depthUnits,
                heightScale,
                parentPosition,
                parentRotation,
                ownPosition,
                ownRotation,
                EXTRA_MARGIN
        );
    }

    private static PartBox createAnimatedExact(
            LivingEntity entity,
            Part part,
            float yaw,
            Vec3d centerModelUnits,
            double widthUnits,
            double heightUnits,
            double depthUnits,
            double heightScale,
            HumanoidAnimationPoseLibrary.BonePose parentPosition,
            HumanoidAnimationPoseLibrary.BonePose parentRotation,
            HumanoidAnimationPoseLibrary.BonePose ownPosition,
            HumanoidAnimationPoseLibrary.BonePose ownRotation
    ) {
        return createAnimated(
                entity,
                part,
                yaw,
                centerModelUnits,
                widthUnits,
                heightUnits,
                depthUnits,
                heightScale,
                parentPosition,
                parentRotation,
                ownPosition,
                ownRotation,
                0.0
        );
    }

    private static PartBox createAnimatedWithPivot(
            LivingEntity entity,
            Part part,
            float yaw,
            Vec3d centerModelUnits,
            Vec3d pivotModelUnits,
            double widthUnits,
            double heightUnits,
            double depthUnits,
            double heightScale,
            HumanoidAnimationPoseLibrary.BonePose parentPosition,
            HumanoidAnimationPoseLibrary.BonePose parentRotation,
            HumanoidAnimationPoseLibrary.BonePose ownPosition,
            HumanoidAnimationPoseLibrary.BonePose ownRotation,
            boolean mirrorHorizontal
    ) {
        return createAnimatedWithPivot(
                entity,
                part,
                yaw,
                centerModelUnits,
                pivotModelUnits,
                widthUnits,
                heightUnits,
                depthUnits,
                heightScale,
                parentPosition,
                parentRotation,
                ownPosition,
                ownRotation,
                mirrorHorizontal,
                EXTRA_MARGIN
        );
    }

    private static PartBox createAnimatedWithPivot(
            LivingEntity entity,
            Part part,
            float yaw,
            Vec3d centerModelUnits,
            Vec3d pivotModelUnits,
            double widthUnits,
            double heightUnits,
            double depthUnits,
            double heightScale,
            HumanoidAnimationPoseLibrary.BonePose parentPosition,
            HumanoidAnimationPoseLibrary.BonePose parentRotation,
            HumanoidAnimationPoseLibrary.BonePose ownPosition,
            HumanoidAnimationPoseLibrary.BonePose ownRotation,
            boolean mirrorHorizontal,
            double extraMargin
    ) {
        Vec3d animatedPivot = applyParentToPivot(pivotModelUnits, parentPosition, parentRotation)
                .add(ownPosition.x(), ownPosition.y(), ownPosition.z());
        Vec3d animatedCenter = animatedPivot.add(rotateLocal(
                centerModelUnits,
                ownRotation,
                mirrorHorizontal
        ));
        HumanoidAnimationPoseLibrary.BonePose rotation = parentRotation.add(ownRotation);

        return createWithRotation(
                entity,
                part,
                yaw,
                animatedCenter,
                widthUnits,
                heightUnits,
                depthUnits,
                heightScale,
                rotation,
                mirrorHorizontal,
                extraMargin
        );
    }

    private static Vec3d applyParentToPivot(
            Vec3d pivotModelUnits,
            HumanoidAnimationPoseLibrary.BonePose parentPosition,
            HumanoidAnimationPoseLibrary.BonePose parentRotation
    ) {
        Vec3d rotatedOffset = rotateLocal(pivotModelUnits, parentRotation).subtract(pivotModelUnits);
        return pivotModelUnits
                .add(parentPosition.x(), parentPosition.y(), parentPosition.z())
                .add(rotatedOffset);
    }

    private static PartBox createAnimated(
            LivingEntity entity,
            Part part,
            float yaw,
            Vec3d centerModelUnits,
            double widthUnits,
            double heightUnits,
            double depthUnits,
            double heightScale,
            HumanoidAnimationPoseLibrary.BonePose parentPosition,
            HumanoidAnimationPoseLibrary.BonePose parentRotation,
            HumanoidAnimationPoseLibrary.BonePose ownPosition,
            HumanoidAnimationPoseLibrary.BonePose ownRotation,
            double extraMargin
    ) {
        HumanoidAnimationPoseLibrary.BonePose rotation = parentRotation.add(ownRotation);
        Vec3d animatedCenter = rotateLocal(
                centerModelUnits.add(
                        ownPosition.x(),
                        ownPosition.y(),
                        ownPosition.z()
                ),
                parentRotation
        ).add(parentPosition.x(), parentPosition.y(), parentPosition.z());

        return createWithRotation(
                entity,
                part,
                yaw,
                animatedCenter,
                widthUnits,
                heightUnits,
                depthUnits,
                heightScale,
                rotation,
                false,
                extraMargin
        );
    }

    private static PartBox createWithRotation(
            LivingEntity entity,
            Part part,
            float yaw,
            Vec3d centerModelUnits,
            double widthUnits,
            double heightUnits,
            double depthUnits,
            double heightScale,
            HumanoidAnimationPoseLibrary.BonePose rotation
    ) {
        return createWithRotation(entity, part, yaw, centerModelUnits, widthUnits, heightUnits, depthUnits, heightScale, rotation, false, EXTRA_MARGIN);
    }

    private static PartBox createWithRotation(
            LivingEntity entity,
            Part part,
            float yaw,
            Vec3d centerModelUnits,
            double widthUnits,
            double heightUnits,
            double depthUnits,
            double heightScale,
            HumanoidAnimationPoseLibrary.BonePose rotation,
            boolean mirrorHorizontal,
            double extraMargin
    ) {
        Vec3d localCenter = new Vec3d(
                centerModelUnits.x * MODEL_UNIT_TO_BLOCK,
                centerModelUnits.y * MODEL_UNIT_TO_BLOCK * heightScale,
                centerModelUnits.z * MODEL_UNIT_TO_BLOCK
        );

        Vec3d center = entity.getPos().add(
                AnimatedAttackHitboxLibrary.modelToWorld(localCenter, yaw)
        );
        Vec3d axisX = AnimatedAttackHitboxLibrary.modelToWorld(
                rotateLocal(new Vec3d(1.0, 0.0, 0.0), rotation, mirrorHorizontal),
                yaw
        ).normalize();
        Vec3d axisY = AnimatedAttackHitboxLibrary.modelToWorld(
                rotateLocal(new Vec3d(0.0, 1.0, 0.0), rotation, mirrorHorizontal),
                yaw
        ).normalize();
        Vec3d axisZ = AnimatedAttackHitboxLibrary.modelToWorld(
                rotateLocal(new Vec3d(0.0, 0.0, 1.0), rotation, mirrorHorizontal),
                yaw
        ).normalize();

        Vec3d halfExtents = new Vec3d(
                widthUnits * MODEL_UNIT_TO_BLOCK * 0.5 + extraMargin,
                heightUnits * MODEL_UNIT_TO_BLOCK * 0.5 * heightScale + extraMargin,
                depthUnits * MODEL_UNIT_TO_BLOCK * 0.5 + extraMargin
        );

        return new PartBox(
                part,
                new AnimatedAttackHitboxLibrary.OrientedBox(center, halfExtents, axisX, axisY, axisZ)
        );
    }

    private static Vec3d rotateLocal(
            Vec3d vector,
            HumanoidAnimationPoseLibrary.BonePose rotationDegrees
    ) {
        return rotateLocal(vector, rotationDegrees, false);
    }

    private static Vec3d rotateLocal(
            Vec3d vector,
            HumanoidAnimationPoseLibrary.BonePose rotationDegrees,
            boolean mirrorHorizontal
    ) {
        double pitch = Math.toRadians(rotationDegrees.x());
        double yaw = Math.toRadians(rotationDegrees.y());
        double roll = Math.toRadians(rotationDegrees.z());

        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosRoll = Math.cos(roll);
        double sinRoll = Math.sin(roll);

        double rollX = vector.x * cosRoll - vector.y * sinRoll;
        double rollY = vector.x * sinRoll + vector.y * cosRoll;
        double rollZ = vector.z;


        double pitchX = rollX;
        double pitchY = rollY * cosPitch - rollZ * sinPitch;
        double pitchZ = rollY * sinPitch + rollZ * cosPitch;

        return new Vec3d(
                pitchX * cosYaw + pitchZ * sinYaw,
                pitchY,
                -pitchX * sinYaw + pitchZ * cosYaw
        );
    }

    public enum Part {
        HEAD,
        SHOULDERS,
        BODY,
        LOWER,
        LEFT_ARM,
        RIGHT_ARM,
        LEFT_LEG,
        RIGHT_LEG
    }

    public record HitResult(
            Part part,
            String detailedPart,
            Vec3d position,
            AnimatedAttackHitboxLibrary.OrientedBox box,
            AnimatedAttackHitboxLibrary.OrientedBox attackBox
    ) {
    }

    public record PartBox(
            Part part,
            AnimatedAttackHitboxLibrary.OrientedBox box
    ) {
    }

    private record GenericHeadSpec(
            double forwardOffsetScale,
            double heightCenterScale,
            double widthScale,
            double heightScale,
            double depthScale
    ) {
    }
}
