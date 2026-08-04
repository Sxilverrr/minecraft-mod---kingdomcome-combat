package com.kingdomcomecombat;

import com.kingdomcomecombat.ai.HumanoidCombatAiTicker;
import com.kingdomcomecombat.ai.BeastCombatAiTicker;
import com.kingdomcomecombat.ai.ExperimentalFactionHostility;
import com.kingdomcomecombat.command.CombatCommands;
import com.kingdomcomecombat.combat.PlayerComboProgress;
import com.kingdomcomecombat.config.CombatServerConfig;
import com.kingdomcomecombat.combat.ServerCombatTicker;
import com.kingdomcomecombat.data.CombatDataReloadListener;
import com.kingdomcomecombat.injury.InjuryTicker;
import com.kingdomcomecombat.injury.ModStatusEffects;
import com.kingdomcomecombat.network.CombatNetworking;
import com.kingdomcomecombat.passive.PlayerPassiveSkillProgress;
import com.kingdomcomecombat.item.SkillBookAcquisition;
import com.kingdomcomecombat.item.SkillBookItem;
import com.kingdomcomecombat.entity.ModEntities;
import com.kingdomcomecombat.game.ModGameRules;
import com.kingdomcomecombat.hardship.HardshipSelectionState;
import com.kingdomcomecombat.hardship.HardshipEffects;
import com.kingdomcomecombat.item.HandCannonProjectileTracker;
import com.kingdomcomecombat.particle.ModParticles;
import com.kingdomcomecombat.potion.PotionCoatingHandler;
import com.kingdomcomecombat.recipe.ModRecipes;
import com.kingdomcomecombat.sound.ModSounds;
import com.kingdomcomecombat.boss.EnderDragonBossHandler;
import com.kingdomcomecombat.boss.WitherBossHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.kingdomcomecombat.item.ModItems;
public class KingdomComeCombat implements ModInitializer {
	public static final String MOD_ID = "kingdom_come_combat";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CombatServerConfig.load();
		ModStatusEffects.registerAll();
		ModGameRules.register();
		ModSounds.registerAll();
		ModParticles.registerAll();
		ModEntities.register();
		ModRecipes.register();
		CombatNetworking.registerCommon();
		ResourceManagerHelper.get(ResourceType.SERVER_DATA)
				.registerReloadListener(new CombatDataReloadListener());
		ServerCombatTicker.register();
		HumanoidCombatAiTicker.register();
		BeastCombatAiTicker.register();
		ExperimentalFactionHostility.register();
		InjuryTicker.register();
		HardshipEffects.register();
		PotionCoatingHandler.register();
		PlayerComboProgress.register();
		PlayerPassiveSkillProgress.register();
		CombatCommands.register();
		ModItems.registerModItems();
		SkillBookItem.registerHeldLocalization();
		HandCannonProjectileTracker.register();
		EnderDragonBossHandler.register();
		WitherBossHandler.register();
		SkillBookAcquisition.register();
		ServerLifecycleEvents.SERVER_STARTING.register(server -> clearPlayerProgressCaches());
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			CombatDataReloadListener.reapplyWeaponDefinitionsAfterTagsBound();
			ModGameRules.initializeForServer(server);
		});
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
			if (success) {
				CombatDataReloadListener.reapplyWeaponDefinitionsAfterTagsBound();
				server.getPlayerManager().getPlayerList().forEach(player -> {
					CombatNetworking.syncDataDrivenClientState(player);
					PlayerPassiveSkillProgress.sync(player);
					HardshipSelectionState.onJoin(player);
				});
			}
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			clearPlayerProgressCaches();
			HardshipSelectionState.clear(server);
		});

		LOGGER.info("Kingdom Come Combat initialized.");
	}

	private static void clearPlayerProgressCaches() {
		PlayerComboProgress.clearCache();
		PlayerPassiveSkillProgress.clearCache();
	}
}
