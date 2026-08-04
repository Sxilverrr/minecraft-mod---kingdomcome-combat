package com.kingdomcomecombat.combat;

import net.minecraft.item.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ExecutionMoveConfigs {
    private static final Map<String, Map<CombatDirection, ComboMoveConfig>> BY_SET = new ConcurrentHashMap<>();

    private ExecutionMoveConfigs() {
    }

    public static void register(String setId, CombatDirection direction, ComboMoveConfig config) {
        if (setId == null || setId.isBlank() || config == null) {
            return;
        }
        BY_SET.compute(setId, (ignored, existing) -> {
            Map<CombatDirection, ComboMoveConfig> configs = new java.util.EnumMap<>(CombatDirection.class);
            if (existing != null) {
                configs.putAll(existing);
            }
            configs.put(direction, config);
            return Map.copyOf(configs);
        });
    }

    public static Optional<ComboMoveConfig> find(
            String setId,
            CombatDirection direction,
            ItemStack weapon
    ) {
        if (setId == null || setId.isBlank()) {
            return Optional.empty();
        }
        Map<CombatDirection, ComboMoveConfig> configs = BY_SET.get(setId);
        if (configs == null) {
            return Optional.empty();
        }
        ComboMoveConfig config = configs.get(direction);
        if (config == null) {
            config = configs.get(direction == CombatDirection.DOWN ? CombatDirection.LEFT : CombatDirection.RIGHT);
        }
        if (config == null) {
            config = configs.get(CombatDirection.RIGHT);
        }
        if (config == null || !ComboMoveConfigs.canUseWith(config, weapon)) {
            return Optional.empty();
        }
        return Optional.of(config);
    }

    public static boolean isExecution(ComboMoveConfig config) {
        if (config == null) {
            return false;
        }
        for (Map<CombatDirection, ComboMoveConfig> configs : BY_SET.values()) {
            if (configs.containsValue(config)) {
                return true;
            }
        }
        return false;
    }

    public static void clear() {
        BY_SET.clear();
    }

    public static Map<String, Map<CombatDirection, ComboMoveConfig>> snapshot() {
        return Map.copyOf(BY_SET);
    }

    public static void replace(Map<String, Map<CombatDirection, ComboMoveConfig>> executions) {
        BY_SET.clear();
        if (executions == null) return;
        executions.forEach((setId, moves) -> {
            if (setId != null && !setId.isBlank() && moves != null) {
                BY_SET.put(setId, Map.copyOf(moves));
            }
        });
    }
}
