package com.journey.jsdrinks.worldgen.feature;

import com.journey.jsdrinks.block.TeaBushBlock;
import com.journey.jsdrinks.block.entity.TeaBushBlockEntity;
import com.journey.jsdrinks.registry.JSDBlocks;
import com.mojang.serialization.Codec;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.plant.ITallPlant;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.EnvironmentHelpers;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class TeaBushFeature extends Feature<NoneFeatureConfiguration> {

    public TeaBushFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        BlockPos abovePos = pos.above();

        if (!Helpers.isBlock(level.getBlockState(pos.below()), TFCTags.Blocks.BUSH_PLANTABLE_ON)) {
            return false;
        }

        if (!EnvironmentHelpers.isWorldgenReplaceable(level, pos) || !EnvironmentHelpers.isWorldgenReplaceable(level, abovePos)) {
            return false;
        }

        TeaBushBlock block = JSDBlocks.TEA_BUSH.get();
        Lifecycle lifecycle = block.getLifecycleForCurrentMonth(level.getLevel(), pos);
        if (lifecycle == Lifecycle.DORMANT) {
            lifecycle = Lifecycle.HEALTHY;
        }

        BlockState lowerState = block.defaultBlockState()
                .setValue(TeaBushBlock.PART, ITallPlant.Part.LOWER)
                .setValue(TeaBushBlock.LIFECYCLE, lifecycle);

        BlockState upperState = block.defaultBlockState()
                .setValue(TeaBushBlock.PART, ITallPlant.Part.UPPER)
                .setValue(TeaBushBlock.LIFECYCLE, lifecycle);

        setBlock(level, pos, lowerState);
        setBlock(level, abovePos, upperState);

        if (level.getBlockEntity(pos) instanceof TeaBushBlockEntity be) {
            be.resetSeasonsWithoutHarvest();
        }

        return true;
    }
}
