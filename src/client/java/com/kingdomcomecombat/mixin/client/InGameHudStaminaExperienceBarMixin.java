package com.kingdomcomecombat.mixin.client;

import com.kingdomcomecombat.client.lockon.LockOnState;
import com.kingdomcomecombat.client.stamina.ClientStaminaState;
import com.kingdomcomecombat.riding.KccHorseRidingData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.passive.AbstractHorseEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudStaminaExperienceBarMixin {
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;

    @Inject(method = "render", at = @At("TAIL"))
    private void kingdomcomecombat$renderYellowStaminaExperienceBar(
            DrawContext context,
            RenderTickCounter tickCounter,
            CallbackInfo ci
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        int x = (context.getScaledWindowWidth() - BAR_WIDTH) / 2;
        int y = context.getScaledWindowHeight() - 29;
        int nextY = y;

        if (client.player.getVehicle() instanceof AbstractHorseEntity horse
                && (Object) horse instanceof KccHorseRidingData horseData) {
            renderBar(context, x, nextY, horseData.kingdomcomecombat$getHorseCurrentSpeed(), 0xFF0B1C28, 0xFF59C7FF, 0xFF1877B8);
            nextY -= 8;

            double maxStamina = horseData.kingdomcomecombat$getHorseMaxStamina();
            double staminaProgress = maxStamina <= 0.0 ? 0.0 : horseData.kingdomcomecombat$getHorseStamina() / maxStamina;
            renderBar(context, x, nextY, staminaProgress, 0xFF26230D, 0xFFFFE36E, 0xFFC99B13);
            nextY -= 8;
        }

        if (LockOnState.locked) {
            renderBar(context, x, nextY, ClientStaminaState.progress(), 0xFF2D2608, 0xFFFFD447, 0xFFC88600);
        }
    }

    private static void renderBar(
            DrawContext context,
            int x,
            int y,
            double progress,
            int backgroundColor,
            int topColor,
            int bottomColor
    ) {
        int filled = Math.round((float) Math.max(0.0, Math.min(1.0, progress)) * (BAR_WIDTH + 1));
        int fillRight = x + Math.max(0, Math.min(BAR_WIDTH, filled));

        context.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, 0xCC000000);
        context.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, backgroundColor);
        if (fillRight > x) {
            context.fill(x, y, fillRight, y + 4, topColor);
            context.fill(x, y + 4, fillRight, y + BAR_HEIGHT, bottomColor);
        }
    }
}
