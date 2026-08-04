package com.kingdomcomecombat.recipe;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.component.ComponentType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class CombatManualRecipe extends SpecialCraftingRecipe {
    private static final Identifier MODONOMICON_RED_ID = Identifier.of("modonomicon", "modonomicon_red");
    private static final Identifier MODONOMICON_BOOK_ID_COMPONENT = Identifier.of("modonomicon", "book_id");
    private static final Identifier COMBAT_MANUAL_ID = Identifier.of(KingdomComeCombat.MOD_ID, "combat_manual");

    public CombatManualRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        boolean foundBook = false;
        boolean foundSword = false;

        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.isOf(Items.BOOK) && !foundBook) {
                foundBook = true;
                continue;
            }
            if (stack.isOf(Items.IRON_SWORD) && !foundSword) {
                foundSword = true;
                continue;
            }
            return false;
        }

        return foundBook && foundSword;
    }

    @Override
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup registries) {
        return createManualStack();
    }

    @Override
    public RecipeSerializer<? extends SpecialCraftingRecipe> getSerializer() {
        return ModRecipes.COMBAT_MANUAL;
    }

    private static ItemStack createManualStack() {
        Item item = Registries.ITEM.get(MODONOMICON_RED_ID);
        ItemStack stack = new ItemStack(item);
        ComponentType<?> componentType = Registries.DATA_COMPONENT_TYPE.get(MODONOMICON_BOOK_ID_COMPONENT);
        setBookIdComponent(stack, componentType);
        return stack;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setBookIdComponent(ItemStack stack, ComponentType<?> componentType) {
        stack.set((ComponentType) componentType, COMBAT_MANUAL_ID);
    }
}
