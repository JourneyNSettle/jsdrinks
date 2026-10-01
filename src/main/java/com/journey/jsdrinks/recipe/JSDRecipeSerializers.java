package com.journey.jsdrinks.recipe;

import com.mojang.serialization.MapCodec;
import net.dries007.tfc.common.recipes.RecipeSerializerImpl;
import net.dries007.tfc.common.recipes.TFCRecipeSerializers.Id;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.journey.jsdrinks.JourneyDrinks.MOD_ID;

public class JSDRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID);

    public static final Id<TeaPileRecipe> TEA_PILE = register("tea_pile", TeaPileRecipe.CODEC, TeaPileRecipe.STREAM_CODEC);

    private static <R extends Recipe<?>> Id<R> register(String name, MapCodec<R> codec, StreamCodec<RegistryFriendlyByteBuf, R> stream) {
        return register(name, new RecipeSerializerImpl<>(codec, stream));
    }

    private static <R extends Recipe<?>> Id<R> register(String name, RecipeSerializer<R> serializer) {
        return new Id<>(RECIPE_SERIALIZERS.register(name, () -> serializer));
    }
}
