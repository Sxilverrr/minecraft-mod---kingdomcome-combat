package com.kingdomcomecombat.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import com.kingdomcomecombat.injury.ModStatusEffects;
import com.kingdomcomecombat.network.CombatNetworkBroadcaster;
import com.kingdomcomecombat.network.EntityDodgeAnimationPayload;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

public class ServerCombatControlState {
    private static final Map<UUID, Integer> ATTACK_DISABLED = new HashMap<>();
    private static final Map<UUID, Integer> BLOCK_DISABLED = new HashMap<>();
    private static final Map<UUID, Integer> MOVEMENT_DISABLED = new HashMap<>();
    private static final Map<UUID, Integer> DODGE_DISABLED = new HashMap<>();
    private static final Map<UUID, Integer> DODGE_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Integer> PERFECT_COUNTER_WINDOWS = new HashMap<>();
    private static final Map<UUID, DodgeWindow> DODGES = new HashMap<>();

    private ServerCombatControlState() {
    }

    public static void tick() {
        tickMap(ATTACK_DISABLED);
        tickMap(BLOCK_DISABLED);
        tickMap(MOVEMENT_DISABLED);
        tickMap(DODGE_DISABLED);
        tickMap(DODGE_COOLDOWNS);
        tickMap(PERFECT_COUNTER_WINDOWS);

        Iterator<Map.Entry<UUID, DodgeWindow>> iterator = DODGES.entrySet().iterator();
        while (iterator.hasNext()) {
            DodgeWindow dodge = iterator.next().getValue();
            dodge.ageTicks++;
            if (dodge.ageTicks >= CombatControlConfig.DODGE_TOTAL_TICKS) {
                iterator.remove();
            }
        }
    }

    public static void disableAttack(UUID uuid, int ticks) {
        ATTACK_DISABLED.merge(uuid, Math.max(0, ticks), Math::max);
    }

    public static void disableBlock(UUID uuid, int ticks) {
        BLOCK_DISABLED.merge(uuid, Math.max(0, ticks), Math::max);
    }

    public static void disableMovement(UUID uuid, int ticks) {
        MOVEMENT_DISABLED.merge(uuid, Math.max(0, ticks), Math::max);
    }

    public static void disableDodge(UUID uuid, int ticks) {
        DODGE_DISABLED.merge(uuid, Math.max(0, ticks), Math::max);
        DODGES.remove(uuid);
    }

    public static void clearMovementDisable(UUID uuid) {
        MOVEMENT_DISABLED.remove(uuid);
        DODGE_DISABLED.remove(uuid);
    }

    public static void clearBlockDisable(UUID uuid) {
        BLOCK_DISABLED.remove(uuid);
    }

    public static void clear(UUID uuid) {
        ATTACK_DISABLED.remove(uuid);
        BLOCK_DISABLED.remove(uuid);
        MOVEMENT_DISABLED.remove(uuid);
        DODGE_DISABLED.remove(uuid);
        DODGE_COOLDOWNS.remove(uuid);
        PERFECT_COUNTER_WINDOWS.remove(uuid);
        DODGES.remove(uuid);
    }

    public static boolean canAttack(LivingEntity entity) {
        return !isAttackDisabled(entity.getUuid());
    }

    public static boolean canBlock(LivingEntity entity) {
        return !isBlockDisabled(entity.getUuid());
    }

    public static boolean isAttackDisabled(UUID uuid) {
        return ATTACK_DISABLED.getOrDefault(uuid, 0) > 0;
    }

    public static boolean isBlockDisabled(UUID uuid) {
        return BLOCK_DISABLED.getOrDefault(uuid, 0) > 0;
    }

    public static boolean isMovementDisabled(UUID uuid) {
        return MOVEMENT_DISABLED.getOrDefault(uuid, 0) > 0;
    }

    public static void collectMovementControlledUuids(Set<UUID> destination) {
        destination.addAll(MOVEMENT_DISABLED.keySet());
        destination.addAll(DODGES.keySet());
    }

    public static void startPerfectCounterWindow(UUID uuid) {
        PERFECT_COUNTER_WINDOWS.put(uuid, CombatControlConfig.PERFECT_COUNTER_WINDOW_TICKS);
    }

