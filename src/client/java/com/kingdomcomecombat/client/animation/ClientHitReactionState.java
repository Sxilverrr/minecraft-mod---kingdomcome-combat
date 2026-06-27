package com.kingdomcomecombat.client.animation;

import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ClientHitReactionState {
    private static final float DURATION_SECONDS = 0.40F;
    private static final float AMPLITUDE_MULTIPLIER = 0.60F;
    private static final Map<Integer, Reaction> REACTIONS = new HashMap<>();

    private ClientHitReactionState() {
    }

    public static void start(
            int entityId,
            HumanoidHurtboxLibrary.Part part,
            String detailedPart,
            CombatDirection attackDirection,
            boolean strong
    ) {
        start(entityId, part, detailedPart, attackDirection, strong, strong ? 1.75F : 1.0F);
    }

    public static void start(
            int entityId,
            HumanoidHurtboxLibrary.Part part,
            String detailedPart,
            CombatDirection attackDirection,
            boolean strong,
            float strength
    ) {
        REACTIONS.put(entityId, new Reaction(
                part,
                sanitizeDetailedPart(detailedPart),
                attackDirection,
                Math.max(0.25F, Math.min(1.85F, strength)),
                animationTicks()
        ));
    }

    public static BoneDelta getDelta(int entityId, HumanoidHurtboxLibrary.Part part) {
        Reaction reaction = REACTIONS.get(entityId);
        if (reaction == null) {
            return BoneDelta.ZERO;
        }

        float age = reaction.ageSeconds();
        float durationSeconds = durationSeconds();
        if (age >= durationSeconds) {
            REACTIONS.remove(entityId);
            return BoneDelta.ZERO;
        }

        float progress = age / durationSeconds;
        float strength = progress < 0.48F
                ? 1.0F - progress / 0.48F
                : -0.32F * (1.0F - (progress - 0.48F) / 0.52F);
        float influence = influence(reaction, part);
        if (influence <= 0.0F) {
            return BoneDelta.ZERO;
        }

        float amount = strength
                * influence
                * reaction.strength()
                * AMPLITUDE_MULTIPLIER
                * (float) CombatClientConfig.hitReactionAnimationStrength();

        return deltaFor(reaction, part, amount);
    }

    public static BoneDelta getTorsoDrivenDelta(int entityId) {
        Reaction reaction = REACTIONS.get(entityId);
        if (reaction == null) {
            return BoneDelta.ZERO;
        }

        float age = reaction.ageSeconds();
        float durationSeconds = durationSeconds();
        if (age >= durationSeconds) {
            REACTIONS.remove(entityId);
            return BoneDelta.ZERO;
        }

        float progress = age / durationSeconds;
        float strength = progress < 0.48F
                ? 1.0F - progress / 0.48F
                : -0.32F * (1.0F - (progress - 0.48F) / 0.52F);
        float amount = strength
                * reaction.strength()
                * AMPLITUDE_MULTIPLIER
                * (float) CombatClientConfig.hitReactionAnimationStrength();
        return deltaFor(reaction, HumanoidHurtboxLibrary.Part.BODY, amount);
    }

    public static void tickCleanup() {
        MinecraftClient client = MinecraftClient.getInstance();
        Iterator<Map.Entry<Integer, Reaction>> iterator = REACTIONS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Integer, Reaction> entry = iterator.next();
            if (entry.getValue().ageSeconds() > durationSeconds()
                    || client.world == null
                    || client.world.getEntityById(entry.getKey()) == null) {
                iterator.remove();
            }
        }
    }

    private static float durationSeconds() {
        return CombatClientConfig.hitReactionReturnTicks() / 20.0F;
    }

    private static float influence(
            Reaction reaction,
            HumanoidHurtboxLibrary.Part animatedPart
    ) {
        HumanoidHurtboxLibrary.Part hitPart = reaction.part();
        CombatDirection attackDirection = reaction.attackDirection();

        if (hitPart == animatedPart) {
            return 1.0F;
        }

        float directional = directionalInfluence(attackDirection, animatedPart);
        float zone = switch (hitPart) {
            case HEAD -> switch (animatedPart) {
                case HEAD -> 1.0F;
                case SHOULDERS, BODY -> 0.48F;
                case LEFT_ARM, RIGHT_ARM -> 0.22F;
                default -> 0.08F;
            };
            case SHOULDERS -> switch (animatedPart) {
                case SHOULDERS -> 1.0F;
                case BODY -> 0.62F;
                case LEFT_ARM, RIGHT_ARM -> 0.70F;
                case HEAD -> 0.28F;
                default -> 0.14F;
            };
            case BODY -> switch (animatedPart) {
                case BODY -> 0.82F;
                case SHOULDERS -> 0.72F;
                case HEAD -> 0.32F;
                case LEFT_ARM, RIGHT_ARM -> 0.82F;
                default -> 0.18F;
            };
            case LOWER -> switch (animatedPart) {
                case LEFT_LEG, RIGHT_LEG -> 0.86F;
                case BODY -> 0.40F;
                case LEFT_ARM, RIGHT_ARM -> 0.15F;
                default -> 0.08F;
            };
            case LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG -> hitPart == animatedPart ? 1.0F : 0.25F;
        };

        return Math.max(zone, directional);
    }

    private static float directionalInfluence(
            CombatDirection attackDirection,
            HumanoidHurtboxLibrary.Part animatedPart
    ) {
        if (attackDirection == CombatDirection.RIGHT) {
            return switch (animatedPart) {
                case LEFT_ARM -> 1.0F;
                case SHOULDERS -> 0.72F;
                case BODY -> 0.68F;
                case HEAD -> 0.32F;
                case RIGHT_ARM -> 0.28F;
                case LEFT_LEG, RIGHT_LEG, LOWER -> 0.16F;
            };
        }

        if (attackDirection == CombatDirection.LEFT) {
            return switch (animatedPart) {
                case RIGHT_ARM -> 1.0F;
                case SHOULDERS -> 0.72F;
                case BODY -> 0.68F;
                case HEAD -> 0.32F;
                case LEFT_ARM -> 0.28F;
                case LEFT_LEG, RIGHT_LEG, LOWER -> 0.16F;
            };
        }

        return switch (animatedPart) {
            case HEAD -> attackDirection == CombatDirection.UP ? 0.9F : 0.35F;
            case SHOULDERS -> 0.70F;
            case BODY -> 0.62F;
            case LEFT_ARM, RIGHT_ARM -> 0.48F;
            case LEFT_LEG, RIGHT_LEG, LOWER -> attackDirection == CombatDirection.DOWN ? 0.72F : 0.16F;
        };
    }

    private static BoneDelta deltaFor(
            Reaction reaction,
            HumanoidHurtboxLibrary.Part animatedPart,
            float amount
    ) {
        float signed = signedDirection(reaction.attackDirection());
        float forward = reaction.attackDirection() == CombatDirection.DOWN ? 1.0F : -1.0F;

        if (reaction.part() == HumanoidHurtboxLibrary.Part.HEAD
                && animatedPart == HumanoidHurtboxLibrary.Part.HEAD) {
            return switch (reaction.detailedPart()) {
                case "face" -> new BoneDelta(
                        signed * 1.8F * amount,
                        0.0F,
                        3.6F * amount,
                        -34.0F * amount,
                        signed * 8.0F * amount,
                        signed * 6.0F * amount
                );
                case "crown" -> new BoneDelta(
                        signed * 1.2F * amount,
                        1.0F * amount,
                        -1.8F * amount,
                        30.0F * amount,
                        signed * 5.0F * amount,
                        signed * 4.0F * amount
                );
                case "side_head" -> new BoneDelta(
                        signed * 4.4F * amount,
                        0.0F,
                        -0.8F * amount,
                        -6.0F * amount,
                        signed * 68.0F * amount,
                        signed * 18.0F * amount
                );
                default -> genericDelta(signed, forward, amount);
            };
        }

        return genericDelta(signed, forward, amount);
    }

    private static BoneDelta genericDelta(float signed, float forward, float amount) {
        return new BoneDelta(
                signed * 5.2F * amount,
                0.0F,
                forward * 2.4F * amount,
                -forward * 18.0F * amount,
                signed * 48.0F * amount,
                signed * 20.0F * amount
        );
    }

    private static float signedDirection(CombatDirection attackDirection) {
        return switch (attackDirection) {
            case LEFT -> -1.0F;
            case RIGHT -> 1.0F;
            case UP -> 0.35F;
            case DOWN -> -0.35F;
        };
    }

    private static String sanitizeDetailedPart(String detailedPart) {
        return detailedPart == null || detailedPart.isBlank() ? "" : detailedPart;
    }

    public record BoneDelta(
            float positionX,
            float positionY,
            float positionZ,
            float rotationX,
            float rotationY,
            float rotationZ
    ) {
        static final BoneDelta ZERO = new BoneDelta(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    private record Reaction(
            HumanoidHurtboxLibrary.Part part,
            String detailedPart,
            CombatDirection attackDirection,
            float strength,
            double startedAtTicks
    ) {
        float ageSeconds() {
            return (float) ((animationTicks() - startedAtTicks) / 20.0);
        }
    }

    private static double animationTicks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return 0.0;
        }

        return client.world.getTime()
                + client.getRenderTickCounter().getTickProgress(false);
    }
}
