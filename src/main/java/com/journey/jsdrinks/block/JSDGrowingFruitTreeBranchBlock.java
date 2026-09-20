package com.journey.jsdrinks.block;

import com.journey.jsdrinks.block.entity.JSDTickingPlantBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.plant.fruit.GrowingFruitTreeBranchBlock;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateRange;

import java.util.function.Supplier;

public class JSDGrowingFruitTreeBranchBlock extends GrowingFruitTreeBranchBlock {
    private final Supplier<ClimateRange> climateRange;

    public JSDGrowingFruitTreeBranchBlock(ExtendedProperties properties, Supplier<? extends Block> body, Supplier<? extends Block> leaves, Supplier<ClimateRange> climateRange) {
        super(properties, body, leaves, climateRange);
        this.climateRange = climateRange;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand) {
        int hydration = getFruitBranchHydration(level, pos);
        float temp = Climate.getAverageTemperature(level, pos);
        if (!this.climateRange.get().checkBoth(hydration, temp, false) && !state.getValue(NATURAL)) {
            JSDTickingPlantBlockEntity.reset(level, pos);
        }
        super.randomTick(state, level, pos, rand);
    }
}
