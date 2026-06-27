package com.kingdomcomecombat.stamina;

import com.kingdomcomecombat.network.StaminaSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ServerStaminaState {
    private static final Map<UUID, StaminaState> STAMINA = new HashMap<>();

    private ServerStaminaState() {
    }

    public static StaminaState get(LivingEntity entity) {
        double max = StaminaMaxProvider.getMaxStamina(entity);
        return STAMINA.computeIfAbsent(
                entity.getUuid(),
                ignored -> new StaminaState(max)
        );
    }

    public static double getCurrent(LivingEntity entity) {
        return get(entity).current();
    }

    public static double getMax(LivingEntity entity) {
        return StaminaMaxProvider.getMaxStamina(entity);
    }

    public static boolean hasAtLeast(LivingEntity entity, double amount) {
        return get(entity).hasAtLeast(amount);
    }

    public static boolean consume(LivingEntity entity, double amount) {
        return get(entity).consume(amount);
    }

    public static void damage(LivingEntity entity, double amount) {
        get(entity).damage(amount);
    }

    public static void restore(LivingEntity entity, double amount) {
        get(entity).restore(amount);
    }

    public static void setCurrent(LivingEntity entity, double value) {
        get(entity).setCurrent(value);
    }

    public static void setEntityTypeMaxStamina(EntityType<?> entityType, double maxStamina) {
        StaminaMaxProvider.setEntityTypeMaxStamina(entityType, maxStamina);
    }

    public static void setEntityMaxStamina(UUID entityUuid, double maxStamina) {
        StaminaMaxProvider.setEntityMaxStamina(entityUuid, maxStamina);
    }

    public static void setEntityTypeRegenPerTick(EntityType<?> entityType, double regenPerTick) {
        StaminaMaxProvider.setEntityTypeRegenPerTick(entityType, regenPerTick);
    }

    public static void setEntityRegenPerTick(UUID entityUuid, double regenPerTick) {
        StaminaMaxProvider.setEntityRegenPerTick(entityUuid, regenPerTick);
    }

    public static void tick(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            for (var player : world.getPlayers()) {
                StaminaState state = get(player);
                sendSync(player, state);
            }
        }

        Iterator<Map.Entry<UUID, StaminaState>> iterator = STAMINA.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, StaminaState> entry = iterator.next();
            LivingEntity entity = findLivingEntity(server, entry.getKey());

            if (entity == null || entity.isRemoved() || !entity.isAlive()) {
                iterator.remove();
                StaminaMaxProvider.clearEntityMaxStamina(entry.getKey());
                StaminaMaxProvider.clearEntityRegenPerTick(entry.getKey());
                continue;
            }

            entry.getValue().tick(
                    StaminaMaxProvider.getMaxStamina(entity),
                    StaminaMaxProvider.getRegenPerTick(entity)
            );
        }
    }

    private static LivingEntity findLivingEntity(MinecraftServer server, UUID uuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
        }

        return null;
    }

    private static void sendSync(ServerPlayerEntity player, StaminaState state) {
        ServerPlayNetworking.send(
                player,
                new StaminaSyncPayload(
                        (float) state.current(),
                        (float) state.max()
                )
        );
    }
}
