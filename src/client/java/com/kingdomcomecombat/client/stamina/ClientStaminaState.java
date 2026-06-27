package com.kingdomcomecombat.client.stamina;

public class ClientStaminaState {
    private static float current = 100.0F;
    private static float max = 100.0F;

    private ClientStaminaState() {
    }

    public static void update(float currentStamina, float maxStamina) {
        max = Math.max(1.0F, maxStamina);
        current = clamp(currentStamina, 0.0F, max);
    }

    public static float current() {
        return current;
    }

    public static float progress() {
        return clamp(current / Math.max(1.0F, max), 0.0F, 1.0F);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
