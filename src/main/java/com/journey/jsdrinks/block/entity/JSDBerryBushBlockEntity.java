package com.journey.jsdrinks.block.entity;

import com.journey.jsdrinks.registry.JSDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blockentities.TickCounterBlockEntity;
import net.dries007.tfc.util.calendar.Calendars;

/**
 * Berry bush block entity for jsdrinks leaf blocks (tea leaves, coffee leaves).
 * <p>
 * Extends the TFC {@link BerryBushBlockEntity} with an additional {@code placedTick}
 * field used by coffee leaves to implement the 2-year age gate on fruiting.
 * Unlike {@code getTicksSinceUpdate()} (which resets on various growth events),
 * {@code placedTick} is set exactly once — when the block entity is first created —
 * and never reset, providing a reliable absolute age measurement.
 */
public class JSDBerryBushBlockEntity extends BerryBushBlockEntity {

    public static void reset(Level level, BlockPos pos) {
        level.getBlockEntity(pos, JSDBlockEntities.BERRY_BUSH.get()).ifPresent(TickCounterBlockEntity::resetCounter);
    }

    public static void resetPickedTick(Level level, BlockPos pos) {
        level.getBlockEntity(pos, JSDBlockEntities.BERRY_BUSH.get()).ifPresent(BerryBushBlockEntity::resetLastPickedCounter);
    }

    /**
     * The calendar tick at which this block entity was first placed in the world.
     * Used by coffee leaves for the 2-year fruiting age gate.
     * Set once in the constructor and persisted via NBT; never reset.
     */
    private long placedTick;

    public JSDBerryBushBlockEntity(BlockPos pos, BlockState state) {
        super(JSDBlockEntities.BERRY_BUSH.get(), pos, state);
        this.placedTick = Calendars.SERVER.getTicks();
    }

    @Override
    public BlockEntityType<?> getType() {
        return JSDBlockEntities.BERRY_BUSH.get();
    }

    /**
     * Returns the calendar tick at which this block entity was originally placed.
     */
    public long getPlacedTick() {
        return placedTick;
    }

    /**
     * Explicitly sets the placed tick. Used by worldgen features to backdate
     * wild trees so they are immediately mature (≥ 2 years old).
     */
    public void setPlacedTick(long tick) {
        this.placedTick = tick;
        setChanged();
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        if (nbt.contains("placedTick")) {
            placedTick = nbt.getLong("placedTick");
        }
        // else: keep the constructor default (current time) for legacy/migrated blocks
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider) {
        super.saveAdditional(nbt, provider);
        nbt.putLong("placedTick", placedTick);
    }
}
