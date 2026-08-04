package com.kingdomcomecombat.stamina;

import com.kingdomcomecombat.injury.ModStatusEffects;
import com.kingdomcomecombat.network.CombatNetworkBroadcaster;
import com.kingdomcomecombat.network.EntityStaminaSyncPayload;
import com.kingdomcomecombat.network.StaminaSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.passive.AbstractHorseEntity;
import com.kingdomcomecombat.riding.KccHorseRidingData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.lang.ref.WeakReference;

public class ServerStaminaState {
    private static final Map<UUID, StaminaState> STAMINA = new HashMap<>();
    private static final Map<UUID, WeakReference<LivingEntity>> ENTITY_REFERENCES = new HashMap<>();
    private static final Map<UUID, Integer> RIDING_LOCK_TICKS = new HashMap<>();
    private static final Map<UUID, SyncSnapshot> LAST_PLAYER_SYNCS = new HashMap<>();
    private static final Map<UUID, SyncSnapshot> LAST_ENTITY_SYNCS = new HashMap<>();
    private static final double RUNNING_HORSE_THRESHOLD = 0.60;
    private static final int PLAYER_SYNC_INTERVAL_TICKS = 2;
    private static final int SYNC_HEARTBEAT_TICKS = 20;

    private ServerStaminaState() {
    }

    public static StaminaState get(LivingEntity entity) {
        WeakReference<LivingEntity> reference = ENTITY_REFERENCES.get(entity.getUuid());
        if (reference == null || reference.get() != entity) {
            ENTITY_REFERENCES.put(entity.getUuid(), new WeakReference<>(entity));
        }
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

    public static double getOriginalMax(LivingEntity entity) {
        return StaminaMaxProvider.getOriginalMaxStamina(entity);
    }

    public static boolean hasAtLeast(LivingEntity entity, double amount) {
        return get(entity).hasAtLeast(amount);
    }

    public static boolean consume(LivingEntity entity, double amount) {
        StaminaState state = get(entity);
        double before = state.current();
        boolean consumed = state.consume(amount);
        handleMountedExhaustion(entity, before, state.current());
        if (state.current() != before) {
            sendEntitySync(entity, state);
        }
        return consumed;
    }

    public static void damage(LivingEntity entity, double amount) {
        StaminaState state = get(entity); double before = state.current();
        state.damage(amount); handleMountedExhaustion(entity, before, state.current());
        if (state.current() != before) {
            sendEntitySync(entity, state);
        }
    }

    public static void damage(LivingEntity entity, double amount, int regenDelayTicks) {
        StaminaState state = get(entity); double before = state.current();
        state.damage(amount, regenDelayTicks); handleMountedExhaustion(entity, before, state.current());
        if (state.current() != before) {
            sendEntitySync(entity, state);
        }
    }

    public static void restore(LivingEntity entity, double amount) {
        StaminaState state = get(entity);
        double before = state.current();
        state.restore(amount);
        if (state.current() != before) {
            sendEntitySync(entity, state);
        }
    }

    public static void setCurrent(LivingEntity entity, double value) {
        StaminaState state = get(entity);
        double before = state.current();
        state.setCurrent(value);
        if (state.current() != before) {
            sendEntitySync(entity, state);
        }
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
        RIDING_LOCK_TICKS.replaceAll((uuid, ticks) -> ticks - 1);
        RIDING_LOCK_TICKS.entrySet().removeIf(entry -> entry.getValue() <= 0);
        for (ServerWorld world : server.getWorlds()) {
            for (var player : world.getPlayers()) {
                StaminaState state = get(player);
                sendSync(player, state);
            }
        }

        Iterator<Map.Entry<UUID, StaminaState>> iterator = STAMINA.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, StaminaState> entry = iterator.next();
            WeakReference<LivingEntity> reference = ENTITY_REFERENCES.get(entry.getKey());
            LivingEntity entity = reference == null ? null : reference.get();
            if (entity == null) {
                // Compatibility fallback for state created before a reference was
                // recorded; the normal hot path no longer searches every world.
                entity = findLivingEntity(server, entry.getKey());
                if (entity != null) {
                    ENTITY_REFERENCES.put(entry.getKey(), new WeakReference<>(entity));
                }
            }

            if (entity == null || entity.isRemoved() || !entity.isAlive()) {
                iterator.remove();
                ENTITY_REFERENCES.remove(entry.getKey());
                LAST_PLAYER_SYNCS.remove(entry.getKey());
                LAST_ENTITY_SYNCS.remove(entry.getKey());
                StaminaMaxProvider.clearEntityMaxStamina(entry.getKey());
                StaminaMaxProvider.clearEntityRegenPerTick(entry.getKey());
                continue;
            }

            double resolvedMax = StaminaMaxProvider.getMaxStamina(entity);
            if (!entry.getValue().isIdleAtMaximum(resolvedMax)) {
                double regen = StaminaMaxProvider.getRegenPerTick(entity);
                if (entity instanceof ServerPlayerEntity player) {
                    regen *= com.kingdomcomecombat.hardship.HardshipEffects.staminaRegenerationMultiplier(player);
                }
                regen *= 1.0 + 0.20 * ModStatusEffects.level(entity, ModStatusEffects.VITALITY);
                if (isOnRunningHorse(entity)) regen *= 0.40;
                entry.getValue().tick(resolvedMax, regen);
            }
            if (entity.age % 5 == 0) {
                sendEntitySync(entity, entry.getValue());
            }
            if (RIDING_LOCK_TICKS.containsKey(entity.getUuid()) && entity.hasVehicle()) entity.stopRiding();
        }
    }

