package com.kingdomcomecombat.item;

import com.kingdomcomecombat.KingdomComeCombat;
import com.kingdomcomecombat.combat.ComboMoveConfig;
import com.kingdomcomecombat.combat.ComboMoveConfigs;
import com.kingdomcomecombat.combat.PlayerComboProgress;
import com.kingdomcomecombat.passive.PassiveSkillConfig;
import com.kingdomcomecombat.passive.PassiveSkillConfigs;
import com.kingdomcomecombat.passive.PlayerPassiveSkillProgress;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.LootTable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradedItem;
import net.minecraft.village.VillagerProfession;

import java.util.ArrayList;
import java.util.List;

public final class SkillBookAcquisition {
    private SkillBookAcquisition() {
    }

    public static void register() {
        registerLoot();
        registerTrades();
    }

    private static void registerLoot() {
        LootTableEvents.MODIFY_DROPS.register((entry, context, drops) -> {
            String path = entry.getKey()
                    .map(RegistryKey::getValue)
                    .map(Identifier::getPath)
                    .orElse("");
            ServerPlayerEntity player = contextPlayer(context);
            if (!isSkillBookChest(path) || context.getRandom().nextFloat() > lootChance(path)) {
                return;
            }

            ItemStack book = player == null
                    ? randomSkillBook(context.getRandom())
                    : randomUnknownSkillBook(context.getRandom(), player);
            if (!book.isEmpty()) {
                drops.add(book);
            }
        });
    }

    private static void registerTrades() {
        for (int level : List.of(2, 4, 5)) {
            TradeOfferHelper.registerVillagerOffers(VillagerProfession.LIBRARIAN, level, factories ->
                    factories.add((entity, random) -> {
                        ItemStack book = randomSkillBook(random, entity instanceof ServerPlayerEntity player ? player : null);
                        if (book.isEmpty()) {
                            return null;
                        }
                        int cost = level == 2 ? 14 : level == 4 ? 22 : 32;
                        return new TradeOffer(
                                new TradedItem(Items.EMERALD, cost),
                                book,
                                3,
                                level * 5,
                                0.08F
                        );
                    })
            );
        }
    }

    private static boolean isSkillBookChest(String path) {
        return path.equals("chests/ancient_city")
                || path.equals("chests/ancient_city_ice_box")
                || path.equals("chests/buried_treasure")
                || path.equals("chests/desert_pyramid")
                || path.equals("chests/end_city_treasure")
                || path.equals("chests/jungle_temple")
                || path.equals("chests/nether_bridge")
                || path.equals("chests/pillager_outpost")
                || path.equals("chests/ruined_portal")
                || path.equals("chests/shipwreck_map")
                || path.equals("chests/shipwreck_supply")
                || path.equals("chests/shipwreck_treasure")
                || path.equals("chests/simple_dungeon")
                || path.equals("chests/stronghold_corridor")
                || path.equals("chests/stronghold_crossing")
                || path.equals("chests/stronghold_library")
                || path.equals("chests/underwater_ruin_big")
                || path.equals("chests/underwater_ruin_small")
                || path.equals("chests/woodland_mansion")
                || path.equals("chests/trial_chambers/reward")
                || path.equals("chests/trial_chambers/reward_common")
                || path.equals("chests/trial_chambers/reward_rare")
                || path.equals("chests/trial_chambers/reward_unique")
                || path.equals("chests/trial_chambers/reward_ominous")
                || path.equals("chests/trial_chambers/reward_ominous_common")
                || path.equals("chests/trial_chambers/reward_ominous_rare")
                || path.equals("chests/trial_chambers/reward_ominous_unique")
                || path.startsWith("chests/bastion_")
                || path.contains("ruin")
                || path.contains("ruins");
    }

