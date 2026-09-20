package com.journey.jsdrinks.block.entity;

import com.journey.jsdrinks.registry.JSDBlockEntities;
import com.journey.jsdrinks.registry.JSDBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.calendar.Calendars;

public class TeaBushBlockEntity extends BerryBushBlockEntity {

    private int seasonsWithoutHarvest = 0;
    private int lastRecordedYear = -1;

    public TeaBushBlockEntity(BlockPos pos, BlockState state) {
        super(JSDBlockEntities.TEA_BUSH.get(), pos, state);
    }

    public int getSeasonsWithoutHarvest() {
        return seasonsWithoutHarvest;
    }

    public void resetSeasonsWithoutHarvest() {
        this.seasonsWithoutHarvest = 0;
        setChanged();
    }

    public void checkYearlyGrowth(Level level, BlockPos pos, Lifecycle currentLifecycle) {
        if (level.isClientSide()) return;

        long currentYear = Calendars.get(level).getCalendarYear();
        if (lastRecordedYear == -1) {
            lastRecordedYear = (int) currentYear;
            return;
        }

        if (currentYear > lastRecordedYear) {
            lastRecordedYear = (int) currentYear;
            seasonsWithoutHarvest++;
            setChanged();

            // Threshold: 2 years without harvest -> transform to Tea Tree
            if (seasonsWithoutHarvest >= 2) {
                transformToTree(level, pos);
            }
        }
    }

    private void transformToTree(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
        level.setBlockAndUpdate(pos, JSDBlocks.TEA_GROWING_BRANCH.get().defaultBlockState());
        BlockPos upper = pos.above();
        if (level.getBlockState(upper).isAir() || level.getBlockState(upper).is(JSDBlocks.TEA_BUSH.get())) {
            level.setBlockAndUpdate(upper, JSDBlocks.TEA_LEAVES.get().defaultBlockState());
        }
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        seasonsWithoutHarvest = nbt.getInt("seasonsWithoutHarvest");
        lastRecordedYear = nbt.getInt("lastRecordedYear");
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.putInt("seasonsWithoutHarvest", seasonsWithoutHarvest);
        nbt.putInt("lastRecordedYear", lastRecordedYear);
    }
}
