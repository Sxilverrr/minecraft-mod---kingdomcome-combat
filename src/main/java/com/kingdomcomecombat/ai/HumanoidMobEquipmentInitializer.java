package com.kingdomcomecombat.ai;

import com.kingdomcomecombat.item.ModItems;
import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.config.CombatServerConfig;
import com.kingdomcomecombat.compat.GuardVillagersCompat;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.mob.VindicatorEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;

public class HumanoidMobEquipmentInitializer {
    private static final double SHIELD_CHANCE = 0.08;
    private static final double PILLAGER_SHIELD_CHANCE = 0.03;
    private static final double LONGSWORD_UPGRADE_CHANCE = 0.12;

    private HumanoidMobEquipmentInitializer() {
    }

    public static void initialize(MobEntity mob, HumanoidCombatAiState state) {
        if (GuardVillagersCompat.isGuard(mob)) {
            // Guard Villagers owns the guard's sword/crossbow, shield, armor and inventory.
            state.equipmentInitialized = true;
            return;
        }
        if (CombatServerConfig.disableVanillaLeftHandedMobs() && mob.isLeftHanded()) {
            mob.setLeftHanded(false);
        }
        ZombieLeaderUtil.fixInitialHealthIfEnabled(mob);
        if (state.equipmentInitialized) {
            if (!CombatServerConfig.modEquipmentGenerationEnabled()) {
                return;
            }
            if (mob instanceof WitherSkeletonEntity witherSkeleton) {
                equipWitherSkeleton(witherSkeleton);
            }
            enforceEquipmentRules(mob, false);
            return;
        }

        state.equipmentInitialized = true;
        if (!CombatServerConfig.modEquipmentGenerationEnabled()) {
            return;
        }
        Random random = mob.getRandom();

        if (mob instanceof WitherSkeletonEntity witherSkeleton) {
            equipWitherSkeleton(witherSkeleton);
            equipArmor(mob, random);
            enforceEquipmentRules(mob, true);
            return;
        }

        if (mob instanceof AbstractSkeletonEntity skeleton) {
            equipSkeleton(skeleton, random);
            equipArmor(mob, random);
            maybeUpgradeShortSword(mob, random);
            maybeEquipShield(mob, random);
            enforceEquipmentRules(mob, true);
            return;
        }

        if (mob instanceof ZombieEntity zombie) {
            boolean leader = ZombieLeaderUtil.isLeader(zombie);
            equipZombie(zombie, random, leader);
            if (!zombie.isBaby()) {
                equipArmor(mob, random, leader);
            }
            maybeUpgradeShortSword(mob, random);
            maybeEquipShield(mob, random);
            enforceEquipmentRules(mob, true);
            return;
        }

        if (mob instanceof VindicatorEntity || mob instanceof PillagerEntity) {
            equipIllager(mob, random);
            enforceEquipmentRules(mob, true);
            return;
        }

        equipGenericHumanoid(mob, random);
        maybeUpgradeShortSword(mob, random);
        maybeEquipShield(mob, random);
        enforceEquipmentRules(mob, true);
    }

    private static void equipSkeleton(MobEntity skeleton, Random random) {
        if (random.nextDouble() >= 0.65) {
            return;
        }

        equipReplacingRangedWeapon(
                skeleton,
                randomUndeadMeleeWeapon(random)
        );
    }