    public static boolean isPerfectCounterWindowActive(UUID uuid) {
        return PERFECT_COUNTER_WINDOWS.getOrDefault(uuid, 0) > 0;
    }

    public static boolean consumePerfectCounterAttackSlow(UUID uuid) {
        if (PERFECT_COUNTER_WINDOWS.getOrDefault(uuid, 0) <= 0) {
            return false;
        }

        PERFECT_COUNTER_WINDOWS.remove(uuid);
        return true;
    }

    public static void startDodge(UUID uuid, DodgeDirection direction) {
        DODGES.put(uuid, new DodgeWindow(direction, 0));
        DODGE_COOLDOWNS.put(uuid, CombatControlConfig.DODGE_TOTAL_TICKS + CombatControlConfig.DODGE_COOLDOWN_TICKS);
        disableAttack(
                uuid,
                direction == DodgeDirection.FORWARD
                        ? CombatControlConfig.FORWARD_STEP_ATTACK_DISABLE_TICKS
                        : CombatControlConfig.DODGE_ATTACK_DISABLE_TICKS
        );
        disableBlock(
                uuid,
                direction == DodgeDirection.FORWARD
                        ? CombatControlConfig.FORWARD_STEP_BLOCK_DISABLE_TICKS
                        : CombatControlConfig.DODGE_BLOCK_DISABLE_TICKS
        );
    }

    public static void startDodge(LivingEntity entity, DodgeDirection direction) {
        startDodge(entity.getUuid(), direction);
        if (!entity.getWorld().isClient()) {
            CombatNetworkBroadcaster.sendTrackingAndSelf(
                    entity,
                    new EntityDodgeAnimationPayload(entity.getId(), direction.ordinal())
            );
        }
    }

    public static boolean dodgesAttack(LivingEntity entity, CombatDirection attackDirection) {
        DodgeWindow dodge = DODGES.get(entity.getUuid());
        return dodge != null
                && dodge.hasInvulnerability()
                && dodge.inInvulnerabilityWindow()
                && dodge.direction.evades(attackDirection);
    }

    public static boolean dodgesDamage(LivingEntity entity, DamageSource source) {
        DodgeWindow dodge = DODGES.get(entity.getUuid());
        return dodge != null
                && dodge.hasInvulnerability()
                && dodge.inInvulnerabilityWindow()
                && DodgeDamageConfig.isDodgeable(source);
    }

    public static DodgeDirection getDodgeDirection(LivingEntity entity) {
        DodgeWindow dodge = DODGES.get(entity.getUuid());
        return dodge == null ? null : dodge.direction;
    }

    public static int getDodgeAgeTicks(LivingEntity entity) {
        DodgeWindow dodge = DODGES.get(entity.getUuid());
        return dodge == null ? 0 : dodge.ageTicks;
    }

    public static boolean isDodging(LivingEntity entity) {
        return DODGES.containsKey(entity.getUuid());
    }

    public static boolean canDodge(LivingEntity entity) {
        return DODGE_DISABLED.getOrDefault(entity.getUuid(), 0) <= 0
                && DODGE_COOLDOWNS.getOrDefault(entity.getUuid(), 0) <= 0
                && ModStatusEffects.effectiveLevel(entity, ModStatusEffects.LEG_INJURY) <= 2;
    }

    private static void tickMap(Map<UUID, Integer> map) {
        Iterator<Map.Entry<UUID, Integer>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int ticks = entry.getValue() - 1;
            if (ticks <= 0) {
                iterator.remove();
            } else {
                entry.setValue(ticks);
            }
        }
    }

    private static class DodgeWindow {
        private final DodgeDirection direction;
        private int ageTicks;

        private DodgeWindow(DodgeDirection direction, int ageTicks) {
            this.direction = direction;
            this.ageTicks = ageTicks;
        }

        boolean hasInvulnerability() {
            return direction.hasInvulnerability();
        }

        boolean inInvulnerabilityWindow() {
            return ageTicks >= CombatControlConfig.DODGE_INVULN_START_TICK
                    && ageTicks <= CombatControlConfig.DODGE_INVULN_END_TICK;
        }
    }
}
