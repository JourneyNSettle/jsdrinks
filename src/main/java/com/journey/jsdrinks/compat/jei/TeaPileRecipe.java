package com.journey.jsdrinks.compat.jei;

import java.util.List;
import com.journey.jsdrinks.registry.JSDDataComponents;
import com.journey.jsdrinks.registry.JSDItems;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record TeaPileRecipe(
    ItemStack input,
    boolean requiresLargeLeaf,
    List<ItemStack> surfaceBlocks,
    Component surfaceDescription,
    Component durationText,
    List<ChanceOutput> outputs
) {
    public record ChanceOutput(ItemStack stack, float chance, boolean isSpoilage) {}

    public static List<TeaPileRecipe> getRecipes() {
        // 1. Fermentation (1 day, clear weather, any surface)
        ItemStack bruised = new ItemStack(JSDItems.BRUISED_TEA_LEAF.get());
        FoodCapability.setNonDecaying(bruised);

        ItemStack fermented = new ItemStack(JSDItems.FERMENTED_TEA_LEAF.get());
        FoodCapability.setNonDecaying(fermented);

        List<ItemStack> anySurface = List.of(
            new ItemStack(Items.DIRT),
            new ItemStack(Items.GRASS_BLOCK),
            new ItemStack(Items.OAK_PLANKS),
            new ItemStack(Items.STONE)
        );

        TeaPileRecipe fermentation = new TeaPileRecipe(
            bruised,
            false,
            anySurface,
            Component.translatable("jsdrinks.jei.tea_pile.surface.any"),
            Component.translatable("jsdrinks.jei.tea_pile.duration.1_day"),
            List.of(new ChanceOutput(fermented, 1.0f, false))
        );

        // 2. Pu-erh on Solid Surface (2 months, clear weather, large leaf)
        ItemStack largeFermented = new ItemStack(JSDItems.FERMENTED_TEA_LEAF.get());
        largeFermented.set(JSDDataComponents.LARGE_LEAF.get(), true);
        FoodCapability.setNonDecaying(largeFermented);

        ItemStack puerh = new ItemStack(JSDItems.PUERH_TEA.get());
        FoodCapability.setNonDecaying(puerh);

        ItemStack rotten = new ItemStack(JSDItems.FERMENTED_TEA_LEAF.get());
        FoodCapability.setRotten(rotten);
        FoodCapability.setNonDecaying(rotten);

        List<ItemStack> solidSurface = List.of(
            new ItemStack(Items.OAK_PLANKS),
            new ItemStack(Items.STONE),
            new ItemStack(Items.COBBLESTONE),
            new ItemStack(Items.BRICKS)
        );

        TeaPileRecipe puerhSolid = new TeaPileRecipe(
            largeFermented,
            true,
            solidSurface,
            Component.translatable("jsdrinks.jei.tea_pile.surface.solid"),
            Component.translatable("jsdrinks.jei.tea_pile.duration.2_months"),
            List.of(
                new ChanceOutput(puerh, 0.85f, false),
                new ChanceOutput(rotten, 0.15f, true)
            )
        );

        // 3. Pu-erh on Soil (2 months, clear weather, large leaf)
        ItemStack strongPuerh = new ItemStack(JSDItems.STRONG_PUERH_TEA.get());
        FoodCapability.setNonDecaying(strongPuerh);

        List<ItemStack> soilSurface = List.of(
            new ItemStack(Items.DIRT),
            new ItemStack(Items.GRASS_BLOCK),
            new ItemStack(Items.PODZOL),
            new ItemStack(Items.COARSE_DIRT)
        );

        TeaPileRecipe puerhSoil = new TeaPileRecipe(
            largeFermented.copy(),
            true,
            soilSurface,
            Component.translatable("jsdrinks.jei.tea_pile.surface.soil"),
            Component.translatable("jsdrinks.jei.tea_pile.duration.2_months"),
            List.of(
                new ChanceOutput(strongPuerh, 0.50f, false),
                new ChanceOutput(puerh.copy(), 0.25f, false),
                new ChanceOutput(rotten.copy(), 0.25f, true)
            )
        );

        return List.of(fermentation, puerhSolid, puerhSoil);
    }
}
