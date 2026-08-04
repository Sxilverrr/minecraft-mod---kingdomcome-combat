package com.kingdomcomecombat.equipment;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EquipmentFallbackConfig {
    private static DamageTypeProfile defaultWeapon = DamageTypeProfile.even(1.0);
    private static DamageTypeProfile sword = new DamageTypeProfile(1.2, 0.2, 1.3);
    private static DamageTypeProfile longsword = new DamageTypeProfile(1.2, 0.25, 1.2);
    private static DamageTypeProfile polearm = new DamageTypeProfile(1.0, 0.35, 0.65);
    private static DamageTypeProfile pickaxe = new DamageTypeProfile(1.8, 0.9, 0.3);
    private static DamageTypeProfile axe = new DamageTypeProfile(0.3, 0.9, 0.9);
    private static DamageTypeProfile fightingMace = new DamageTypeProfile(0.0, 1.7, 0.0);
    private static DamageTypeProfile shovel = DamageTypeProfile.even(1.0);
    private static DamageTypeProfile hoe = DamageTypeProfile.even(1.0);
    private static double defaultWeaponBaseImpact = 12.0;
    private static double swordBaseImpact = 20.0;
    private static double longswordBaseImpact = 20.0;
    private static double polearmBaseImpact = 22.0;
    private static double pickaxeBaseImpact = 24.0;
    private static double axeBaseImpact = 26.0;
    private static double fightingMaceBaseImpact = 12.0;
    private static double shovelBaseImpact = 12.0;
    private static double hoeBaseImpact = 12.0;
    private static double defaultWeaponBlockImpactMitigation = 0.75;
    private static double swordBlockImpactMitigation = 0.75;
    private static double longswordBlockImpactMitigation = 0.75;
    private static double polearmBlockImpactMitigation = 0.70;
    private static double pickaxeBlockImpactMitigation = 0.75;
    private static double axeBlockImpactMitigation = 0.75;
    private static double fightingMaceBlockImpactMitigation = 0.75;
    private static double shovelBlockImpactMitigation = 0.75;
    private static double hoeBlockImpactMitigation = 0.75;
    private static double defaultArmorBreakMultiplier = 1.0;
    private static double swordArmorBreakMultiplier = 1.0;
    private static double longswordArmorBreakMultiplier = 1.15;
    private static double polearmArmorBreakMultiplier = 0.45;
    private static double pickaxeArmorBreakMultiplier = 1.35;
    private static double axeArmorBreakMultiplier = 1.2;
    private static double fightingMaceArmorBreakMultiplier = 1.0;
    private static double shovelArmorBreakMultiplier = 1.0;
    private static double hoeArmorBreakMultiplier = 1.0;
    private static double defaultWeaponAttackSpeedMultiplier = 1.0;
    private static double swordAttackSpeedMultiplier = 1.0;
    private static double longswordAttackSpeedMultiplier = 1.0;
    private static double polearmAttackSpeedMultiplier = 1.0;
    private static double pickaxeAttackSpeedMultiplier = 1.0;
    private static double axeAttackSpeedMultiplier = 1.0;
    private static double fightingMaceAttackSpeedMultiplier = 1.0;
    private static double shovelAttackSpeedMultiplier = 1.0;
    private static double hoeAttackSpeedMultiplier = 1.0;
    private static double defaultWeaponToughness = 1.0;
    private static double defaultMinimumDurabilityPanelMultiplier = 0.40;
    private static double polearmWeaponToughness = 1.0;
    private static double polearmMinimumDurabilityPanelMultiplier = 0.40;
    private static double defaultHeldMovementSpeedMultiplier = 1.0;
    private static double polearmHeldMovementSpeedMultiplier = 1.0;
    private static Vec3d defaultWeaponRealHitboxSizeUnits = Vec3d.ZERO;
    private static Vec3d defaultWeaponRealHitboxOffsetUnits = Vec3d.ZERO;
    private static Vec3d defaultWeaponRealHitboxRotationDegrees = Vec3d.ZERO;
    private static Vec3d swordRealHitboxSizeUnits = Vec3d.ZERO;
    private static Vec3d swordRealHitboxOffsetUnits = Vec3d.ZERO;
    private static Vec3d swordRealHitboxRotationDegrees = Vec3d.ZERO;
    private static Vec3d longswordRealHitboxSizeUnits = new Vec3d(5.0, 1.5, 19.0);
    private static Vec3d longswordRealHitboxOffsetUnits = Vec3d.ZERO;
    private static Vec3d longswordRealHitboxRotationDegrees = new Vec3d(0.0, 0.0, -45.0);
    private static Vec3d polearmRealHitboxSizeUnits = new Vec3d(4.0, 4.0, 34.0);
    private static Vec3d polearmRealHitboxOffsetUnits = Vec3d.ZERO;
    private static Vec3d polearmRealHitboxRotationDegrees = Vec3d.ZERO;
    private static Vec3d pickaxeRealHitboxSizeUnits = Vec3d.ZERO;
    private static Vec3d pickaxeRealHitboxOffsetUnits = Vec3d.ZERO;
    private static Vec3d pickaxeRealHitboxRotationDegrees = Vec3d.ZERO;
    private static Vec3d axeRealHitboxSizeUnits = Vec3d.ZERO;
    private static Vec3d axeRealHitboxOffsetUnits = Vec3d.ZERO;
    private static Vec3d axeRealHitboxRotationDegrees = Vec3d.ZERO;
    private static Vec3d fightingMaceRealHitboxSizeUnits = Vec3d.ZERO;
    private static Vec3d fightingMaceRealHitboxOffsetUnits = Vec3d.ZERO;
    private static Vec3d fightingMaceRealHitboxRotationDegrees = Vec3d.ZERO;
    private static Vec3d shovelRealHitboxSizeUnits = Vec3d.ZERO;
    private static Vec3d shovelRealHitboxOffsetUnits = Vec3d.ZERO;
    private static Vec3d shovelRealHitboxRotationDegrees = Vec3d.ZERO;
    private static Vec3d hoeRealHitboxSizeUnits = Vec3d.ZERO;
    private static Vec3d hoeRealHitboxOffsetUnits = Vec3d.ZERO;
    private static Vec3d hoeRealHitboxRotationDegrees = Vec3d.ZERO;
    private static Map<String, String> defaultWeaponAttackMoveIds = Map.of();
    private static Map<String, String> defaultWeaponStanceAnimationNames = Map.of();
    private static Map<String, String> defaultWeaponExecutionMoveIds = Map.of();
    private static Map<String, String> swordAttackMoveIds = Map.of();
    private static Map<String, String> swordStanceAnimationNames = Map.of();
    private static Map<String, String> longswordAttackMoveIds = Map.of();
    private static Map<String, String> longswordStanceAnimationNames = Map.of();
    private static Map<String, String> polearmAttackMoveIds = Map.of(
            "right", "attack_right_longweapon", "left", "attack_left_longweapon", "up", "attack_up_longsword", "down", "attack_down_longweapon");
    private static Map<String, String> polearmStanceAnimationNames = Map.of(
            "right", "stance_right_longweapon", "left", "stance_left_longweapon", "up", "stance_up_longsword", "down", "stance_down_longweapon");
    private static Map<String, String> pickaxeAttackMoveIds = Map.of();
    private static Map<String, String> pickaxeStanceAnimationNames = Map.of();
    private static Map<String, String> axeAttackMoveIds = Map.of();
    private static Map<String, String> axeStanceAnimationNames = Map.of();
    private static Map<String, String> fightingMaceAttackMoveIds = Map.of(
            "right", "attack_right_heavy",
            "left", "attack_left_heavy",
            "up", "attack_up_heavy",
            "down", "attack_down_heavy"
    );
    private static Map<String, String> fightingMaceStanceAnimationNames = Map.of();
    private static Map<String, String> shovelAttackMoveIds = Map.of();
    private static Map<String, String> shovelStanceAnimationNames = Map.of();
    private static Map<String, String> hoeAttackMoveIds = Map.of();
    private static Map<String, String> hoeStanceAnimationNames = Map.of();
    private static Set<Identifier> configuredLongswordItems = Set.of();
    private static Set<Identifier> configuredShortSwordItems = Set.of();
    private static Set<Identifier> configuredHeavyWeaponItems = Set.of();
    private static Set<Identifier> configuredPolearmItems = Set.of();
    private static DamageTypeProfile helmetAndBootsArmorMultiplier =
            new DamageTypeProfile(18.0, 12.0, 20.0);
    private static DamageTypeProfile chestplateAndLeggingsArmorMultiplier =
            new DamageTypeProfile(10.0, 7.0, 12.0);
    private static double protectionPercent = 0.5;
    private static double impactMitigationPercent = 0.0;
    private static double blockedStrikeToImpactRatio = 0.15;
    private static Map<EquipmentSlot, List<String>> slotParts = defaultSlotParts();
    private static Map<String, Double> partDamageMultipliers = defaultPartDamageMultipliers();
    private static Map<Identifier, DamageTypeProfile> armorDefenseEnchantments = defaultArmorDefenseEnchantments();
    private static Map<Identifier, DamageTypeProfile> armorDefenseFirstTwoBonusEnchantments =
            defaultArmorDefenseFirstTwoBonusEnchantments();
    private static Map<Identifier, Double> armorImpactMitigationEnchantments =
            defaultArmorImpactMitigationEnchantments();
    private static Map<Identifier, DamageTypeProfile> weaponDamageEnchantments = defaultWeaponDamageEnchantments();
    private static Map<Identifier, DamageTypeProfile> weaponDamageFirstTwoBonusEnchantments =
            defaultWeaponDamageFirstTwoBonusEnchantments();
    private static Map<Identifier, Double> durabilityReductionEnchantments = defaultDurabilityReductionEnchantments();
    private static double maxDurabilityReduction = 0.9;
    private static double lowStaminaArmorPanelReduction = 0.10;
    private static double exhaustedArmorPanelReduction = 0.25;
    private static double imperfectBlockImpactMitigation = 0.75;
    private static double perfectBlockImpactMitigation = 0.95;
    private static double perfectBlockImpact = 15.0;
    public static double blockedStrikeToImpactRatio() {
        return blockedStrikeToImpactRatio;
    }

    public static void setBlockedStrikeToImpactRatio(double value) {
        blockedStrikeToImpactRatio = Math.max(0.0, value);
    }
    private EquipmentFallbackConfig() {
    }

    public static void reset() {
        defaultWeapon = DamageTypeProfile.even(1.0);
        sword = new DamageTypeProfile(1.2, 0.2, 1.3);
        longsword = new DamageTypeProfile(1.2, 0.25, 1.2);
        polearm = new DamageTypeProfile(1.0, 0.35, 0.65);
        pickaxe = new DamageTypeProfile(1.8, 0.9, 0.3);
        axe = new DamageTypeProfile(0.3, 0.9, 0.9);
        fightingMace = new DamageTypeProfile(0.0, 1.7, 0.0);
        shovel = DamageTypeProfile.even(1.0);
        hoe = DamageTypeProfile.even(1.0);
        defaultWeaponBaseImpact = 12.0;
        swordBaseImpact = 20.0;
        longswordBaseImpact = 20.0;
        polearmBaseImpact = 22.0;
        pickaxeBaseImpact = 24.0;
        axeBaseImpact = 26.0;
        fightingMaceBaseImpact = 12.0;
        shovelBaseImpact = 12.0;
        hoeBaseImpact = 12.0;
        defaultWeaponBlockImpactMitigation = 0.75;
        swordBlockImpactMitigation = 0.75;
        longswordBlockImpactMitigation = 0.75;
        polearmBlockImpactMitigation = 0.70;
        pickaxeBlockImpactMitigation = 0.75;
        axeBlockImpactMitigation = 0.75;
        fightingMaceBlockImpactMitigation = 0.75;
        shovelBlockImpactMitigation = 0.75;
        hoeBlockImpactMitigation = 0.75;
        defaultArmorBreakMultiplier = 1.0;
        swordArmorBreakMultiplier = 1.0;
        longswordArmorBreakMultiplier = 1.15;
        polearmArmorBreakMultiplier = 0.45;
        pickaxeArmorBreakMultiplier = 1.35;
        axeArmorBreakMultiplier = 1.2;
        fightingMaceArmorBreakMultiplier = 1.0;
        shovelArmorBreakMultiplier = 1.0;
        hoeArmorBreakMultiplier = 1.0;
        defaultWeaponAttackSpeedMultiplier = 1.0;
        swordAttackSpeedMultiplier = 1.0;
        longswordAttackSpeedMultiplier = 1.0;
        polearmAttackSpeedMultiplier = 1.0;
        pickaxeAttackSpeedMultiplier = 1.0;
        axeAttackSpeedMultiplier = 1.0;
        fightingMaceAttackSpeedMultiplier = 1.0;
        shovelAttackSpeedMultiplier = 1.0;
        hoeAttackSpeedMultiplier = 1.0;
        defaultWeaponToughness = 1.0;
        defaultMinimumDurabilityPanelMultiplier = 0.40;
        polearmWeaponToughness = 1.0;
        polearmMinimumDurabilityPanelMultiplier = 0.40;
        defaultHeldMovementSpeedMultiplier = 1.0;
        polearmHeldMovementSpeedMultiplier = 1.0;
        defaultWeaponRealHitboxSizeUnits = Vec3d.ZERO;
        defaultWeaponRealHitboxOffsetUnits = Vec3d.ZERO;
        defaultWeaponRealHitboxRotationDegrees = Vec3d.ZERO;
        swordRealHitboxSizeUnits = Vec3d.ZERO;
        swordRealHitboxOffsetUnits = Vec3d.ZERO;
        swordRealHitboxRotationDegrees = Vec3d.ZERO;
        longswordRealHitboxSizeUnits = new Vec3d(5.0, 1.5, 19.0);
        longswordRealHitboxOffsetUnits = Vec3d.ZERO;
        longswordRealHitboxRotationDegrees = new Vec3d(0.0, 0.0, -45.0);
        polearmRealHitboxSizeUnits = new Vec3d(4.0, 4.0, 34.0);
        polearmRealHitboxOffsetUnits = Vec3d.ZERO;
        polearmRealHitboxRotationDegrees = Vec3d.ZERO;
        pickaxeRealHitboxSizeUnits = Vec3d.ZERO;
        pickaxeRealHitboxOffsetUnits = Vec3d.ZERO;
        pickaxeRealHitboxRotationDegrees = Vec3d.ZERO;
        axeRealHitboxSizeUnits = Vec3d.ZERO;
        axeRealHitboxOffsetUnits = Vec3d.ZERO;
        axeRealHitboxRotationDegrees = Vec3d.ZERO;
        fightingMaceRealHitboxSizeUnits = Vec3d.ZERO;
        fightingMaceRealHitboxOffsetUnits = Vec3d.ZERO;
        fightingMaceRealHitboxRotationDegrees = Vec3d.ZERO;
        shovelRealHitboxSizeUnits = Vec3d.ZERO;
        shovelRealHitboxOffsetUnits = Vec3d.ZERO;
        shovelRealHitboxRotationDegrees = Vec3d.ZERO;
        hoeRealHitboxSizeUnits = Vec3d.ZERO;
        hoeRealHitboxOffsetUnits = Vec3d.ZERO;
        hoeRealHitboxRotationDegrees = Vec3d.ZERO;
        defaultWeaponAttackMoveIds = Map.of();
        defaultWeaponStanceAnimationNames = Map.of();
        defaultWeaponExecutionMoveIds = Map.of();
        swordAttackMoveIds = Map.of();
        swordStanceAnimationNames = Map.of();
        longswordAttackMoveIds = Map.of();
        longswordStanceAnimationNames = Map.of();
        polearmAttackMoveIds = Map.of(
                "right", "attack_right_longweapon", "left", "attack_left_longweapon", "up", "attack_up_longsword", "down", "attack_down_longweapon");
        polearmStanceAnimationNames = Map.of(
                "right", "stance_right_longweapon", "left", "stance_left_longweapon", "up", "stance_up_longsword", "down", "stance_down_longweapon");
        pickaxeAttackMoveIds = Map.of();
        pickaxeStanceAnimationNames = Map.of();
        axeAttackMoveIds = Map.of();
        axeStanceAnimationNames = Map.of();
        fightingMaceAttackMoveIds = Map.of(
                "right", "attack_right_heavy",
                "left", "attack_left_heavy",
                "up", "attack_up_heavy",
                "down", "attack_down_heavy"
        );
        fightingMaceStanceAnimationNames = Map.of();
        shovelAttackMoveIds = Map.of();
        shovelStanceAnimationNames = Map.of();
        hoeAttackMoveIds = Map.of();
        hoeStanceAnimationNames = Map.of();
        configuredLongswordItems = Set.of();
        configuredShortSwordItems = Set.of();
        configuredHeavyWeaponItems = Set.of();
        configuredPolearmItems = Set.of();
        helmetAndBootsArmorMultiplier = new DamageTypeProfile(18.0, 12.0, 20.0);
        chestplateAndLeggingsArmorMultiplier = new DamageTypeProfile(10.0, 7.0, 12.0);
        protectionPercent = 0.5;
        impactMitigationPercent = 0.0;
        slotParts = defaultSlotParts();
        partDamageMultipliers = defaultPartDamageMultipliers();
        armorDefenseEnchantments = defaultArmorDefenseEnchantments();
        armorDefenseFirstTwoBonusEnchantments = defaultArmorDefenseFirstTwoBonusEnchantments();
        armorImpactMitigationEnchantments = defaultArmorImpactMitigationEnchantments();
        weaponDamageEnchantments = defaultWeaponDamageEnchantments();
        weaponDamageFirstTwoBonusEnchantments = defaultWeaponDamageFirstTwoBonusEnchantments();
        durabilityReductionEnchantments = defaultDurabilityReductionEnchantments();
        maxDurabilityReduction = 0.9;
        lowStaminaArmorPanelReduction = 0.10;
        exhaustedArmorPanelReduction = 0.25;
        imperfectBlockImpactMitigation = 0.75;
        perfectBlockImpactMitigation = 0.95;
        perfectBlockImpact = 15.0;
        blockedStrikeToImpactRatio = 0.15;
    }

    public static DamageTypeProfile defaultWeapon() {
        return defaultWeapon;
    }

    public static void setDefaultWeapon(DamageTypeProfile profile) {
        defaultWeapon = profile;
    }

    public static double defaultWeaponBaseImpact() {
        return defaultWeaponBaseImpact;
    }

    public static void setDefaultWeaponBaseImpact(double value) {
        defaultWeaponBaseImpact = Math.max(0.0, value);
    }

    public static double defaultWeaponBlockImpactMitigation() {
        return defaultWeaponBlockImpactMitigation;
    }

    public static void setDefaultWeaponBlockImpactMitigation(double value) {
        defaultWeaponBlockImpactMitigation = clamp01(value);
    }

    public static double defaultArmorBreakMultiplier() {
        return defaultArmorBreakMultiplier;
    }

    public static void setDefaultArmorBreakMultiplier(double value) {
        defaultArmorBreakMultiplier = Math.max(0.0, value);
    }

    public static double defaultWeaponAttackSpeedMultiplier() {
        return defaultWeaponAttackSpeedMultiplier;
    }

    public static void setDefaultWeaponAttackSpeedMultiplier(double value) {
        defaultWeaponAttackSpeedMultiplier = sanitizeAttackSpeedMultiplier(value);
    }

    public static double defaultWeaponToughness() {
        return defaultWeaponToughness;
    }

    public static void setDefaultWeaponToughness(double value) {
        defaultWeaponToughness = Math.max(0.0, value);
    }

    public static double defaultMinimumDurabilityPanelMultiplier() {
        return defaultMinimumDurabilityPanelMultiplier;
    }

    public static void setDefaultMinimumDurabilityPanelMultiplier(double value) {
        defaultMinimumDurabilityPanelMultiplier = clamp01(value);
    }

    public static double polearmWeaponToughness() {
        return polearmWeaponToughness;
    }

    public static void setPolearmWeaponToughness(double value) {
        polearmWeaponToughness = Math.max(0.0, value);
    }

    public static double polearmMinimumDurabilityPanelMultiplier() {
        return polearmMinimumDurabilityPanelMultiplier;
    }

    public static void setPolearmMinimumDurabilityPanelMultiplier(double value) {
        polearmMinimumDurabilityPanelMultiplier = clamp01(value);
    }

    public static double defaultHeldMovementSpeedMultiplier() {
        return defaultHeldMovementSpeedMultiplier;
    }

    public static void setDefaultHeldMovementSpeedMultiplier(double value) {
        defaultHeldMovementSpeedMultiplier = clamp01(value);
    }

    public static double polearmHeldMovementSpeedMultiplier() {
        return polearmHeldMovementSpeedMultiplier;
    }

    public static void setPolearmHeldMovementSpeedMultiplier(double value) {
        polearmHeldMovementSpeedMultiplier = clamp01(value);
    }

    public static Vec3d defaultWeaponRealHitboxSizeUnits() {
        return defaultWeaponRealHitboxSizeUnits;
    }

    public static void setDefaultWeaponRealHitboxSizeUnits(Vec3d value) {
        defaultWeaponRealHitboxSizeUnits = nonNullVec(value);
    }

    public static Vec3d defaultWeaponRealHitboxOffsetUnits() {
        return defaultWeaponRealHitboxOffsetUnits;
    }

    public static void setDefaultWeaponRealHitboxOffsetUnits(Vec3d value) {
        defaultWeaponRealHitboxOffsetUnits = nonNullVec(value);
    }

    public static Vec3d defaultWeaponRealHitboxRotationDegrees() {
        return defaultWeaponRealHitboxRotationDegrees;
    }

    public static void setDefaultWeaponRealHitboxRotationDegrees(Vec3d value) {
        defaultWeaponRealHitboxRotationDegrees = nonNullVec(value);
    }

    public static Map<String, String> defaultWeaponAttackMoveIds() {
        return defaultWeaponAttackMoveIds;
    }

    public static void setDefaultWeaponAttackMoveIds(Map<String, String> values) {
        defaultWeaponAttackMoveIds = copyStringMap(values);
    }

    public static Map<String, String> defaultWeaponExecutionMoveIds() {
        return defaultWeaponExecutionMoveIds;
    }

    public static void setDefaultWeaponExecutionMoveIds(Map<String, String> values) {
        defaultWeaponExecutionMoveIds = copyStringMap(values);
    }

    public static Map<String, String> defaultWeaponStanceAnimationNames() {
        return defaultWeaponStanceAnimationNames;
    }

    public static void setDefaultWeaponStanceAnimationNames(Map<String, String> values) {
        defaultWeaponStanceAnimationNames = copyStringMap(values);
    }

    public static DamageTypeProfile sword() {
        return sword;
    }

    public static void setSword(DamageTypeProfile profile) {
        sword = profile;
    }

    public static DamageTypeProfile longsword() {
        return longsword;
    }

    public static void setLongsword(DamageTypeProfile profile) {
        longsword = profile;
    }

    public static double swordBaseImpact() {
        return swordBaseImpact;
    }

    public static void setSwordBaseImpact(double value) {
        swordBaseImpact = Math.max(0.0, value);
    }

    public static double longswordBaseImpact() {
        return longswordBaseImpact;
    }

    public static void setLongswordBaseImpact(double value) {
        longswordBaseImpact = Math.max(0.0, value);
    }

    public static double swordBlockImpactMitigation() {
        return swordBlockImpactMitigation;
    }

    public static void setSwordBlockImpactMitigation(double value) {
        swordBlockImpactMitigation = clamp01(value);
    }

    public static double longswordBlockImpactMitigation() {
        return longswordBlockImpactMitigation;
    }

    public static void setLongswordBlockImpactMitigation(double value) {
        longswordBlockImpactMitigation = clamp01(value);
    }

    public static double swordArmorBreakMultiplier() {
        return swordArmorBreakMultiplier;
    }

    public static void setSwordArmorBreakMultiplier(double value) {
        swordArmorBreakMultiplier = Math.max(0.0, value);
    }

    public static double swordAttackSpeedMultiplier() {
        return swordAttackSpeedMultiplier;
    }

    public static void setSwordAttackSpeedMultiplier(double value) {
        swordAttackSpeedMultiplier = sanitizeAttackSpeedMultiplier(value);
    }

    public static Vec3d swordRealHitboxSizeUnits() {
        return swordRealHitboxSizeUnits;
    }

    public static void setSwordRealHitboxSizeUnits(Vec3d value) {
        swordRealHitboxSizeUnits = nonNullVec(value);
    }

    public static Vec3d swordRealHitboxOffsetUnits() {
        return swordRealHitboxOffsetUnits;
    }

    public static void setSwordRealHitboxOffsetUnits(Vec3d value) {
        swordRealHitboxOffsetUnits = nonNullVec(value);
    }

    public static Vec3d swordRealHitboxRotationDegrees() {
        return swordRealHitboxRotationDegrees;
    }

    public static void setSwordRealHitboxRotationDegrees(Vec3d value) {
        swordRealHitboxRotationDegrees = nonNullVec(value);
    }

    public static Map<String, String> swordAttackMoveIds() {
        return swordAttackMoveIds;
    }

    public static void setSwordAttackMoveIds(Map<String, String> values) {
        swordAttackMoveIds = copyStringMap(values);
    }

    public static Map<String, String> swordStanceAnimationNames() {
        return swordStanceAnimationNames;
    }

    public static void setSwordStanceAnimationNames(Map<String, String> values) {
        swordStanceAnimationNames = copyStringMap(values);
    }

    public static double longswordArmorBreakMultiplier() {
        return longswordArmorBreakMultiplier;
    }

    public static void setLongswordArmorBreakMultiplier(double value) {
        longswordArmorBreakMultiplier = Math.max(0.0, value);
    }

    public static double longswordAttackSpeedMultiplier() {
        return longswordAttackSpeedMultiplier;
    }

    public static void setLongswordAttackSpeedMultiplier(double value) {
        longswordAttackSpeedMultiplier = sanitizeAttackSpeedMultiplier(value);
    }

    public static Vec3d longswordRealHitboxSizeUnits() {
        return longswordRealHitboxSizeUnits;
    }

    public static void setLongswordRealHitboxSizeUnits(Vec3d value) {
        longswordRealHitboxSizeUnits = nonNullVec(value);
    }

    public static Vec3d longswordRealHitboxOffsetUnits() {
        return longswordRealHitboxOffsetUnits;
    }

    public static void setLongswordRealHitboxOffsetUnits(Vec3d value) {
        longswordRealHitboxOffsetUnits = nonNullVec(value);
    }

    public static Vec3d longswordRealHitboxRotationDegrees() {
        return longswordRealHitboxRotationDegrees;
    }

    public static void setLongswordRealHitboxRotationDegrees(Vec3d value) {
        longswordRealHitboxRotationDegrees = nonNullVec(value);
    }

    public static Map<String, String> longswordAttackMoveIds() {
        return longswordAttackMoveIds;
    }

    public static void setLongswordAttackMoveIds(Map<String, String> values) {
        longswordAttackMoveIds = copyStringMap(values);
    }

    public static Map<String, String> longswordStanceAnimationNames() {
        return longswordStanceAnimationNames;
    }

    public static void setLongswordStanceAnimationNames(Map<String, String> values) {
        longswordStanceAnimationNames = copyStringMap(values);
    }

    public static DamageTypeProfile polearm() { return polearm; }
    public static void setPolearm(DamageTypeProfile value) { polearm = value; }
    public static double polearmBaseImpact() { return polearmBaseImpact; }
    public static void setPolearmBaseImpact(double value) { polearmBaseImpact = Math.max(0.0, value); }
    public static double polearmBlockImpactMitigation() { return polearmBlockImpactMitigation; }
    public static void setPolearmBlockImpactMitigation(double value) { polearmBlockImpactMitigation = clamp01(value); }
    public static double polearmArmorBreakMultiplier() { return polearmArmorBreakMultiplier; }
    public static void setPolearmArmorBreakMultiplier(double value) { polearmArmorBreakMultiplier = Math.max(0.0, value); }
    public static double polearmAttackSpeedMultiplier() { return polearmAttackSpeedMultiplier; }
    public static void setPolearmAttackSpeedMultiplier(double value) { polearmAttackSpeedMultiplier = sanitizeAttackSpeedMultiplier(value); }
    public static Vec3d polearmRealHitboxSizeUnits() { return polearmRealHitboxSizeUnits; }
    public static void setPolearmRealHitboxSizeUnits(Vec3d value) { polearmRealHitboxSizeUnits = nonNullVec(value); }
    public static Vec3d polearmRealHitboxOffsetUnits() { return polearmRealHitboxOffsetUnits; }
    public static void setPolearmRealHitboxOffsetUnits(Vec3d value) { polearmRealHitboxOffsetUnits = nonNullVec(value); }
    public static Vec3d polearmRealHitboxRotationDegrees() { return polearmRealHitboxRotationDegrees; }
    public static void setPolearmRealHitboxRotationDegrees(Vec3d value) { polearmRealHitboxRotationDegrees = nonNullVec(value); }
    public static Map<String, String> polearmAttackMoveIds() { return polearmAttackMoveIds; }
    public static void setPolearmAttackMoveIds(Map<String, String> values) { polearmAttackMoveIds = copyStringMap(values); }
    public static Map<String, String> polearmStanceAnimationNames() { return polearmStanceAnimationNames; }
    public static void setPolearmStanceAnimationNames(Map<String, String> values) { polearmStanceAnimationNames = copyStringMap(values); }

    public static boolean isConfiguredLongswordItem(Identifier itemId) {
        return itemId != null && configuredLongswordItems.contains(itemId);
    }

    public static Set<Identifier> configuredLongswordItems() { return configuredLongswordItems; }
    public static Set<Identifier> configuredShortSwordItems() { return configuredShortSwordItems; }
    public static Set<Identifier> configuredHeavyWeaponItems() { return configuredHeavyWeaponItems; }
    public static Set<Identifier> configuredPolearmItems() { return configuredPolearmItems; }

    public static void setConfiguredLongswordItems(Set<Identifier> itemIds) {
        configuredLongswordItems = itemIds == null ? Set.of() : Set.copyOf(itemIds);
    }

    public static boolean isConfiguredShortSwordItem(Identifier itemId) {
        return itemId != null && configuredShortSwordItems.contains(itemId);
    }

    public static void setConfiguredShortSwordItems(Set<Identifier> itemIds) {
        configuredShortSwordItems = itemIds == null ? Set.of() : Set.copyOf(itemIds);
    }

    public static boolean isConfiguredHeavyWeaponItem(Identifier itemId) {
        return itemId != null && configuredHeavyWeaponItems.contains(itemId);
    }

    public static void setConfiguredHeavyWeaponItems(Set<Identifier> itemIds) {
        configuredHeavyWeaponItems = itemIds == null ? Set.of() : Set.copyOf(itemIds);
    }

    public static boolean isConfiguredPolearmItem(Identifier itemId) {
        return itemId != null && configuredPolearmItems.contains(itemId);
    }

    public static void setConfiguredPolearmItems(Set<Identifier> itemIds) {
        configuredPolearmItems = itemIds == null ? Set.of() : Set.copyOf(itemIds);
    }

    public static DamageTypeProfile pickaxe() {
        return pickaxe;
    }

    public static void setPickaxe(DamageTypeProfile profile) {
        pickaxe = profile;
    }

    public static double pickaxeBaseImpact() {
        return pickaxeBaseImpact;
    }

    public static void setPickaxeBaseImpact(double value) {
        pickaxeBaseImpact = Math.max(0.0, value);
    }

    public static double pickaxeBlockImpactMitigation() {
        return pickaxeBlockImpactMitigation;
    }

    public static void setPickaxeBlockImpactMitigation(double value) {
        pickaxeBlockImpactMitigation = clamp01(value);
    }

    public static double pickaxeArmorBreakMultiplier() {
        return pickaxeArmorBreakMultiplier;
    }

    public static void setPickaxeArmorBreakMultiplier(double value) {
        pickaxeArmorBreakMultiplier = Math.max(0.0, value);
    }

    public static double pickaxeAttackSpeedMultiplier() {
        return pickaxeAttackSpeedMultiplier;
    }

    public static void setPickaxeAttackSpeedMultiplier(double value) {
        pickaxeAttackSpeedMultiplier = sanitizeAttackSpeedMultiplier(value);
    }

    public static Vec3d pickaxeRealHitboxSizeUnits() {
        return pickaxeRealHitboxSizeUnits;
    }

    public static void setPickaxeRealHitboxSizeUnits(Vec3d value) {
        pickaxeRealHitboxSizeUnits = nonNullVec(value);
    }

    public static Vec3d pickaxeRealHitboxOffsetUnits() {
        return pickaxeRealHitboxOffsetUnits;
    }

    public static void setPickaxeRealHitboxOffsetUnits(Vec3d value) {
        pickaxeRealHitboxOffsetUnits = nonNullVec(value);
    }

    public static Vec3d pickaxeRealHitboxRotationDegrees() {
        return pickaxeRealHitboxRotationDegrees;
    }

    public static void setPickaxeRealHitboxRotationDegrees(Vec3d value) {
        pickaxeRealHitboxRotationDegrees = nonNullVec(value);
    }

    public static Map<String, String> pickaxeAttackMoveIds() {
        return pickaxeAttackMoveIds;
    }

    public static void setPickaxeAttackMoveIds(Map<String, String> values) {
        pickaxeAttackMoveIds = copyStringMap(values);
    }

    public static Map<String, String> pickaxeStanceAnimationNames() {
        return pickaxeStanceAnimationNames;
    }

    public static void setPickaxeStanceAnimationNames(Map<String, String> values) {
        pickaxeStanceAnimationNames = copyStringMap(values);
    }

    public static DamageTypeProfile axe() {
        return axe;
    }

    public static void setAxe(DamageTypeProfile profile) {
        axe = profile;
    }

    public static double axeBaseImpact() {
        return axeBaseImpact;
    }

    public static void setAxeBaseImpact(double value) {
        axeBaseImpact = Math.max(0.0, value);
    }

    public static double axeBlockImpactMitigation() {
        return axeBlockImpactMitigation;
    }

    public static void setAxeBlockImpactMitigation(double value) {
        axeBlockImpactMitigation = clamp01(value);
    }

    public static double axeArmorBreakMultiplier() {
        return axeArmorBreakMultiplier;
    }

    public static void setAxeArmorBreakMultiplier(double value) {
        axeArmorBreakMultiplier = Math.max(0.0, value);
    }

    public static double axeAttackSpeedMultiplier() {
        return axeAttackSpeedMultiplier;
    }

    public static void setAxeAttackSpeedMultiplier(double value) {
        axeAttackSpeedMultiplier = sanitizeAttackSpeedMultiplier(value);
    }

    public static Vec3d axeRealHitboxSizeUnits() {
        return axeRealHitboxSizeUnits;
    }

    public static void setAxeRealHitboxSizeUnits(Vec3d value) {
        axeRealHitboxSizeUnits = nonNullVec(value);
    }

    public static Vec3d axeRealHitboxOffsetUnits() {
        return axeRealHitboxOffsetUnits;
    }

    public static void setAxeRealHitboxOffsetUnits(Vec3d value) {
        axeRealHitboxOffsetUnits = nonNullVec(value);
    }

    public static Vec3d axeRealHitboxRotationDegrees() {
        return axeRealHitboxRotationDegrees;
    }

    public static void setAxeRealHitboxRotationDegrees(Vec3d value) {
        axeRealHitboxRotationDegrees = nonNullVec(value);
    }

    public static Map<String, String> axeAttackMoveIds() {
        return axeAttackMoveIds;
    }

    public static void setAxeAttackMoveIds(Map<String, String> values) {
        axeAttackMoveIds = copyStringMap(values);
    }

    public static Map<String, String> axeStanceAnimationNames() {
        return axeStanceAnimationNames;
    }

    public static void setAxeStanceAnimationNames(Map<String, String> values) {
        axeStanceAnimationNames = copyStringMap(values);
    }

    public static DamageTypeProfile fightingMace() {
        return fightingMace;
    }

    public static void setFightingMace(DamageTypeProfile profile) {
        fightingMace = profile;
    }

    public static double fightingMaceBaseImpact() {
        return fightingMaceBaseImpact;
    }

    public static void setFightingMaceBaseImpact(double value) {
        fightingMaceBaseImpact = Math.max(0.0, value);
    }

    public static double fightingMaceBlockImpactMitigation() {
        return fightingMaceBlockImpactMitigation;
    }

    public static void setFightingMaceBlockImpactMitigation(double value) {
        fightingMaceBlockImpactMitigation = clamp01(value);
    }

    public static double fightingMaceArmorBreakMultiplier() {
        return fightingMaceArmorBreakMultiplier;
    }

    public static void setFightingMaceArmorBreakMultiplier(double value) {
        fightingMaceArmorBreakMultiplier = Math.max(0.0, value);
    }

    public static double fightingMaceAttackSpeedMultiplier() {
        return fightingMaceAttackSpeedMultiplier;
    }

    public static void setFightingMaceAttackSpeedMultiplier(double value) {
        fightingMaceAttackSpeedMultiplier = sanitizeAttackSpeedMultiplier(value);
    }

    public static Vec3d fightingMaceRealHitboxSizeUnits() {
        return fightingMaceRealHitboxSizeUnits;
    }

    public static void setFightingMaceRealHitboxSizeUnits(Vec3d value) {
        fightingMaceRealHitboxSizeUnits = nonNullVec(value);
    }

    public static Vec3d fightingMaceRealHitboxOffsetUnits() {
        return fightingMaceRealHitboxOffsetUnits;
    }

    public static void setFightingMaceRealHitboxOffsetUnits(Vec3d value) {
        fightingMaceRealHitboxOffsetUnits = nonNullVec(value);
    }

    public static Vec3d fightingMaceRealHitboxRotationDegrees() {
        return fightingMaceRealHitboxRotationDegrees;
    }

    public static void setFightingMaceRealHitboxRotationDegrees(Vec3d value) {
        fightingMaceRealHitboxRotationDegrees = nonNullVec(value);
    }

    public static Map<String, String> fightingMaceAttackMoveIds() {
        return fightingMaceAttackMoveIds;
    }

    public static void setFightingMaceAttackMoveIds(Map<String, String> values) {
        fightingMaceAttackMoveIds = copyStringMap(values);
    }

    public static Map<String, String> fightingMaceStanceAnimationNames() {
        return fightingMaceStanceAnimationNames;
    }

    public static void setFightingMaceStanceAnimationNames(Map<String, String> values) {
        fightingMaceStanceAnimationNames = copyStringMap(values);
    }

    public static DamageTypeProfile shovel() {
        return shovel;
    }

    public static void setShovel(DamageTypeProfile profile) {
        shovel = profile;
    }

    public static double shovelBaseImpact() {
        return shovelBaseImpact;
    }

    public static void setShovelBaseImpact(double value) {
        shovelBaseImpact = Math.max(0.0, value);
    }

    public static double shovelBlockImpactMitigation() {
        return shovelBlockImpactMitigation;
    }

    public static void setShovelBlockImpactMitigation(double value) {
        shovelBlockImpactMitigation = clamp01(value);
    }

    public static double shovelArmorBreakMultiplier() {
        return shovelArmorBreakMultiplier;
    }

    public static void setShovelArmorBreakMultiplier(double value) {
        shovelArmorBreakMultiplier = Math.max(0.0, value);
    }

    public static double shovelAttackSpeedMultiplier() {
        return shovelAttackSpeedMultiplier;
    }

    public static void setShovelAttackSpeedMultiplier(double value) {
        shovelAttackSpeedMultiplier = sanitizeAttackSpeedMultiplier(value);
    }

    public static Vec3d shovelRealHitboxSizeUnits() {
        return shovelRealHitboxSizeUnits;
    }

    public static void setShovelRealHitboxSizeUnits(Vec3d value) {
        shovelRealHitboxSizeUnits = nonNullVec(value);
    }

    public static Vec3d shovelRealHitboxOffsetUnits() {
        return shovelRealHitboxOffsetUnits;
    }

    public static void setShovelRealHitboxOffsetUnits(Vec3d value) {
        shovelRealHitboxOffsetUnits = nonNullVec(value);
    }

    public static Vec3d shovelRealHitboxRotationDegrees() {
        return shovelRealHitboxRotationDegrees;
    }

    public static void setShovelRealHitboxRotationDegrees(Vec3d value) {
        shovelRealHitboxRotationDegrees = nonNullVec(value);
    }

    public static Map<String, String> shovelAttackMoveIds() {
        return shovelAttackMoveIds;
    }

    public static void setShovelAttackMoveIds(Map<String, String> values) {
        shovelAttackMoveIds = copyStringMap(values);
    }

    public static Map<String, String> shovelStanceAnimationNames() {
        return shovelStanceAnimationNames;
    }

    public static void setShovelStanceAnimationNames(Map<String, String> values) {
        shovelStanceAnimationNames = copyStringMap(values);
    }

    public static DamageTypeProfile hoe() {
        return hoe;
    }

    public static void setHoe(DamageTypeProfile profile) {
        hoe = profile;
    }

    public static double hoeBaseImpact() {
        return hoeBaseImpact;
    }

    public static void setHoeBaseImpact(double value) {
        hoeBaseImpact = Math.max(0.0, value);
    }

    public static double hoeBlockImpactMitigation() {
        return hoeBlockImpactMitigation;
    }

    public static void setHoeBlockImpactMitigation(double value) {
        hoeBlockImpactMitigation = clamp01(value);
    }

    public static double hoeArmorBreakMultiplier() {
        return hoeArmorBreakMultiplier;
    }

    public static void setHoeArmorBreakMultiplier(double value) {
        hoeArmorBreakMultiplier = Math.max(0.0, value);
    }

    public static double hoeAttackSpeedMultiplier() {
        return hoeAttackSpeedMultiplier;
    }

    public static void setHoeAttackSpeedMultiplier(double value) {
        hoeAttackSpeedMultiplier = sanitizeAttackSpeedMultiplier(value);
    }

    public static Vec3d hoeRealHitboxSizeUnits() {
        return hoeRealHitboxSizeUnits;
    }

    public static void setHoeRealHitboxSizeUnits(Vec3d value) {
        hoeRealHitboxSizeUnits = nonNullVec(value);
    }

    public static Vec3d hoeRealHitboxOffsetUnits() {
        return hoeRealHitboxOffsetUnits;
    }

    public static void setHoeRealHitboxOffsetUnits(Vec3d value) {
        hoeRealHitboxOffsetUnits = nonNullVec(value);
    }

    public static Vec3d hoeRealHitboxRotationDegrees() {
        return hoeRealHitboxRotationDegrees;
    }

    public static void setHoeRealHitboxRotationDegrees(Vec3d value) {
        hoeRealHitboxRotationDegrees = nonNullVec(value);
    }

    public static Map<String, String> hoeAttackMoveIds() {
        return hoeAttackMoveIds;
    }

    public static void setHoeAttackMoveIds(Map<String, String> values) {
        hoeAttackMoveIds = copyStringMap(values);
    }

    public static Map<String, String> hoeStanceAnimationNames() {
        return hoeStanceAnimationNames;
    }

    public static void setHoeStanceAnimationNames(Map<String, String> values) {
        hoeStanceAnimationNames = copyStringMap(values);
    }

    public static DamageTypeProfile armorMultiplier(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD, FEET -> helmetAndBootsArmorMultiplier;
            case CHEST, LEGS, BODY -> chestplateAndLeggingsArmorMultiplier;
            default -> DamageTypeProfile.even(0.0);
        };
    }

    public static void setHelmetAndBootsArmorMultiplier(DamageTypeProfile profile) {
        helmetAndBootsArmorMultiplier = profile;
    }

    public static void setChestplateAndLeggingsArmorMultiplier(DamageTypeProfile profile) {
        chestplateAndLeggingsArmorMultiplier = profile;
    }

    public static double protectionPercent() {
        return protectionPercent;
    }

    public static void setProtectionPercent(double value) {
        protectionPercent = clamp01(value);
    }

    public static double impactMitigationPercent() {
        return impactMitigationPercent;
    }

    public static void setImpactMitigationPercent(double value) {
        impactMitigationPercent = clamp01(value);
    }

    public static List<String> partsFor(EquipmentSlot slot) {
        return slotParts.getOrDefault(slot, List.of());
    }

    public static void setSlotParts(Map<EquipmentSlot, List<String>> parts) {
        slotParts = Map.copyOf(parts);
    }

    public static double partDamageMultiplier(String part) {
        return partDamageMultipliers.getOrDefault(part, 1.0);
    }

    public static void setPartDamageMultipliers(Map<String, Double> multipliers) {
        partDamageMultipliers = Map.copyOf(multipliers);
    }

    public static Map<Identifier, DamageTypeProfile> armorDefenseEnchantments() {
        return armorDefenseEnchantments;
    }

    public static void setArmorDefenseEnchantments(Map<Identifier, DamageTypeProfile> enchantments) {
        armorDefenseEnchantments = Map.copyOf(enchantments);
    }

    public static Map<Identifier, DamageTypeProfile> armorDefenseFirstTwoBonusEnchantments() {
        return armorDefenseFirstTwoBonusEnchantments;
    }

    public static void setArmorDefenseFirstTwoBonusEnchantments(Map<Identifier, DamageTypeProfile> enchantments) {
        armorDefenseFirstTwoBonusEnchantments = Map.copyOf(enchantments);
    }

    public static Map<Identifier, Double> armorImpactMitigationEnchantments() {
        return armorImpactMitigationEnchantments;
    }

    public static void setArmorImpactMitigationEnchantments(Map<Identifier, Double> enchantments) {
        armorImpactMitigationEnchantments = Map.copyOf(enchantments);
    }

    public static Map<Identifier, DamageTypeProfile> weaponDamageEnchantments() {
        return weaponDamageEnchantments;
    }

    public static void setWeaponDamageEnchantments(Map<Identifier, DamageTypeProfile> enchantments) {
        weaponDamageEnchantments = Map.copyOf(enchantments);
    }

    public static Map<Identifier, DamageTypeProfile> weaponDamageFirstTwoBonusEnchantments() {
        return weaponDamageFirstTwoBonusEnchantments;
    }

    public static void setWeaponDamageFirstTwoBonusEnchantments(Map<Identifier, DamageTypeProfile> enchantments) {
        weaponDamageFirstTwoBonusEnchantments = Map.copyOf(enchantments);
    }

    public static Map<Identifier, Double> durabilityReductionEnchantments() {
        return durabilityReductionEnchantments;
    }

    public static void setDurabilityReductionEnchantments(Map<Identifier, Double> enchantments) {
        durabilityReductionEnchantments = Map.copyOf(enchantments);
    }

    public static double maxDurabilityReduction() {
        return maxDurabilityReduction;
    }

    public static void setMaxDurabilityReduction(double value) {
        maxDurabilityReduction = clamp01(value);
    }

    public static double lowStaminaArmorPanelReduction() {
        return lowStaminaArmorPanelReduction;
    }

    public static void setLowStaminaArmorPanelReduction(double value) {
        lowStaminaArmorPanelReduction = clamp01(value);
    }

    public static double exhaustedArmorPanelReduction() {
        return exhaustedArmorPanelReduction;
    }

    public static void setExhaustedArmorPanelReduction(double value) {
        exhaustedArmorPanelReduction = clamp01(value);
    }

    public static double imperfectBlockImpactMitigation() {
        return imperfectBlockImpactMitigation;
    }

    public static void setImperfectBlockImpactMitigation(double value) {
        imperfectBlockImpactMitigation = clamp01(value);
    }

    public static double perfectBlockImpactMitigation() {
        return perfectBlockImpactMitigation;
    }

    public static void setPerfectBlockImpactMitigation(double value) {
        perfectBlockImpactMitigation = clamp01(value);
    }

    public static double perfectBlockImpact() {
        return perfectBlockImpact;
    }

    public static void setPerfectBlockImpact(double value) {
        perfectBlockImpact = Math.max(0.0, value);
    }

    private static Map<EquipmentSlot, List<String>> defaultSlotParts() {
        return Map.of(
                EquipmentSlot.HEAD,
                List.of("face", "neck", "crown", "side_head"),
                EquipmentSlot.CHEST,
                List.of("shoulder", "arm", "hand", "chest", "abdomen"),
                EquipmentSlot.LEGS,
                List.of("abdomen", "thigh", "knee"),
                EquipmentSlot.FEET,
                List.of("calf", "foot"),
                EquipmentSlot.BODY,
                List.of("shoulder", "chest", "abdomen", "thigh")
        );
    }

    private static Map<String, Double> defaultPartDamageMultipliers() {
        Map<String, Double> multipliers = new HashMap<>();
        for (String part : List.of(
                "face",
                "neck",
                "crown",
                "side_head",
                "shoulder",
                "arm",
                "hand",
                "chest",
                "abdomen",
                "thigh",
                "knee",
                "calf",
                "foot"
        )) {
            multipliers.put(part, 1.0);
        }
        multipliers.put("face", 2.25);
        return multipliers;
    }

    private static Map<Identifier, DamageTypeProfile> defaultArmorDefenseEnchantments() {
        return Map.of(
                Identifier.of("minecraft", "protection"),
                new DamageTypeProfile(5.0, 5.0, 6.0)
        );
    }

    private static Map<Identifier, DamageTypeProfile> defaultArmorDefenseFirstTwoBonusEnchantments() {
        return Map.of();
    }

    private static Map<Identifier, Double> defaultArmorImpactMitigationEnchantments() {
        return Map.of(
                Identifier.of("kingdom_come_combat", "cushioning"),
                0.08
        );
    }

    private static Map<Identifier, DamageTypeProfile> defaultWeaponDamageEnchantments() {
        return Map.of(
                Identifier.of("minecraft", "sharpness"),
                new DamageTypeProfile(0.0, 0.0, 2.0)
        );
    }

    private static Map<Identifier, DamageTypeProfile> defaultWeaponDamageFirstTwoBonusEnchantments() {
        return Map.of(
                Identifier.of("minecraft", "sharpness"),
                new DamageTypeProfile(0.0, 0.0, 1.0)
        );
    }

    private static Map<Identifier, Double> defaultDurabilityReductionEnchantments() {
        return Map.of(
                Identifier.of("minecraft", "unbreaking"),
                0.10
        );
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double sanitizeAttackSpeedMultiplier(double value) {
        return Math.max(0.05, value);
    }

    private static Vec3d nonNullVec(Vec3d value) {
        return value == null ? Vec3d.ZERO : value;
    }

    private static Map<String, String> copyStringMap(Map<String, String> values) {
        return values == null ? Map.of() : Map.copyOf(values);
    }
}
