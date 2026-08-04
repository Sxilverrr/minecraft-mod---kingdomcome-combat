package com.kingdomcomecombat.client.animation;

import com.kingdomcomecombat.combat.CombatDirection;
import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ClientEntityGeckoAnimationState {
    private static final float FADE_SECONDS = 0.12F;

    private static final Map<Integer, AttackAnimation> ATTACKS = new HashMap<>();
    private static final Map<Integer, HitReactionAnimation> HIT_REACTIONS = new HashMap<>();
    private static final Map<Integer, BlockAnimation> BLOCKS = new HashMap<>();
    private static final Map<Integer, StanceAnimation> STANCES = new HashMap<>();
    private static final Map<Integer, ImpactSlowdown> ATTACK_IMPACT_SLOWDOWNS = new HashMap<>();
    private static final Map<Integer, Double> INTERRUPT_FADE_STARTS = new HashMap<>();
    private static final Map<Integer, ItemTransition> ITEM_TRANSITIONS = new HashMap<>();
    private static final Map<Integer, UUID> ENTITY_IDENTITIES = new HashMap<>();

    private ClientEntityGeckoAnimationState() {
    }

    public static void startAttack(int entityId, CombatDirection direction, float speedMultiplier) {
        startAttack(entityId, direction, speedMultiplier, 0.0F, "");
    }

    public static void startAttack(
            int entityId,
            CombatDirection direction,
            float speedMultiplier,
            String animationName
    ) {
        startAttack(entityId, direction, speedMultiplier, 0.0F, animationName);
    }

    public static void startAttack(
            int entityId,
            CombatDirection direction,
            float speedMultiplier,
            float startupSlowdown,
            String animationName
    ) {
        bindEntityIdentity(entityId);
        float sanitizedSpeed = sanitizeSpeed(speedMultiplier);
        float sanitizedSlowdown = sanitizeSlowdown(startupSlowdown);
        String sanitizedAnimation = animationName == null || animationName.isBlank() ? "" : animationName;
        AttackAnimation current = ATTACKS.get(entityId);
        if (current != null
                && current.direction() == direction
                && Math.abs(current.speedMultiplier() - sanitizedSpeed) < 0.001F
                && Math.abs(current.startupSlowdown() - sanitizedSlowdown) < 0.001F
                && current.animationName().equals(sanitizedAnimation)) {
            return;
        }

        ATTACK_IMPACT_SLOWDOWNS.remove(entityId);
        ATTACKS.put(
                entityId,
                new AttackAnimation(
                        direction,
                        sanitizedSpeed,
                        sanitizedSlowdown,
                        animationTicks(),
                        sanitizedAnimation
                )
        );
    }

    public static void setStance(int entityId, CombatDirection direction, float speedMultiplier) {
        setStance(entityId, -1, direction, speedMultiplier, "");
    }

    public static void setStance(
            int entityId,
            int targetEntityId,
            CombatDirection direction,
            float speedMultiplier,
            String animationName
    ) {
        bindEntityIdentity(entityId);
        StanceAnimation current = STANCES.get(entityId);
        float sanitizedSpeed = sanitizeSpeed(speedMultiplier);
        String sanitizedAnimation = animationName == null ? "" : animationName;

        if (current != null
                && current.direction() == direction
                && current.targetEntityId() == targetEntityId
                && Math.abs(current.speedMultiplier() - sanitizedSpeed) < 0.001F
                && current.animationName().equals(sanitizedAnimation)) {
            return;
        }

        STANCES.put(
                entityId,
                new StanceAnimation(
                        direction,
                        targetEntityId,
                        sanitizedSpeed,
                        animationTicks(),
                        sanitizedAnimation
                )
        );
    }

    public static void clearStance(int entityId) {
        STANCES.remove(entityId);
    }

    public static void startBlock(
            int entityId,
            GeckoLikeAnimationLibrary.Kind kind,
            CombatDirection direction
    ) {
        bindEntityIdentity(entityId);
        BLOCKS.put(entityId, new BlockAnimation(kind, direction, animationTicks()));
        interruptAttack(entityId);
    }

    public static void applyAttackImpactSlowdown(int entityId) {
        double now = animationTicks();
        ATTACK_IMPACT_SLOWDOWNS.put(entityId, new ImpactSlowdown(now, now + 2.0));
    }

    public static void interruptAttack(int entityId) {
        if (ATTACKS.containsKey(entityId)) {
            INTERRUPT_FADE_STARTS.put(entityId, animationTicks());
        }
        ATTACK_IMPACT_SLOWDOWNS.remove(entityId);
    }

    public static void startHitReaction(
            int entityId,
            CombatDirection direction,
            String animationName
    ) {
        startHitReaction(entityId, direction, animationName, false);
    }

    public static void startHitReaction(
            int entityId,
            CombatDirection direction,
            String animationName,
            boolean holdLastFrame
    ) {
        startHitReaction(entityId, direction, animationName, holdLastFrame, 1.0F);
    }

    public static void startHitReaction(
            int entityId,
            CombatDirection direction,
            String animationName,
            boolean holdLastFrame,
            float speedMultiplier
    ) {
        if (animationName == null || animationName.isBlank()) {
            return;
        }
        if (isExecutionAttackActive(entityId)) {
            return;
        }

        bindEntityIdentity(entityId);
        interruptAttack(entityId);
        HIT_REACTIONS.put(
                entityId,
                new HitReactionAnimation(
                        direction,
                        animationName,
                        animationTicks(),
                        holdLastFrame,
                        sanitizeSpeed(speedMultiplier)
                )
        );
    }

    public static boolean isHoldingFinalHitReactionFrame(int entityId) {
        HitReactionAnimation reaction = HIT_REACTIONS.get(entityId);
        if (reaction == null || !reaction.holdLastFrame()) {
            return false;
        }

        float length = GeckoLikeAnimationLibrary.getNamedLengthSeconds(reaction.animationName());
        return reaction.elapsedSeconds() >= length;
    }

    public static ActiveAnimation get(int entityId) {
        ActiveAnimation hitReaction = getHitReactionLayer(entityId);
        if (hitReaction != null) {
            return hitReaction;
        }

        ActiveAnimation block = getBlockLayer(entityId);
        if (block != null) {
            return block;
        }

        ActiveAnimation attack = getAttackLayer(entityId);
        if (attack != null) {
            return attack;
        }

        return getStanceLayer(entityId);
    }

    public static List<ActiveAnimation> getLayers(int entityId) {
        ActiveAnimation stance = getStanceLayer(entityId);
        ActiveAnimation attack = getAttackLayer(entityId);
        ActiveAnimation block = getBlockLayer(entityId);
        ActiveAnimation hitReaction = getHitReactionLayer(entityId);

        if (hitReaction != null) {
            return stance != null ? List.of(stance, hitReaction) : List.of(hitReaction);
        }

        if (stance != null && block != null) {
            return List.of(stance, block);
        }

        if (block != null) {
            return List.of(block);
        }

        if (stance != null && attack != null) {
            return List.of(stance, attack);
        }

        if (attack != null) {
            return List.of(attack);
        }

        if (stance != null) {
            return List.of(stance);
        }

        return List.of();
    }

    public static boolean isStanceOnly(int entityId) {
        return getStanceLayer(entityId) != null
                && getAttackLayer(entityId) == null
                && getBlockLayer(entityId) == null
                && getHitReactionLayer(entityId) == null;
    }

    public static int getStanceTargetEntityId(int entityId) {
        StanceAnimation stance = STANCES.get(entityId);
        return stance == null ? -1 : stance.targetEntityId();
    }

    public static boolean hasActiveCombatLayer(int entityId) {
        return entityId >= 0
                && (HIT_REACTIONS.containsKey(entityId)
                || BLOCKS.containsKey(entityId)
                || ATTACKS.containsKey(entityId)
                || STANCES.containsKey(entityId));
    }

    /**
     * Combat poses also drive rendered-bone collision boxes, so every entity
     * with an active combat layer must keep its animation applied. Render-distance
     * LOD here would make another nearby player's hit validation use a stale pose.
     */
    public static boolean shouldRenderCombatAnimation(int entityId) {
        return hasActiveCombatLayer(entityId);
    }

    /** Current instantaneous playback rate used by PAL-backed remote players. */
    public static float playbackSpeed(int entityId) {
        HitReactionAnimation reaction = HIT_REACTIONS.get(entityId);
        if (reaction != null) return reaction.speedMultiplier();
        if (BLOCKS.containsKey(entityId)) return 1.0F;

        AttackAnimation attack = ATTACKS.get(entityId);
        if (attack != null) {
            if (isImpactSlowdownActive(entityId)) return 0.01F;
            float slowWindowSeconds = com.kingdomcomecombat.combat.CombatControlConfig
                    .PERFECT_COUNTER_SLOW_TICKS / 20.0F;
            if (attack.startupSlowdown() > 0.0F
                    && attack.rawElapsedSeconds() <= slowWindowSeconds) {
                return Math.max(0.01F,
                        attack.speedMultiplier() * (1.0F - attack.startupSlowdown()));
            }
            return attack.speedMultiplier();
        }

        StanceAnimation stance = STANCES.get(entityId);
        return stance == null ? 1.0F : stance.speedMultiplier();
    }

    public static ActiveAnimation getBlockLayer(int entityId) {
        BlockAnimation block = BLOCKS.get(entityId);
        if (block == null) {
            return null;
        }

        float length = block.lengthSeconds();
        float elapsed = block.elapsedSeconds(entityId);
        if (elapsed > length) {
            BLOCKS.remove(entityId);
            return null;
        }

        return new ActiveAnimation(
                block.kind(),
                block.direction(),
                elapsed,
                block.weight(length, elapsed),
                ""
        );
    }

    public static ActiveAnimation getHitReactionLayer(int entityId) {
        HitReactionAnimation reaction = HIT_REACTIONS.get(entityId);
        if (reaction == null) {
            return null;
        }

        float length = GeckoLikeAnimationLibrary.getNamedLengthSeconds(reaction.animationName());
        float elapsed = reaction.elapsedSeconds();
        if (elapsed > length) {
            if (reaction.holdLastFrame()) {
                elapsed = Math.max(0.0F, length - 0.001F);
                return new ActiveAnimation(
                        GeckoLikeAnimationLibrary.Kind.ATTACK,
                        reaction.direction(),
                        elapsed,
                        1.0F,
                        reaction.animationName()
                );
            }
            HIT_REACTIONS.remove(entityId);
            return null;
        }

        return new ActiveAnimation(
                GeckoLikeAnimationLibrary.Kind.ATTACK,
                reaction.direction(),
                elapsed,
                reaction.weight(length),
                reaction.animationName()
        );
    }

    public static ActiveAnimation getAttackLayer(int entityId) {
        AttackAnimation attack = ATTACKS.get(entityId);
        if (attack != null) {
            float animationLength = attack.animationName().isBlank()
                    ? GeckoLikeAnimationLibrary.getLengthSeconds(
                            GeckoLikeAnimationLibrary.Kind.ATTACK,
                            attack.direction()
                    )
                    : GeckoLikeAnimationLibrary.getNamedLengthSeconds(attack.animationName());

            float elapsed = attack.elapsedSeconds(entityId);
            if (elapsed <= animationLength) {
                float interruptWeight = getInterruptWeight(entityId);
                if (interruptWeight <= 0.0F) {
                    ATTACKS.remove(entityId);
                    ATTACK_IMPACT_SLOWDOWNS.remove(entityId);
                    INTERRUPT_FADE_STARTS.remove(entityId);
                    return null;
                }

                return new ActiveAnimation(
                        GeckoLikeAnimationLibrary.Kind.ATTACK,
                        attack.direction(),
                        elapsed,
                        attack.weight(animationLength, elapsed) * interruptWeight,
                        attack.animationName()
                );
            }

            ATTACKS.remove(entityId);
            ATTACK_IMPACT_SLOWDOWNS.remove(entityId);
            INTERRUPT_FADE_STARTS.remove(entityId);
        }

        return null;
    }

    private static boolean isExecutionAttackActive(int entityId) {
        AttackAnimation attack = ATTACKS.get(entityId);
        return attack != null && isExecutionAnimationName(attack.animationName());
    }

    private static boolean isExecutionAnimationName(String animationName) {
        return animationName != null && animationName.startsWith("execution_");
    }

    public static Set<Integer> getActiveAttackEntityIds() {
        return Set.copyOf(ATTACKS.keySet());
    }

    public static boolean hasAnyActiveAttack() { return !ATTACKS.isEmpty(); }

    public static boolean hasActiveAttack(int entityId) { return ATTACKS.containsKey(entityId); }

    public static boolean hasActiveStance(int entityId) { return STANCES.containsKey(entityId); }

    public static ActiveAnimation getStanceLayer(int entityId) {
        StanceAnimation stance = STANCES.get(entityId);
        if (stance == null) {
            return null;
        }

        float length = stance.animationName().isBlank()
                ? GeckoLikeAnimationLibrary.getLengthSeconds(
                        GeckoLikeAnimationLibrary.Kind.STANCE,
                        stance.direction()
                )
                : GeckoLikeAnimationLibrary.getNamedLengthSeconds(stance.animationName());
        float elapsed = stance.elapsedSeconds();
        if (length > 0.0F) {
            elapsed %= length;
        }

        return new ActiveAnimation(
                GeckoLikeAnimationLibrary.Kind.STANCE,
                stance.direction(),
                elapsed,
                stance.weight(),
                stance.animationName()
        );
    }

    public static Optional<GeckoLikeAnimationLibrary.BoneTransform> getItemTransform(int entityId) {
        GeckoLikeAnimationLibrary.BoneTransform target = zeroItemTransform();
        ActiveAnimation hitReaction = getHitReactionLayer(entityId);
        if (hitReaction != null) {
            target = itemTransformOrZero(hitReaction);
        } else {
            ActiveAnimation block = getBlockLayer(entityId);
            if (block != null) {
                target = itemTransformOrZero(block);
            } else {
                ActiveAnimation attack = getAttackLayer(entityId);
                if (attack != null) {
                    target = itemTransformOrZero(attack);
                } else {
                    ActiveAnimation stance = getStanceLayer(entityId);
                    if (stance != null) {
                        target = itemTransformOrZero(stance);
                    }
                }
            }
        }

        return Optional.of(updateItemTransition(entityId, target));
    }

    private static GeckoLikeAnimationLibrary.BoneTransform itemTransformOrZero(ActiveAnimation layer) {
        GeckoLikeAnimationLibrary.BoneTransform transform =
                layer.customAnimationName().isBlank()
                        ? GeckoLikeAnimationLibrary.sampleBone(
                                layer.kind(),
                                layer.direction(),
                                "item",
                                layer.elapsedSeconds()
                        )
                        : GeckoLikeAnimationLibrary.sampleNamedBone(
                                layer.customAnimationName(),
                                "item",
                                layer.elapsedSeconds()
                        );
        return transform.empty() ? zeroItemTransform() : fillMissingChannelsWithZero(transform);
    }

    public static void tickCleanup() {
        MinecraftClient client = MinecraftClient.getInstance();
        cleanupEntityIdentities(client);
        cleanupAttacks(client);
        cleanupHitReactions(client);
        cleanupBlocks(client);
        cleanupStances(client);
        cleanupItemTransitions(client);
    }

    private static void bindEntityIdentity(int entityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        var entity = client.world.getEntityById(entityId);
        if (entity == null) {
            return;
        }
        UUID current = entity.getUuid();
        UUID previous = ENTITY_IDENTITIES.put(entityId, current);
        if (previous != null && !previous.equals(current)) {
            clearEntityState(entityId);
            ENTITY_IDENTITIES.put(entityId, current);
        }
    }

    private static void cleanupEntityIdentities(MinecraftClient client) {
        Iterator<Map.Entry<Integer, UUID>> iterator = ENTITY_IDENTITIES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, UUID> entry = iterator.next();
            var entity = client.world == null ? null : client.world.getEntityById(entry.getKey());
            if (entity == null || !entry.getValue().equals(entity.getUuid())) {
                clearEntityState(entry.getKey());
                iterator.remove();
            }
        }
    }

    private static void clearEntityState(int entityId) {
        ATTACKS.remove(entityId);
        HIT_REACTIONS.remove(entityId);
        BLOCKS.remove(entityId);
        STANCES.remove(entityId);
        ATTACK_IMPACT_SLOWDOWNS.remove(entityId);
        INTERRUPT_FADE_STARTS.remove(entityId);
        ITEM_TRANSITIONS.remove(entityId);
    }

    private static GeckoLikeAnimationLibrary.BoneTransform updateItemTransition(
            int entityId,
            GeckoLikeAnimationLibrary.BoneTransform target
    ) {
        double now = animationTicks();
        ItemTransition transition = ITEM_TRANSITIONS.computeIfAbsent(
                entityId,
                id -> new ItemTransition(target, now)
        );
        float deltaSeconds = (float) ((now - transition.updatedAtTicks) / 20.0);
        transition.updatedAtTicks = now;
        float progress = Math.min(1.0F, deltaSeconds / FADE_SECONDS);
        transition.displayed = lerpItemTransform(transition.displayed, target, progress);
        return transition.displayed;
    }

    private static GeckoLikeAnimationLibrary.BoneTransform lerpItemTransform(
            GeckoLikeAnimationLibrary.BoneTransform from,
            GeckoLikeAnimationLibrary.BoneTransform to,
            float progress
    ) {
        return new GeckoLikeAnimationLibrary.BoneTransform(
                Optional.of(from.rotation()
                        .orElse(GeckoLikeAnimationLibrary.BonePose.ZERO)
                        .lerp(to.rotation()
                                .orElse(GeckoLikeAnimationLibrary.BonePose.ZERO), progress)),
                Optional.of(from.position()
                        .orElse(GeckoLikeAnimationLibrary.BonePose.ZERO)
                        .lerp(to.position()
                                .orElse(GeckoLikeAnimationLibrary.BonePose.ZERO), progress))
        );
    }

    private static GeckoLikeAnimationLibrary.BoneTransform fillMissingChannelsWithZero(
            GeckoLikeAnimationLibrary.BoneTransform transform
    ) {
        return new GeckoLikeAnimationLibrary.BoneTransform(
                Optional.of(transform.rotation().orElse(GeckoLikeAnimationLibrary.BonePose.ZERO)),
                Optional.of(transform.position().orElse(GeckoLikeAnimationLibrary.BonePose.ZERO))
        );
    }

    private static GeckoLikeAnimationLibrary.BoneTransform zeroItemTransform() {
        return new GeckoLikeAnimationLibrary.BoneTransform(
                Optional.of(GeckoLikeAnimationLibrary.BonePose.ZERO),
                Optional.of(GeckoLikeAnimationLibrary.BonePose.ZERO)
        );
    }

    private static void cleanupBlocks(MinecraftClient client) {
        Iterator<Map.Entry<Integer, BlockAnimation>> iterator = BLOCKS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, BlockAnimation> entry = iterator.next();
            if (entry.getValue().rawElapsedSeconds() > entry.getValue().lengthSeconds()
                    || client.world == null
                    || client.world.getEntityById(entry.getKey()) == null) {
                iterator.remove();
            }
        }
    }

    private static void cleanupAttacks(MinecraftClient client) {
        Iterator<Map.Entry<Integer, AttackAnimation>> iterator = ATTACKS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, AttackAnimation> entry = iterator.next();
            AttackAnimation animation = entry.getValue();
            float animationLength = animation.animationName().isBlank()
                    ? GeckoLikeAnimationLibrary.getLengthSeconds(
                            GeckoLikeAnimationLibrary.Kind.ATTACK,
                            animation.direction()
                    )
                    : GeckoLikeAnimationLibrary.getNamedLengthSeconds(animation.animationName());

            if (animation.elapsedSeconds(entry.getKey()) > animationLength
                    || client.world == null
                    || client.world.getEntityById(entry.getKey()) == null) {
                ATTACK_IMPACT_SLOWDOWNS.remove(entry.getKey());
                INTERRUPT_FADE_STARTS.remove(entry.getKey());
                iterator.remove();
            }
        }
    }

    private static void cleanupHitReactions(MinecraftClient client) {
        Iterator<Map.Entry<Integer, HitReactionAnimation>> iterator = HIT_REACTIONS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, HitReactionAnimation> entry = iterator.next();
            HitReactionAnimation animation = entry.getValue();
            float animationLength = GeckoLikeAnimationLibrary.getNamedLengthSeconds(animation.animationName());

            if (client.world == null
                    || client.world.getEntityById(entry.getKey()) == null) {
                iterator.remove();
                continue;
            }

            if (!animation.holdLastFrame() && animation.elapsedSeconds() > animationLength) {
                iterator.remove();
            }
        }
    }

    private static void cleanupStances(MinecraftClient client) {
        Iterator<Map.Entry<Integer, StanceAnimation>> iterator = STANCES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, StanceAnimation> entry = iterator.next();

            if (client.world == null || client.world.getEntityById(entry.getKey()) == null) {
                iterator.remove();
            }
        }
    }

    private static void cleanupItemTransitions(MinecraftClient client) {
        Iterator<Integer> iterator = ITEM_TRANSITIONS.keySet().iterator();

        while (iterator.hasNext()) {
            int entityId = iterator.next();
            if (client.world == null || client.world.getEntityById(entityId) == null) {
                iterator.remove();
            }
        }
    }

    private static float sanitizeSpeed(float speedMultiplier) {
        return Math.max(0.05F, speedMultiplier);
    }

    private static float sanitizeSlowdown(float slowdown) {
        return Math.max(0.0F, Math.min(0.95F, slowdown));
    }

    private static float applyStartupSlowdown(float elapsedSeconds, float slowdown) {
        if (slowdown <= 0.0F) {
            return elapsedSeconds;
        }

        float slowWindowSeconds = com.kingdomcomecombat.combat.CombatControlConfig.PERFECT_COUNTER_SLOW_TICKS / 20.0F;
        float scale = 1.0F - slowdown;
        if (elapsedSeconds <= slowWindowSeconds) {
            return elapsedSeconds * scale;
        }

        return slowWindowSeconds * scale + (elapsedSeconds - slowWindowSeconds);
    }

    private static double animationTicks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return 0.0;
        }

        return client.world.getTime()
                + client.getRenderTickCounter().getTickProgress(false);
    }

    private static float getInterruptWeight(int entityId) {
        Double startedAt = INTERRUPT_FADE_STARTS.get(entityId);
        if (startedAt == null) {
            return 1.0F;
        }

        float elapsed = (float) ((animationTicks() - startedAt) / 20.0);
        return Math.max(0.0F, 1.0F - elapsed / FADE_SECONDS);
    }

    private static float getSlowedRawElapsedSeconds(int entityId, double startedAtTicks) {
        double now = animationTicks();
        ImpactSlowdown slowdown = ATTACK_IMPACT_SLOWDOWNS.get(entityId);
        if (slowdown == null) {
            return (float) ((now - startedAtTicks) / 20.0);
        }

        return (float) ((now - startedAtTicks - slowdown.lostTicks(now)) / 20.0);
    }

    private static boolean isImpactSlowdownActive(int entityId) {
        ImpactSlowdown slowdown = ATTACK_IMPACT_SLOWDOWNS.get(entityId);
        if (slowdown == null) return false;
        return animationTicks() < slowdown.endTicks();
    }

    public record ActiveAnimation(
            GeckoLikeAnimationLibrary.Kind kind,
            CombatDirection direction,
            float elapsedSeconds,
            float weight,
            String customAnimationName
    ) {
    }

    private record AttackAnimation(
            CombatDirection direction,
            float speedMultiplier,
            float startupSlowdown,
            double startedAtTicks,
            String animationName
    ) {
        float rawElapsedSeconds() {
            return (float) ((animationTicks() - startedAtTicks) / 20.0);
        }

        float elapsedSeconds(int entityId) {
            return applyStartupSlowdown(
                    getSlowedRawElapsedSeconds(entityId, startedAtTicks),
                    startupSlowdown
            ) * speedMultiplier;
        }

        float weight(float animationLengthSeconds, float elapsedSeconds) {
            float rawElapsed = rawElapsedSeconds();
            float fadeIn = Math.min(1.0F, rawElapsed / FADE_SECONDS);
            float fadeOut = Math.min(
                    1.0F,
                    Math.max(0.0F, animationLengthSeconds - elapsedSeconds) / FADE_SECONDS
            );
            return Math.max(0.0F, Math.min(fadeIn, fadeOut));
        }
    }

    private record StanceAnimation(
            CombatDirection direction,
            int targetEntityId,
            float speedMultiplier,
            double startedAtTicks,
            String animationName
    ) {
        float elapsedSeconds() {
            return (float) ((animationTicks() - startedAtTicks) / 20.0)
                    * speedMultiplier;
        }

        float weight() {
            float rawElapsed = (float) ((animationTicks() - startedAtTicks) / 20.0);
            return Math.min(1.0F, rawElapsed / FADE_SECONDS);
        }
    }

    private record HitReactionAnimation(
            CombatDirection direction,
            String animationName,
            double startedAtTicks,
            boolean holdLastFrame,
            float speedMultiplier
    ) {
        float elapsedSeconds() {
            return (float) ((animationTicks() - startedAtTicks) / 20.0)
                    * speedMultiplier;
        }

        float weight(float animationLengthSeconds) {
            float elapsed = elapsedSeconds();
            float fadeIn = Math.min(1.0F, elapsed / FADE_SECONDS);
            float fadeOut = Math.min(
                    1.0F,
                    Math.max(0.0F, animationLengthSeconds - elapsed) / FADE_SECONDS
            );
            return Math.max(0.0F, Math.min(fadeIn, fadeOut));
        }
    }

    private record BlockAnimation(
            GeckoLikeAnimationLibrary.Kind kind,
            CombatDirection direction,
            double startedAtTicks
    ) {
        float rawElapsedSeconds() {
            return (float) ((animationTicks() - startedAtTicks) / 20.0);
        }

        float elapsedSeconds(int entityId) {
            return getSlowedRawElapsedSeconds(entityId, startedAtTicks);
        }

        float lengthSeconds() {
            return kind == GeckoLikeAnimationLibrary.Kind.BLOCK
                    ? GeckoLikeAnimationLibrary.getLengthSeconds(kind, direction)
                    : GeckoLikeAnimationLibrary.getLengthSeconds(kind);
        }

        float weight(float lengthSeconds, float elapsed) {
            float fadeIn = 1.0F;
            float fadeOut = Math.min(1.0F, Math.max(0.0F, lengthSeconds - elapsed) / FADE_SECONDS);
            return Math.max(0.0F, Math.min(fadeIn, fadeOut));
        }
    }

    private record ImpactSlowdown(double startTicks, double endTicks) {
        double lostTicks(double now) {
            return Math.max(0.0, Math.min(now, endTicks) - startTicks);
        }
    }

    private static class ItemTransition {
        private GeckoLikeAnimationLibrary.BoneTransform displayed;
        private double updatedAtTicks;

        private ItemTransition(
                GeckoLikeAnimationLibrary.BoneTransform displayed,
                double updatedAtTicks
        ) {
            this.displayed = displayed;
            this.updatedAtTicks = updatedAtTicks;
        }
    }
}
