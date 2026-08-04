package com.kingdomcomecombat.client.stamina;

import com.kingdomcomecombat.client.lockon.LockOnState;
import com.kingdomcomecombat.client.game.ClientGameRuleState;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

public class StaminaExperienceBarDisplay {
    private static boolean overridingExperience = false;
    private static int savedExperienceLevel = 0;
    private static int savedTotalExperience = 0;
    private static float savedExperienceProgress = 0.0F;

    private StaminaExperienceBarDisplay() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(StaminaExperienceBarDisplay::tick);
    }

    private static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;

        if (player == null) {
            overridingExperience = false;
            return;
        }

        if (client.options.hudHidden || !LockOnState.locked || ClientGameRuleState.hardcoreMode()) {
            restoreExperience(player);
            return;
        }

        if (!overridingExperience) {
            savedExperienceLevel = player.experienceLevel;
            savedTotalExperience = player.totalExperience;
            savedExperienceProgress = player.experienceProgress;
            overridingExperience = true;
        }

        player.experienceLevel = Math.round(ClientStaminaState.current());
        player.totalExperience = savedTotalExperience;
        player.experienceProgress = ClientStaminaState.progress();
    }

    private static void restoreExperience(ClientPlayerEntity player) {
        if (!overridingExperience) {
            return;
        }

        player.experienceLevel = savedExperienceLevel;
        player.totalExperience = savedTotalExperience;
        player.experienceProgress = savedExperienceProgress;
        overridingExperience = false;
    }

    public static void reset() {
        overridingExperience = false;
    }
}