    public static void clear(UUID entityUuid) {
        STAMINA.remove(entityUuid);
        ENTITY_REFERENCES.remove(entityUuid);
        RIDING_LOCK_TICKS.remove(entityUuid);
        LAST_PLAYER_SYNCS.remove(entityUuid);
        LAST_ENTITY_SYNCS.remove(entityUuid);
        StaminaMaxProvider.clearEntityMaxStamina(entityUuid);
        StaminaMaxProvider.clearEntityRegenPerTick(entityUuid);
    }

    private static boolean isOnRunningHorse(LivingEntity entity) {
        return entity.getVehicle() instanceof AbstractHorseEntity horse
                && (Object) horse instanceof KccHorseRidingData data
                && data.kingdomcomecombat$getHorseCurrentSpeed() > RUNNING_HORSE_THRESHOLD;
    }

    private static void handleMountedExhaustion(LivingEntity entity, double before, double after) {
        if (before <= 0.0 || after > 0.0 || !isOnRunningHorse(entity)) return;
        Vec3d knockback = entity.getVelocity();
        Vec3d mountVelocity = entity.getVehicle().getVelocity();
        entity.stopRiding();
        entity.setVelocity(knockback.add(mountVelocity));
        entity.velocityModified = true;
        RIDING_LOCK_TICKS.put(entity.getUuid(), 10 * 20);
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
        long now = player.getWorld().getTime();
        float current = (float) state.current();
        float max = (float) state.max();
        SyncSnapshot previous = LAST_PLAYER_SYNCS.get(player.getUuid());
        boolean changed = previous == null || previous.current() != current || previous.max() != max;
        boolean heartbeat = previous == null || now - previous.sentAtTick() >= SYNC_HEARTBEAT_TICKS;
        if (!heartbeat && (!changed || now - previous.sentAtTick() < PLAYER_SYNC_INTERVAL_TICKS)) {
            return;
        }
        ServerPlayNetworking.send(
                player,
                new StaminaSyncPayload(current, max)
        );
        LAST_PLAYER_SYNCS.put(player.getUuid(), new SyncSnapshot(current, max, now));
    }

    private static void sendEntitySync(LivingEntity entity, StaminaState state) {
        if (entity.getWorld().isClient()) {
            return;
        }
        long now = entity.getWorld().getTime();
        float current = (float) state.current();
        float max = (float) StaminaMaxProvider.getOriginalMaxStamina(entity);
        SyncSnapshot previous = LAST_ENTITY_SYNCS.get(entity.getUuid());
        boolean changed = previous == null || previous.current() != current || previous.max() != max;
        boolean heartbeat = previous == null || now - previous.sentAtTick() >= SYNC_HEARTBEAT_TICKS;
        if (!changed && !heartbeat) {
            return;
        }
        CombatNetworkBroadcaster.sendTrackingAndSelf(
                entity,
                new EntityStaminaSyncPayload(
                        entity.getId(),
                        current,
                        max
                )
        );
        LAST_ENTITY_SYNCS.put(entity.getUuid(), new SyncSnapshot(current, max, now));
    }

    private record SyncSnapshot(float current, float max, long sentAtTick) {
    }
}
