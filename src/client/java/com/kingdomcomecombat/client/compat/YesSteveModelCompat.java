package com.kingdomcomecombat.client.compat;

import com.kingdomcomecombat.KingdomComeCombat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

/** Optional bridge into YSM's swing-driven compatibility controller. */
public final class YesSteveModelCompat {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("yes_steve_model");

    private YesSteveModelCompat() {
    }

    public static void register() {
        if (LOADED) {
            KingdomComeCombat.LOGGER.info("Enabled Yes Steve Model combat animation compatibility.");
        }
    }

    public static void triggerAttack(Entity entity) {
        if (LOADED && entity instanceof PlayerEntity player) {
            player.swingHand(Hand.MAIN_HAND);
        }
    }
}
