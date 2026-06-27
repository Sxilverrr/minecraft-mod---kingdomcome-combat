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
            if (!isRuinsChest(path) || player == null || context.getRandom().nextFloat() > lootChance(path)) {
                return;
            }

            ItemStack book = randomUnknownSkillBook(context.getRandom(), player);
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

    private static boolean isRuinsChest(String path) {
        return path.equals("chests/ancient_city")
                || path.equals("chests/ancient_city_ice_box")
                || path.equals("chests/desert_pyramid")
                || path.equals("chests/jungle_temple")
                || path.equals("chests/stronghold_library")
                || path.equals("chests/woodland_mansion")
                || path.equals("chests/trial_chambers/reward")
                || path.contains("ruin")
                || path.contains("ruins");
    }

    private static float lootChance(String path) {
        if (path.equals("chests/ancient_city") || path.equals("chests/desert_pyramid")) {
            return 0.70F;
        }
        if (path.equals("chests/ancient_city_ice_box")) {
            return 0.50F;
        }
        return 0.42F;
    }

    public static ItemStack randomSkillBook(Random random) {
        return randomSkillBook(random, null);
    }

    public static ItemStack randomSkillBook(Random random, ServerPlayerEntity player) {
        if (player != null) {
            List<ItemStack> unknown = unknownSkillBooks(player);
            if (!unknown.isEmpty()) {
                return unknown.get(random.nextInt(unknown.size())).copy();
            }
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
        return entity instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
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
        stack.set(DataComponentTypes.ITEM_NAME, Text.literal(name));
        return stack;
    }
}