    private static float lootChance(String path) {
        if (path.equals("chests/ancient_city")
                || path.equals("chests/end_city_treasure")
                || path.equals("chests/stronghold_library")
                || path.equals("chests/woodland_mansion")
                || path.contains("reward_ominous")
                || path.contains("reward_unique")) {
            return 0.85F;
        }
        if (path.equals("chests/desert_pyramid")
                || path.equals("chests/jungle_temple")
                || path.equals("chests/shipwreck_treasure")
                || path.equals("chests/buried_treasure")
                || path.startsWith("chests/bastion_")
                || path.contains("reward_rare")) {
            return 0.70F;
        }
        return 0.45F;
    }

    public static ItemStack randomSkillBook(Random random) {
        return randomSkillBook(random, null);
    }

    public static ItemStack randomSkillBook(Random random, ServerPlayerEntity player) {
        if (player != null) {
            List<ItemStack> unknown = unknownSkillBooks(player);
            return unknown.isEmpty()
                    ? ItemStack.EMPTY
                    : unknown.get(random.nextInt(unknown.size())).copy();
        }
        List<ItemStack> books = allSkillBooks();
        if (books.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return books.get(random.nextInt(books.size())).copy();
    }

    private static ItemStack randomUnknownSkillBook(Random random, ServerPlayerEntity player) {
        List<ItemStack> unknown = unknownSkillBooks(player);
        if (unknown.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return unknown.get(random.nextInt(unknown.size())).copy();
    }

    private static ServerPlayerEntity contextPlayer(net.minecraft.loot.context.LootContext context) {
        PlayerEntity lastDamagePlayer = context.hasParameter(LootContextParameters.LAST_DAMAGE_PLAYER)
                ? context.get(LootContextParameters.LAST_DAMAGE_PLAYER)
                : null;
        if (lastDamagePlayer instanceof ServerPlayerEntity serverPlayer) {
            return serverPlayer;
        }

        Entity entity = context.hasParameter(LootContextParameters.THIS_ENTITY)
                ? context.get(LootContextParameters.THIS_ENTITY)
                : null;
        if (entity instanceof ServerPlayerEntity serverPlayer) {
            return serverPlayer;
        }

        Vec3d origin = context.hasParameter(LootContextParameters.ORIGIN)
                ? context.get(LootContextParameters.ORIGIN)
                : null;
        if (origin == null) {
            return null;
        }
        PlayerEntity nearbyPlayer = context.getWorld().getClosestPlayer(
                origin.x, origin.y, origin.z, 8.0, false);
        return nearbyPlayer instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
    }

    private static List<ItemStack> unknownSkillBooks(ServerPlayerEntity player) {
        List<ItemStack> books = new ArrayList<>();
        for (ComboMoveConfig combo : ComboMoveConfigs.all()) {
            if (!PlayerComboProgress.isUnlocked(player, combo)) {
                books.add(namedBook(SkillBookItem.createStack(combo.id(), combo.id()), combo.displayName()));
            }
        }
        for (PassiveSkillConfig passive : PassiveSkillConfigs.all()) {
            if (passive.source() == PassiveSkillConfig.Source.BOOK
                    && !PlayerPassiveSkillProgress.isUnlocked(player, passive.id())) {
                books.add(namedBook(SkillBookItem.createPassiveStack(passive.id(), passive.id()), passive.name()));
            }
        }
        return books;
    }

    public static List<ItemStack> allSkillBooks() {
        List<ItemStack> books = new ArrayList<>();
        for (ComboMoveConfig combo : ComboMoveConfigs.all()) {
            books.add(namedBook(SkillBookItem.createStack(combo.id(), combo.id()), combo.displayName()));
        }
        for (PassiveSkillConfig passive : PassiveSkillConfigs.all()) {
            if (passive.source() == PassiveSkillConfig.Source.BOOK) {
                books.add(namedBook(SkillBookItem.createPassiveStack(passive.id(), passive.id()), passive.name()));
            }
        }
        return books;
    }

    private static ItemStack namedBook(ItemStack stack, String name) {
        if (name == null || name.isBlank()) {
            name = "技能书";
        }
        stack.set(DataComponentTypes.ITEM_NAME, Text.literal(name).formatted(net.minecraft.util.Formatting.WHITE));
        return stack;
    }
}
