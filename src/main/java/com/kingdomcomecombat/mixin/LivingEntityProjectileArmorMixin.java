package com.kingdomcomecombat.mixin;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.ai.HumanoidCombatAiProfile;
import com.kingdomcomecombat.ai.HumanoidCombatAiProfiles;
import com.kingdomcomecombat.combat.ServerCombatControlState;
import com.kingdomcomecombat.collision.ServerHitDetectionSystem;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import com.kingdomcomecombat.injury.ModStatusEffects;
import com.kingdomcomecombat.projectile.ProjectileImpactHandler;
import com.kingdomcomecombat.passive.PassiveSkillPerks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.DamageUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(LivingEntity.class)
public class LivingEntityProjectileArmorMixin {
    private static final float FALL_LEG_INJURY_DAMAGE_THRESHOLD = 6.0F;
    private static final float EXPLOSION_RANDOM_INJURY_DAMAGE_STEP = 2.0F;
    private static final float BLEEDING_ROLL_DAMAGE_THRESHOLD = 6.0F;
    private static final float ACCUMULATED_FIRE_INJURY_DAMAGE = 6.0F;
    private static final float ACCUMULATED_SUFFOCATION_INJURY_DAMAGE = 6.0F;
    private static final float ACCUMULATED_FREEZE_INJURY_DAMAGE = 6.0F;

    private static final Map<UUID, Float> FIRE_DAMAGE_ACCUMULATORS = new HashMap<>();
    private static final Map<UUID, Float> SUFFOCATION_DAMAGE_ACCUMULATORS = new HashMap<>();
    private static final Map<UUID, Float> FREEZE_DAMAGE_ACCUMULATORS = new HashMap<>();
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    @Unique
    private Vec3d kingdomcomecombat$velocityBeforeBleedingDamage = null;

    private static final RegistryKey<DamageType> KCC_CUSTOM_COMBAT_DAMAGE =
            RegistryKey.of(
                    RegistryKeys.DAMAGE_TYPE,
                    Identifier.of(KingdomComeCombat.MOD_ID, "custom_combat")
            );
    private static final RegistryKey<DamageType> KCC_BLEEDING_DAMAGE =
            RegistryKey.of(
                    RegistryKeys.DAMAGE_TYPE,
                    Identifier.of(KingdomComeCombat.MOD_ID, "bleeding")
            );

