package com.kingdomcomecombat.client.combat;

import net.minecraft.client.MinecraftClient;

public final class MountedAttackCooldown {
    private static final int COOLDOWN_TICKS = 16;
    private static long lastAttackTick = Long.MIN_VALUE;

    private MountedAttackCooldown() {
    }

    public static boolean isCoolingDown() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || lastAttackTick == Long.MIN_VALUE) {
            return false;
        }
        return client.player.getWorld().getTime() - lastAttackTick < COOLDOWN_TICKS;
    }

    public static void markAttack(long worldTime) {
        lastAttackTick = worldTime;
    }
}
