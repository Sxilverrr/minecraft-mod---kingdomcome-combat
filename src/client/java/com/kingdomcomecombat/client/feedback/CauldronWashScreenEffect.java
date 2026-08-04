package com.kingdomcomecombat.client.feedback;

import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.client.gui.DrawContext;

public final class CauldronWashScreenEffect {
    private static final int TOTAL_TICKS = 24;
    private static int ticksRemaining = 0;

    private CauldronWashScreenEffect() {
    }

    public static void start() {
        if (CombatClientConfig.cauldronFaceWashEnabled()) {
            ticksRemaining = TOTAL_TICKS;
        }
    }

    public static void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
    }

    public static void render(DrawContext context) {
        if (ticksRemaining <= 0) {
            return;
        }

        float progress = 1.0F - ticksRemaining / (float) TOTAL_TICKS;
        float wave = (float) Math.sin(progress * Math.PI);
        int alpha = Math.max(0, Math.min(220, Math.round(wave * 220.0F)));
        int color = alpha << 24;
        context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), color);
    }
}