    @Inject(method = "applyArmorToDamage", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$applyArmorWithoutBreaking(
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Float> cir
    ) {
        if (source.isIn(DamageTypeTags.BYPASSES_ARMOR)
                || source.isOf(KCC_CUSTOM_COMBAT_DAMAGE)
                || source.isOf(KCC_BLEEDING_DAMAGE)) {
            return;
        }

        LivingEntity target = (LivingEntity) (Object) this;
        kingdomcomecombat$damageEquippedArmor(target, source, amount);
        float armor = kingdomcomecombat$effectiveEquipmentSensitiveAttribute(
                target,
                EntityAttributes.ARMOR
        );
        float toughness = kingdomcomecombat$effectiveEquipmentSensitiveAttribute(
                target,
                EntityAttributes.ARMOR_TOUGHNESS
        );
        cir.setReturnValue(DamageUtil.getDamageLeft(target, amount, source, armor, toughness));
    }

    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void kingdomcomecombat$applyProjectileArmor(
            ServerWorld world,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (source.isOf(KCC_BLEEDING_DAMAGE)) {
            kingdomcomecombat$velocityBeforeBleedingDamage = target.getVelocity();
        }

        if (!source.isOf(KCC_CUSTOM_COMBAT_DAMAGE)
                && !source.isOf(KCC_BLEEDING_DAMAGE)
                && ServerHitDetectionSystem.tryHandleConfiguredMobAttack(world, target, source, amount)) {
            cir.setReturnValue(false);
            return;
        }

        if (!source.isOf(KCC_CUSTOM_COMBAT_DAMAGE)
                && !source.isOf(KCC_BLEEDING_DAMAGE)
                && ServerHitDetectionSystem.tryHandleVanillaMeleePanelAttack(world, target, source, amount)) {
            cir.setReturnValue(false);
            return;
        }

        if (!source.isOf(KCC_CUSTOM_COMBAT_DAMAGE)
                && !source.isOf(KCC_BLEEDING_DAMAGE)
                && ServerHitDetectionSystem.tryHandleExternalAttackDefense(world, target, source, amount)) {
            cir.setReturnValue(false);
            return;
        }

        if (!source.isOf(KCC_CUSTOM_COMBAT_DAMAGE)
                && !source.isOf(KCC_BLEEDING_DAMAGE)
                && ServerCombatControlState.dodgesDamage(target, source)) {
            cir.setReturnValue(false);
            return;
        }

        if (source.isOf(KCC_CUSTOM_COMBAT_DAMAGE) || !(source.getSource() instanceof ProjectileEntity projectile)) {
            return;
        }

        if (projectile instanceof WitherSkullEntity) {
            return;
        }

        if (ServerHitDetectionSystem.tryHandleProjectileShieldDefense(world, target, projectile, amount)) {
            cir.setReturnValue(false);
            return;
        }

        ServerHitDetectionSystem.ProjectileArmorResult projectileArmorResult = ServerHitDetectionSystem.applyProjectileArmorDamage(
                target,
                projectile,
                amount
        );
        ServerHitDetectionSystem.spawnProjectileImpactParticles(world, target, projectile, projectileArmorResult);
        ProjectileImpactHandler.stickArrowInTarget(projectile, target);
        float resolvedDamage = projectileArmorResult.damage();
        if (resolvedDamage <= 0.0F) {
            cir.setReturnValue(false);
            return;
        }

        Entity owner = projectile.getOwner();
        DamageSource customSource = world.getDamageSources().create(
                KCC_CUSTOM_COMBAT_DAMAGE,
                projectile,
                owner == null ? projectile : owner
        );
        Vec3d velocityBeforeDamage = target.getVelocity();
        target.timeUntilRegen = 0;
        target.hurtTime = 0;
        boolean damaged = target.damage(world, customSource, resolvedDamage);
        if (damaged) {
            target.timeUntilRegen = 0;
            ProjectileImpactHandler.removeProjectileKnockback(
                    world,
                    projectile,
                    target,
                    velocityBeforeDamage
            );
            ServerHitDetectionSystem.syncProjectileHitReaction(world, target, projectile);
            ServerHitDetectionSystem.syncSuppressHurtOverlay(world, target);
            if (!target.isAlive() && owner instanceof LivingEntity livingOwner) {
                PassiveSkillPerks.afterKill(livingOwner);
            }
        }
        cir.setReturnValue(damaged);
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void kingdomcomecombat$applyEnvironmentalInjury(
            ServerWorld world,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (source.isOf(KCC_BLEEDING_DAMAGE)) {
            if (cir.getReturnValueZ()) {
                if (kingdomcomecombat$velocityBeforeBleedingDamage != null) {
                    target.setVelocity(kingdomcomecombat$velocityBeforeBleedingDamage);
                    target.velocityModified = true;
                }
                target.hurtTime = 0;
                ServerHitDetectionSystem.syncSuppressHurtOverlay(world, target);
            }
            kingdomcomecombat$velocityBeforeBleedingDamage = null;
            return;
        }

        if (!cir.getReturnValueZ()) {
            return;
        }

        if (!source.isOf(KCC_CUSTOM_COMBAT_DAMAGE) && source.getSource() instanceof ProjectileEntity) {
            return;
        }

        if (source.isOf(KCC_CUSTOM_COMBAT_DAMAGE) && source.getSource() instanceof ProjectileEntity projectile) {
            ServerHitDetectionSystem.maybeApplyProjectileDamageInjury(target, projectile, amount);
        }

        if (source.isIn(DamageTypeTags.IS_EXPLOSION)) {
            applyExplosionInjuries(target, amount);
            maybeApplyBleeding(target, source, amount);
            return;
        }

        if (source.isOf(DamageTypes.FALL) && amount >= FALL_LEG_INJURY_DAMAGE_THRESHOLD) {
            int injuryLevel = 1 + (int) ((amount - FALL_LEG_INJURY_DAMAGE_THRESHOLD) / 3.0F);
            ModStatusEffects.applyInjury(target, "legs", injuryLevel);
        }
        applyAccumulatedEnvironmentalInjury(target, source, amount);
        maybeApplyBleeding(target, source, amount);
    }

    private static void applyExplosionInjuries(LivingEntity target, float amount) {
        int rolls = (int) (amount / EXPLOSION_RANDOM_INJURY_DAMAGE_STEP);
        for (int i = 0; i < rolls; i++) {
            ModStatusEffects.applyInjury(target, randomInjuryPart(target), 1);
        }
    }

    private static String randomInjuryPart(LivingEntity target) {
        return switch (target.getRandom().nextInt(5)) {
            case 0 -> "head";
            case 1 -> "torso";
            case 2 -> "arms";
            case 3 -> "hands";
            default -> "legs";
        };
    }

    private static void applyAccumulatedEnvironmentalInjury(
            LivingEntity target,
            DamageSource source,
            float amount
    ) {
        if (amount <= 0.0F) {
            return;
        }

        if (source.isIn(DamageTypeTags.IS_FIRE)) {
            accumulateDamage(
                    FIRE_DAMAGE_ACCUMULATORS,
                    target,
                    amount,
                    ACCUMULATED_FIRE_INJURY_DAMAGE,
                    "torso"
            );
        } else if (source.isOf(DamageTypes.IN_WALL)) {
            accumulateDamage(
                    SUFFOCATION_DAMAGE_ACCUMULATORS,
                    target,
                    amount,
                    ACCUMULATED_SUFFOCATION_INJURY_DAMAGE,
                    "head"
            );
        } else if (source.isOf(DamageTypes.FREEZE)) {
            accumulateDamage(
                    FREEZE_DAMAGE_ACCUMULATORS,
                    target,
                    amount,
                    ACCUMULATED_FREEZE_INJURY_DAMAGE,
                    "torso"
            );
        }
    }

    private static void accumulateDamage(
            Map<UUID, Float> accumulators,
            LivingEntity target,
            float amount,
            float threshold,
            String injuryType
    ) {
        UUID uuid = target.getUuid();
        float total = accumulators.getOrDefault(uuid, 0.0F) + amount;
        int injuryLevels = (int) (total / threshold);
        if (injuryLevels > 0) {
            ModStatusEffects.applyInjury(target, injuryType, injuryLevels);
            total -= injuryLevels * threshold;
        }

        if (target.isAlive() && total > 0.0F) {
            accumulators.put(uuid, total);
        } else {
            accumulators.remove(uuid);
        }
    }

    private static void kingdomcomecombat$damageEquippedArmor(
            LivingEntity target,
            DamageSource source,
            float amount
    ) {
        if (amount <= 0.0F) {
            return;
        }

        int baseAmount = Math.max(1, (int) (amount / 4.0F));
        double multiplier = kingdomcomecombat$armorDurabilityDamageMultiplier(target, source);
        int durabilityDamage = (int) Math.ceil(baseAmount * multiplier);
        if (durabilityDamage <= 0) {
            return;
        }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = target.getEquippedStack(slot);
            if (stack.isEmpty() || !stack.isDamageable() || stack.getMaxDamage() <= 0) {
                continue;
            }

            stack.setDamage(Math.min(stack.getMaxDamage(), stack.getDamage() + durabilityDamage));
        }
    }

    private static double kingdomcomecombat$armorDurabilityDamageMultiplier(
            LivingEntity target,
            DamageSource source
    ) {
        double weaponMultiplier = 1.0;
        if (source.getAttacker() instanceof LivingEntity attacker) {
            weaponMultiplier = EquipmentCombatAttributesRegistry
                    .getWeapon(attacker.getMainHandStack())
                    .armorBreakMultiplier();
        }

        HumanoidCombatAiProfile profile = HumanoidCombatAiProfiles.getProfile(target);
        double wornArmorMultiplier = profile == null
                ? 1.0
                : profile.wornArmorDurabilityMultiplier();
        double playerArmorWearMultiplier = target instanceof net.minecraft.entity.player.PlayerEntity ? 2.0 : 1.0;
        return Math.max(0.0, weaponMultiplier * wornArmorMultiplier * playerArmorWearMultiplier);
    }

    private static float kingdomcomecombat$effectiveEquipmentSensitiveAttribute(
            LivingEntity target,
            RegistryEntry<EntityAttribute> attribute
    ) {
        double total = target.getAttributeValue(attribute);
        double allArmorEquipment = kingdomcomecombat$equipmentAttributeValue(target, attribute, true);
        double effectiveArmorEquipment = kingdomcomecombat$equipmentAttributeValue(target, attribute, false);
        return (float) Math.max(0.0, total - allArmorEquipment + effectiveArmorEquipment);
    }

    private static double kingdomcomecombat$equipmentAttributeValue(
            LivingEntity target,
            RegistryEntry<EntityAttribute> attribute,
            boolean includeBroken
    ) {
        double[] value = {0.0};
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = target.getEquippedStack(slot);
            if (stack.isEmpty() || (!includeBroken && kingdomcomecombat$isFullyDamaged(stack))) {
                continue;
            }

            AttributeModifiersComponent modifiers = stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (modifiers == null) {
                continue;
            }

            modifiers.applyModifiers(
                    slot,
                    (modifierAttribute, modifier) -> {
                        if (modifierAttribute.equals(attribute)) {
                            value[0] += modifier.value();
                        }
                    }
            );
        }
        return value[0];
    }

    private static boolean kingdomcomecombat$isFullyDamaged(ItemStack stack) {
        return stack.isDamageable()
                && stack.getMaxDamage() > 0
                && stack.getDamage() >= stack.getMaxDamage();
    }

    private static void maybeApplyBleeding(LivingEntity target, DamageSource source, float amount) {
        if (amount <= BLEEDING_ROLL_DAMAGE_THRESHOLD || !canCauseBleeding(source)) {
            return;
        }

        double chance = 0.15 + ModStatusEffects.totalWoundLevels(target) * 0.10;
        if (target.getRandom().nextDouble() < chance) {
            ModStatusEffects.applyInjury(target, "bleeding", 1);
        }
    }

    private static boolean canCauseBleeding(DamageSource source) {
        if (source.isOf(KCC_CUSTOM_COMBAT_DAMAGE)) {
            return true;
        }

        return !source.isIn(DamageTypeTags.BYPASSES_ARMOR)
                && !source.isIn(DamageTypeTags.IS_FIRE)
                && !source.isOf(DamageTypes.OUT_OF_WORLD)
                && !source.isOf(DamageTypes.IN_WALL);
    }
}
