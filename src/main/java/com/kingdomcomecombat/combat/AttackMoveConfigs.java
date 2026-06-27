package com.kingdomcomecombat.combat;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AttackMoveConfigs {
    private static final Map<CombatDirection, AttackMoveConfig> BY_DIRECTION =
            new EnumMap<>(CombatDirection.class);
    private static final Map<String, AttackMoveConfig> BY_ID = new java.util.HashMap<>();

    static {
        register(
                CombatDirection.RIGHT,
                new AttackMoveConfig(
                        "right_slash",
                        0.8,
                        1.0,
                        0.4,
                        List.of(
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.HEAD,
                                        List.of("side_head")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.SHOULDERS,
                                        List.of("chest")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.LEGS,
                                        List.of("thigh")
                                )
                        ),
                        23.0,
                        5.0,
                        true,
                        ""
                )
        );
        register(
                CombatDirection.LEFT,
                new AttackMoveConfig(
                        "left_slash",
                        0.8,
                        1.0,
                        0.4,
                        List.of(
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.HEAD,
                                        List.of("side_head")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.SHOULDERS,
                                        List.of("chest")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.LEGS,
                                        List.of("thigh")
                                )
                        ),
                        23.0,
                        5.0,
                        true,
                        ""
                )
        );
        register(
                CombatDirection.UP,
                new AttackMoveConfig(
                        "upper_cut",
                        0.7,
                        0.85,
                        0.55,
                        List.of(
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.HEAD,
                                        List.of("face")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.BODY,
                                        List.of("chest")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.LEGS,
                                        List.of("abdomen")
                                )
                        ),
                        22.0,
                        6.0,
                        true,
                        ""
                )
        );
        register(
                CombatDirection.DOWN,
                new AttackMoveConfig(
                        "downward_cut",
                        0.9,
                        1.05,
                        0.35,
                        List.of(
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.HEAD,
                                        List.of("crown")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.BODY,
                                        List.of("chest")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.LEGS,
                                        List.of("thigh")
                                )
                        ),
                        24.0,
                        6.0,
                        true,
                        ""
                )
        );
        registerNamed(
                "zombie_left",
                new AttackMoveConfig(
                        "zombie_left",
                        1.35,
                        0.0,
                        0.0,
                        List.of(
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.HEAD,
                                        List.of("face")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.BODY,
                                        List.of("chest")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.ARMS,
                                        List.of("arm")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.LEGS,
                                        List.of("thigh")
                                )
                        ),
                        18.0,
                        4.0,
                        false,
                        "zombie_left"
                )
        );
        registerNamed(
                "zombie_right",
                new AttackMoveConfig(
                        "zombie_right",
                        1.35,
                        0.0,
                        0.0,
                        List.of(
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.HEAD,
                                        List.of("face")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.BODY,
                                        List.of("chest")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.ARMS,
                                        List.of("arm")
                                ),
                                new AttackMoveConfig.HitZoneRule(
                                        AttackMoveConfig.HitZone.LEGS,
                                        List.of("thigh")
                                )
                        ),
                        18.0,
                        4.0,
                        false,
                        "zombie_right"
                )
        );
    }

    private AttackMoveConfigs() {
    }

    public static AttackMoveConfig get(CombatDirection direction) {
        return BY_DIRECTION.get(direction);
    }

    public static AttackMoveConfig getNamed(String id) {
        return BY_ID.get(id);
    }

    public static void clear() {
        BY_DIRECTION.clear();
        BY_ID.clear();
    }

    public static void register(CombatDirection direction, AttackMoveConfig config) {
        BY_DIRECTION.put(direction, config);
        registerNamed(config.id(), config);
    }

    public static void registerNamed(String id, AttackMoveConfig config) {
        if (id == null || id.isBlank()) {
            return;
        }

        BY_ID.put(id, config);
    }
}
