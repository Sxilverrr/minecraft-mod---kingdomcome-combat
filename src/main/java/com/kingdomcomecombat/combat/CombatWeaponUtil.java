package com.kingdomcomecombat.combat;

import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.equipment.EquipmentFallbackConfig;
import com.kingdomcomecombat.equipment.WeaponCombatAttributes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;

public class CombatWeaponUtil {
    private CombatWeaponUtil() {
    }

    public static boolean usesHeavyAttack(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && (CombatItemUtil.isHeavyWeapon(stack)
                || stack.isIn(ItemTags.PICKAXES)
                || stack.isIn(ItemTags.AXES)
                || stack.isIn(ItemTags.HOES));
    }

    public static String heavyAttackAnimationName(CombatDirection direction) {
        return switch (direction) {
            case RIGHT -> "attack_right_heavy";
            case LEFT -> "attack_left_heavy";
            case UP -> "attack_up_heavy";
            case DOWN -> "attack_down_heavy";
        };
    }

    public static AttackMoveConfig resolveAttackMove(LivingEntity attacker, CombatDirection direction) {
        AttackMoveConfig base = AttackMoveConfigs.get(direction);
        if (attacker == null) {
            return base;
        }

        if (BeowulfArmState.isActive(attacker)
                && !CombatItemUtil.isPolearm(attacker.getMainHandStack())) {
            String shortSwordMoveId = EquipmentFallbackConfig.swordAttackMoveIds()
                    .getOrDefault(directionKey(direction), "");
            AttackMoveConfig shortSwordMove = AttackMoveConfigs.getNamed(shortSwordMoveId);
            return shortSwordMove == null ? base : shortSwordMove;
        }

        WeaponCombatAttributes attributes =
                EquipmentCombatAttributesRegistry.getWeapon(attacker.getMainHandStack());
        String configuredMoveId = attributes.attackMoveId(directionKey(direction));
        if (!configuredMoveId.isBlank()) {
            AttackMoveConfig configuredMove = AttackMoveConfigs.getNamed(configuredMoveId);
            if (configuredMove != null) {
                return configuredMove;
            }
        }

        if (!usesHeavyAttack(attacker.getMainHandStack())) {
            return base;
        }

        String animationName = heavyAttackAnimationName(direction);
        AttackMoveConfig heavyMove = AttackMoveConfigs.getNamed(animationName);
        if (heavyMove != null) {
            return heavyMove;
        }

        return withAnimation(
                base,
                animationName,
                true,
                CombatTiming.HEAVY_ATTACK_TRANSITION_TICKS
        );
    }

    public static String stanceAnimationName(LivingEntity entity, CombatDirection direction) {
        if (entity == null) {
            return "";
        }

        if (BeowulfArmState.isActive(entity)
                && !CombatItemUtil.isPolearm(entity.getMainHandStack())) {
            return EquipmentFallbackConfig.swordStanceAnimationNames()
                    .getOrDefault(directionKey(direction), "");
        }

        return EquipmentCombatAttributesRegistry.getWeapon(entity.getMainHandStack())
                .stanceAnimationName(directionKey(direction));
    }

    public static ComboMoveConfig resolveExecutionMove(LivingEntity attacker, CombatDirection stanceDirection) {
        if (attacker == null) {
            return null;
        }

        WeaponCombatAttributes attributes =
                EquipmentCombatAttributesRegistry.getWeapon(attacker.getMainHandStack());
        String configuredSet = attributes.executionMoveId(directionKey(stanceDirection));
        if (configuredSet.isBlank() && CombatItemUtil.isShortSword(attacker.getMainHandStack())) {
            configuredSet = "shortsword";
        }
        if (configuredSet.isBlank() && CombatItemUtil.isHeavyWeapon(attacker.getMainHandStack())) {
            configuredSet = "heavy_weapon";
        }
        if (configuredSet.isBlank()) {
            configuredSet = EquipmentFallbackConfig.defaultWeaponExecutionMoveIds()
                    .getOrDefault(directionKey(stanceDirection), "");
        }
        if (configuredSet.isBlank()) {
            configuredSet = defaultExecutionSet(attacker);
        }
        return ExecutionMoveConfigs.find(configuredSet, stanceDirection, attacker.getMainHandStack())
                .orElse(null);
    }

    private static String defaultExecutionSet(LivingEntity attacker) {
        if (CombatItemUtil.isShortSword(attacker.getMainHandStack())) {
            return "shortsword";
        }
        return CombatItemUtil.isHeavyWeapon(attacker.getMainHandStack()) ? "heavy_weapon" : "base";
    }

    private static String directionKey(CombatDirection direction) {
        return direction.name().toLowerCase();
    }

    private static AttackMoveConfig withAnimation(
            AttackMoveConfig source,
            String animationName,
            boolean useRealHitbox,
            int transitionTicks
    ) {
        return new AttackMoveConfig(
                source.id(),
                source.strikeModifier(),
                source.slashModifier(),
                source.thrustModifier(),
                source.hitZoneRules(),
                source.impact(),
                source.staminaCost(),
                useRealHitbox,
                animationName,
                transitionTicks,
                source.directHitTick(),
                source.weaponClashTick(),
                source.weaponClashSound(),
                source.masterCounterSpacing(),
                source.horizontalKnockback(),
                source.hitReaction(),
                source.directHitHeightParts(),
                source.defenseDirection(),
                source.classicDirectionalBlock(),
                source.blockable(),
                source.dodgeable(),
                source.jumpDodgeLegs()
        );
    }
}
