package com.kingdomcomecombat.recipe;

import com.kingdomcomecombat.KingdomComeCombat;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModRecipes {
    public static final RecipeSerializer<CombatManualRecipe> COMBAT_MANUAL =
            new SpecialCraftingRecipe.SpecialRecipeSerializer<>(CombatManualRecipe::new);

    private ModRecipes() {
    }

    public static void register() {
        Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(KingdomComeCombat.MOD_ID, "combat_manual"),
                COMBAT_MANUAL
        );
    }
}
