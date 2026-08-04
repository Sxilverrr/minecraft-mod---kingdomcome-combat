package com.kingdomcomecombat.client.feedback;

import com.kingdomcomecombat.client.animation.CombatAnimationClient;
import com.kingdomcomecombat.client.config.ClientServerConfigState;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.combat.DodgeDirection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class CombatHitFeedbackClient {
    private static final int CUSTOM_HIT_SLOW_TICKS = 24;
    private static final double CUSTOM_HIT_MOVEMENT_MULTIPLIER = 0.60D;

    /**
     * 小幅震动持续时间。
     */
    private static final int SHAKE_TOTAL_TICKS = 12;
    private static final int RED_FLASH_TOTAL_TICKS = 11;

    private static int hitStopTicks = 0;
    private static int shakeTicks = 0;
    private static int redFlashTicks = 0;
    private static int cameraRecoilTicks = 0;
    private static int customHitSlowTicks = 0;
    private static float hitFeedbackStrength = 1.0F;
    private static int dodgeVisualTicks = 0;
    private static final int DODGE_VISUAL_TOTAL_TICKS = 18;
    private static CombatDirection hitDirection = CombatDirection.RIGHT;
    private static DodgeDirection dodgeDirection = DodgeDirection.BACK;
    private static int predictedHitTicks = 0;

    public static void startPredictedHitFeedback(CombatDirection direction) {
        hitDirection = direction;
        hitFeedbackStrength = 0.55F;
        int stopTicks = ClientServerConfigState.hitStopTicks();
        hitStopTicks = Math.max(hitStopTicks, stopTicks);
        shakeTicks = Math.max(shakeTicks, SHAKE_TOTAL_TICKS);
        cameraRecoilTicks = Math.max(cameraRecoilTicks, 10);
        predictedHitTicks = 20;
        startLocalHitStop(stopTicks);
    }

    public static void startHitFeedback(CombatDirection direction, boolean penetratedArmor) {
        startHitFeedback(direction, penetratedArmor, 1.0F);
    }

    public static void startHitFeedback(CombatDirection direction, boolean penetratedArmor, float feedbackScale) {
        boolean confirmsPrediction = predictedHitTicks > 0 && hitDirection == direction;
        hitDirection = direction;
        hitFeedbackStrength = (penetratedArmor ? 0.92F : 0.55F) * MathHelper.clamp(feedbackScale, 0.1F, 3.0F);
        int stopTicks = confirmsPrediction ? Math.min(1, ClientServerConfigState.hitStopTicks()) : ClientServerConfigState.hitStopTicks();
        hitStopTicks = confirmsPrediction ? Math.max(hitStopTicks, stopTicks) : stopTicks;
        shakeTicks = confirmsPrediction ? Math.max(shakeTicks, 5) : SHAKE_TOTAL_TICKS;
        customHitSlowTicks = Math.max(customHitSlowTicks, CUSTOM_HIT_SLOW_TICKS);
        if (penetratedArmor && feedbackScale <= 1.0F) {
            redFlashTicks = Math.max(redFlashTicks, RED_FLASH_TOTAL_TICKS);
        }
        cameraRecoilTicks = Math.max(cameraRecoilTicks, 10);
        predictedHitTicks = 0;

        startLocalHitStop(stopTicks);
    }

    public static void startAnimationImpactStop() {
        int stopTicks = ClientServerConfigState.hitStopTicks();
        hitStopTicks = Math.max(hitStopTicks, stopTicks);
        shakeTicks = Math.max(shakeTicks, 5);
        startLocalHitStop(stopTicks);
    }

    public static void startDodgeFeedback(DodgeDirection direction) {
        dodgeDirection = direction;
        dodgeVisualTicks = DODGE_VISUAL_TOTAL_TICKS;
    }

    public static boolean isCameraHitFeedbackActive() {
        return shakeTicks > 0 || cameraRecoilTicks > 0;
    }

    public static float getCameraRollDegrees(float tickDelta) {
        if (shakeTicks <= 0) {
            return 0.0F;
        }

        float progress = MathHelper.clamp((shakeTicks - tickDelta) / SHAKE_TOTAL_TICKS, 0.0F, 1.0F);
        float signed = switch (hitDirection) {
            case LEFT -> -1.0F;
            case RIGHT -> 1.0F;
            case UP -> 0.45F;
            case DOWN -> -0.45F;
        };

        return signed * 6.2F * hitFeedbackStrength * progress * progress;
    }

    public static float getCameraYawDegrees(float tickDelta) {
        float recoil = getCameraRecoilWave(tickDelta);
        if (recoil <= 0.0F) {
            return 0.0F;
        }

        float signed = switch (hitDirection) {
            case LEFT -> -1.0F;
            case RIGHT -> 1.0F;
            case UP -> 0.25F;
            case DOWN -> -0.25F;
        };

        return signed * 2.4F * hitFeedbackStrength * recoil;
    }

    public static float getCameraPitchDegrees(float tickDelta) {
        float recoil = getCameraRecoilWave(tickDelta);
        if (recoil <= 0.0F) {
            return 0.0F;
        }

        float signed = switch (hitDirection) {
            case UP -> -1.0F;
            case DOWN -> 1.0F;
            case LEFT, RIGHT -> 0.35F;
        };

        return signed * 1.4F * hitFeedbackStrength * recoil;
    }

    public static float getHitCameraXOffset(float tickDelta) {
        if (shakeTicks <= 0) {
            return 0.0F;
        }

        float signed = switch (hitDirection) {
            case LEFT -> 1.0F;
            case RIGHT -> -1.0F;
            case UP -> -0.25F;
            case DOWN -> 0.25F;
        };
        return signed * 0.070F * hitFeedbackStrength * getHitDisplacementWave(tickDelta);
    }

    public static float getHitCameraYOffset(float tickDelta) {
        if (shakeTicks <= 0) {
            return 0.0F;
        }

        float signed = switch (hitDirection) {
            case UP -> -1.0F;
            case DOWN -> 1.0F;
            case LEFT, RIGHT -> 0.18F;
        };
        return signed * 0.055F * hitFeedbackStrength * getHitDisplacementWave(tickDelta);
    }

    public static float getHitCameraZOffset(float tickDelta) {
        if (shakeTicks <= 0) {
            return 0.0F;
        }

        return -0.050F * hitFeedbackStrength * getHitDisplacementWave(tickDelta);
    }

    public static double getCustomHitMovementMultiplier() {
        return customHitSlowTicks > 0 ? CUSTOM_HIT_MOVEMENT_MULTIPLIER : 1.0D;
    }

    public static float getRedFlashAlpha(float tickDelta) {
        if (redFlashTicks <= 0) {
            return 0.0F;
        }

        float progress = MathHelper.clamp(
                (redFlashTicks - tickDelta) / (float) RED_FLASH_TOTAL_TICKS,
                0.0F,
                1.0F
        );
        return 0.34F * progress * progress;
    }

    public static float getDodgeCameraRollDegrees(float tickDelta) {
        if (dodgeVisualTicks <= 0) {
            return 0.0F;
        }

        float wave = getDodgeVisualWave(tickDelta);
        float signed = switch (dodgeDirection) {
            case LEFT -> -1.0F;
            case RIGHT -> 1.0F;
            case BACK -> 0.35F;
            case FORWARD -> -0.25F;
        };

        return signed * 5.0F * wave;
    }

    public static float getDodgeCameraYOffset(float tickDelta) {
        if (dodgeVisualTicks <= 0) {
            return 0.0F;
        }

        float amount = switch (dodgeDirection) {
            case BACK -> 0.10F;
            case FORWARD -> -0.08F;
            case LEFT, RIGHT -> -0.24F;
        };

        return amount * getDodgeVisualWave(tickDelta);
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        if (hitStopTicks > 0) {
            hitStopTicks--;
        }

        if (shakeTicks > 0) {
            shakeTicks--;
        }

        if (redFlashTicks > 0) {
            redFlashTicks--;
        }

        if (cameraRecoilTicks > 0) {
            cameraRecoilTicks--;
        }

        if (customHitSlowTicks > 0) {
            customHitSlowTicks--;
        }

        if (dodgeVisualTicks > 0) {
            dodgeVisualTicks--;
        }
        if (predictedHitTicks > 0) {
            predictedHitTicks--;
        }
    }

    private static float getHitDisplacementWave(float tickDelta) {
        float age = SHAKE_TOTAL_TICKS - Math.max(0.0F, shakeTicks - tickDelta);
        float progress = MathHelper.clamp(age / SHAKE_TOTAL_TICKS, 0.0F, 1.0F);
        float impact = 1.0F - progress;
        float rebound = (float) Math.sin(progress * Math.PI) * 0.18F;
        return MathHelper.clamp(impact * impact + rebound, 0.0F, 1.0F);
    }

    private static float getDodgeVisualWave(float tickDelta) {
        float age = DODGE_VISUAL_TOTAL_TICKS - Math.max(0.0F, dodgeVisualTicks - tickDelta);
        float progress = MathHelper.clamp(age / DODGE_VISUAL_TOTAL_TICKS, 0.0F, 1.0F);
        return (float) Math.sin(progress * Math.PI);
    }

    private static float getCameraRecoilWave(float tickDelta) {
        if (cameraRecoilTicks <= 0) {
            return 0.0F;
        }

        float remaining = MathHelper.clamp(
                (cameraRecoilTicks - tickDelta) / (float) 10,
                0.0F,
                1.0F
        );
        return remaining * remaining * (3.0F - 2.0F * remaining);
    }

    private static void startLocalHitStop(int ticks) {
        CombatAnimationClient.startLocalHitStop(ticks);
    }
}
