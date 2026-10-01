package com.journey.jsdrinks.block.entity;

import com.journey.jsdrinks.block.TeaPileBlock;
import com.journey.jsdrinks.recipe.JSDRecipeTypes;
import com.journey.jsdrinks.recipe.TeaPileInput;
import com.journey.jsdrinks.recipe.TeaPileRecipe;
import com.journey.jsdrinks.registry.JSDBlockEntities;
import com.journey.jsdrinks.registry.JSDItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.TFCBlockEntity;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.util.calendar.Calendars;

public class TeaPileBlockEntity extends TFCBlockEntity {

    private ItemStack storedItem = ItemStack.EMPTY;
    private long agingTicks = 0;
    private long lastTick = -1;

    public TeaPileBlockEntity(BlockPos pos, BlockState state) {
        super(JSDBlockEntities.TEA_PILE.get(), pos, state);
    }

    public void setStoredItem(ItemStack item) {
        this.storedItem = item;
        setChanged();
    }

    public ItemStack getStoredItem() {
        return storedItem;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TeaPileBlockEntity pile) {
        int currentStage = state.getValue(TeaPileBlock.STAGE);
        if (currentStage >= 2) return; // mature pu-erh or rotten

        long now = Calendars.get(level).getCalendarTicks();
        if (pile.lastTick == -1) {
            pile.lastTick = now;
            return;
        }

        // Rain stops aging timer (TZ 6.5)
        if (level.isRainingAt(pos)) {
            pile.lastTick = now;
            return;
        }

        long delta = now - pile.lastTick;
        pile.lastTick = now;
        pile.agingTicks += delta;

        BlockState belowState = level.getBlockState(pos.below());
        TeaPileInput input = new TeaPileInput(pile.storedItem, belowState);
        RecipeHolder<TeaPileRecipe> recipeHolder = level.getRecipeManager()
            .getRecipeFor(JSDRecipeTypes.TEA_PILE.get(), input, level)
            .orElse(null);

        if (recipeHolder != null) {
            TeaPileRecipe recipe = recipeHolder.value();
            if (pile.agingTicks >= recipe.getDurationTicks(level)) {
                TeaPileRecipe.ChanceOutput outcome = recipe.rollOutcome(level.random);
                if (outcome != null) {
                    ItemStack result = outcome.result().getSingleStack(pile.storedItem);
                    if (outcome.rotten()) {
                        FoodCapability.setRotten(result);
                    }
                    pile.storedItem = result;
                    state = state.setValue(TeaPileBlock.STAGE, outcome.targetStage());
                    pile.agingTicks = 0;
                    level.setBlockAndUpdate(pos, state);
                    pile.setChanged();
                }
            }
        } else if (currentStage == 1) {
            // Fallback: If in stage 1 (fermented leaf) without large leaf (or invalid setup),
            // it rots after 2 months (TZ 6.5)
            long twoMonthsTicks = 2L * Calendars.get(level).getCalendarTicksInMonth();
            if (pile.agingTicks >= twoMonthsTicks) {
                state = state.setValue(TeaPileBlock.STAGE, 3);
                ItemStack rotten = new ItemStack(JSDItems.FERMENTED_TEA_LEAF.get());
                FoodCapability.setRotten(rotten);
                pile.storedItem = rotten;
                pile.agingTicks = 0;
                level.setBlockAndUpdate(pos, state);
                pile.setChanged();
            }
        }
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        agingTicks = nbt.getLong("agingTicks");
        lastTick = nbt.getLong("lastTick");
        if (nbt.contains("storedItem")) {
            storedItem = ItemStack.parseOptional(provider, nbt.getCompound("storedItem"));
        }
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.putLong("agingTicks", agingTicks);
        nbt.putLong("lastTick", lastTick);
        if (!storedItem.isEmpty()) {
            nbt.put("storedItem", storedItem.save(provider));
        }
    }
}
