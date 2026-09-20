package com.journey.jsdrinks.block.entity;

import com.journey.jsdrinks.registry.JSDBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blockentities.TickCounterBlockEntity;

public class JSDBerryBushBlockEntity extends BerryBushBlockEntity {
    public static void reset(Level level, BlockPos pos) {
        level.getBlockEntity(pos, JSDBlockEntities.BERRY_BUSH.get()).ifPresent(TickCounterBlockEntity::resetCounter);
    }

    public static void resetPickedTick(Level level, BlockPos pos) {
        level.getBlockEntity(pos, JSDBlockEntities.BERRY_BUSH.get()).ifPresent(BerryBushBlockEntity::resetLastPickedCounter);
    }

    public JSDBerryBushBlockEntity(BlockPos pos, BlockState state) {
        super(JSDBlockEntities.BERRY_BUSH.get(), pos, state);
    }

    @Override
    public BlockEntityType<?> getType() {
        return JSDBlockEntities.BERRY_BUSH.get();
    }
}
