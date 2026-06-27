package com.kingdomcomecombat.game;

import com.kingdomcomecombat.network.HardcoreModeSyncPayload;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.fabric.api.gamerule.v1.rule.DoubleRule;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

public class ModGameRules {
    public static final GameRules.Key<GameRules.BooleanRule> CLASSIC_MODE =
            GameRuleRegistry.register(
                    "kccClassicMode",
                    GameRules.Category.PLAYER,
                    GameRuleFactory.createBooleanRule(true)
            );

    public static final GameRules.Key<DoubleRule> COMBAT_SPEED =
            GameRuleRegistry.register(
                    "kccCombatSpeed",
                    GameRules.Category.PLAYER,
                    GameRuleFactory.createDoubleRule(1.0, 0.05, 10.0)
            );

    public static final GameRules.Key<GameRules.BooleanRule> TRAUMA =
            GameRuleRegistry.register(
                    "kccTrauma",
                    GameRules.Category.PLAYER,
                    GameRuleFactory.createBooleanRule(true)
            );

    public static final GameRules.Key<GameRules.BooleanRule> HARDCORE_MODE =
            GameRuleRegistry.register(
                    "kccHardcoreMode",
                    GameRules.Category.PLAYER,
                    GameRuleFactory.createBooleanRule(false, (server, rule) -> syncHardcoreMode(server))
            );

    private ModGameRules() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                syncHardcoreMode(handler.player));
    }

    public static void initializeForServer(MinecraftServer server) {
        if (server.isHardcore() && !server.getGameRules().getBoolean(HARDCORE_MODE)) {
            server.getGameRules().get(HARDCORE_MODE).set(true, server);
        }
    }

    public static boolean traumaEnabled(LivingEntity entity) {
        return !(entity instanceof PlayerEntity) || traumaEnabled(entity.getWorld());
    }

    public static boolean traumaEnabled(World world) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return true;
        }
        return serverWorld.getGameRules().getBoolean(TRAUMA);
    }

    public static boolean hardcoreMode(ServerWorld world) {
        return world.getGameRules().getBoolean(HARDCORE_MODE);
    }

    private static void syncHardcoreMode(MinecraftServer server) {
        for (var player : server.getPlayerManager().getPlayerList()) {
            syncHardcoreMode(player);
        }
    }

    private static void syncHardcoreMode(net.minecraft.server.network.ServerPlayerEntity player) {
        ServerPlayNetworking.send(
                player,
                new HardcoreModeSyncPayload(hardcoreMode((ServerWorld) player.getWorld()))
        );
    }

    public static boolean classicMode(World world) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return false;
        }

        return serverWorld.getGameRules().getBoolean(CLASSIC_MODE);
    }

    public static boolean classicMode(LivingEntity entity) {
        return entity != null && classicMode(entity.getWorld());
    }

    public static double combatSpeed(World world) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return 1.0;
        }

        return sanitizeCombatSpeed(serverWorld.getGameRules().get(COMBAT_SPEED).get());
    }

    public static double combatSpeed(LivingEntity entity) {
        return entity == null ? 1.0 : combatSpeed(entity.getWorld());
    }

    public static double combatSpeed(ServerWorld world) {
        return combatSpeed((World) world);
    }

    private static double sanitizeCombatSpeed(double value) {
        if (!Double.isFinite(value)) {
            return 1.0;
        }

        return Math.max(0.05, Math.min(10.0, value));
    }
}
