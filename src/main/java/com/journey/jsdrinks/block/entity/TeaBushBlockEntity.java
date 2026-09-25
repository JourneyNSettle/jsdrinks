package com.journey.jsdrinks.block.entity;

import com.journey.jsdrinks.JSDConfig;
import com.journey.jsdrinks.block.TeaBushBlock;
import com.journey.jsdrinks.registry.JSDBlockEntities;
import com.journey.jsdrinks.registry.JSDBlocks;
import com.journey.jsdrinks.registry.JSDClimateRanges;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.blocks.plant.fruit.SeasonalPlantBlock;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateRange;

/**
 * Block entity for the 2-block tall tea bush (carried on both halves for HoeOverlay info).
 * Tracks harvest history and handles the irreversible bush → tree transformation
 * after {@link JSDConfig#TEA_BUSH_TRANSFORM_YEARS} calendar years without harvest.
 */
public class TeaBushBlockEntity extends BerryBushBlockEntity {

    private int seasonsWithoutHarvest = 0;
    private int lastRecordedYear = -1;

    public TeaBushBlockEntity(BlockPos pos, BlockState state) {
        super(JSDBlockEntities.TEA_BUSH.get(), pos, state);
    }

    public int getSeasonsWithoutHarvest() {
        return seasonsWithoutHarvest;
    }

    /** Called on successful harvest — resets the running counter so the bush stays a bush. */
    public void resetSeasonsWithoutHarvest() {
        this.seasonsWithoutHarvest = 0;
        setChanged();
    }

    /**
     * Periodic server ticker for the LOWER half.
     * Evaluates calendar years every 100 ticks (5 seconds) so commands like /time add
     * trigger transformation without waiting for random ticks.
     */
    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % 100 == 0) {
            Lifecycle lifecycle = state.hasProperty(TeaBushBlock.LIFECYCLE) ? state.getValue(TeaBushBlock.LIFECYCLE) : Lifecycle.HEALTHY;
            final ClimateRange range = JSDClimateRanges.TEA_BUSH.get();
            final int hydration = TeaBushBlock.getHydration(level, pos.below());
            final float temp = Climate.getAverageTemperature(level, pos);
            boolean climateValid = range != null && range.checkBoth(hydration, temp, false);
            checkYearlyGrowth(level, pos, lifecycle, climateValid);
        }
    }

    /**
     * Checks calendar year advancement and handles bush → tree transformation.
     */
    public void checkYearlyGrowth(Level level, BlockPos pos, Lifecycle currentLifecycle, boolean climateValid) {
        if (level.isClientSide()) return;

        long currentYear = Calendars.get(level).getCalendarYear();

        // First ever tick — record the year and return.
        if (lastRecordedYear == -1) {
            lastRecordedYear = (int) currentYear;
            setChanged();
            return;
        }

        if (currentYear > lastRecordedYear) {
            int yearsPassed = (int) (currentYear - lastRecordedYear);
            lastRecordedYear = (int) currentYear;

            // TZ §3.1 — Dormant or bad climate → year does not count towards growth.
            if (currentLifecycle != Lifecycle.DORMANT && climateValid) {
                seasonsWithoutHarvest += yearsPassed;
            }
            setChanged();

            int threshold = JSDConfig.TEA_BUSH_TRANSFORM_YEARS.get();
            if (seasonsWithoutHarvest >= threshold) {
                transformToTree(level, pos);
            }
        }
    }

    /**
     * Irreversibly replaces this 2-block bush with a tea tree trunk (growing branch)
     * and a single ring of leaves above it.
     */
    private void transformToTree(Level level, BlockPos pos) {
        BlockPos upperPos = pos.above();
        BlockState upperState = level.getBlockState(upperPos);

        // 1. Remove the UPPER part and its block entity explicitly BEFORE touching LOWER.
        if (upperState.getBlock() instanceof TeaBushBlock) {
            level.removeBlockEntity(upperPos);
            level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }

        level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);

        // 2. Replace LOWER with a growing branch (trunk base).
        level.setBlockAndUpdate(pos, JSDBlocks.TEA_GROWING_BRANCH.get().defaultBlockState());

        // 3. Initialise the new growing branch's block entity.
        if (level.getBlockEntity(pos) instanceof JSDTickingPlantBlockEntity branch) {
            branch.resetCounter();
            branch.setStemPos(pos);
        }

        // 4. Place initial leaves above the new trunk.
        if (level.getBlockState(upperPos).isAir()) {
            level.setBlockAndUpdate(upperPos, JSDBlocks.TEA_LEAVES.get().defaultBlockState());
            if (level.getBlockEntity(upperPos) instanceof BerryBushBlockEntity leaf) {
                leaf.setStemPos(pos);
            }
        }
    }

    // ---------------------------------------------------------------------- NBT

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
