package com.kingdomcomecombat.client.collision;

import com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState;
import com.kingdomcomecombat.client.animation.ClientHitReactionState;
import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.animation.GeckoLikeAnimationLibrary;
import com.kingdomcomecombat.client.collision.ClientItemHitboxCache;
import com.kingdomcomecombat.client.combat.CombatClientState;
import com.kingdomcomecombat.client.input.CombatInputClient;
import com.kingdomcomecombat.collision.AnimatedAttackHitboxLibrary;
import com.kingdomcomecombat.collision.HumanoidAnimationPoseLibrary;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import com.kingdomcomecombat.combat.AttackMoveConfig;
import com.kingdomcomecombat.combat.AttackMoveConfigs;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.ComboMoveConfigs;
import com.kingdomcomecombat.combat.CombatAnimationNames;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.network.ClientAttackHitPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

public class ClientAttackHitReporter {
    private static final int MIN_SWEEP_STEPS = 3;
    private static final int MAX_SWEEP_STEPS = 16;
    private static final double SWEEP_STEP_LENGTH = 0.06;
    private static final double ENTITY_ATTACK_REPORT_DISTANCE = 8.0;
    private static final double ENTITY_ATTACK_REPORT_DISTANCE_SQUARED =
            ENTITY_ATTACK_REPORT_DISTANCE * ENTITY_ATTACK_REPORT_DISTANCE;
    private static final Map<Integer, AnimatedAttackHitboxLibrary.OrientedBox> LAST_ENTITY_ATTACK_BOXES =
            new HashMap<>();
    private static final Map<Integer, Float> LAST_ENTITY_ATTACK_SECONDS = new HashMap<>();
    private static boolean wasAttacking = false;
    private static int lastAttackTicks = -1;
    private static String lastAttackKey = "";
    private static AnimatedAttackHitboxLibrary.OrientedBox lastLocalAttackBox;
    private static AnimatedAttackHitboxLibrary.OrientedBox lastLocalSampledAttackBox;
    private static final Set<Integer> REPORTED_LOCAL_TARGETS = new HashSet<>();

