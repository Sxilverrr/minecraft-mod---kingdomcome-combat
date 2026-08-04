package com.kingdomcomecombat.equipment;

import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.combat.CombatControlConfig;
import com.kingdomcomecombat.passive.PassiveSkillPerks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import com.kingdomcomecombat.config.CombatServerConfig;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class EquipmentCombatAttributesRegistry {
    public static final Identifier BRACERS_ENCHANTMENT_ID =
            Identifier.of("kingdom_come_combat", "bracers");
    private static final Map<Identifier, WeaponCombatAttributes> WEAPONS = new HashMap<>();
    private static final Map<TagKey<Item>, WeaponCombatAttributes> TAG_WEAPONS = new LinkedHashMap<>();
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

    public static void registerWeapon(TagKey<Item> itemTag, WeaponCombatAttributes attributes) {
        TAG_WEAPONS.put(itemTag, attributes);
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

    public static Map<String, ShieldCombatAttributes> shieldSnapshot() {
        Map<String, ShieldCombatAttributes> result = new HashMap<>();
        SHIELDS.forEach((id, attributes) -> result.put(id.toString(), attributes));
        return Map.copyOf(result);
    }

    public static Map<String, WeaponCombatAttributes> weaponSnapshot() {
        Map<String, WeaponCombatAttributes> result = new HashMap<>();
        WEAPONS.forEach((id, attributes) -> result.put(id.toString(), attributes));
        TAG_WEAPONS.forEach((tag, attributes) -> {
            for (RegistryEntry<Item> item : Registries.ITEM.iterateEntries(tag)) {
                result.putIfAbsent(Registries.ITEM.getId(item.value()).toString(), attributes);
            }
        });
        return Map.copyOf(result);
    }

    /** A client-facing snapshot also materializes category fallbacks per item. */
    public static Map<String, WeaponCombatAttributes> clientWeaponSnapshot() {
        Map<String, WeaponCombatAttributes> result = new HashMap<>(weaponSnapshot());
        for (Item item : Registries.ITEM) {
            ItemStack stack = safeDefaultStack(item);
            if (stack.isEmpty()) continue;
            if (CombatItemUtil.isLongsword(stack)
                    || CombatItemUtil.isPolearm(stack)
                    || CombatItemUtil.isShortSword(stack)
                    || CombatItemUtil.isHeavyWeapon(stack)
                    || stack.isIn(ItemTags.PICKAXES)
                    || stack.isIn(ItemTags.HOES)
                    || stack.isIn(ItemTags.SHOVELS)) {
                result.putIfAbsent(Registries.ITEM.getId(item).toString(), getWeapon(stack));
            }
        }
        return Map.copyOf(result);
    }

    public static Map<String, ArmorCombatAttributes> armorSnapshot() {
        Map<String, ArmorCombatAttributes> result = new HashMap<>();
        ARMOR.forEach((id, attributes) -> result.put(id.toString(), attributes));
        return Map.copyOf(result);
    }

    /** A client-facing snapshot also materializes automatic armor panels. */
    public static Map<String, ArmorCombatAttributes> clientArmorSnapshot() {
        Map<String, ArmorCombatAttributes> result = new HashMap<>(armorSnapshot());
        for (Item item : Registries.ITEM) {
            ItemStack stack = safeDefaultStack(item);
            if (stack.isEmpty()) continue;
            EquipmentSlot slot = armorSlot(stack);
            if (slot != null && armorValue(stack, slot) > 0.0) {
                result.putIfAbsent(Registries.ITEM.getId(item).toString(), fallbackArmor(stack));
            }
        }
        return Map.copyOf(result);
    }

    public static void replaceWeapons(Map<String, WeaponCombatAttributes> weapons) {
        WEAPONS.clear();
        TAG_WEAPONS.clear();
        if (weapons == null) return;
        weapons.forEach((id, attributes) -> {
            Identifier parsed = Identifier.tryParse(id);
            if (parsed != null && attributes != null) WEAPONS.put(parsed, attributes);
        });
    }

    public static void replaceShields(Map<String, ShieldCombatAttributes> shields) {
        SHIELDS.clear();
        if (shields == null) return;
        shields.forEach((id, attributes) -> {
            Identifier parsed = Identifier.tryParse(id);
            if (parsed != null && attributes != null) SHIELDS.put(parsed, attributes);
        });
    }

    public static void replaceArmor(Map<String, ArmorCombatAttributes> armor) {
        ARMOR.clear();
        if (armor == null) return;
        armor.forEach((id, attributes) -> {
            Identifier parsed = Identifier.tryParse(id);
            if (parsed != null && attributes != null) ARMOR.put(parsed, attributes);
        });
    }

    public static void clear() {
        WEAPONS.clear();
        TAG_WEAPONS.clear();
        ARMOR.clear();
        SHIELDS.clear();
    }

    public static void clearWeapons() {
        WEAPONS.clear();
        TAG_WEAPONS.clear();
    }

    public static WeaponCombatAttributes getWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return defaultFallbackWeapon();
        }

        WeaponCombatAttributes exact = WEAPONS.get(Registries.ITEM.getId(stack.getItem()));
        if (exact != null) return exact;
        for (Map.Entry<TagKey<Item>, WeaponCombatAttributes> entry : TAG_WEAPONS.entrySet()) {
            if (stack.isIn(entry.getKey())) return entry.getValue();
        }
        return fallbackWeapon(stack);
    }

    public static Optional<WeaponCombatAttributes> getConfiguredWeapon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return Optional.empty();
        }

        WeaponCombatAttributes exact = WEAPONS.get(Registries.ITEM.getId(stack.getItem()));
        if (exact != null) return Optional.of(exact);
        for (Map.Entry<TagKey<Item>, WeaponCombatAttributes> entry : TAG_WEAPONS.entrySet()) {
            if (stack.isIn(entry.getKey())) return Optional.of(entry.getValue());
        }
        return Optional.empty();
    }

    public static Vec3d realHitboxSizeUnits(ItemStack stack, Vec3d fallback) {
        WeaponCombatAttributes attributes = getWeapon(stack);
        return attributes.hasCustomRealHitboxSize() ? attributes.realHitboxSizeUnits() : fallback;
    }

    public static Vec3d realHitboxSizeUnits(LivingEntity wielder, ItemStack stack, Vec3d fallback) {
        Vec3d size = realHitboxSizeUnits(stack, fallback);
        if (wielder == null || !CombatServerConfig.reachAttributeHitboxScalingEnabled()) return size;
        double delta = weaponEntityAttackRangeModifier(stack);
        double scale = Math.max(0.1,
                1.0 + delta * CombatServerConfig.reachAttributeHitboxScalePerBlock());
        // Reach changes weapon length, not its thickness: scale only the
        // configured hitbox's longest axis.
        if (size.z >= size.x && size.z >= size.y) {
            return new Vec3d(size.x, size.y, size.z * scale);
        }
        if (size.x >= size.y) {
            return new Vec3d(size.x * scale, size.y, size.z);
        }
        return new Vec3d(size.x, size.y * scale, size.z);
    }

    private static double weaponEntityAttackRangeModifier(ItemStack stack) {
        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0.0;
        }

        Map<RegistryEntry<EntityAttribute>, ReachModifierValues> valuesByAttribute = new LinkedHashMap<>();
        modifiers.applyModifiers(
                EquipmentSlot.MAINHAND,
                (attribute, modifier) -> {
                    if (!isEntityAttackRangeAttribute(attribute)) {
                        return;
                    }
                    valuesByAttribute.computeIfAbsent(
                            attribute,
                            ignored -> new ReachModifierValues(safeAttributeDefaultValue(attribute))
                    ).add(modifier);
                }
        );

        return valuesByAttribute.values().stream()
                .mapToDouble(ReachModifierValues::delta)
                .sum();
    }

    private static ItemStack safeDefaultStack(Item item) {
        try {
            return item.getDefaultStack();
        } catch (RuntimeException exception) {
            // Some modded items expose deferred attribute holders from their
            // default components. During datapack/bootstrap reload those
            // holders (for example swim_speed) may not be bound yet.
            return ItemStack.EMPTY;
        }
    }

    private static double safeAttributeDefaultValue(RegistryEntry<EntityAttribute> attribute) {
        try {
            return attribute.value().getDefaultValue();
        } catch (RuntimeException exception) {
            // ADD_VALUE reach modifiers remain valid without a bound base;
            // defer multiplied-base semantics until the registry is ready.
            return 0.0;
        }
    }

    private static boolean isEntityAttackRangeAttribute(RegistryEntry<EntityAttribute> attribute) {
        if (attribute.equals(EntityAttributes.ENTITY_INTERACTION_RANGE)) {
            return true;
        }

        String path = attribute.getKey()
                .map(key -> key.getValue().getPath())
                .orElse("");
        if (path.contains("block")) {
            return false;
        }
        return path.equals("reach")
                || path.equals("entity_interaction_range")
                || path.equals("entity_reach")
                || path.equals("entity_reach_distance")
                || path.equals("attack_range")
                || path.equals("attack_reach")
                || path.equals("attack_distance")
                || path.equals("entity_attack_range")
                || path.equals("entity_attack_reach")
                || path.equals("melee_range")
                || path.equals("melee_reach");
    }

    private static final class ReachModifierValues {
        private final double base;
        private double added;
        private double multipliedBase;
        private double multipliedTotal = 1.0;

        private ReachModifierValues(double base) {
            this.base = base;
        }

        private void add(EntityAttributeModifier modifier) {
            switch (modifier.operation()) {
                case ADD_VALUE -> added += modifier.value();
                case ADD_MULTIPLIED_BASE -> multipliedBase += modifier.value();
                case ADD_MULTIPLIED_TOTAL -> multipliedTotal *= 1.0 + modifier.value();
            }
        }

        private double delta() {
            return (base + added + base * multipliedBase) * multipliedTotal - base;
        }
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
                && getConfiguredWeapon(stack).isEmpty()
                && (CombatItemUtil.isFightingMace(stack)
                        || CombatItemUtil.isPolearm(stack)
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
                || CombatItemUtil.isPolearm(stack)
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
                    EquipmentFallbackConfig.imperfectBlockImpactMitigation(), 1, 1, 1, 1, 1, 0, 1, 1, 0
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
                    EquipmentFallbackConfig.imperfectBlockImpactMitigation(), 1, 1, 1, 1, 1, 0, 1, 1, 0
            );
        }

        return new ShieldCombatAttributes(
                ShieldCombatAttributes.Size.SMALL,
                EquipmentFallbackConfig.imperfectBlockImpactMitigation(), 1, 1, 1, 1, 1, 0, 1, 1, 0
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
        if (CombatItemUtil.isPolearm(stack)) {
            return new WeaponCombatAttributes(
                    EquipmentFallbackConfig.polearm(),
                    EquipmentFallbackConfig.polearmBlockImpactMitigation(),
                    EquipmentFallbackConfig.polearmBaseImpact(),
                    EquipmentFallbackConfig.polearmArmorBreakMultiplier(),
                    EquipmentFallbackConfig.polearmAttackSpeedMultiplier(),
                    EquipmentFallbackConfig.polearmRealHitboxSizeUnits(),
                    EquipmentFallbackConfig.polearmRealHitboxOffsetUnits(),
                    EquipmentFallbackConfig.polearmRealHitboxRotationDegrees(),
                    EquipmentFallbackConfig.polearmAttackMoveIds(),
                    EquipmentFallbackConfig.polearmStanceAnimationNames(),
                    EquipmentFallbackConfig.polearmWeaponToughness(),
                    EquipmentFallbackConfig.polearmMinimumDurabilityPanelMultiplier(),
                    EquipmentFallbackConfig.polearmHeldMovementSpeedMultiplier()
            );
        }
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

        // Axes and pickaxes are also members of the broad heavy-weapon
        // category, but they have dedicated configurable fallbacks below.
        // Only fighting maces belong in this branch.
        if (CombatItemUtil.isFightingMace(stack)) {
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
                EquipmentFallbackConfig.defaultWeaponStanceAnimationNames(),
                EquipmentFallbackConfig.defaultWeaponExecutionMoveIds(),
                EquipmentFallbackConfig.defaultWeaponToughness(),
                EquipmentFallbackConfig.defaultMinimumDurabilityPanelMultiplier(),
                EquipmentFallbackConfig.defaultHeldMovementSpeedMultiplier()
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
        var equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable != null && equippable.slot().isArmorSlot()) {
            return equippable.slot();
        }

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
