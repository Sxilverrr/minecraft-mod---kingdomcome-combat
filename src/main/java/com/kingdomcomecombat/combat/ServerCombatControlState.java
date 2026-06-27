package com.kingdomcomecombat.combat;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import com.kingdomcomecombat.injury.ModStatusEffects;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class ServerCombatControlState {
    private static final Map<UUID, Integer> ATTACK_DISABLED = new HashMap<>();
    private static final Map<UUID, Integer> BLOCK_DISABLED = new HashMap<>();
    private static final Map<UUID, Integer> MOVEMENT_DISABLED = new HashMap<>();
    private static final Map<UUID, Integer> PERFECT_COUNTER_WINDOWS = new HashMap<>();
    private static final Map<UUID, DodgeWindow> DODGES = new HashMap<>();

    private ServerCombatControlState() {
    }

    public static void tick() {
        tickMap(ATTACK_DISABLED);
        tickMap(BLOCK_DISABLED);
        tickMap(MOVEMENT_DISABLED);
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

    public static void clearMovementDisable(UUID uuid) {
        MOVEMENT_DISABLED.remove(uuid);
    }

    public static void clearBlockDisable(UUID uuid) {
        BLOCK_DISABLED.remove(uuid);
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

    public static void startPerfectCounterWindow(UUID uuid) {
        PERFECT_COUNTER_WINDOWS.put(uuid, CombatControlConfig.PERFECT_COUNTER_WINDOW_TICKS);
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
        return ModStatusEffects.effectiveLevel(entity, ModStatusEffects.LEG_INJURY) <= 0;
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
