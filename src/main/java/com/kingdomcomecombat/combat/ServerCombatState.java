package com.kingdomcomecombat.combat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerCombatState {
    private static final Map<UUID, ActiveServerAttack> ACTIVE_ATTACKS = new HashMap<>();

    public static void startAttack(
            UUID playerUuid,
            CombatDirection direction,
            float yaw,
            int targetEntityId,
            int attackTotalTicks
    ) {
        startAttack(playerUuid, direction, yaw, targetEntityId, attackTotalTicks, 1.0F);
    }

    public static void startAttack(
            UUID playerUuid,
            CombatDirection direction,
            float yaw,
            int targetEntityId,
            int attackTotalTicks,
            float animationSpeedMultiplier
    ) {
        startAttack(
                playerUuid,
                direction,
                yaw,
                targetEntityId,
                attackTotalTicks,
                animationSpeedMultiplier,
                null
        );
    }

    public static void startAttack(
            UUID playerUuid,
            CombatDirection direction,
            float yaw,
            int targetEntityId,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove
    ) {
        startAttack(
                playerUuid,
                direction,
                yaw,
                targetEntityId,
                attackTotalTicks,
                animationSpeedMultiplier,
                comboMove,
                0L
        );
    }

    public static void startAttack(
            UUID playerUuid,
            CombatDirection direction,
            float yaw,
            int targetEntityId,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            long startWorldTick
    ) {
        startAttack(
                playerUuid,
                direction,
                yaw,
                targetEntityId,
                attackTotalTicks,
                animationSpeedMultiplier,
                comboMove,
                startWorldTick,
                false
        );
    }

    public static void startAttack(
            UUID playerUuid,
            CombatDirection direction,
            float yaw,
            int targetEntityId,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            long startWorldTick,
            boolean perfectCounterSlow
    ) {
        startAttack(
                playerUuid,
                direction,
                yaw,
                targetEntityId,
                attackTotalTicks,
                animationSpeedMultiplier,
                comboMove,
                null,
                startWorldTick,
                perfectCounterSlow
        );
    }

    public static void startAttack(
            UUID playerUuid,
            CombatDirection direction,
            float yaw,
            int targetEntityId,
            boolean lockedLunge,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            AttackMoveConfig moveConfig,
            long startWorldTick,
            boolean perfectCounterSlow
    ) {
        startAttack(
                playerUuid,
                direction,
                yaw,
                targetEntityId,
                lockedLunge,
                attackTotalTicks,
                animationSpeedMultiplier,
                comboMove,
                moveConfig,
                startWorldTick,
                perfectCounterSlow,
                0.0
        );
    }

    public static void startAttack(
            UUID playerUuid,
            CombatDirection direction,
            float yaw,
            int targetEntityId,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            AttackMoveConfig moveConfig,
            long startWorldTick,
            boolean perfectCounterSlow
    ) {
        startAttack(
                playerUuid,
                direction,
                yaw,
                targetEntityId,
                attackTotalTicks,
                animationSpeedMultiplier,
                comboMove,
                moveConfig,
                startWorldTick,
                perfectCounterSlow,
                0.0
        );
    }

    public static void startAttack(
            UUID playerUuid,
            CombatDirection direction,
            float yaw,
            int targetEntityId,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            AttackMoveConfig moveConfig,
            long startWorldTick,
            boolean perfectCounterSlow,
            double startupSlowdown
    ) {
        startAttack(
                playerUuid,
                direction,
                yaw,
                targetEntityId,
                targetEntityId >= 0,
                attackTotalTicks,
                animationSpeedMultiplier,
                comboMove,
                moveConfig,
                startWorldTick,
                perfectCounterSlow,
                startupSlowdown
        );
    }

    public static void startAttack(
            UUID playerUuid,
            CombatDirection direction,
            float yaw,
            int targetEntityId,
            boolean lockedLunge,
            int attackTotalTicks,
            float animationSpeedMultiplier,
            ComboMoveConfig comboMove,
            AttackMoveConfig moveConfig,
            long startWorldTick,
            boolean perfectCounterSlow,
            double startupSlowdown
    ) {
        ACTIVE_ATTACKS.put(
                playerUuid,
                new ActiveServerAttack(
                        direction,
                        yaw,
                        targetEntityId,
                        lockedLunge,
                        attackTotalTicks,
                        animationSpeedMultiplier,
                        comboMove,
                        moveConfig,
                        startWorldTick,
                        perfectCounterSlow,
                        startupSlowdown
                )
        );
    }

    public static ActiveServerAttack getAttack(UUID playerUuid) {
        return ACTIVE_ATTACKS.get(playerUuid);
    }

    public static void removeAttack(UUID playerUuid) {
        ACTIVE_ATTACKS.remove(playerUuid);
    }

    public static Map<UUID, ActiveServerAttack> activeAttacks() {
        return ACTIVE_ATTACKS;
    }
}
