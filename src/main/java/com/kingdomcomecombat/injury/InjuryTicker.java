package com.kingdomcomecombat.injury;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.game.ModGameRules;
import com.kingdomcomecombat.item.ModItems;
import com.kingdomcomecombat.passive.PassiveSkillPerks;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private static final Map<UUID, Integer> naturalRecoveryTimers = new HashMap<>();

    private InjuryTicker() {
    }

    public static void register() {
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

                    if (!ModGameRules.traumaEnabled(living)) {
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
                                    bleeding
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
        return Math.max(0.05, 1.0 - woundLevels * 0.03);
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

        int nextDamage = stack.getDamage() + 1;
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
