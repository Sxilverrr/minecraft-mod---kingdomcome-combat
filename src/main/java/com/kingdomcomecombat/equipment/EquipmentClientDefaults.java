package com.kingdomcomecombat.equipment;

import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Client-relevant classification and special move defaults from defaults/equipment.json. */
public record EquipmentClientDefaults(
        Set<String> longswords,
        Set<String> shortSwords,
        Set<String> heavyWeapons,
        Set<String> polearms,
        Map<String, String> shortSwordAttackMoves,
        Map<String, String> shortSwordStanceAnimations
) {
    public static EquipmentClientDefaults current() {
        return new EquipmentClientDefaults(
                strings(EquipmentFallbackConfig.configuredLongswordItems()),
                strings(EquipmentFallbackConfig.configuredShortSwordItems()),
                strings(EquipmentFallbackConfig.configuredHeavyWeaponItems()),
                strings(EquipmentFallbackConfig.configuredPolearmItems()),
                EquipmentFallbackConfig.swordAttackMoveIds(),
                EquipmentFallbackConfig.swordStanceAnimationNames()
        );
    }

    public void apply() {
        EquipmentFallbackConfig.setConfiguredLongswordItems(identifiers(longswords));
        EquipmentFallbackConfig.setConfiguredShortSwordItems(identifiers(shortSwords));
        EquipmentFallbackConfig.setConfiguredHeavyWeaponItems(identifiers(heavyWeapons));
        EquipmentFallbackConfig.setConfiguredPolearmItems(identifiers(polearms));
        EquipmentFallbackConfig.setSwordAttackMoveIds(shortSwordAttackMoves);
        EquipmentFallbackConfig.setSwordStanceAnimationNames(shortSwordStanceAnimations);
    }

    private static Set<String> strings(Set<Identifier> ids) {
        return ids.stream().map(Identifier::toString).collect(Collectors.toUnmodifiableSet());
    }

    private static Set<Identifier> identifiers(Set<String> ids) {
        if (ids == null) return Set.of();
        return ids.stream().map(Identifier::tryParse).filter(java.util.Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}
