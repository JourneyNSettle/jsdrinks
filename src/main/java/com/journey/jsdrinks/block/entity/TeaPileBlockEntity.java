package com.journey.jsdrinks.block.entity;

import com.journey.jsdrinks.block.TeaPileBlock;
import com.journey.jsdrinks.registry.JSDBlockEntities;
import com.journey.jsdrinks.registry.JSDDataComponents;
import com.journey.jsdrinks.registry.JSDItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.TFCBlockEntity;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;

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

        // Stage 0 -> Stage 1: 1 TFC Day (Bruised -> Fermented)
        if (currentStage == 0) {
            if (pile.agingTicks >= ICalendar.CALENDAR_TICKS_IN_DAY) {
                state = state.setValue(TeaPileBlock.STAGE, 1);
                level.setBlockAndUpdate(pos, state);

                ItemStack fermented = new ItemStack(JSDItems.FERMENTED_TEA_LEAF.get());
                // Preserve large leaf tag if it had one
                if (Boolean.TRUE.equals(pile.storedItem.get(JSDDataComponents.LARGE_LEAF.get()))) {
                    fermented.set(JSDDataComponents.LARGE_LEAF.get(), true);
                }
                pile.storedItem = fermented;
                pile.agingTicks = 0;
                pile.setChanged();
            }
        }
        // Stage 1 -> Stage 2 (Pu-erh) or Stage 3 (Rotten): 2 TFC Months
        else if (currentStage == 1) {
            long twoMonthsTicks = 2L * Calendars.get(level).getCalendarTicksInMonth();
            if (pile.agingTicks >= twoMonthsTicks) {
                boolean hasLargeLeaf = Boolean.TRUE.equals(pile.storedItem.get(JSDDataComponents.LARGE_LEAF.get()));

                if (!hasLargeLeaf) {
                    // Without large leaf: rots (TZ 6.5)
                    state = state.setValue(TeaPileBlock.STAGE, 3);
                    ItemStack rotten = new ItemStack(JSDItems.FERMENTED_TEA_LEAF.get());
                    FoodCapability.setRotten(rotten);
                    pile.storedItem = rotten;
                } else {
                    BlockState belowState = level.getBlockState(pos.below());
                    boolean onSoil = belowState.is(BlockTags.DIRT) || belowState.is(TFCTags.Blocks.DIRT) || belowState.is(TFCTags.Blocks.GRASS);

                    if (onSoil) {
                        // On soil: higher chance of strong pu-erh AND rot (TZ 6.5)
                        float roll = level.random.nextFloat();
                        if (roll < 0.50f) {
                            ItemStack puerh = new ItemStack(JSDItems.STRONG_PUERH_TEA.get());
                            FoodCapability.setCreatedNow(puerh);
                            pile.storedItem = puerh;
                            state = state.setValue(TeaPileBlock.STAGE, 2);
                        } else if (roll < 0.75f) {
                            ItemStack puerh = new ItemStack(JSDItems.PUERH_TEA.get());
                            FoodCapability.setCreatedNow(puerh);
                            pile.storedItem = puerh;
                            state = state.setValue(TeaPileBlock.STAGE, 2);
                        } else {
                            state = state.setValue(TeaPileBlock.STAGE, 3); // Spoilage
                            ItemStack rotten = new ItemStack(JSDItems.FERMENTED_TEA_LEAF.get());
                            FoodCapability.setRotten(rotten);
                            pile.storedItem = rotten;
                        }
                    } else {
                        // Not on soil: pu-erh with lower spoilage chance, no strong pu-erh
                        float roll = level.random.nextFloat();
                        if (roll < 0.85f) {
                            ItemStack puerh = new ItemStack(JSDItems.PUERH_TEA.get());
                            FoodCapability.setCreatedNow(puerh);
                            pile.storedItem = puerh;
                            state = state.setValue(TeaPileBlock.STAGE, 2);
                        } else {
                            state = state.setValue(TeaPileBlock.STAGE, 3); // Spoilage
                            ItemStack rotten = new ItemStack(JSDItems.FERMENTED_TEA_LEAF.get());
                            FoodCapability.setRotten(rotten);
                            pile.storedItem = rotten;
                        }
                    }
                }

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
