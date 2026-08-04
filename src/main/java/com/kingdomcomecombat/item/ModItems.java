package com.kingdomcomecombat.item;

import com.kingdomcomecombat.KingdomComeCombat;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Rarity;
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import com.kingdomcomecombat.passive.PassiveSkillConfigs;

public class ModItems {
    private static java.util.function.Supplier<String> clientLanguage = () -> "";
    public static final Item WOODEN_LONGSWORD = registerLongsword(
            "wooden_longsword",
            6.0,
            -2.4,
            710,
            15
    );

    public static final Item STONE_LONGSWORD = registerLongsword(
            "stone_longsword",
            7.0,
            -2.4,
            157,
            5
    );

    public static final Item COPPER_LONGSWORD = registerLongsword(
            "copper_longsword",
            7.0,
            -2.4,
            320,
            14
    );

    public static final Item GOLDEN_LONGSWORD = registerLongsword(
            "golden_longsword",
            6.0,
            -2.4,
            38,
            22
    );

    public static final Item IRON_LONGSWORD = registerLongsword(
            "iron_longsword",
            8.0,
            -2.4,
            300,
            14
    );

    public static final Item DIAMOND_LONGSWORD = registerLongsword(
            "diamond_longsword",
            9.0,
            -2.4,
            1873,
            10
    );

    public static final Item NETHERITE_LONGSWORD = registerLongsword(
            "netherite_longsword",
            10.0,
            -2.4,
            2437,
            15
    );

    public static final Item COPPER_FIGHTING_MACE = registerFightingMace(
            "copper_fighting_mace",
            7.0,
            320,
            14
    );

    public static final Item GOLDEN_FIGHTING_MACE = registerFightingMace(
            "golden_fighting_mace",
            6.0,
            64,
            22
    );

    public static final Item IRON_FIGHTING_MACE = registerFightingMace(
            "iron_fighting_mace",
            8.0,
            500,
            14
    );

    public static final Item DIAMOND_FIGHTING_MACE = registerFightingMace(
            "diamond_fighting_mace",
            9.0,
            3122,
            10
    );

    public static final Item NETHERITE_FIGHTING_MACE = registerFightingMace(
            "netherite_fighting_mace",
            10.0,
            4062,
            15
    );

    public static final Item COPPER_POLEAXE = registerPoleaxe("copper_poleaxe", 7.0, 143, 14);
    public static final Item GOLDEN_POLEAXE = registerPoleaxe("golden_poleaxe", 6.0, 24, 22);
    public static final Item IRON_POLEAXE = registerPoleaxe("iron_poleaxe", 8.0, 188, 14);
    public static final Item DIAMOND_POLEAXE = registerPoleaxe("diamond_poleaxe", 9.0, 1171, 10);
    public static final Item NETHERITE_POLEAXE = registerPoleaxe("netherite_poleaxe", 10.0, 1523, 15);

    public static final Item BANDAGE = registerBandage();
    public static final Item SKILL_BOOK = registerSkillBook();
    public static final Item BULLET_WITH_GUNPOWDER = registerSimple("bullet_with_gunpowder", new Item.Settings().maxCount(64));
    public static final Item HEAVY_BULLET_WITH_GUNPOWDER = registerSimple("heavy_bullet_with_gunpowder", new Item.Settings().maxCount(64));
    public static final Item BUCKSHOT_WITH_GUNPOWDER = registerSimple("buckshot_with_gunpowder", new Item.Settings().maxCount(64));
    public static final Item HAND_CANNON = registerHandCannon();
    public static final Item MEDIUM_SHIELD = registerMediumShield();

