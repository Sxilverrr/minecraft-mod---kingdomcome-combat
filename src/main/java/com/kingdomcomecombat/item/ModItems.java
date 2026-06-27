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
import net.minecraft.text.Text;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import com.kingdomcomecombat.passive.PassiveSkillConfigs;

public class ModItems {
    public static final Item WOODEN_LONGSWORD = registerLongsword(
            "wooden_longsword",
            6.0,
            -2.4,
            71
    );

    public static final Item STONE_LONGSWORD = registerLongsword(
            "stone_longsword",
            7.0,
            -2.4,
            157
    );

    public static final Item COPPER_LONGSWORD = registerLongsword(
            "copper_longsword",
            7.0,
            -2.4,
            320
    );

    public static final Item GOLDEN_LONGSWORD = registerLongsword(
            "golden_longsword",
            6.0,
            -2.4,
            38
    );

    public static final Item IRON_LONGSWORD = registerLongsword(
            "iron_longsword",
            8.0,
            -2.4,
            300
    );

    public static final Item DIAMOND_LONGSWORD = registerLongsword(
            "diamond_longsword",
            9.0,
            -2.4,
            1873
    );

    public static final Item NETHERITE_LONGSWORD = registerLongsword(
            "netherite_longsword",
            10.0,
            -2.4,
            2437
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

    public static final Item BANDAGE = registerSimple("bandage", new Item.Settings().maxDamage(20));
    public static final Item SKILL_BOOK = registerSkillBook();
    public static final Item BULLET_WITH_GUNPOWDER = registerSimple("bullet_with_gunpowder", new Item.Settings().maxCount(64));
    public static final Item HAND_CANNON = registerHandCannon();

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

    private static Item registerHandCannon() {
        Identifier id = Identifier.of(KingdomComeCombat.MOD_ID, "hand_cannon");
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

        return Registry.register(
                Registries.ITEM,
                key,
                new HandCannonItem(new Item.Settings().registryKey(key).maxCount(1).maxDamage(64))
        );
    }

    private static Item registerLongsword(
            String name,
            double attackDamage,
            double attackSpeed,
            int durability
    ) {
        Identifier id = Identifier.of(KingdomComeCombat.MOD_ID, name);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);

        Item item = new Item(
                new Item.Settings()
                        .registryKey(key)
                        .maxDamage(durability)
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
                        .registryKey(key)
                        .maxDamage(durability)
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
        );

        return Registry.register(
                Registries.ITEM,
                key,
                item
        );
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
            entries.add(BANDAGE);
            entries.add(BULLET_WITH_GUNPOWDER);
            entries.add(HAND_CANNON);
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
        stack.set(DataComponentTypes.ITEM_NAME, Text.literal(entry.title()));
        return stack;
    }
}
