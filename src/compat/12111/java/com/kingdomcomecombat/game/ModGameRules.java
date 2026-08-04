package com.kingdomcomecombat.game;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.network.HardcoreModeSyncPayload;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.rule.GameRule;
import net.minecraft.world.rule.GameRuleCategory;
import net.minecraft.world.rule.GameRules;

public final class ModGameRules {
    public static final GameRule<Boolean> CLASSIC_MODE = boolRule("kcc_classic_mode", true);
    public static final GameRule<Boolean> PLAYER_DIRECTIONAL_BLOCKING = boolRule("kcc_player_directional_blocking", true);
    public static final GameRule<Double> COMBAT_SPEED = doubleRule("kcc_combat_speed", 1.0);
    public static final GameRule<Boolean> TRAUMA = boolRule("kcc_trauma", true);
    public static final GameRule<Boolean> HARDCORE_MODE = boolRule("kcc_hardcore_mode", false);
    private ModGameRules() {}
    private static GameRule<Boolean> boolRule(String id, boolean value) {
        return Registry.register(Registries.GAME_RULE, Identifier.of(KingdomComeCombat.MOD_ID, id),
                new GameRule<>(GameRuleCategory.PLAYER, net.minecraft.world.rule.GameRuleType.BOOL,
                        BoolArgumentType.bool(), (visitor, rule) -> visitor.visitBoolean(rule), Codec.BOOL,
                        v -> v ? 1 : 0, value, FeatureFlags.VANILLA_FEATURES));
    }
    private static GameRule<Double> doubleRule(String id, double value) {
        return Registry.register(Registries.GAME_RULE, Identifier.of(KingdomComeCombat.MOD_ID, id),
                new GameRule<>(GameRuleCategory.PLAYER, net.minecraft.world.rule.GameRuleType.INT,
                        DoubleArgumentType.doubleArg(0.05, 10.0), (visitor, rule) -> visitor.visit(rule), Codec.DOUBLE,
                        Double::intValue, value, FeatureFlags.VANILLA_FEATURES));
    }
    public static void register() { ServerPlayConnectionEvents.JOIN.register((h, s, server) -> sync(h.player)); }
    public static void initializeForServer(MinecraftServer server) {
        if (server.isHardcore()) server.getOverworld().getGameRules().setValue(HARDCORE_MODE, true, server);
        for (var player : server.getPlayerManager().getPlayerList()) sync(player);
    }
    public static boolean traumaEnabled(LivingEntity e) { return !(e instanceof PlayerEntity) || traumaEnabled(e.getEntityWorld()); }
    public static boolean traumaEnabled(World w) { return !(w instanceof ServerWorld sw) || sw.getGameRules().getValue(TRAUMA); }
    public static boolean hardcoreMode(ServerWorld w) { return w.getServer().isHardcore() || w.getGameRules().getValue(HARDCORE_MODE); }
    public static boolean hardcoreMode(GameRules rules) { return rules.getValue(HARDCORE_MODE); }
    /** Updates world-creation rules before a MinecraftServer exists. */
    public static void setHardcoreMode(GameRules rules, boolean enabled) { rules.setValue(HARDCORE_MODE, enabled, null); }
    public static boolean classicMode(World w) { return w instanceof ServerWorld sw && sw.getGameRules().getValue(CLASSIC_MODE); }
    public static boolean classicMode(LivingEntity e) { return e != null && classicMode(e.getEntityWorld()); }
    public static boolean playerDirectionalBlocking(World w) { return !(w instanceof ServerWorld sw) || sw.getGameRules().getValue(PLAYER_DIRECTIONAL_BLOCKING); }
    public static double combatSpeed(World w) { return w instanceof ServerWorld sw ? Math.max(.05, Math.min(10, sw.getGameRules().getValue(COMBAT_SPEED))) : 1; }
    public static double combatSpeed(LivingEntity e) { return e == null ? 1 : combatSpeed(e.getEntityWorld()); }
    public static double combatSpeed(ServerWorld w) { return combatSpeed((World) w); }
    private static void sync(net.minecraft.server.network.ServerPlayerEntity p) { ServerPlayNetworking.send(p, new HardcoreModeSyncPayload(hardcoreMode((ServerWorld)p.getEntityWorld()))); }
}