    private static Item registerSimple(String name, Item.Settings settings) {
        Identifier id = Identifier.of(KingdomComeCombat.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

        return Registry.register(
                Registries.ITEM,
                key,
                new Item(settings.registryKey(key))
        );
    }

    private static Item registerSkillBook() {
        Identifier id = Identifier.of(KingdomComeCombat.MOD_ID, "skill_book");
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

        return Registry.register(
                Registries.ITEM,
                key,
                new SkillBookItem(new Item.Settings().registryKey(key).maxCount(1))
        );
    }

    private static Item registerBandage() {
        Identifier id = Identifier.of(KingdomComeCombat.MOD_ID, "bandage");
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        return Registry.register(
                Registries.ITEM,
                key,
                new BandageItem(new Item.Settings().registryKey(key).maxDamage(20))
        );
    }

    private static Item registerHandCannon() {
        Identifier id = Identifier.of(KingdomComeCombat.MOD_ID, "hand_cannon");
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

        return Registry.register(
                Registries.ITEM,
                key,
                new HandCannonItem(new Item.Settings().registryKey(key).maxCount(1).maxDamage(64).enchantable(12))
        );
    }

    private static Item registerMediumShield() {
        Identifier id = Identifier.of(KingdomComeCombat.MOD_ID, "medium_shield");
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        return Registry.register(Registries.ITEM, key, new ShieldItem(
                new Item.Settings().registryKey(key).maxDamage(130).maxCount(1)
        ));
    }

    private static Item registerLongsword(
            String name,
            double attackDamage,
            double attackSpeed,
            int durability,
            int enchantability
    ) {
        Identifier id = Identifier.of(KingdomComeCombat.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

        Item item = new Item(
                new Item.Settings()
                        .maxDamage(durability)
                        .rarity(Rarity.COMMON)
                        .enchantable(enchantability)
                        .attributeModifiers(
                                AttributeModifiersComponent.builder()
                                        .add(
                                                EntityAttributes.ATTACK_DAMAGE,
                                                new EntityAttributeModifier(
                                                        Identifier.of(KingdomComeCombat.MOD_ID, name + "_attack_damage"),
                                                        attackDamage,
                                                        EntityAttributeModifier.Operation.ADD_VALUE
                                                ),
                                                AttributeModifierSlot.MAINHAND
                                        )
                                        .add(
                                                EntityAttributes.ATTACK_SPEED,
                                                new EntityAttributeModifier(
                                                        Identifier.of(KingdomComeCombat.MOD_ID, name + "_attack_speed"),
                                                        attackSpeed,
                                                        EntityAttributeModifier.Operation.ADD_VALUE
                                                ),
                                                AttributeModifierSlot.MAINHAND
                                        )
                                        .build()
                        )
                        .registryKey(key)
        );

        return Registry.register(
                Registries.ITEM,
                key,
                item
        );
    }

    private static Item registerFightingMace(
            String name,
            double attackDamage,
            int durability,
            int enchantability
    ) {
        Identifier id = Identifier.of(KingdomComeCombat.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

        Item item = new Item(
                new Item.Settings()
                        .maxDamage(durability)
                        .rarity(Rarity.COMMON)
                        .enchantable(enchantability)
                        .attributeModifiers(
                                AttributeModifiersComponent.builder()
                                        .add(
                                                EntityAttributes.ATTACK_DAMAGE,
                                                new EntityAttributeModifier(
                                                        Identifier.of(KingdomComeCombat.MOD_ID, name + "_attack_damage"),
                                                        attackDamage,
                                                        EntityAttributeModifier.Operation.ADD_VALUE
                                                ),
                                                AttributeModifierSlot.MAINHAND
                                        )
                                        .add(
                                                EntityAttributes.ATTACK_SPEED,
                                                new EntityAttributeModifier(
                                                        Identifier.of(KingdomComeCombat.MOD_ID, name + "_attack_speed"),
                                                        -2.8,
                                                        EntityAttributeModifier.Operation.ADD_VALUE
                                                ),
                                                AttributeModifierSlot.MAINHAND
                                        )
                                        .build()
                        )
                        .registryKey(key)
        );

        return Registry.register(
                Registries.ITEM,
                key,
                item
        );
    }

    private static Item registerPoleaxe(String name, double attackDamage, int durability, int enchantability) {
        return registerLongsword(name, attackDamage, -3.0, durability, enchantability);
    }

    public static void registerModItems() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> {
            entries.add(WOODEN_LONGSWORD);
            entries.add(STONE_LONGSWORD);
            entries.add(COPPER_LONGSWORD);
            entries.add(GOLDEN_LONGSWORD);
            entries.add(IRON_LONGSWORD);
            entries.add(DIAMOND_LONGSWORD);
            entries.add(NETHERITE_LONGSWORD);
            entries.add(COPPER_FIGHTING_MACE);
            entries.add(GOLDEN_FIGHTING_MACE);
            entries.add(IRON_FIGHTING_MACE);
            entries.add(DIAMOND_FIGHTING_MACE);
            entries.add(NETHERITE_FIGHTING_MACE);
            entries.add(COPPER_POLEAXE);
            entries.add(GOLDEN_POLEAXE);
            entries.add(IRON_POLEAXE);
            entries.add(DIAMOND_POLEAXE);
            entries.add(NETHERITE_POLEAXE);
            entries.add(BANDAGE);
            entries.add(BULLET_WITH_GUNPOWDER);
            entries.add(HEAVY_BULLET_WITH_GUNPOWDER);
            entries.add(BUCKSHOT_WITH_GUNPOWDER);
            entries.add(HAND_CANNON);
            entries.add(MEDIUM_SHIELD);
            entries.add(com.kingdomcomecombat.potion.PotionCoatingHandler.createDragonBreathArrowStack(1));
            if (SkillBookTexts.all().isEmpty()) {
                entries.add(SKILL_BOOK);
            } else {
                SkillBookTexts.all().entrySet().stream()
                        .sorted(java.util.Map.Entry.comparingByKey())
                        .forEach(entry -> entries.add(skillBookStack(entry.getKey(), entry.getValue())));
            }
        });
    }

    private static ItemStack skillBookStack(String bookId, SkillBookTexts.Entry entry) {
        ItemStack stack = PassiveSkillConfigs.get(bookId) != null
                ? SkillBookItem.createPassiveStack(bookId, bookId)
                : SkillBookItem.createStack(bookId, bookId);
        String language = clientLanguage.get();
        stack.set(DataComponentTypes.ITEM_NAME, Text.literal(entry.title(language)).formatted(net.minecraft.util.Formatting.WHITE));
        return stack;
    }

    public static void setClientLanguageSupplier(java.util.function.Supplier<String> supplier) {
        clientLanguage = supplier == null ? () -> "" : supplier;
    }
}
