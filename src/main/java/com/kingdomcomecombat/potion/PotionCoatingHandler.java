package com.kingdomcomecombat.potion;

import com.kingdomcomecombat.combat.CombatItemUtil;
import com.kingdomcomecombat.equipment.EquipmentCombatAttributesRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public final class PotionCoatingHandler {
    private static final int COATING_TICKS = 60;
    private static final int ARROWS_PER_COATING = 4;
    private static final int WEAPON_CHARGES = 8;
    private static final String CHARGES_KEY = "kingdom_come_combat_potion_coating_charges";
    private static final String ACTIVE_ANIMATION_KEY = "kingdom_come_combat_potion_coating_active";
    private static final String DRAGON_BREATH_ARROW_NAME = "kingdom_come_combat.dragon_breath_arrow";
    private static final int DRAGON_BREATH_ARROW_COLOR = 0xB02CFF;

    private static final Map<UUID, ActiveCoating> ACTIVE_COATINGS = new HashMap<>();

    private PotionCoatingHandler() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(PotionCoatingHandler::tick);
    }

    public static boolean tryStart(ServerPlayerEntity player) {
        ItemStack mainHand = player.getMainHandStack();
        ItemStack offHand = player.getOffHandStack();
        PotionContentsComponent potionContents = getUsablePotionContents(offHand);
        if (potionContents == null) {
            return false;
        }

        ActiveCoating.Kind kind;
        if (isCoatableArrowStack(mainHand, potionContents)) {
            kind = ActiveCoating.Kind.ARROWS;
        } else if (isCoatableWeapon(player, mainHand)) {
            if (isDragonBreathContents(potionContents)) {
                return false;
            }
            kind = ActiveCoating.Kind.WEAPON;
        } else {
            return false;
        }

        ActiveCoating activeCoating = ACTIVE_COATINGS.get(player.getUuid());
        if (activeCoating != null
                && activeCoating.kind == kind
                && activeCoating.potionContents.equals(potionContents)
                && isStillValid(player, activeCoating)) {
            return true;
        }

        setActiveAnimationMarker(mainHand, true);
        ACTIVE_COATINGS.put(player.getUuid(), new ActiveCoating(kind, player.age, potionContents, mainHand));
        return true;
    }

    public static boolean isActivelyCoating(ServerPlayerEntity player, ItemStack stack) {
        ActiveCoating coating = ACTIVE_COATINGS.get(player.getUuid());
        if (coating == null || stack != coating.stack || stack != player.getMainHandStack()) {
            return false;
        }

        return isStillValid(player, coating);
    }

    public static boolean hasActiveAnimationMarker(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        return customData != null && customData.copyNbt().getInt(ACTIVE_ANIMATION_KEY, 0) > 0;
    }

    public static boolean isPotionCoatingCandidate(PlayerEntity player, ItemStack stack) {
        if (player == null || player.getMainHandStack() != stack) {
            return false;
        }
        PotionContentsComponent contents = getUsablePotionContents(player.getOffHandStack());
        if (contents == null) return false;
        return isCoatableArrowStack(stack, contents)
                || (!isDragonBreathContents(contents) && isCoatableWeapon(player, stack));
    }

    public static void applyCoatedWeaponEffects(LivingEntity attacker, LivingEntity target, float damage) {
        if (damage <= 1.0F) {
            return;
        }

        ItemStack weapon = attacker.getMainHandStack();
        PotionContentsComponent potionContents = weapon.get(DataComponentTypes.POTION_CONTENTS);
        int charges = getWeaponCharges(weapon);
        if (potionContents == null || !potionContents.hasEffects() || charges <= 0) {
            return;
        }

        potionContents.forEachEffect(
                effect -> target.addStatusEffect(effect.withScaledDuration(0.25F), attacker),
                0.25F
        );
        setWeaponCharges(weapon, charges - 1);
    }

    public static boolean isDragonBreathArrow(Entity projectile) {
        if (!(projectile instanceof PersistentProjectileEntity persistent)) {
            return false;
        }
        PotionContentsComponent contents = persistent.getItemStack().get(DataComponentTypes.POTION_CONTENTS);
        return isDragonBreathContents(contents);
    }

    public static ItemStack createDragonBreathArrowStack(int count) {
        ItemStack stack = new ItemStack(Items.TIPPED_ARROW, Math.max(1, count));
        stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                Optional.empty(), Optional.of(DRAGON_BREATH_ARROW_COLOR), List.of(),
                Optional.of(DRAGON_BREATH_ARROW_NAME)));
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.translatable("item.minecraft.tipped_arrow.effect." + DRAGON_BREATH_ARROW_NAME)
                        .formatted(Formatting.LIGHT_PURPLE));
        return stack;
    }

    private static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, ActiveCoating>> iterator = ACTIVE_COATINGS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ActiveCoating> entry = iterator.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            ActiveCoating coating = entry.getValue();
            if (player == null || !player.isUsingItem() || !isStillValid(player, coating)) {
                setActiveAnimationMarker(coating.stack, false);
                iterator.remove();
                continue;
            }

            slowPlayer(player);
            if (player.age - coating.startAge < COATING_TICKS) {
                continue;
            }

            if (coating.kind == ActiveCoating.Kind.ARROWS) {
                finishArrowCoating(player, coating.potionContents);
            } else {
                finishWeaponCoating(player, coating.potionContents);
            }
            setActiveAnimationMarker(coating.stack, false);
            iterator.remove();
        }
    }

    private static boolean isStillValid(ServerPlayerEntity player, ActiveCoating coating) {
        PotionContentsComponent potionContents = getUsablePotionContents(player.getOffHandStack());
        if (potionContents == null || !potionContents.equals(coating.potionContents)) {
            return false;
        }

        if (player.getMainHandStack() != coating.stack) {
            return false;
        }

        if (coating.kind == ActiveCoating.Kind.ARROWS) {
            return isCoatableArrowStack(player.getMainHandStack(), coating.potionContents);
        }
        return isCoatableWeapon(player, player.getMainHandStack());
    }

    private static void finishArrowCoating(ServerPlayerEntity player, PotionContentsComponent potionContents) {
        ItemStack arrows = player.getMainHandStack();
        if (!isCoatableArrowStack(arrows, potionContents)) {
            return;
        }

        int arrowCount = arrowsPerCoating(potionContents);
        arrows.decrement(arrowCount);
        ItemStack tippedArrows = new ItemStack(Items.TIPPED_ARROW, arrowCount);
        tippedArrows.set(DataComponentTypes.POTION_CONTENTS, potionContents);
        if (isDragonBreathContents(potionContents)) {
            tippedArrows.set(DataComponentTypes.CUSTOM_NAME,
                    Text.translatable("item.minecraft.tipped_arrow.effect." + DRAGON_BREATH_ARROW_NAME)
                            .formatted(Formatting.LIGHT_PURPLE));
        }
        giveOrDrop(player, tippedArrows);
        consumeOffhandPotion(player);
    }

    private static void finishWeaponCoating(ServerPlayerEntity player, PotionContentsComponent potionContents) {
        ItemStack weapon = player.getMainHandStack();
        if (!isCoatableWeapon(player, weapon)) {
            return;
        }

        weapon.set(DataComponentTypes.POTION_CONTENTS, potionContents);
        setWeaponCharges(weapon, WEAPON_CHARGES);
        consumeOffhandPotion(player);
    }

    private static void giveOrDrop(ServerPlayerEntity player, ItemStack stack) {
        PlayerInventory inventory = player.getInventory();
        if (!inventory.insertStack(stack) && !stack.isEmpty()) {
            player.dropItem(stack, false, true);
        }
    }

    private static void consumeOffhandPotion(ServerPlayerEntity player) {
        ItemStack offHand = player.getOffHandStack();
        ItemStack exchanged = ItemUsage.exchangeStack(offHand, player, new ItemStack(Items.GLASS_BOTTLE));
        player.setStackInHand(Hand.OFF_HAND, exchanged);
    }

    private static PotionContentsComponent getUsablePotionContents(ItemStack stack) {
        if (stack.isOf(Items.DRAGON_BREATH)) {
            return new PotionContentsComponent(
                    Optional.empty(),
                    Optional.of(DRAGON_BREATH_ARROW_COLOR),
                    List.of(),
                    Optional.of(DRAGON_BREATH_ARROW_NAME)
            );
        }
        if (!stack.isOf(Items.POTION)) {
            return null;
        }

        PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
        return potionContents != null && potionContents.hasEffects() ? potionContents : null;
    }

    private static boolean isDragonBreathContents(PotionContentsComponent contents) {
        return contents != null && contents.customName()
                .map(DRAGON_BREATH_ARROW_NAME::equals)
                .orElse(false);
    }

    private static boolean isCoatableArrowStack(ItemStack stack, PotionContentsComponent contents) {
        return stack.isOf(Items.ARROW) && stack.getCount() >= arrowsPerCoating(contents);
    }

    private static int arrowsPerCoating(PotionContentsComponent contents) {
        return isDragonBreathContents(contents) ? 2 : ARROWS_PER_COATING;
    }

    private static boolean isCoatableWeapon(PlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        return EquipmentCombatAttributesRegistry.getConfiguredWeapon(stack).isPresent()
                || CombatItemUtil.canUseCustomCombat(player)
                || stack.isIn(ItemTags.SWORDS)
                || stack.isIn(ItemTags.AXES)
                || stack.isIn(ItemTags.PICKAXES)
                || stack.isIn(ItemTags.HOES)
                || stack.isIn(ItemTags.SHOVELS);
    }

    private static void slowPlayer(ServerPlayerEntity player) {
        Vec3d velocity = player.getVelocity();
        player.setVelocity(velocity.x * 0.45D, velocity.y, velocity.z * 0.45D);
        player.velocityModified = true;
    }

    private static int getWeaponCharges(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) {
            return 0;
        }
        return customData.copyNbt().getInt(CHARGES_KEY, 0);
    }

    private static void setWeaponCharges(ItemStack stack, int charges) {
        if (charges <= 0) {
            stack.remove(DataComponentTypes.POTION_CONTENTS);
        }
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            if (charges > 0) {
                nbt.putInt(CHARGES_KEY, charges);
            } else {
                nbt.remove(CHARGES_KEY);
            }
        });
    }

    private static void setActiveAnimationMarker(ItemStack stack, boolean active) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            if (active) {
                nbt.putInt(ACTIVE_ANIMATION_KEY, 1);
            } else {
                nbt.remove(ACTIVE_ANIMATION_KEY);
            }
        });
    }

    private record ActiveCoating(
            Kind kind,
            int startAge,
            PotionContentsComponent potionContents,
            ItemStack stack
    ) {
        private enum Kind {
            ARROWS,
            WEAPON
        }
    }
}
