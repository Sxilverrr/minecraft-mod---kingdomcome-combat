package com.kingdomcomecombat.injury;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.config.CombatServerConfig;
import com.kingdomcomecombat.game.ModGameRules;
import com.kingdomcomecombat.item.ModItems;
import com.kingdomcomecombat.passive.PassiveSkillPerks;
import com.kingdomcomecombat.collision.HumanoidHurtboxLibrary;
import com.kingdomcomecombat.equipment.MobCombatAttributes;
import com.kingdomcomecombat.equipment.MobCombatAttributesRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.ActionResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;

public class InjuryTicker {
    private static final int LOW_TIER_WOUND_MAX_LEVEL = 3;
    private static final int NATURAL_RECOVERY_TICKS = 20 * 60 * 10;
    private static final int NATURAL_HEAL_RECOVERY_CREDIT_PER_HEALTH = 20 * 20;
    private static final int SLEEP_MIN_FOOD_LEVEL = 6;

    private static final RegistryKey<DamageType> BLEEDING_DAMAGE =
            RegistryKey.of(
                    RegistryKeys.DAMAGE_TYPE,
                    Identifier.of(KingdomComeCombat.MOD_ID, "bleeding")
            );

    private static final List<EquipmentSlot> BANDAGE_SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS
    );
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    );

    private static final Map<UUID, Integer> naturalRecoveryTimers = new HashMap<>();
    private static final Map<UUID, Float> mobHealProgress = new HashMap<>();

    private InjuryTicker() {
    }

    public static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (CombatServerConfig.changedBandageUseEnabled()
                    || world.isClient() || !player.isSneaking() || !(entity instanceof LivingEntity target)) {
                return ActionResult.PASS;
            }
            ItemStack bandage = player.getStackInHand(hand);
            if (!bandage.isOf(ModItems.BANDAGE) || !ModStatusEffects.reduceInjury(
                    target, ModStatusEffects.BLEEDING, 1)) {
                return ActionResult.PASS;
            }
            int bandageDamage = player instanceof ServerPlayerEntity serverPlayer
                    && com.kingdomcomecombat.hardship.HardshipSelectionState.active(serverPlayer, "hardship_05")
                    ? Math.max(1, (int) Math.ceil(com.kingdomcomecombat.hardship.HardshipSelectionState.prek(
                            serverPlayer, "hardship_05", "bandage_durability_multiplier", 2.0))) : 1;
            bandage.damage(bandageDamage, player, hand == net.minecraft.util.Hand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            return ActionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                long worldTime = world.getTime();
                boolean bandageTick = worldTime % 10L == 0L;
                boolean maintenanceTick = worldTime % 20L == 0L;
                if (!bandageTick && !maintenanceTick) {
                    continue;
                }

                for (var entity : world.iterateEntities()) {
                    if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                        continue;
                    }

                    if (maintenanceTick) {
                        tickCorrosion(living);
                    }

                    if (com.kingdomcomecombat.config.CombatServerConfig.lightweightDamageModeEnabled()
                            || !ModGameRules.traumaEnabled(living)) {
                        ModStatusEffects.clearInjuries(living);
                        naturalRecoveryTimers.remove(living.getUuid());
                        continue;
                    }

                    if (worldTime % PassiveSkillPerks.bleedingIntervalTicks(living) == 0L) {
                        int bleeding = ModStatusEffects.level(living, ModStatusEffects.BLEEDING);
                        if (bleeding > 0) {
                            living.damage(
                                    world,
                                    world.getDamageSources().create(BLEEDING_DAMAGE),
                                    1.0F
                            );
                        }
                    }
                    if (bandageTick) {
                        healWithEquippedBandages(living);
                    }
                    if (maintenanceTick) {
                        if (ModStatusEffects.totalWoundLevels(living) > 0
                                || ModStatusEffects.level(living, ModStatusEffects.ANALGESIA) > 0) {
                            ModStatusEffects.syncEffectiveAttributeModifiers(living);
                        }
                        tickNaturalWoundRecovery(living, 20);
                    }
                }
            }
        });
    }

    public static void onEntityHealed(LivingEntity entity, float healedHealth) {
        if (healedHealth <= 0.0F || entity instanceof net.minecraft.entity.player.PlayerEntity) return;
        if (!ModStatusEffects.hasBleeding(entity) && ModStatusEffects.totalWoundLevels(entity) <= 0) {
            mobHealProgress.remove(entity.getUuid());
            return;
        }
        float threshold = Math.max(0.001F, entity.getMaxHealth() * 0.05F);
        float progress = mobHealProgress.getOrDefault(entity.getUuid(), 0.0F) + healedHealth;
        while (progress + 1.0E-5F >= threshold) {
            if (!ModStatusEffects.healBleedingOrRandomWound(entity)) {
                mobHealProgress.remove(entity.getUuid());
                return;
            }
            progress -= threshold;
        }
        if (progress > 0.0F) mobHealProgress.put(entity.getUuid(), progress);
        else mobHealProgress.remove(entity.getUuid());
    }

    private static void tickCorrosion(LivingEntity entity) {
        int level = ModStatusEffects.level(entity, ModStatusEffects.CORROSION);
        if (level <= 0) {
            return;
        }

        if (!HumanoidHurtboxLibrary.isHumanoidTarget(entity)) {
            corrodeNaturalArmor(entity, level);
            return;
        }

        List<EquipmentSlot> candidates = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getEquippedStack(slot);
            if (!stack.isEmpty() && stack.isDamageable() && stack.getDamage() < stack.getMaxDamage()) {
                candidates.add(slot);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        EquipmentSlot slot = candidates.get(entity.getRandom().nextInt(candidates.size()));
        ItemStack stack = entity.getEquippedStack(slot);
        int amount = Math.max(1, (int) Math.ceil(stack.getMaxDamage() * level * 0.01));
        stack.damage(amount, entity, slot);
    }

    private static void corrodeNaturalArmor(LivingEntity entity, int level) {
        MobCombatAttributes attributes = MobCombatAttributesRegistry.get(entity).orElse(null);
        if (attributes == null) {
            return;
        }

        List<MobCombatAttributesRegistry.ArmorSection> candidates = new ArrayList<>();
        if (hasNaturalArmorRemaining(entity, attributes.headArmor(), MobCombatAttributesRegistry.ArmorSection.HEAD)) {
            candidates.add(MobCombatAttributesRegistry.ArmorSection.HEAD);
        }
        if (hasNaturalArmorRemaining(entity, attributes.bodyArmor(), MobCombatAttributesRegistry.ArmorSection.BODY)) {
            candidates.add(MobCombatAttributesRegistry.ArmorSection.BODY);
        }
        if (candidates.isEmpty()) {
            return;
        }

        MobCombatAttributesRegistry.ArmorSection section =
                candidates.get(entity.getRandom().nextInt(candidates.size()));
        MobCombatAttributes.NaturalArmor armor = section == MobCombatAttributesRegistry.ArmorSection.HEAD
                ? attributes.headArmor()
                : attributes.bodyArmor();
        int amount = Math.max(1, (int) Math.ceil(armor.durability() * level * 0.01));
        MobCombatAttributesRegistry.damageArmor(entity, section, amount);
    }

    private static boolean hasNaturalArmorRemaining(
            LivingEntity entity,
            MobCombatAttributes.NaturalArmor armor,
            MobCombatAttributesRegistry.ArmorSection section
    ) {
        return armor.durability() > 0
                && MobCombatAttributesRegistry.armorDamage(entity, section) < armor.durability();
    }

    public static void advanceNaturalRecoveryFromHeal(LivingEntity entity, float healedHealth) {
        if (healedHealth <= 0.0F || !hasLowTierWounds(entity)) {
            return;
        }

        int credit = Math.max(1, Math.round(healedHealth * NATURAL_HEAL_RECOVERY_CREDIT_PER_HEALTH));
        UUID uuid = entity.getUuid();
        int remaining = naturalRecoveryTimers.getOrDefault(uuid, NATURAL_RECOVERY_TICKS) - credit;
        if (remaining <= 0) {
            if (ModStatusEffects.reduceFirstWoundAtOrBelow(entity, LOW_TIER_WOUND_MAX_LEVEL)) {
                remaining += NATURAL_RECOVERY_TICKS;
            } else {
                naturalRecoveryTimers.remove(uuid);
                return;
            }
        }
        naturalRecoveryTimers.put(uuid, Math.max(1, remaining));
    }

    public static double naturalRegenerationMultiplier(LivingEntity entity) {
        int woundLevels = ModStatusEffects.totalEffectiveWoundLevels(entity);
        double multiplier = Math.max(0.05, 1.0 - woundLevels * 0.03);
        if (entity instanceof ServerPlayerEntity player
                && com.kingdomcomecombat.hardship.HardshipSelectionState.active(player, "hardship_05")) {
            multiplier *= com.kingdomcomecombat.hardship.HardshipSelectionState.prek(
                    player, "hardship_05", "natural_regeneration_multiplier", 0.7);
        }
        return multiplier;
    }

    public static void healLowTierWoundsFromSleep(ServerPlayerEntity player) {
        int spendableFood = Math.max(0, player.getHungerManager().getFoodLevel() - SLEEP_MIN_FOOD_LEVEL);
        if (spendableFood <= 0) {
            return;
        }

        int healed = ModStatusEffects.reduceWoundsAtOrBelow(player, LOW_TIER_WOUND_MAX_LEVEL, spendableFood);
        if (healed > 0) {
            player.getHungerManager().setFoodLevel(player.getHungerManager().getFoodLevel() - healed);
            naturalRecoveryTimers.remove(player.getUuid());
        }
    }

    private static void healWithEquippedBandages(LivingEntity entity) {
        if (CombatServerConfig.changedBandageUseEnabled()) {
            return;
        }
        for (EquipmentSlot slot : BANDAGE_SLOTS) {
            ItemStack stack = entity.getEquippedStack(slot);
            if (!stack.isOf(ModItems.BANDAGE)) {
                continue;
            }

            if (healOneLayer(entity, slot)) {
                damageBandage(entity, slot, stack);
            }
        }
    }

    private static boolean healOneLayer(LivingEntity entity, EquipmentSlot slot) {
        if (ModStatusEffects.reduceInjury(entity, ModStatusEffects.BLEEDING, 1)) {
            return true;
        }

        for (RegistryEntry<StatusEffect> effect : treatableWoundsFor(slot)) {
            if (ModStatusEffects.level(entity, effect) > 1
                    && ModStatusEffects.reduceInjury(entity, effect, 1)) {
                return true;
            }
        }
        return false;
    }

    private static List<RegistryEntry<StatusEffect>> treatableWoundsFor(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> List.of(ModStatusEffects.HEAD_INJURY);
            case CHEST -> List.of(
                    ModStatusEffects.TORSO_INJURY,
                    ModStatusEffects.ARM_INJURY,
                    ModStatusEffects.HAND_INJURY
            );
            case LEGS -> List.of(ModStatusEffects.LEG_INJURY);
            default -> List.of();
        };
    }

    private static void damageBandage(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {
        if (!stack.isDamageable() || stack.getMaxDamage() <= 0) {
            return;
        }

        int amount = entity instanceof ServerPlayerEntity player
                && com.kingdomcomecombat.hardship.HardshipSelectionState.active(player, "hardship_05")
                ? Math.max(1, (int) Math.ceil(com.kingdomcomecombat.hardship.HardshipSelectionState.prek(
                        player, "hardship_05", "bandage_durability_multiplier", 2.0))) : 1;
        int nextDamage = stack.getDamage() + amount;
        if (nextDamage >= stack.getMaxDamage()) {
            entity.equipStack(slot, ItemStack.EMPTY);
            entity.sendEquipmentBreakStatus(stack.getItem(), slot);
            return;
        }

        stack.setDamage(nextDamage);
    }

    private static void tickNaturalWoundRecovery(LivingEntity entity, int elapsedTicks) {
        UUID uuid = entity.getUuid();
        if (!hasLowTierWounds(entity)) {
            naturalRecoveryTimers.remove(uuid);
            return;
        }

        int remaining = naturalRecoveryTimers.getOrDefault(uuid, NATURAL_RECOVERY_TICKS) - elapsedTicks;
        if (remaining > 0) {
            naturalRecoveryTimers.put(uuid, remaining);
            return;
        }

        if (ModStatusEffects.reduceFirstWoundAtOrBelow(entity, LOW_TIER_WOUND_MAX_LEVEL)) {
            naturalRecoveryTimers.put(uuid, NATURAL_RECOVERY_TICKS);
        } else {
            naturalRecoveryTimers.remove(uuid);
        }
    }

    private static boolean hasLowTierWounds(LivingEntity entity) {
        return isLowTierWound(entity, ModStatusEffects.ARM_INJURY)
                || isLowTierWound(entity, ModStatusEffects.HAND_INJURY)
                || isLowTierWound(entity, ModStatusEffects.TORSO_INJURY)
                || isLowTierWound(entity, ModStatusEffects.LEG_INJURY)
                || isLowTierWound(entity, ModStatusEffects.HEAD_INJURY);
    }

    private static boolean isLowTierWound(
            LivingEntity entity,
            RegistryEntry<StatusEffect> effect
    ) {
        int level = ModStatusEffects.level(entity, effect);
        return level > 0 && level <= LOW_TIER_WOUND_MAX_LEVEL;
    }
}
