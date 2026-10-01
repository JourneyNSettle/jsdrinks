package com.journey.jsdrinks.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.state.BlockState;

public record TeaPileInput(
    ItemStack stack,
    BlockState surfaceBlock
) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? stack : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }
}