    private ClientAttackHitReporter() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientAttackHitReporter::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            LAST_ENTITY_ATTACK_BOXES.clear();
            LAST_ENTITY_ATTACK_SECONDS.clear();
            wasAttacking = false;
            lastLocalAttackBox = null;
            REPORTED_LOCAL_TARGETS.clear();
            return;
        }

        reportEntityAttacksAgainstLocalPlayer(client);

        if (!CombatClientState.attacking || CombatClientState.lastAttackDirection == null) {
            wasAttacking = false;
            lastAttackTicks = -1;
            lastAttackKey = "";
            lastLocalAttackBox = null;
            lastLocalSampledAttackBox = null;
            REPORTED_LOCAL_TARGETS.clear();
            return;
        }

        String attackKey = currentAttackKey();
        if (!wasAttacking
                || CombatClientState.attackTicks <= 1
                || CombatClientState.attackTicks < lastAttackTicks
                || !attackKey.equals(lastAttackKey)) {
            lastLocalAttackBox = null;
            lastLocalSampledAttackBox = null;
            REPORTED_LOCAL_TARGETS.clear();
            ClientItemHitboxCache.invalidate(client.player.getId());
        }
        wasAttacking = true;
        lastAttackTicks = CombatClientState.attackTicks;
        lastAttackKey = attackKey;

        Optional<AnimatedAttackHitboxLibrary.OrientedBox> sampledHitbox =
                sampleLocalAttackBox(client.player);
        if (sampledHitbox.isEmpty()) {
            return;
        }

        AnimatedAttackHitboxLibrary.OrientedBox attackBox =
                paddedAttackBox(sampledHitbox.get());
        AnimatedAttackHitboxLibrary.OrientedBox previousTraceBox =
                lastLocalAttackBox != null ? lastLocalAttackBox : lastLocalSampledAttackBox;
        AttackTrace attackTrace = buildAttackTrace(previousTraceBox, attackBox);
        lastLocalAttackBox = attackBox;
        lastLocalSampledAttackBox = attackBox;

        Box candidateBox = attackTrace.candidateBox().expand(0.35);
        List<LivingEntity> candidates = client.world.getEntitiesByClass(
                LivingEntity.class,
                candidateBox,
                entity -> entity != client.player
                        && entity.isAlive()
                        && !REPORTED_LOCAL_TARGETS.contains(entity.getId())
        );

        for (LivingEntity target : candidates) {
            Optional<SweptHitResult> hitResult = getSweptHitResult(
                    attackTrace,
                    target,
                    localAttackPrefersHead()
            );
            if (hitResult.isEmpty()) {
                continue;
            }

            CombatInputClient.autoLockHitTarget(target);
            REPORTED_LOCAL_TARGETS.add(target.getId());
            com.kingdomcomecombat.client.feedback.CombatHitFeedbackClient
                    .startPredictedHitFeedback(CombatClientState.lastAttackDirection);
            ClientHitReactionState.start(
                    target.getId(),
                    hitResult.get().hitResult().part(),
                    "",
                    CombatClientState.lastAttackDirection,
                    false,
                    0.65F
            );
            Vec3d hitPosition = hitResult.get().hitResult().position();
            ClientPlayNetworking.send(new ClientAttackHitPayload(
                    client.player.getId(),
                    target.getId(),
                    hitResult.get().hitResult().part().ordinal(),
                    hitPosition.x,
                    hitPosition.y,
                    hitPosition.z,
                    hitResult.get().extraHeadHit(),
                    CombatClientState.attackInstanceId
            ));
        }
    }

    private static void reportEntityAttacksAgainstLocalPlayer(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }

        Set<Integer> activeAttackIds = ClientEntityGeckoAnimationState.getActiveAttackEntityIds();
        LAST_ENTITY_ATTACK_BOXES.keySet().removeIf(id -> !activeAttackIds.contains(id));
        LAST_ENTITY_ATTACK_SECONDS.keySet().removeIf(id -> !activeAttackIds.contains(id));

        for (int entityId : activeAttackIds) {
            if (!(client.world.getEntityById(entityId) instanceof LivingEntity attacker)
                    || attacker == client.player) {
                LAST_ENTITY_ATTACK_BOXES.remove(entityId);
                LAST_ENTITY_ATTACK_SECONDS.remove(entityId);
                continue;
            }

            if (attacker.squaredDistanceTo(client.player) > ENTITY_ATTACK_REPORT_DISTANCE_SQUARED) {
                LAST_ENTITY_ATTACK_BOXES.remove(entityId);
                LAST_ENTITY_ATTACK_SECONDS.remove(entityId);
                continue;
            }

            ClientEntityGeckoAnimationState.ActiveAnimation animation =
                    ClientEntityGeckoAnimationState.getAttackLayer(entityId);
            if (animation == null) {
                LAST_ENTITY_ATTACK_BOXES.remove(entityId);
                LAST_ENTITY_ATTACK_SECONDS.remove(entityId);
                continue;
            }

            if (animation.elapsedSeconds() <= 0.05F) {
                LAST_ENTITY_ATTACK_BOXES.remove(entityId);
                LAST_ENTITY_ATTACK_SECONDS.remove(entityId);
            }

            Optional<AnimatedAttackHitboxLibrary.OrientedBox> sampledHitbox =
                    sampleEntityAttackBox(attacker, animation);
            if (sampledHitbox.isEmpty()) {
                continue;
            }

            AnimatedAttackHitboxLibrary.OrientedBox attackBox =
                    paddedAttackBox(sampledHitbox.get());
            AnimatedAttackHitboxLibrary.OrientedBox previousBox =
                    getPreviousEntityAttackBox(entityId, animation.elapsedSeconds());
            AttackTrace attackTrace = buildAttackTrace(previousBox, attackBox);
            LAST_ENTITY_ATTACK_BOXES.put(entityId, attackBox);
            LAST_ENTITY_ATTACK_SECONDS.put(entityId, animation.elapsedSeconds());

            Optional<SweptHitResult> hitResult = getSweptHitResult(
                    attackTrace,
                    client.player,
                    false
            );
            if (hitResult.isEmpty()) {
                continue;
            }

            Vec3d hitPosition = hitResult.get().hitResult().position();
            ClientPlayNetworking.send(new ClientAttackHitPayload(
                    entityId,
                    client.player.getId(),
                    hitResult.get().hitResult().part().ordinal(),
                    hitPosition.x,
                    hitPosition.y,
                    hitPosition.z,
                    hitResult.get().extraHeadHit(),
                    0L
            ));
        }
    }

    private static Optional<AnimatedAttackHitboxLibrary.OrientedBox> sampleLocalAttackBox(
            LivingEntity attacker
    ) {
        float elapsedSeconds = CombatAnimationClient.getLocalAttackAnimationElapsedSeconds()
                .orElseGet(CombatClientState::currentAttackAnimationElapsedSeconds);
        String animationName = CombatClientState.currentAttackAnimationName;
        if (animationName != null) {
            AttackMoveConfig namedMove = AttackMoveConfigs.getNamed(animationName);
            boolean useRealHitbox = CombatAnimationNames.isHeavyAttack(animationName)
                    || (namedMove != null
                    ? namedMove.useRealHitbox()
                    : ComboMoveConfigs.findByAnimationName(animationName)
                            .map(ComboMoveConfig::useRealHitbox)
                            .orElse(false));
            Vec3d realHitboxSize = EquipmentCombatAttributesRegistry.realHitboxSizeUnits(
                    attacker.getMainHandStack(),
                    AnimatedAttackHitboxLibrary.getRealHitboxSizeUnits()
            );
            Vec3d realHitboxOffset = EquipmentCombatAttributesRegistry.realHitboxOffsetUnits(
                    attacker.getMainHandStack(),
                    AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits()
            );
            Vec3d realHitboxRotation = EquipmentCombatAttributesRegistry.realHitboxRotationDegrees(
                    attacker.getMainHandStack(),
                    AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees()
            );
            Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sample =
                    AnimatedAttackHitboxLibrary.sampleNamedSeconds(
                    animationName,
                    elapsedSeconds,
                    useRealHitbox,
                    realHitboxSize,
                    realHitboxOffset,
                    realHitboxRotation
            );
            return resolveAttackBox(attacker, sample, useRealHitbox);
        }

        boolean useRealHitbox = AttackMoveConfigs.get(
                CombatClientState.lastAttackDirection
        ).useRealHitbox();
        Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sample =
                AnimatedAttackHitboxLibrary.sampleSeconds(
                        CombatClientState.lastAttackDirection,
                        elapsedSeconds,
                        useRealHitbox
                );
        return resolveAttackBox(attacker, sample, useRealHitbox);
    }

    private static Optional<AnimatedAttackHitboxLibrary.OrientedBox> sampleEntityAttackBox(
            LivingEntity attacker,
            ClientEntityGeckoAnimationState.ActiveAnimation animation
    ) {
        if (CombatAnimationNames.isHeavyAttack(animation.customAnimationName())) {
            Vec3d realHitboxSize = EquipmentCombatAttributesRegistry.realHitboxSizeUnits(
                    attacker.getMainHandStack(),
                    AnimatedAttackHitboxLibrary.getRealHitboxSizeUnits()
            );
            Vec3d realHitboxOffset = EquipmentCombatAttributesRegistry.realHitboxOffsetUnits(
                    attacker.getMainHandStack(),
                    AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits()
            );
            Vec3d realHitboxRotation = EquipmentCombatAttributesRegistry.realHitboxRotationDegrees(
                    attacker.getMainHandStack(),
                    AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees()
            );
            Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sample =
                    AnimatedAttackHitboxLibrary.sampleNamedSeconds(
                            animation.customAnimationName(),
                            animation.elapsedSeconds(),
                            true,
                            realHitboxSize,
                            realHitboxOffset,
                            realHitboxRotation
                    );
            return resolveAttackBox(attacker, sample, true);
        }

        AttackMoveConfig moveConfig = animation.customAnimationName().isBlank()
                ? AttackMoveConfigs.get(animation.direction())
                : AttackMoveConfigs.getNamed(animation.customAnimationName());
        if (moveConfig == null) {
            moveConfig = AttackMoveConfigs.get(animation.direction());
        }
        boolean useRealHitbox = moveConfig.useRealHitbox();
        Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sample =
                moveConfig.animationName().isBlank()
                        ? AnimatedAttackHitboxLibrary.sampleSeconds(
                                animation.direction(),
                                animation.elapsedSeconds(),
                                useRealHitbox
                        )
                        : AnimatedAttackHitboxLibrary.sampleNamedSeconds(
                                moveConfig.animationName(),
                                animation.elapsedSeconds(),
                                useRealHitbox,
                                EquipmentCombatAttributesRegistry.realHitboxSizeUnits(
                                        attacker.getMainHandStack(),
                                        AnimatedAttackHitboxLibrary.getRealHitboxSizeUnits()
                                ),
                                EquipmentCombatAttributesRegistry.realHitboxOffsetUnits(
                                        attacker.getMainHandStack(),
                                        AnimatedAttackHitboxLibrary.getRealHitboxOffsetUnits()
                                ),
                                EquipmentCombatAttributesRegistry.realHitboxRotationDegrees(
                                        attacker.getMainHandStack(),
                                        AnimatedAttackHitboxLibrary.getRealHitboxRotationDegrees()
                                )
                        );
        return resolveAttackBox(attacker, sample, useRealHitbox);
    }

    private static Optional<AnimatedAttackHitboxLibrary.OrientedBox> resolveAttackBox(
            LivingEntity attacker,
            Optional<AnimatedAttackHitboxLibrary.SampledHitbox> sample,
            boolean useRealHitbox
    ) {
        if (sample.isEmpty()) {
            return Optional.empty();
        }

        if (useRealHitbox) {
            Optional<AnimatedAttackHitboxLibrary.OrientedBox> itemBox =
                    ClientItemHitboxCache.get(attacker.getId());
            if (itemBox.isPresent()) {
                return itemBox;
            }
        }

        return Optional.of(sample.get().toWorldBox(attacker));
    }

    private static List<HumanoidHurtboxLibrary.PartBox> animatedHurtboxes(LivingEntity entity) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (entity == client.player) {
            return ClientModelHurtboxCache.simulateLocalPlayer(entity);
        }

        Optional<List<HumanoidHurtboxLibrary.PartBox>> modelBoxes =
                ClientModelHurtboxCache.get(entity);
        if (modelBoxes.isPresent()) {
            return modelBoxes.get();
        }

        ClientEntityGeckoAnimationState.ActiveAnimation animation =
                ClientEntityGeckoAnimationState.get(entity.getId());
        if (animation == null) {
            return HumanoidHurtboxLibrary.getHurtboxes(entity);
        }

        if (animation.kind() == GeckoLikeAnimationLibrary.Kind.ATTACK) {
            return HumanoidHurtboxLibrary.getAnimatedHurtboxes(
                    entity,
                    HumanoidAnimationPoseLibrary.Kind.ATTACK,
                    animation.direction(),
                    animation.elapsedSeconds()
            );
        }

        if (animation.kind() == GeckoLikeAnimationLibrary.Kind.STANCE) {
            return HumanoidHurtboxLibrary.getAnimatedHurtboxes(
                    entity,
                    HumanoidAnimationPoseLibrary.Kind.STANCE,
                    animation.direction(),
                    animation.elapsedSeconds()
            );
        }

        return HumanoidHurtboxLibrary.getHurtboxes(entity);
    }

    private static Optional<SweptHitResult> getSweptHitResult(
            AttackTrace attackTrace,
            LivingEntity target
    ) {
        return getSweptHitResult(attackTrace, target, false);
    }

    private static Optional<SweptHitResult> getSweptHitResult(
            AttackTrace attackTrace,
            LivingEntity target,
            boolean preferHead
    ) {
        List<HumanoidHurtboxLibrary.PartBox> hurtboxes = animatedHurtboxes(target);
        boolean genericTarget = !HumanoidHurtboxLibrary.isHumanoidTarget(target);
        HumanoidHurtboxLibrary.HitResult firstHumanoidHit = null;
        HumanoidHurtboxLibrary.HitResult firstHumanoidHeadHit = null;
        boolean hitGenericHead = false;
        boolean hitGenericBody = false;
        HumanoidHurtboxLibrary.HitResult firstBodyHit = null;
        HumanoidHurtboxLibrary.HitResult firstHeadHit = null;

        for (AnimatedAttackHitboxLibrary.OrientedBox box : attackTrace.boxes()) {
            Optional<HumanoidHurtboxLibrary.HitResult> hitResult =
                    HumanoidHurtboxLibrary.getHitResult(box, target, hurtboxes);
            if (hitResult.isPresent()) {
                if (!genericTarget) {
                    if (!preferHead) {
                        return Optional.of(new SweptHitResult(hitResult.get(), false));
                    }
                    if (firstHumanoidHit == null) {
                        firstHumanoidHit = hitResult.get();
                    }
                    if (hitResult.get().part() == HumanoidHurtboxLibrary.Part.HEAD
                            && firstHumanoidHeadHit == null) {
                        firstHumanoidHeadHit = hitResult.get();
                    }
                    continue;
                }

                if (hitResult.get().part() == HumanoidHurtboxLibrary.Part.HEAD) {
                    hitGenericHead = true;
                    if (firstHeadHit == null) {
                        firstHeadHit = hitResult.get();
                    }
                } else {
                    hitGenericBody = true;
                    if (firstBodyHit == null) {
                        firstBodyHit = hitResult.get();
                    }
                }
            }
        }

        if (preferHead && firstHumanoidHeadHit != null) {
            return Optional.of(new SweptHitResult(firstHumanoidHeadHit, false));
        }

        if (preferHead && firstHumanoidHit != null) {
            return Optional.of(new SweptHitResult(firstHumanoidHit, false));
        }

        if (genericTarget && hitGenericBody && hitGenericHead && firstBodyHit != null) {
            return Optional.of(new SweptHitResult(firstBodyHit, true));
        }

        if (genericTarget && firstHeadHit != null) {
            return Optional.of(new SweptHitResult(firstHeadHit, false));
        }

        if (genericTarget && firstBodyHit != null) {
            return Optional.of(new SweptHitResult(firstBodyHit, false));
        }

        return Optional.empty();
    }

    private static boolean localAttackPrefersHead() {
        return "attack_smash".equals(CombatClientState.currentAttackAnimationName);
    }

    private static AnimatedAttackHitboxLibrary.OrientedBox getPreviousEntityAttackBox(
            int entityId,
            float elapsedSeconds
    ) {
        Float previousSeconds = LAST_ENTITY_ATTACK_SECONDS.get(entityId);
        if (previousSeconds == null || elapsedSeconds < previousSeconds) {
            LAST_ENTITY_ATTACK_BOXES.remove(entityId);
            return null;
        }

        return LAST_ENTITY_ATTACK_BOXES.get(entityId);
    }

    private static AttackTrace buildAttackTrace(
            AnimatedAttackHitboxLibrary.OrientedBox previous,
            AnimatedAttackHitboxLibrary.OrientedBox current
    ) {
        if (previous == null) {
            return new AttackTrace(List.of(current), current.candidateBox());
        }

        int steps = sweepSteps(previous, current);
        List<AnimatedAttackHitboxLibrary.OrientedBox> boxes = new ArrayList<>(steps + 1);
        Box candidateBox = null;
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            AnimatedAttackHitboxLibrary.OrientedBox box = interpolateBox(
                    previous,
                    current,
                    progress
            );
            boxes.add(box);
            candidateBox = candidateBox == null
                    ? box.candidateBox()
                    : union(candidateBox, box.candidateBox());
        }

        return new AttackTrace(boxes, candidateBox);
    }

    private static int sweepSteps(
            AnimatedAttackHitboxLibrary.OrientedBox previous,
            AnimatedAttackHitboxLibrary.OrientedBox current
    ) {
        double centerDistance = previous.center().distanceTo(current.center());
        double tipDistance = maxCornerDistance(previous, current);
        int dynamicSteps = (int) Math.ceil(Math.max(centerDistance, tipDistance) / SWEEP_STEP_LENGTH);
        return Math.max(MIN_SWEEP_STEPS, Math.min(MAX_SWEEP_STEPS, dynamicSteps));
    }

    private static double maxCornerDistance(
            AnimatedAttackHitboxLibrary.OrientedBox previous,
            AnimatedAttackHitboxLibrary.OrientedBox current
    ) {
        Vec3d previousTipA = previous.center().add(previous.axisZ().multiply(previous.halfExtents().z));
        Vec3d previousTipB = previous.center().subtract(previous.axisZ().multiply(previous.halfExtents().z));
        Vec3d currentTipA = current.center().add(current.axisZ().multiply(current.halfExtents().z));
        Vec3d currentTipB = current.center().subtract(current.axisZ().multiply(current.halfExtents().z));
        return Math.max(
                previousTipA.distanceTo(currentTipA),
                previousTipB.distanceTo(currentTipB)
        );
    }

    private static AnimatedAttackHitboxLibrary.OrientedBox interpolateBox(
            AnimatedAttackHitboxLibrary.OrientedBox previous,
            AnimatedAttackHitboxLibrary.OrientedBox current,
            double progress
    ) {
        Vec3d axisX = normalizeOr(lerp(previous.axisX(), current.axisX(), progress), current.axisX());
        Vec3d axisYSeed = normalizeOr(lerp(previous.axisY(), current.axisY(), progress), current.axisY());
        Vec3d axisZ = normalizeOr(axisX.crossProduct(axisYSeed), current.axisZ());
        Vec3d axisY = normalizeOr(axisZ.crossProduct(axisX), current.axisY());

        return new AnimatedAttackHitboxLibrary.OrientedBox(
                lerp(previous.center(), current.center(), progress),
                lerp(previous.halfExtents(), current.halfExtents(), progress),
                axisX,
                axisY,
                axisZ
        );
    }

    private static Vec3d lerp(Vec3d from, Vec3d to, double progress) {
        return from.multiply(1.0 - progress).add(to.multiply(progress));
    }

    private static Vec3d normalizeOr(Vec3d value, Vec3d fallback) {
        if (value.lengthSquared() < 1.0E-8) {
            return fallback.normalize();
        }

        return value.normalize();
    }

    private static Box union(Box first, Box second) {
        return new Box(
                Math.min(first.minX, second.minX),
                Math.min(first.minY, second.minY),
                Math.min(first.minZ, second.minZ),
                Math.max(first.maxX, second.maxX),
                Math.max(first.maxY, second.maxY),
                Math.max(first.maxZ, second.maxZ)
        );
    }

    private static AnimatedAttackHitboxLibrary.OrientedBox paddedAttackBox(
            AnimatedAttackHitboxLibrary.OrientedBox box
    ) {
        return new AnimatedAttackHitboxLibrary.OrientedBox(
                box.center(),
                box.halfExtents().add(new Vec3d(0.16, 0.10, 0.16)),
                box.axisX(),
                box.axisY(),
                box.axisZ()
        );
    }

    private static String currentAttackKey() {
        return CombatClientState.attackInstanceId
                + ":"
                + CombatClientState.lastAttackDirection
                + ":"
                + CombatClientState.currentAttackAnimationName
                + ":"
                + CombatClientState.attackTotalTicks;
    }

    private record AttackTrace(
            List<AnimatedAttackHitboxLibrary.OrientedBox> boxes,
            Box candidateBox
    ) {
    }

    private record SweptHitResult(
            HumanoidHurtboxLibrary.HitResult hitResult,
            boolean extraHeadHit
    ) {
    }
}
