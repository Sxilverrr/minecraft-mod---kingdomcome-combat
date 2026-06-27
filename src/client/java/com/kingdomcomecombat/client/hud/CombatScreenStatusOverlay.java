package com.kingdomcomecombat.client.hud;

import com.kingdomcomecombat.client.feedback.CombatHitFeedbackClient;
import com.kingdomcomecombat.client.stamina.ClientStaminaState;
import com.kingdomcomecombat.injury.ModStatusEffects;
import com.kingdomcomecombat.config.CombatClientConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;

public class CombatScreenStatusOverlay {
    private CombatScreenStatusOverlay() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(CombatScreenStatusOverlay::renderHud);
    }

    private static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || !CombatClientConfig.screenEffectsEnabled()) {
            return;
        }

        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        float tickDelta = tickCounter.getTickProgress(true);

        float staminaProgress = ClientStaminaState.progress();
        double blueStrength = CombatClientConfig.screenBlueEffectStrength();
        if (staminaProgress < 0.35F && blueStrength > 0.0) {
            float danger = MathHelper.clamp((0.35F - staminaProgress) / 0.35F, 0.0F, 1.0F);
            int alpha = MathHelper.clamp(Math.round((float) (96.0F * danger * danger * blueStrength)), 0, 255);
            context.fill(0, 0, width, height, (alpha << 24) | 0x164DFF);
        }

        float redAlpha = CombatHitFeedbackClient.getRedFlashAlpha(tickDelta);
        double redStrength = CombatClientConfig.screenRedEffectStrength();
        if (redAlpha > 0.0F && redStrength > 0.0) {
            int alpha = MathHelper.clamp(Math.round((float) (255.0F * redAlpha * redStrength)), 0, 255);
            context.fill(0, 0, width, height, (alpha << 24) | 0xFF1E1E);
        }

        int headInjury = ModStatusEffects.effectiveLevel(client.player, ModStatusEffects.HEAD_INJURY);
        if (headInjury > 0) {
            int alpha = Math.min(86, 14 + headInjury * 8);
            context.fill(0, 0, width, height, (alpha << 24) | 0xA80018);
        }
    }
}
