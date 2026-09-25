package com.journey.jsdrinks.block.entity;

import com.journey.jsdrinks.JSDConfig;
import com.journey.jsdrinks.block.TeaBushBlock;
import com.journey.jsdrinks.registry.JSDBlockEntities;
import com.journey.jsdrinks.registry.JSDBlocks;
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
import net.dries007.tfc.util.calendar.Calendars;

/**
 * Block entity for the LOWER half of a 2-block tall tea bush.
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
     * Called every {@code randomTick} for the LOWER part of the bush.
     * <p>
     * Increments a per-calendar-year counter <b>only</b> when the bush is in an active
     * lifecycle AND the climate is valid.  When the counter reaches the configurable
     * threshold the bush is irreversibly replaced with a tea tree trunk + initial leaves.
     *
     * @param level            server level
     * @param pos              position of the LOWER bush block
     * @param currentLifecycle current {@link Lifecycle} of the bush blockstate
     * @param climateValid     {@code true} when temperature + hydration are inside the
     *                         bush's {@link net.dries007.tfc.util.climate.ClimateRange}
     */
    public void checkYearlyGrowth(Level level, BlockPos pos, Lifecycle currentLifecycle, boolean climateValid) {
        if (level.isClientSide()) return;

        // TZ §3.1 — Dormant / no fruit season / bad climate → year does not count.
        // Keep lastRecordedYear in sync so the very next valid year doesn't double-fire.
        if (currentLifecycle == Lifecycle.DORMANT || !climateValid) {
            lastRecordedYear = (int) Calendars.get(level).getCalendarYear();
            return;
        }

        long currentYear = Calendars.get(level).getCalendarYear();

        // First ever tick — just record the year, don't increment.
        if (lastRecordedYear == -1) {
            lastRecordedYear = (int) currentYear;
            return;
        }

        if (currentYear > lastRecordedYear) {
            lastRecordedYear = (int) currentYear;
            seasonsWithoutHarvest++;
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

        // 1. Remove the UPPER part explicitly BEFORE touching LOWER.
        //    This avoids a neighbor-update race: changing LOWER would trigger
        //    updateShape on UPPER, which sees LOWER is no longer a TeaBushBlock
        //    and returns AIR — but the timing is implementation-dependent.
        if (upperState.getBlock() instanceof TeaBushBlock) {
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
