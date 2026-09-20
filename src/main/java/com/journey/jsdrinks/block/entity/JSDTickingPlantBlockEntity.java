package com.journey.jsdrinks.block.entity;

import com.journey.jsdrinks.registry.JSDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.TickCounterBlockEntity;
import net.dries007.tfc.common.blockentities.TickingPlantBlockEntity;

public class JSDTickingPlantBlockEntity extends TickingPlantBlockEntity {
    public static void reset(Level level, BlockPos pos) {
        level.getBlockEntity(pos, JSDBlockEntities.TICKING_PLANT.get()).ifPresent(TickCounterBlockEntity::resetCounter);
    }

    public static void addTicks(Level level, BlockPos pos, long ticks) {
        level.getBlockEntity(pos, JSDBlockEntities.TICKING_PLANT.get()).ifPresent(entity -> entity.increaseCounter(ticks));
    }

    public static void setStemPos(Level level, BlockPos pos, BlockPos stemPos) {
        level.getBlockEntity(pos, JSDBlockEntities.TICKING_PLANT.get()).ifPresent(entity -> entity.setStemPos(stemPos));
    }

    public JSDTickingPlantBlockEntity(BlockPos pos, BlockState state) {
        super(JSDBlockEntities.TICKING_PLANT.get(), pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return JSDBlockEntities.TICKING_PLANT.get();
    }
}
