package com.kingdomcomecombat.client.ui;

import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.client.config.ClientServerConfigState;
import com.kingdomcomecombat.item.ModItems;
import com.kingdomcomecombat.item.HandCannonItem;
import com.kingdomcomecombat.equipment.ArmorCombatAttributes;
import com.kingdomcomecombat.equipment.DamageTypeProfile;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.EquipmentFallbackConfig;
import com.kingdomcomecombat.equipment.WeaponCombatAttributes;
import com.kingdomcomecombat.equipment.RangedWeaponAttributes;
import com.kingdomcomecombat.equipment.RangedWeaponAttributesRegistry;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Map;

public class CombatAttributeTooltipClient {
    private static final double ARMOR_LOW_DURABILITY_THRESHOLD = 0.10;
    private static final Identifier DENSITY_ENCHANTMENT_ID = Identifier.ofVanilla("density");
    private static final Identifier BREACH_ENCHANTMENT_ID = Identifier.ofVanilla("breach");

    private CombatAttributeTooltipClient() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            Identifier itemId = Registries.ITEM.getId(stack.getItem());
            if (!lines.isEmpty() && "kingdom_come_combat".equals(itemId.getNamespace())) {
                // Some light tooltip/recipe-viewer themes render an inherited
                // (unset) item-name color as black. Keep every KCC item name
                // explicit so registration style differences cannot leak into
                // the first tooltip line.
                lines.set(0, lines.get(0).copy().formatted(Formatting.WHITE));
            }
            if (stack.isOf(ModItems.BANDAGE)) {
                lines.add(Text.translatable("tooltip.kingdom_come_combat.bandage.use")
                        .formatted(Formatting.GRAY));
            }
            if (ClientServerConfigState.lightweightDamageModeEnabled()) {
                return;
            }
            if (stack.isOf(ModItems.HAND_CANNON)) {
                appendHandCannonLoadState(lines, stack);
                appendHandCannonPanel(lines, stack);
            }
            EquipmentCombatAttributesRegistry.getConfiguredWeapon(stack).ifPresentOrElse(
                    attributes -> appendWeapon(lines, stack, attributes, false),
                    () -> {
                        if (EquipmentCombatAttributesRegistry.hasFallbackWeapon(stack)) {
                            appendWeapon(
                                    lines,
                                    stack,
                                    EquipmentCombatAttributesRegistry.getWeapon(stack),
                                    true
                            );
                        }
                    }
            );
            EquipmentCombatAttributesRegistry.getConfiguredArmor(stack).ifPresentOrElse(
                    attributes -> appendArmor(lines, stack, attributes, false),
                    () -> {
                        if (EquipmentCombatAttributesRegistry.hasFallbackArmor(stack)) {
                            appendArmor(
                                    lines,
                                    stack,
                                    EquipmentCombatAttributesRegistry.getArmor(stack),
                                    true
                            );
                        }
                    }
            );
            if (RangedWeaponAttributesRegistry.isConfigured(stack)) {
                appendRangedWeapon(lines, RangedWeaponAttributesRegistry.get(stack));
            }
        });
    }

    private static void appendHandCannonLoadState(java.util.List<Text> lines, ItemStack stack) {
        if (HandCannonItem.isLoaded(stack) || HandCannonItem.isPowderLoaded(stack)) {
            String ammo = switch (HandCannonItem.loadedAmmoType(stack)) {
                case HEAVY -> "tooltip.kingdom_come_combat.hand_cannon.ammo.heavy";
                case BUCKSHOT -> "tooltip.kingdom_come_combat.hand_cannon.ammo.buckshot";
                default -> "tooltip.kingdom_come_combat.hand_cannon.ammo.normal";
            };
            String state = HandCannonItem.isLoaded(stack)
                    ? "tooltip.kingdom_come_combat.hand_cannon.loaded"
                    : "tooltip.kingdom_come_combat.hand_cannon.ramrod";
            lines.add(Text.translatable(state, Text.translatable(ammo)).formatted(Formatting.GREEN));
        } else {
            lines.add(Text.translatable("tooltip.kingdom_come_combat.hand_cannon.unloaded").formatted(Formatting.DARK_GRAY));
        }
    }

    private static void appendHandCannonPanel(java.util.List<Text> lines, ItemStack stack) {
        lines.add(Text.translatable("tooltip.kingdom_come_combat.hand_cannon.panel").formatted(Formatting.GOLD));
        lines.add(Text.translatable(
                "tooltip.kingdom_come_combat.hand_cannon.initial_speed",
                oneDecimal(HandCannonItem.initialSpeedMetersPerSecond(stack)))
                .formatted(Formatting.YELLOW));
        lines.add(Text.translatable(
                "tooltip.kingdom_come_combat.hand_cannon.accuracy",
                oneDecimal(HandCannonItem.accuracyPercent(stack)))
                .formatted(Formatting.YELLOW));
        lines.add(Text.translatable(
                "tooltip.kingdom_come_combat.hand_cannon.fuse_time",
                oneDecimal(HandCannonItem.minFuseTicks(stack) / 20.0),
                oneDecimal(HandCannonItem.maxFuseTicks(stack) / 20.0))
                .formatted(Formatting.YELLOW));
    }

    private static void appendWeapon(
            java.util.List<Text> lines,
            ItemStack stack,
            WeaponCombatAttributes attributes,
            boolean inferred
    ) {
        lines.add(Text.translatable(inferred
                        ? "tooltip.kingdom_come_combat.weapon_panel.inferred"
                        : "tooltip.kingdom_come_combat.weapon_panel")
                .formatted(Formatting.GOLD));
        appendProfile(lines, "tooltip.kingdom_come_combat.modifiers", attributes.damagePanel(), Formatting.RED, true);
        appendProfile(
                lines,
                "tooltip.kingdom_come_combat.actual",
                actualWeaponPanel(stack, attributes),
                Formatting.DARK_RED,
                false
        );
        lines.add(Text.translatable(
                        "tooltip.kingdom_come_combat.block_impact_mitigation",
                        percent(attributes.blockImpactMitigation()))
                .formatted(Formatting.YELLOW));
        lines.add(Text.translatable(
                        "tooltip.kingdom_come_combat.attack_speed_multiplier",
                        percent(attributes.attackSpeedMultiplier()))
                .formatted(Formatting.YELLOW));
        lines.add(Text.translatable(
                        "tooltip.kingdom_come_combat.weapon_impact",
                        oneDecimal(attributes.baseImpact()))
                .formatted(Formatting.GOLD));
        lines.add(Text.translatable(
                        "tooltip.kingdom_come_combat.armor_break_multiplier",
                        percent(attributes.armorBreakMultiplier()))
                .formatted(Formatting.DARK_RED));
        appendHeavyHammerEnchantments(lines, stack);
    }

    private static void appendRangedWeapon(
            java.util.List<Text> lines,
            RangedWeaponAttributes attributes
    ) {
        lines.add(Text.translatable("tooltip.kingdom_come_combat.ranged_weapon_panel")
                .formatted(Formatting.GOLD));
        lines.add(Text.translatable(
                        "tooltip.kingdom_come_combat.ranged_draw_speed",
                        percent(attributes.drawSpeed()))
                .formatted(Formatting.YELLOW));
        lines.add(Text.translatable(
                        "tooltip.kingdom_come_combat.ranged_hardness",
                        oneDecimal(attributes.hardness()))
                .formatted(Formatting.YELLOW));
        lines.add(Text.translatable(
                        "tooltip.kingdom_come_combat.ranged_projectile_speed",
                        percent(attributes.projectileSpeed()))
                .formatted(Formatting.YELLOW));
    }

    private static void appendHeavyHammerEnchantments(java.util.List<Text> lines, ItemStack stack) {
        if (!CombatItemUtil.isFightingMace(stack)) {
            return;
        }

        int density = enchantmentLevel(stack, DENSITY_ENCHANTMENT_ID);
        if (density > 0) {
            lines.add(Text.translatable(
                            "tooltip.kingdom_come_combat.density_bonus",
                            oneDecimal(density * 0.5))
                    .formatted(Formatting.RED));
        }

        int breach = enchantmentLevel(stack, BREACH_ENCHANTMENT_ID);
        if (breach > 0) {
            lines.add(Text.translatable(
                            "tooltip.kingdom_come_combat.breach_bonus",
                            percent(breach * 0.05))
                    .formatted(Formatting.DARK_RED));
        }
    }

    private static void appendArmor(
            java.util.List<Text> lines,
            ItemStack stack,
            ArmorCombatAttributes attributes,
            boolean inferred
    ) {
        lines.add(Text.translatable(inferred
                        ? "tooltip.kingdom_come_combat.armor_panel.inferred"
                        : "tooltip.kingdom_come_combat.armor_panel")
                .formatted(Formatting.AQUA));
        appendProfile(lines, "tooltip.kingdom_come_combat.defense", attributes.defense(), Formatting.BLUE, false);
        appendProfile(lines, "tooltip.kingdom_come_combat.actual", actualArmorPanel(stack, attributes.defense()), Formatting.DARK_BLUE, false);
        double enchantImpactMitigation = enchantmentImpactMitigation(stack);
        if (enchantImpactMitigation > 0.0) {
            lines.add(Text.translatable(
                            "tooltip.kingdom_come_combat.armor_enchant_impact_mitigation",
                            percent(enchantImpactMitigation))
                    .formatted(Formatting.LIGHT_PURPLE));
        }
        if (attributes.staminaCostIncrease() > 0.0
                || attributes.attackSpeedPenalty() > 0.0
                || attributes.movementSpeedPenalty() > 0.0) {
            lines.add(Text.translatable(
                            "tooltip.kingdom_come_combat.armor_burden",
                            percent(attributes.staminaCostIncrease()),
                            percent(attributes.attackSpeedPenalty()),
                            percent(attributes.movementSpeedPenalty()))
                    .formatted(Formatting.DARK_GRAY));
        }

        if (attributes.protectedParts().isEmpty()) {
            return;
        }

        Map<String, ArmorCombatAttributes.PartProtection> effectiveProtectedParts =
                EquipmentCombatAttributesRegistry.effectiveProtectedParts(stack, attributes);
        if (EquipmentCombatAttributesRegistry.enchantmentLevel(
                stack,
                EquipmentCombatAttributesRegistry.BRACERS_ENCHANTMENT_ID
        ) > 0 && attributes.protectedParts().containsKey("shoulder")) {
            lines.add(Text.translatable("tooltip.kingdom_come_combat.bracers_applied")
                    .formatted(Formatting.LIGHT_PURPLE));
        }
        lines.add(Text.translatable("tooltip.kingdom_come_combat.protected_parts").formatted(Formatting.GRAY));
        for (Map.Entry<String, ArmorCombatAttributes.PartProtection> entry
                : effectiveProtectedParts.entrySet()) {
            ArmorCombatAttributes.PartProtection protection = entry.getValue();
            lines.add(Text.translatable(
                            "tooltip.kingdom_come_combat.part_protection",
                            entry.getKey(),
                            scalar(protection.protectionPercent(), true),
                            percent(protection.impactMitigationPercent()))
                    .formatted(Formatting.DARK_AQUA));
        }
    }

    private static void appendProfile(
            java.util.List<Text> lines,
            String labelKey,
            DamageTypeProfile profile,
            Formatting color,
            boolean percent
    ) {
        lines.add(Text.translatable(
                        "tooltip.kingdom_come_combat.damage_profile",
                        Text.translatable(labelKey),
                        scalar(profile.thrust(), percent),
                        scalar(profile.strike(), percent),
                        scalar(profile.slash(), percent))
                .formatted(color));
    }

    private static String scalar(double value, boolean percent) {
        if (percent) {
            return percent(value);
        }

        return String.valueOf(Math.round(value));
    }

    private static String percent(double value) {
        return Math.round(value * 100.0) + "%";
    }

    private static String oneDecimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static DamageTypeProfile actualWeaponPanel(
            ItemStack stack,
            WeaponCombatAttributes attributes
    ) {
        DamageTypeProfile profile = attributes.damagePanel();
        double attackDamage = attackDamage(stack);
        double edgeMultiplier = weaponEdgeDurabilityMultiplier(
                stack,
                attributes.minimumDurabilityPanelMultiplier()
        );
        DamageTypeProfile weaponEnchantPanel = enchantmentWeaponPanel(stack);
        return new DamageTypeProfile(
                attackDamage * profile.thrust() * edgeMultiplier * 5.0 + weaponEnchantPanel.thrust(),
                attackDamage * profile.strike() * 5.0 + weaponEnchantPanel.strike(),
                attackDamage * profile.slash() * edgeMultiplier * 5.0 + weaponEnchantPanel.slash()
        );
    }

    private static double weaponEdgeDurabilityMultiplier(ItemStack stack, double minimumMultiplier) {
        if (stack.isEmpty() || !stack.isDamageable() || stack.getMaxDamage() <= 0) {
            return 1.0;
        }

        double damageRatio = Math.max(0.0, Math.min(1.0, stack.getDamage() / (double) stack.getMaxDamage()));
        return 1.0 - (1.0 - minimumMultiplier) * damageRatio;
    }

    private static DamageTypeProfile actualArmorPanel(ItemStack stack, DamageTypeProfile defense) {
        double durabilityMultiplier = armorDurabilityMultiplier(stack);
        if (durabilityMultiplier <= 0.0) {
            return DamageTypeProfile.even(0.0);
        }

        DamageTypeProfile enchantDefense = enchantmentDefense(stack);
        return new DamageTypeProfile(
                defense.thrust() * durabilityMultiplier + enchantDefense.thrust(),
                defense.strike() * durabilityMultiplier + enchantDefense.strike(),
                defense.slash() * durabilityMultiplier + enchantDefense.slash()
        );
    }

    private static double armorDurabilityMultiplier(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageable() || stack.getMaxDamage() <= 0) {
            return 1.0;
        }

        double remainingRatio = 1.0 - Math.max(
                0.0,
                Math.min(1.0, stack.getDamage() / (double) stack.getMaxDamage())
        );
        if (remainingRatio >= ARMOR_LOW_DURABILITY_THRESHOLD) {
            return 1.0;
        }

        return Math.max(0.0, remainingRatio / ARMOR_LOW_DURABILITY_THRESHOLD);
    }

    private static DamageTypeProfile enchantmentDefense(ItemStack stack) {
        double thrust = 0.0;
        double strike = 0.0;
        double slash = 0.0;

        for (Map.Entry<Identifier, DamageTypeProfile> entry
                : EquipmentFallbackConfig.armorDefenseEnchantments().entrySet()) {
            int level = enchantmentLevel(stack, entry.getKey());
            if (level <= 0) {
                continue;
            }

            DamageTypeProfile profile = entry.getValue();
            thrust += profile.thrust() * level;
            strike += profile.strike() * level;
            slash += profile.slash() * level;
        }

        for (Map.Entry<Identifier, DamageTypeProfile> entry
                : EquipmentFallbackConfig.armorDefenseFirstTwoBonusEnchantments().entrySet()) {
            int level = enchantmentLevel(stack, entry.getKey());
            if (level <= 0) {
                continue;
            }

            int bonusLevels = Math.min(level, 2);
            DamageTypeProfile profile = entry.getValue();
            thrust += profile.thrust() * bonusLevels;
            strike += profile.strike() * bonusLevels;
            slash += profile.slash() * bonusLevels;
        }

        return new DamageTypeProfile(thrust, strike, slash);
    }

    private static DamageTypeProfile enchantmentWeaponPanel(ItemStack stack) {
        return enchantmentWeaponRawDamage(stack);
    }

    private static DamageTypeProfile enchantmentWeaponRawDamage(ItemStack stack) {
        double thrust = 0.0;
        double strike = 0.0;
        double slash = 0.0;

        for (Map.Entry<Identifier, DamageTypeProfile> entry
                : EquipmentFallbackConfig.weaponDamageEnchantments().entrySet()) {
            int level = enchantmentLevel(stack, entry.getKey());
            if (level <= 0) {
                continue;
            }

            DamageTypeProfile profile = entry.getValue();
            thrust += profile.thrust() * level;
            strike += profile.strike() * level;
            slash += profile.slash() * level;
        }

        for (Map.Entry<Identifier, DamageTypeProfile> entry
                : EquipmentFallbackConfig.weaponDamageFirstTwoBonusEnchantments().entrySet()) {
            int level = enchantmentLevel(stack, entry.getKey());
            if (level <= 0) {
                continue;
            }

            int bonusLevels = Math.min(level, 2);
            DamageTypeProfile profile = entry.getValue();
            thrust += profile.thrust() * bonusLevels;
            strike += profile.strike() * bonusLevels;
            slash += profile.slash() * bonusLevels;
        }

        return new DamageTypeProfile(thrust, strike, slash);
    }

    private static double enchantmentImpactMitigation(ItemStack stack) {
        double mitigation = 0.0;
        for (Map.Entry<Identifier, Double> entry
                : EquipmentFallbackConfig.armorImpactMitigationEnchantments().entrySet()) {
            int level = enchantmentLevel(stack, entry.getKey());
            if (level <= 0) {
                continue;
            }

            mitigation += entry.getValue() * level;
        }

        return Math.max(0.0, Math.min(0.95, mitigation));
    }

    private static int enchantmentLevel(ItemStack stack, Identifier enchantmentId) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
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

    private static double attackDamage(ItemStack stack) {
        AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0.0;
        }

        double base = 0.0;
        double[] addValue = {0.0};
        double[] addMultipliedBase = {0.0};
        double[] addMultipliedTotal = {1.0};
        modifiers.applyModifiers(
                EquipmentSlot.MAINHAND,
                (attribute, modifier) -> {
                    if (!attribute.equals(EntityAttributes.ATTACK_DAMAGE)) {
                        return;
                    }

                    if (modifier.operation() == EntityAttributeModifier.Operation.ADD_VALUE) {
                        addValue[0] += modifier.value();
                    } else if (modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                        addMultipliedBase[0] += modifier.value();
                    } else if (modifier.operation() == EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                        addMultipliedTotal[0] *= 1.0 + modifier.value();
                    }
                }
        );

        return Math.max(0.0, (base + addValue[0] + base * addMultipliedBase[0]) * addMultipliedTotal[0]);
    }
}
