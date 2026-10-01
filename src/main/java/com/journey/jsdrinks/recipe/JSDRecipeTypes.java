package com.journey.jsdrinks.recipe;

import net.dries007.tfc.common.recipes.TFCRecipeTypes.Id;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.journey.jsdrinks.JourneyDrinks.MOD_ID;

public class JSDRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, MOD_ID);

    public static final Id<TeaPileRecipe> TEA_PILE = register("tea_pile");

    private static <R extends Recipe<?>> Id<R> register(String name) {
        return new Id<>(RECIPE_TYPES.register(name, () -> new RecipeType<>() {
            @Override
            public String toString() {
                return name;
            }
        }));
    }
}
