package com.kingdomcomecombat.equipment;

import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.combat.CombatControlConfig;
import com.kingdomcomecombat.passive.PassiveSkillPerks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class EquipmentCombatAttributesRegistry {
    public static final Identifier BRACERS_ENCHANTMENT_ID =
            Identifier.of("kingdom_come_combat", "bracers");
    private static final Map<Identifier, WeaponCombatAttributes> WEAPONS = new HashMap<>();
    private static final Map<Identifier, ArmorCombatAttributes> ARMOR = new HashMap<>();
    private static final Map<Identifier, ShieldCombatAttributes> SHIELDS = new HashMap<>();
    public static final java.util.List<EquipmentSlot> ARMOR_PANEL_SLOTS = java.util.List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.BODY
    );

    private EquipmentCombatAttributesRegistry() {
    }

    public static void registerWeapon(Identifier itemId, WeaponCombatAttributes attributes) {
        WEAPONS.put(itemId, attributes);
    }

    public static void registerWeapon(Item item, WeaponCombatAttributes attributes) {
        registerWeapon(Registries.ITEM.getId(item), attributes);
    }

    public static void registerArmor(Identifier itemId, ArmorCombatAttributes attributes) {
        ARMOR.put(itemId, attributes);
    }

    public static void registerArmor(Item item, ArmorCombatAttributes attributes) {
        registerArmor(Registries.ITEM.getId(item), attributes);
    }

    public static void registerShield(Identifier itemId, ShieldCombatAttributes attributes) {
        SHIELDS.put(itemId, attributes);
    }

    public static void registerShield(Item item, ShieldCombatAttributes attributes) {
        registerShield(Registries.ITEM.getId(item), attributes);
    }

    public static void clear() {
        WEAPONS.clear();
        ARMOR.clear();
        SHIELDS.clear();
    }

    public static WeaponCombatAttributes getWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return defaultFallbackWeapon();
        }

        return WEAPONS.getOrDefault(
                Registries.ITEM.getId(stack.getItem()),
                fallbackWeapon(stack)
        );
    }

    public static Optional<WeaponCombatAttributes> getConfiguredWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(WEAPONS.get(Registries.ITEM.getId(stack.getItem())));
    }

    public static Vec3d realHitboxSizeUnits(ItemStack stack, Vec3d fallback) {
        WeaponCombatAttributes attributes = getWeapon(stack);
        return attributes.hasCustomRealHitboxSize() ? attributes.realHitboxSizeUnits() : fallback;
    }

    public static Vec3d realHitboxOffsetUnits(ItemStack stack, Vec3d fallback) {
        WeaponCombatAttributes attributes = getWeapon(stack);
        return attributes.hasCustomRealHitboxOffset() ? attributes.realHitboxOffsetUnits() : fallback;
    }

    public static Vec3d realHitboxRotationDegrees(ItemStack stack, Vec3d fallback) {
        WeaponCombatAttributes attributes = getWeapon(stack);
        return attributes.hasCustomRealHitboxRotation() ? attributes.realHitboxRotationDegrees() : fallback;
    }

    public static boolean hasFallbackWeapon(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && !WEAPONS.containsKey(Registries.ITEM.getId(stack.getItem()))
                && (CombatItemUtil.isFightingMace(stack)
                        || stack.isIn(ItemTags.SWORDS)
                        || stack.isIn(ItemTags.PICKAXES)
                        || stack.isIn(ItemTags.AXES)
                        || stack.isIn(ItemTags.SHOVELS)
                        || stack.isIn(ItemTags.HOES));
    }

    public static boolean canBlockWithHeldItem(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && (CombatItemUtil.isFightingMace(stack)
                || stack.isIn(ItemTags.SWORDS)
                || stack.isIn(ItemTags.AXES)
                || stack.isIn(ItemTags.PICKAXES)
                || stack.isIn(ItemTags.SHOVELS)
                || stack.isIn(ItemTags.HOES));
    }

    public static boolean isShield(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && (stack.isOf(Items.SHIELD)
                || SHIELDS.containsKey(Registries.ITEM.getId(stack.getItem())));
    }

    public static ShieldCombatAttributes getShield(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new ShieldCombatAttributes(
                    ShieldCombatAttributes.Size.SMALL,
                    EquipmentFallbackConfig.imperfectBlockImpactMitigation()
            );
        }

        Identifier itemId = Registries.ITEM.getId(stack.getItem());
        ShieldCombatAttributes configured = SHIELDS.get(itemId);
        if (configured != null) {
            return configured;
        }

        if (stack.isOf(Items.SHIELD)) {
            return new ShieldCombatAttributes(
                    ShieldCombatAttributes.Size.LARGE,
                    CombatControlConfig.LARGE_SHIELD_BLOCK_IMPACT_MITIGATION
            );
        }

        return new ShieldCombatAttributes(
                ShieldCombatAttributes.Size.SMALL,
                EquipmentFallbackConfig.imperfectBlockImpactMitigation()
        );
    }

    public static boolean isLargeShield(ItemStack stack) {
        return isShield(stack) && getShield(stack).size() == ShieldCombatAttributes.Size.LARGE;
    }

    public static boolean isSmallShield(ItemStack stack) {
        return isShield(stack) && getShield(stack).size() == ShieldCombatAttributes.Size.SMALL;
    }

    public static ArmorCombatAttributes getArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ArmorCombatAttributes.empty();
        }

        return ARMOR.getOrDefault(
                Registries.ITEM.getId(stack.getItem()),
                fallbackArmor(stack)
        );
    }

    public static Optional<ArmorCombatAttributes> getConfiguredArmor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(ARMOR.get(Registries.ITEM.getId(stack.getItem())));
    }

    public static Map<String, ArmorCombatAttributes.PartProtection> effectiveProtectedParts(
            ItemStack stack,
            ArmorCombatAttributes armor
    ) {
        Map<String, ArmorCombatAttributes.PartProtection> parts = armor.protectedParts();
        ArmorCombatAttributes.PartProtection shoulder = parts.get("shoulder");
        if (shoulder == null || enchantmentLevel(stack, BRACERS_ENCHANTMENT_ID) <= 0) {
            return parts;
        }

        Map<String, ArmorCombatAttributes.PartProtection> effective = new LinkedHashMap<>(parts);
        effective.put("arm", shoulder);
        return Map.copyOf(effective);
    }

    public static int enchantmentLevel(ItemStack stack, Identifier enchantmentId) {
        var enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            return 0;
        }
        int level = 0;
        for (var entry : enchantments.getEnchantmentEntries()) {
            if (entry.getKey().matchesId(enchantmentId)) {
                level = Math.max(level, entry.getIntValue());
            }
        }
        return level;
    }

    public static boolean hasFallbackArmor(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && !ARMOR.containsKey(Registries.ITEM.getId(stack.getItem()))
                && armorSlot(stack) != null
                && armorValue(stack, armorSlot(stack)) > 0.0;
    }

    public static boolean hasConfiguredArmor(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && ARMOR.containsKey(Registries.ITEM.getId(stack.getItem()));
    }

    public static double armorStaminaCostMultiplier(Iterable<ItemStack> armorStacks) {
        double increase = 0.0;
        for (ItemStack stack : armorStacks) {
            increase += getArmor(stack).staminaCostIncrease();
        }
        return Math.max(0.0, 1.0 + increase);
    }

    public static double armorStaminaCostMultiplier(LivingEntity entity) {
        return 1.0 + (armorStaminaCostMultiplier(equippedArmor(entity)) - 1.0)
                * PassiveSkillPerks.armorPenaltyMultiplier(entity);
    }

    public static double armorAttackSpeedMultiplier(Iterable<ItemStack> armorStacks) {
        double penalty = 0.0;
        for (ItemStack stack : armorStacks) {
            penalty += getArmor(stack).attackSpeedPenalty();
        }
        return Math.max(0.05, 1.0 - penalty);
    }

    public static double armorAttackSpeedMultiplier(LivingEntity entity) {
        return 1.0 - (1.0 - armorAttackSpeedMultiplier(equippedArmor(entity)))
                * PassiveSkillPerks.armorPenaltyMultiplier(entity);
    }

    public static double weaponAttackSpeedMultiplier(ItemStack stack) {
        return getWeapon(stack).attackSpeedMultiplier();
    }

    public static double weaponAttackSpeedMultiplier(LivingEntity entity) {
        return entity == null ? 1.0 : weaponAttackSpeedMultiplier(entity.getMainHandStack());
    }

    public static double armorMovementSpeedMultiplier(Iterable<ItemStack> armorStacks) {
        double penalty = 0.0;
        for (ItemStack stack : armorStacks) {
            penalty += getArmor(stack).movementSpeedPenalty();
        }
        return Math.max(0.05, 1.0 - penalty);
    }

    public static double armorMovementSpeedMultiplier(LivingEntity entity) {
        return 1.0 - (1.0 - armorMovementSpeedMultiplier(equippedArmor(entity)))
                * PassiveSkillPerks.armorPenaltyMultiplier(entity);
    }

    private static java.util.List<ItemStack> equippedArmor(LivingEntity entity) {
        java.util.List<ItemStack> armor = new java.util.ArrayList<>();
        for (EquipmentSlot slot : ARMOR_PANEL_SLOTS) {
            armor.add(entity.getEquippedStack(slot));
        }
        return armor;
    }

    private static WeaponCombatAttributes fallbackWeapon(ItemStack stack) {
        if (CombatItemUtil.isLongsword(stack)) {
            return new WeaponCombatAttributes(
                    EquipmentFallbackConfig.longsword(),
                    EquipmentFallbackConfig.longswordBlockImpactMitigation(),
                    EquipmentFallbackConfig.longswordBaseImpact(),
                    EquipmentFallbackConfig.longswordArmorBreakMultiplier(),
                    EquipmentFallbackConfig.longswordAttackSpeedMultiplier(),
                    EquipmentFallbackConfig.longswordRealHitboxSizeUnits(),
                    EquipmentFallbackConfig.longswordRealHitboxOffsetUnits(),
                    EquipmentFallbackConfig.longswordRealHitboxRotationDegrees(),
                    EquipmentFallbackConfig.longswordAttackMoveIds(),
                    EquipmentFallbackConfig.longswordStanceAnimationNames()
            );
        }

        if (CombatItemUtil.isShortSword(stack)) {
            return new WeaponCombatAttributes(
                    EquipmentFallbackConfig.sword(),
                    EquipmentFallbackConfig.swordBlockImpactMitigation(),
                    EquipmentFallbackConfig.swordBaseImpact(),
                    EquipmentFallbackConfig.swordArmorBreakMultiplier(),
                    EquipmentFallbackConfig.swordAttackSpeedMultiplier(),
                    EquipmentFallbackConfig.swordRealHitboxSizeUnits(),
                    EquipmentFallbackConfig.swordRealHitboxOffsetUnits(),
                    EquipmentFallbackConfig.swordRealHitboxRotationDegrees(),
                    EquipmentFallbackConfig.swordAttackMoveIds(),
                    EquipmentFallbackConfig.swordStanceAnimationNames()
            );
        }

        if (CombatItemUtil.isHeavyWeapon(stack)) {
            return new WeaponCombatAttributes(
                    EquipmentFallbackConfig.fightingMace(),
                    EquipmentFallbackConfig.fightingMaceBlockImpactMitigation(),
                    EquipmentFallbackConfig.fightingMaceBaseImpact(),
                    EquipmentFallbackConfig.fightingMaceArmorBreakMultiplier(),
                    EquipmentFallbackConfig.fightingMaceAttackSpeedMultiplier(),
                    EquipmentFallbackConfig.fightingMaceRealHitboxSizeUnits(),
                    EquipmentFallbackConfig.fightingMaceRealHitboxOffsetUnits(),
                    EquipmentFallbackConfig.fightingMaceRealHitboxRotationDegrees(),
                    EquipmentFallbackConfig.fightingMaceAttackMoveIds(),
                    EquipmentFallbackConfig.fightingMaceStanceAnimationNames()
            );
        }

        if (stack.isIn(ItemTags.PICKAXES)) {
            return new WeaponCombatAttributes(
                    EquipmentFallbackConfig.pickaxe(),
                    EquipmentFallbackConfig.pickaxeBlockImpactMitigation(),
                    EquipmentFallbackConfig.pickaxeBaseImpact(),
                    EquipmentFallbackConfig.pickaxeArmorBreakMultiplier(),
                    EquipmentFallbackConfig.pickaxeAttackSpeedMultiplier(),
                    EquipmentFallbackConfig.pickaxeRealHitboxSizeUnits(),
                    EquipmentFallbackConfig.pickaxeRealHitboxOffsetUnits(),
                    EquipmentFallbackConfig.pickaxeRealHitboxRotationDegrees(),
                    EquipmentFallbackConfig.pickaxeAttackMoveIds(),
                    EquipmentFallbackConfig.pickaxeStanceAnimationNames()
            );
        }

        if (stack.isIn(ItemTags.AXES)) {
            return new WeaponCombatAttributes(
                    EquipmentFallbackConfig.axe(),
                    EquipmentFallbackConfig.axeBlockImpactMitigation(),
                    EquipmentFallbackConfig.axeBaseImpact(),
                    EquipmentFallbackConfig.axeArmorBreakMultiplier(),
                    EquipmentFallbackConfig.axeAttackSpeedMultiplier(),
                    EquipmentFallbackConfig.axeRealHitboxSizeUnits(),
                    EquipmentFallbackConfig.axeRealHitboxOffsetUnits(),
                    EquipmentFallbackConfig.axeRealHitboxRotationDegrees(),
                    EquipmentFallbackConfig.axeAttackMoveIds(),
                    EquipmentFallbackConfig.axeStanceAnimationNames()
            );
        }

        if (stack.isIn(ItemTags.SHOVELS)) {
            return new WeaponCombatAttributes(
                    EquipmentFallbackConfig.shovel(),
                    EquipmentFallbackConfig.shovelBlockImpactMitigation(),
                    EquipmentFallbackConfig.shovelBaseImpact(),
                    EquipmentFallbackConfig.shovelArmorBreakMultiplier(),
                    EquipmentFallbackConfig.shovelAttackSpeedMultiplier(),
                    EquipmentFallbackConfig.shovelRealHitboxSizeUnits(),
                    EquipmentFallbackConfig.shovelRealHitboxOffsetUnits(),
                    EquipmentFallbackConfig.shovelRealHitboxRotationDegrees(),
                    EquipmentFallbackConfig.shovelAttackMoveIds(),
                    EquipmentFallbackConfig.shovelStanceAnimationNames()
            );
        }

        if (stack.isIn(ItemTags.HOES)) {
            return new WeaponCombatAttributes(
                    EquipmentFallbackConfig.hoe(),
                    EquipmentFallbackConfig.hoeBlockImpactMitigation(),
                    EquipmentFallbackConfig.hoeBaseImpact(),
                    EquipmentFallbackConfig.hoeArmorBreakMultiplier(),
                    EquipmentFallbackConfig.hoeAttackSpeedMultiplier(),
                    EquipmentFallbackConfig.hoeRealHitboxSizeUnits(),
                    EquipmentFallbackConfig.hoeRealHitboxOffsetUnits(),
                    EquipmentFallbackConfig.hoeRealHitboxRotationDegrees(),
                    EquipmentFallbackConfig.hoeAttackMoveIds(),
                    EquipmentFallbackConfig.hoeStanceAnimationNames()
            );
        }

        return defaultFallbackWeapon();
    }

    private static WeaponCombatAttributes defaultFallbackWeapon() {
        return new WeaponCombatAttributes(
                EquipmentFallbackConfig.defaultWeapon(),
                EquipmentFallbackConfig.defaultWeaponBlockImpactMitigation(),
                EquipmentFallbackConfig.defaultWeaponBaseImpact(),
                EquipmentFallbackConfig.defaultArmorBreakMultiplier(),
                EquipmentFallbackConfig.defaultWeaponAttackSpeedMultiplier(),
                EquipmentFallbackConfig.defaultWeaponRealHitboxSizeUnits(),
                EquipmentFallbackConfig.defaultWeaponRealHitboxOffsetUnits(),
                EquipmentFallbackConfig.defaultWeaponRealHitboxRotationDegrees(),
                EquipmentFallbackConfig.defaultWeaponAttackMoveIds(),
                EquipmentFallbackConfig.defaultWeaponStanceAnimationNames()
        );
    }

    private static ArmorCombatAttributes fallbackArmor(ItemStack stack) {
        EquipmentSlot slot = armorSlot(stack);
        if (slot == null) {
            return ArmorCombatAttributes.empty();
        }

        double armorValue = armorValue(stack, slot);
        if (armorValue <= 0.0) {
            return ArmorCombatAttributes.empty();
        }

        DamageTypeProfile multiplier = EquipmentFallbackConfig.armorMultiplier(slot);
        Map<String, ArmorCombatAttributes.PartProtection> parts = new HashMap<>();
        for (String part : EquipmentFallbackConfig.partsFor(slot)) {
            parts.put(
                    part,
                    new ArmorCombatAttributes.PartProtection(
                            EquipmentFallbackConfig.protectionPercent(),
                            EquipmentFallbackConfig.impactMitigationPercent()
                    )
            );
        }

        return new ArmorCombatAttributes(
                new DamageTypeProfile(
                        armorValue * multiplier.thrust(),
                        armorValue * multiplier.strike(),
                        armorValue * multiplier.slash()
                ),
                parts,
                0.0,
                0.0,
                0.0
        );
    }

    private static EquipmentSlot armorSlot(ItemStack stack) {
        if (stack.isIn(ItemTags.HEAD_ARMOR)) {
            return EquipmentSlot.HEAD;
        }

        if (stack.isIn(ItemTags.CHEST_ARMOR)) {
            return EquipmentSlot.CHEST;
        }

        if (stack.isIn(ItemTags.LEG_ARMOR)) {
            return EquipmentSlot.LEGS;
        }

        if (stack.isIn(ItemTags.FOOT_ARMOR)) {
            return EquipmentSlot.FEET;
        }

        if (stack.isOf(Items.LEATHER_HORSE_ARMOR)
                || stack.isOf(Items.IRON_HORSE_ARMOR)
                || stack.isOf(Items.GOLDEN_HORSE_ARMOR)
                || stack.isOf(Items.DIAMOND_HORSE_ARMOR)) {
            return EquipmentSlot.BODY;
        }

        return null;
    }

    public static double vanillaArmorValue(ItemStack stack, EquipmentSlot slot) {
        return armorValue(stack, slot);
    }

    private static double armorValue(ItemStack stack, EquipmentSlot slot) {
        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0.0;
        }

        double[] value = {0.0};
        modifiers.applyModifiers(
                slot,
                (attribute, modifier) -> {
                    if (attribute.equals(EntityAttributes.ARMOR)) {
                        value[0] += modifier.value();
                    }
                }
        );
        return value[0];
    }
}