    private static void equipWitherSkeleton(WitherSkeletonEntity skeleton) {
        if (!skeleton.getMainHandStack().isOf(ModItems.STONE_LONGSWORD)) {
            ItemStack longsword = new ItemStack(ModItems.STONE_LONGSWORD);
            maybeEnchantSpawnEquipment(skeleton, longsword);
            skeleton.equipStack(EquipmentSlot.MAINHAND, longsword);
        }
        skeleton.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    private static void equipZombie(ZombieEntity zombie, Random random, boolean leader) {
        if (zombie.isBaby()) {
            return;
        }

        if (!leader && random.nextDouble() >= 0.75) {
            return;
        }

        equipIfEmpty(
                zombie,
                EquipmentSlot.MAINHAND,
                new ItemStack(leader ? Items.IRON_SWORD : randomUndeadMeleeWeapon(random))
        );
    }

    private static Item randomUndeadMeleeWeapon(Random random) {
        return random.nextInt(100) < 2
                ? ModItems.IRON_FIGHTING_MACE
                : randomMeleeWeapon(random);
    }

    private static void enforceEquipmentRules(MobEntity mob, boolean enforceSpawnArmorDurability) {
        clearOffhandWeapon(mob);
        if (CombatItemUtil.isPolearm(mob.getMainHandStack())) {
            mob.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
        if (ZombieLeaderUtil.isLeader(mob)) {
            fillMissingIronArmor(mob, true, true, true, true);
        }
        if (enforceSpawnArmorDurability) {
            enforceArmorDurability(mob);
        }
    }

    private static void enforceArmorDurability(MobEntity mob) {
        for (EquipmentSlot slot : java.util.List.of(
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        )) {
            ItemStack stack = mob.getEquippedStack(slot);
            if (stack.isEmpty() || !stack.isDamageable() || stack.getMaxDamage() <= 0) {
                continue;
            }

            int maxAllowedDamage = (int) Math.floor(stack.getMaxDamage() * 0.20);
            if (stack.getDamage() > maxAllowedDamage) {
                stack.setDamage(maxAllowedDamage);
            }
        }
    }

    private static Item randomMeleeWeapon(Random random) {
        return switch (random.nextInt(10)) {
            case 0 -> Items.STONE_SWORD;
            case 1, 2, 3, 4, 5, 6, 7 -> Items.IRON_SWORD;
            case 8 -> Items.IRON_PICKAXE;
            default -> Items.IRON_AXE;
        };
    }

    private static void maybeEquipShield(MobEntity mob, Random random) {
        if (mob instanceof ZombieEntity zombie && zombie.isBaby()) {
            return;
        }
        if (mob.getMainHandStack().isEmpty()
                || CombatItemUtil.isPolearm(mob.getMainHandStack())
                || mob.getMainHandStack().getItem() instanceof RangedWeaponItem
                || random.nextDouble() >= SHIELD_CHANCE) {
            return;
        }
        equipIfEmpty(mob, EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
    }

    private static void equipReplacingRangedWeapon(MobEntity mob, Item weapon) {
        if (!mob.getMainHandStack().isEmpty()
                && !(mob.getMainHandStack().getItem() instanceof RangedWeaponItem)) {
            return;
        }
        ItemStack stack = new ItemStack(weapon);
        maybeEnchantSpawnEquipment(mob, stack);
        mob.equipStack(EquipmentSlot.MAINHAND, stack);
    }

    private static void equipGenericHumanoid(MobEntity mob, Random random) {
        ItemStack mainHand = mob.getMainHandStack();
        if (mainHand.isEmpty() && random.nextDouble() < 0.70) {
            equipIfEmpty(mob, EquipmentSlot.MAINHAND, new ItemStack(randomMeleeWeapon(random)));
        } else if (mainHand.getItem() instanceof RangedWeaponItem && random.nextDouble() < 0.55) {
            equipReplacingRangedWeapon(mob, randomMeleeWeapon(random));
        }
    }

    private static void maybeUpgradeShortSword(MobEntity mob, Random random) {
        ItemStack shortSword = mob.getMainHandStack();
        if (!shortSword.isIn(ItemTags.SWORDS)
                || CombatItemUtil.isLongsword(shortSword)
                || random.nextDouble() >= LONGSWORD_UPGRADE_CHANCE) {
            return;
        }

        Item longsword = shortSword.isOf(Items.WOODEN_SWORD) || shortSword.isOf(Items.STONE_SWORD)
                ? ModItems.STONE_LONGSWORD
                : ModItems.IRON_LONGSWORD;
        ItemStack upgraded = new ItemStack(longsword);
        maybeEnchantSpawnEquipment(mob, upgraded);
        mob.equipStack(EquipmentSlot.MAINHAND, upgraded);
    }

    private static void clearOffhandWeapon(MobEntity mob) {
        ItemStack offhand = mob.getOffHandStack();
        if (offhand.isEmpty()) {
            return;
        }

        if (isWeapon(offhand)) {
            mob.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        }
    }

    private static boolean isWeapon(ItemStack stack) {
        return stack.isIn(ItemTags.SWORDS)
                || stack.isIn(ItemTags.AXES)
                || stack.isIn(ItemTags.PICKAXES)
                || stack.isIn(ItemTags.HOES)
                || stack.isIn(ItemTags.SHOVELS)
                || stack.getItem() instanceof RangedWeaponItem;
    }

    private static void equipIllager(MobEntity mob, Random random) {
        if (mob instanceof PillagerEntity pillager) {
            if (pillager.isPatrolLeader()) {
                equipMainHand(mob, Items.IRON_SWORD);
                mob.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
                equipExactIronArmor(mob);
                return;
            }

            double weaponRoll = random.nextDouble();
            if (weaponRoll < 0.60) {
                equipMainHand(mob, Items.CROSSBOW);
            } else if (weaponRoll < 0.70) {
                equipMainHand(mob, Items.IRON_SWORD);
                fillMissingIronArmor(mob, true, true, true, true);
            } else {
                equipMainHand(mob, random.nextBoolean() ? Items.IRON_AXE : ModItems.IRON_FIGHTING_MACE);
            }

            if (!(mob.getMainHandStack().getItem() instanceof RangedWeaponItem)
                    && random.nextDouble() < PILLAGER_SHIELD_CHANCE) {
                mob.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            } else {
                mob.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
            }
        }

        if (mob instanceof VindicatorEntity) {
            double weaponRoll = random.nextDouble();
            if (weaponRoll < 0.70) {
                equipMainHand(mob, Items.IRON_SWORD);
            } else {
                equipMainHand(mob, random.nextBoolean() ? Items.IRON_AXE : ModItems.IRON_FIGHTING_MACE);
            }
        }

        if (random.nextDouble() < 0.45) {
            equipIfEmpty(
                    mob,
                    EquipmentSlot.HEAD,
                    new ItemStack(randomArmorMaterial(random, ArmorPiece.HELMET))
            );
        }
        if (random.nextDouble() < 0.30) {
            equipIfEmpty(
                    mob,
                    EquipmentSlot.CHEST,
                    new ItemStack(randomArmorMaterial(random, ArmorPiece.CHESTPLATE))
            );
        }
    }

    private static void equipMainHand(MobEntity mob, Item item) {
        ItemStack stack = new ItemStack(item);
        maybeEnchantSpawnEquipment(mob, stack);
        mob.equipStack(EquipmentSlot.MAINHAND, stack);
    }

    private static void equipExactIronArmor(MobEntity mob) {
        mob.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        mob.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        mob.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        mob.equipStack(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
    }

    private static void equipArmor(MobEntity mob, Random random) {
        equipArmor(mob, random, false);
    }

    private static void equipArmor(MobEntity mob, Random random, boolean forceFullIron) {
        if (forceFullIron) {
            equipIronArmor(mob, true, true, true, true);
            return;
        }

        double roll = random.nextDouble();
        if (roll < 0.02) {
            equipIronArmor(mob, true, true, true, true);
        } else if (roll < 0.10) {
            equipIronArmor(mob, true, false, true, true);
        } else if (roll < 0.22) {
            equipIronArmor(mob, true, false, false, true);
        } else if (roll < 0.38) {
            equipIronArmor(mob, true, true, false, false);
        } else if (roll < 0.60) {
            equipIronArmor(mob, true, false, false, false);
        }
    }

    private static void equipIronArmor(
            MobEntity mob,
            boolean helmet,
            boolean chestplate,
            boolean leggings,
            boolean boots
    ) {
        if (helmet) {
            equipIfEmpty(mob, EquipmentSlot.HEAD, new ItemStack(randomArmorMaterial(mob.getRandom(), ArmorPiece.HELMET)));
        }
        if (chestplate) {
            equipIfEmpty(mob, EquipmentSlot.CHEST, new ItemStack(randomArmorMaterial(mob.getRandom(), ArmorPiece.CHESTPLATE)));
        }
        if (leggings) {
            equipIfEmpty(mob, EquipmentSlot.LEGS, new ItemStack(randomArmorMaterial(mob.getRandom(), ArmorPiece.LEGGINGS)));
        }
        if (boots) {
            equipIfEmpty(mob, EquipmentSlot.FEET, new ItemStack(randomArmorMaterial(mob.getRandom(), ArmorPiece.BOOTS)));
        }
    }

    private static void fillMissingIronArmor(
            MobEntity mob,
            boolean helmet,
            boolean chestplate,
            boolean leggings,
            boolean boots
    ) {
        if (helmet && mob.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
            equipIfEmpty(mob, EquipmentSlot.HEAD, new ItemStack(randomArmorMaterial(mob.getRandom(), ArmorPiece.HELMET)));
        }
        if (chestplate && mob.getEquippedStack(EquipmentSlot.CHEST).isEmpty()) {
            equipIfEmpty(mob, EquipmentSlot.CHEST, new ItemStack(randomArmorMaterial(mob.getRandom(), ArmorPiece.CHESTPLATE)));
        }
        if (leggings && mob.getEquippedStack(EquipmentSlot.LEGS).isEmpty()) {
            equipIfEmpty(mob, EquipmentSlot.LEGS, new ItemStack(randomArmorMaterial(mob.getRandom(), ArmorPiece.LEGGINGS)));
        }
        if (boots && mob.getEquippedStack(EquipmentSlot.FEET).isEmpty()) {
            equipIfEmpty(mob, EquipmentSlot.FEET, new ItemStack(randomArmorMaterial(mob.getRandom(), ArmorPiece.BOOTS)));
        }
    }

    private static void equipIfEmpty(MobEntity mob, EquipmentSlot slot, ItemStack stack) {
        if (!mob.getEquippedStack(slot).isEmpty()) {
            return;
        }

        maybeEnchantSpawnEquipment(mob, stack);
        mob.equipStack(slot, stack);
    }

    private static void maybeEnchantSpawnEquipment(MobEntity mob, ItemStack stack) {
        if (stack.isEmpty()
                || !EnchantmentHelper.canHaveEnchantments(stack)
                || !(mob.getWorld() instanceof ServerWorld world)) {
            return;
        }

        LocalDifficulty difficulty = world.getLocalDifficulty(mob.getBlockPos());
        float chance = 0.25F * difficulty.getClampedLocalDifficulty();
        if (mob.getRandom().nextFloat() >= chance) {
            return;
        }

        int level = 5 + (int) (difficulty.getClampedLocalDifficulty() * mob.getRandom().nextInt(18));
        EnchantmentHelper.enchant(mob.getRandom(), stack, level, world.getRegistryManager(), java.util.Optional.empty());
    }

    private static Item randomArmorMaterial(Random random, ArmorPiece piece) {
        double roll = random.nextDouble();
        ArmorMaterial material = roll < 0.50
                ? ArmorMaterial.LEATHER
                : roll < 0.80
                        ? ArmorMaterial.CHAINMAIL
                        : ArmorMaterial.IRON;
        return switch (piece) {
            case HELMET -> switch (material) {
                case LEATHER -> Items.LEATHER_HELMET;
                case CHAINMAIL -> Items.CHAINMAIL_HELMET;
                case IRON -> Items.IRON_HELMET;
            };
            case CHESTPLATE -> switch (material) {
                case LEATHER -> Items.LEATHER_CHESTPLATE;
                case CHAINMAIL -> Items.CHAINMAIL_CHESTPLATE;
                case IRON -> Items.IRON_CHESTPLATE;
            };
            case LEGGINGS -> switch (material) {
                case LEATHER -> Items.LEATHER_LEGGINGS;
                case CHAINMAIL -> Items.CHAINMAIL_LEGGINGS;
                case IRON -> Items.IRON_LEGGINGS;
            };
            case BOOTS -> switch (material) {
                case LEATHER -> Items.LEATHER_BOOTS;
                case CHAINMAIL -> Items.CHAINMAIL_BOOTS;
                case IRON -> Items.IRON_BOOTS;
            };
        };
    }

    private enum ArmorMaterial {
        LEATHER,
        CHAINMAIL,
        IRON
    }

    private enum ArmorPiece {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS
    }
}
