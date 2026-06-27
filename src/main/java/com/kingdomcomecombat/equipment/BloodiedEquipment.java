package com.kingdomcomecombat.equipment;

import com.kingdomcomecombat.config.CombatClientConfig;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

public final class BloodiedEquipment {
    private static final String BLOOD_PERCENT_KEY = "kingdom_come_combat_blood_percent";
    public static final double MAX_BLOOD_PERCENT = 1.5;
    private static final double BLOOD_GAIN_MULTIPLIER = 1.5;
    private static final double SPLASH_RADIUS = 2.0;
    private static final int MIN_REMAINING_BLOOD = 20;
    private static final int FADE_INTERVAL_TICKS = 105;

    private BloodiedEquipment() {
    }

    public static double getBloodPercent(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return 0.0;
        }
        return clampBlood(customData.copyNbt().getInt(BLOOD_PERCENT_KEY, 0) / 100.0);
    }

    public static void addBloodPercent(ItemStack stack, double percent) {
        if (stack.isEmpty() || percent <= 0.0 || !canShowOverlay(stack)) {
            return;
        }

        int next = (int) Math.round(Math.min(MAX_BLOOD_PERCENT, getBloodPercent(stack) + percent) * 100.0);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putInt(BLOOD_PERCENT_KEY, next));
    }

    public static void tickBloodDecay(LivingEntity entity) {
        boolean water = entity.isTouchingWater();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmorSlot()) {
                decayStackBlood(entity.getEquippedStack(slot), water, entity.age);
            }
        }
        decayStackBlood(entity.getMainHandStack(), water, entity.age);
        decayStackBlood(entity.getOffHandStack(), water, entity.age);
    }

    public static void clearBlood(LivingEntity entity) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmorSlot()) {
                clearStackBlood(entity.getEquippedStack(slot));
            }
        }
        clearStackBlood(entity.getMainHandStack());
        clearStackBlood(entity.getOffHandStack());
    }

    public static void splashBlood(ServerWorld world, LivingEntity damaged, float damageAmount) {
        splashBlood(world, damaged, damageAmount, 1.0);
    }

    public static void splashBlood(
            ServerWorld world,
            LivingEntity damaged,
            float damageAmount,
            double damageTypeMultiplier
    ) {
        if (damageAmount <= 0.0F) {
            return;
        }

        double base = Math.min(0.45, 0.08 + damageAmount * 0.025)
                * CombatClientConfig.bloodStainPercent()
                * Math.max(0.0, damageTypeMultiplier);
        Box box = damaged.getBoundingBox().expand(SPLASH_RADIUS);
        for (LivingEntity entity : world.getEntitiesByClass(
                LivingEntity.class,
                box,
                entity -> entity.isAlive() && entity.squaredDistanceTo(damaged) <= SPLASH_RADIUS * SPLASH_RADIUS
        )) {
            double distance = Math.sqrt(entity.squaredDistanceTo(damaged));
            double falloff = Math.pow(Math.max(0.0, 1.0 - distance / SPLASH_RADIUS), 1.2);
            double amount = base * falloff * BLOOD_GAIN_MULTIPLIER;
            if (entity instanceof BloodiedEntityAccess bloodied) {
                bloodied.kingdomcomecombat$addBodyBloodPercent(amount);
            }
            addBloodToWornAndHeld(entity, amount);
        }
    }

    public static boolean canShowOverlay(ItemStack stack) {
        return isArmor(stack) || isWeaponLike(stack);
    }

    public static boolean isArmor(ItemStack stack) {
        return stack.isIn(ItemTags.HEAD_ARMOR)
                || stack.isIn(ItemTags.CHEST_ARMOR)
                || stack.isIn(ItemTags.LEG_ARMOR)
                || stack.isIn(ItemTags.FOOT_ARMOR);
    }

    public static boolean isWeaponLike(ItemStack stack) {
        return stack.isOf(Items.WOODEN_SWORD)
                || stack.isOf(Items.STONE_SWORD)
                || stack.isOf(Items.IRON_SWORD)
                || stack.isOf(Items.GOLDEN_SWORD)
                || stack.isOf(Items.DIAMOND_SWORD)
                || stack.isOf(Items.NETHERITE_SWORD)
                || stack.isIn(ItemTags.SWORDS)
                || stack.isIn(ItemTags.AXES)
                || stack.isIn(ItemTags.PICKAXES)
                || stack.isIn(ItemTags.HOES)
                || stack.isIn(ItemTags.SHOVELS);
    }

    private static void addBloodToWornAndHeld(LivingEntity entity, double percent) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.isArmorSlot()) {
                addBloodPercent(entity.getEquippedStack(slot), percent);
            }
        }
        addBloodPercent(entity.getMainHandStack(), percent);
        addBloodPercent(entity.getOffHandStack(), percent * 0.75);
    }

    private static void decayStackBlood(ItemStack stack, boolean water, int age) {
        if (stack.isEmpty() || !canShowOverlay(stack)) {
            return;
        }
        int current = (int) Math.round(getBloodPercent(stack) * 100.0);
        if (current <= 0) {
            return;
        }
        if (water) {
            if (current > MIN_REMAINING_BLOOD) {
                setBloodPercent(stack, MIN_REMAINING_BLOOD / 100.0);
            }
            return;
        }
        if (current > MIN_REMAINING_BLOOD && age % FADE_INTERVAL_TICKS == 0) {
            setBloodPercent(stack, Math.max(MIN_REMAINING_BLOOD, current - 1) / 100.0);
        }
    }

    private static void setBloodPercent(ItemStack stack, double percent) {
        int next = (int) Math.round(clampBlood(percent) * 100.0);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putInt(BLOOD_PERCENT_KEY, next));
    }

    private static void clearStackBlood(ItemStack stack) {
        if (stack.isEmpty() || getBloodPercent(stack) <= 0.0) {
            return;
        }
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.remove(BLOOD_PERCENT_KEY));
    }

    private static double clampBlood(double value) {
        return Math.max(0.0, Math.min(MAX_BLOOD_PERCENT, value));
    }
}
