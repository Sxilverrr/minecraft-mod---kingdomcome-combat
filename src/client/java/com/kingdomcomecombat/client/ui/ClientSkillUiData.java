package com.kingdomcomecombat.client.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.ComboMoveConfigs;
import com.kingdomcomecombat.combat.AttackMoveConfig;
import com.kingdomcomecombat.combat.AttackMoveConfigs;
import com.kingdomcomecombat.combat.ExecutionMoveConfigs;
import com.kingdomcomecombat.combat.ExecutionTargetConfig;
import com.kingdomcomecombat.combat.CombatDirection;
import com.kingdomcomecombat.collision.AnimatedAttackHitboxLibrary;
import com.kingdomcomecombat.item.SkillBookTexts;
import com.kingdomcomecombat.equipment.ShieldCombatAttributes;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.WeaponCombatAttributes;
import com.kingdomcomecombat.equipment.ArmorCombatAttributes;
import com.kingdomcomecombat.equipment.RangedWeaponAttributes;
import com.kingdomcomecombat.equipment.RangedWeaponAttributesRegistry;
import com.kingdomcomecombat.equipment.EquipmentClientDefaults;
import java.util.Set;
import java.util.List;
import java.util.Map;

public final class ClientSkillUiData {
    private static final Gson GSON = new Gson();
    private ClientSkillUiData() {}

    public static void replace(String combosJson, String textsJson, String shieldsJson, String weaponsJson,
                               String equipmentDefaultsJson,
                               String armorJson, String rangedWeaponsJson,
                               String executionsJson, String executionTargetsJson,
                               String attackMoveDirectionsJson, String attackMovesJson,
                               double hitboxSizeX, double hitboxSizeY, double hitboxSizeZ,
                               double hitboxOffsetX, double hitboxOffsetY, double hitboxOffsetZ,
                               double hitboxRotationX, double hitboxRotationY, double hitboxRotationZ) {
        try {
            List<ComboMoveConfig> combos = GSON.fromJson(combosJson, new TypeToken<List<ComboMoveConfig>>() {}.getType());
            Map<String, SkillBookTexts.Entry> texts = GSON.fromJson(textsJson, new TypeToken<Map<String, SkillBookTexts.Entry>>() {}.getType());
            Map<String, ShieldCombatAttributes> shields = GSON.fromJson(shieldsJson, new TypeToken<Map<String, ShieldCombatAttributes>>() {}.getType());
            Map<String, WeaponCombatAttributes> weapons = GSON.fromJson(weaponsJson, new TypeToken<Map<String, WeaponCombatAttributes>>() {}.getType());
            EquipmentClientDefaults equipmentDefaults = GSON.fromJson(equipmentDefaultsJson, EquipmentClientDefaults.class);
            Map<String, ArmorCombatAttributes> armor = GSON.fromJson(armorJson, new TypeToken<Map<String, ArmorCombatAttributes>>() {}.getType());
            Map<String, RangedWeaponAttributes> rangedWeapons = GSON.fromJson(rangedWeaponsJson, new TypeToken<Map<String, RangedWeaponAttributes>>() {}.getType());
            Map<String, Map<CombatDirection, ComboMoveConfig>> executions = GSON.fromJson(executionsJson, new TypeToken<Map<String, Map<CombatDirection, ComboMoveConfig>>>() {}.getType());
            Set<String> executionTargets = GSON.fromJson(executionTargetsJson, new TypeToken<Set<String>>() {}.getType());
            Map<CombatDirection, AttackMoveConfig> directionalMoves = GSON.fromJson(attackMoveDirectionsJson, new TypeToken<Map<CombatDirection, AttackMoveConfig>>() {}.getType());
            Map<String, AttackMoveConfig> namedMoves = GSON.fromJson(attackMovesJson, new TypeToken<Map<String, AttackMoveConfig>>() {}.getType());
            if (combos != null && !combos.isEmpty()) {
                ComboMoveConfigs.clear();
                for (ComboMoveConfig combo : combos) {
                    ComboMoveConfigs.register(sequence(combo), combo);
                }
            }
            if (texts != null && !texts.isEmpty()) {
                SkillBookTexts.clear();
                texts.forEach(SkillBookTexts::put);
            }
            // Empty sections mean the server has no authoritative override for
            // that registry; retain the client's bundled/resource-pack data.
            if (shields != null && !shields.isEmpty()) EquipmentCombatAttributesRegistry.replaceShields(shields);
            if (weapons != null && !weapons.isEmpty()) EquipmentCombatAttributesRegistry.replaceWeapons(weapons);
            if (equipmentDefaults != null) equipmentDefaults.apply();
            if (armor != null && !armor.isEmpty()) EquipmentCombatAttributesRegistry.replaceArmor(armor);
            if (rangedWeapons != null && !rangedWeapons.isEmpty()) RangedWeaponAttributesRegistry.replace(rangedWeapons);
            if (executions != null && !executions.isEmpty()) ExecutionMoveConfigs.replace(executions);
            if (executionTargets != null && !executionTargets.isEmpty()) ExecutionTargetConfig.replace(executionTargets);
            boolean hasDirectionalMoves = directionalMoves != null && !directionalMoves.isEmpty();
            boolean hasNamedMoves = namedMoves != null && !namedMoves.isEmpty();
            if (hasDirectionalMoves || hasNamedMoves) {
                AttackMoveConfigs.clear();
                if (hasDirectionalMoves) directionalMoves.forEach(AttackMoveConfigs::register);
                if (hasNamedMoves) namedMoves.forEach(AttackMoveConfigs::registerNamed);
            }
            AnimatedAttackHitboxLibrary.setRealHitboxSizeUnits(hitboxSizeX, hitboxSizeY, hitboxSizeZ);
            AnimatedAttackHitboxLibrary.setRealHitboxOffsetUnits(hitboxOffsetX, hitboxOffsetY, hitboxOffsetZ);
            AnimatedAttackHitboxLibrary.setRealHitboxRotationDegrees(hitboxRotationX, hitboxRotationY, hitboxRotationZ);
            AnimatedAttackHitboxLibrary.clearCache();
            long customWeaponHitboxes = weapons == null ? 0L : weapons.values().stream()
                    .filter(WeaponCombatAttributes::hasCustomRealHitboxSize)
                    .count();
            KingdomComeCombat.LOGGER.info(
                    "Applied server combat datapack state on client: {} combos, {} directional moves, {} named moves, {} weapons, {} custom weapon real hitboxes",
                    combos == null ? 0 : combos.size(),
                    directionalMoves == null ? 0 : directionalMoves.size(),
                    namedMoves == null ? 0 : namedMoves.size(),
                    weapons == null ? 0 : weapons.size(),
                    customWeaponHitboxes
            );
        } catch (RuntimeException exception) {
            KingdomComeCombat.LOGGER.error("Failed to apply server combat datapack state on client", exception);
        }
    }

    private static String sequence(ComboMoveConfig combo) {
        StringBuilder result = new StringBuilder();
        for (var direction : combo.sequence()) result.append(switch (direction) {
            case LEFT -> '左'; case RIGHT -> '右'; case UP -> '上'; case DOWN -> '下';
        });
        return result.toString();
    }
}
