package com.journey.jsdrinks.worldgen.feature;

import com.journey.jsdrinks.block.JSDTeaLeavesBlock;
import com.journey.jsdrinks.registry.JSDBlocks;
import com.mojang.serialization.Codec;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeBranchBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.EnvironmentHelpers;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class LargeTeaTreeFeature extends Feature<NoneFeatureConfiguration> {

    public LargeTeaTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos pos = context.origin();
        RandomSource rand = context.random();

        if (!Helpers.isBlock(level.getBlockState(pos.below()), TFCTags.Blocks.BUSH_PLANTABLE_ON)) {
            return false;
        }

        int trunkHeight = 3 + rand.nextInt(2); // 3-4 blocks trunk

        for (int y = 0; y < trunkHeight + 3; y++) {
            if (!EnvironmentHelpers.isWorldgenReplaceable(level, pos.above(y))) {
                return false;
            }
        }

        FruitTreeBranchBlock branchBlock = JSDBlocks.TEA_BRANCH.get();
        JSDTeaLeavesBlock leavesBlock = JSDBlocks.TEA_LEAVES.get();
        Lifecycle lifecycle = leavesBlock.getLifecycleForCurrentMonth(level.getLevel(), pos);
        if (lifecycle == Lifecycle.DORMANT) {
            lifecycle = Lifecycle.HEALTHY;
        }

        BlockPos stemPos = pos;

        // Place trunk
        for (int y = 0; y < trunkHeight; y++) {
            setBlock(level, pos.above(y), branchBlock.defaultBlockState());
        }

        // Place horizontal branches in cardinal directions
        BlockPos topPos = pos.above(trunkHeight - 1);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int branchLen = 2 + rand.nextInt(2); // 2-3 blocks outward
            BlockPos cur = topPos;
            for (int step = 1; step <= branchLen; step++) {
                cur = cur.relative(dir);
                if (step == 2 && rand.nextBoolean()) {
                    cur = cur.above();
                }
                if (EnvironmentHelpers.isWorldgenReplaceable(level, cur)) {
                    setBlock(level, cur, branchBlock.defaultBlockState());
                    placeLeavesAround(level, cur, leavesBlock, lifecycle, stemPos);
                }
            }
        }

        // Place crown leaves above trunk
        placeLeavesAround(level, pos.above(trunkHeight), leavesBlock, lifecycle, stemPos);
        placeLeavesAround(level, pos.above(trunkHeight + 1), leavesBlock, lifecycle, stemPos);

        return true;
    }

    private void placeLeavesAround(WorldGenLevel level, BlockPos center, JSDTeaLeavesBlock leavesBlock, Lifecycle lifecycle, BlockPos stemPos) {
        BlockState leafState = leavesBlock.defaultBlockState().setValue(JSDTeaLeavesBlock.LIFECYCLE, lifecycle);
        for (Direction d : Direction.values()) {
            BlockPos p = center.relative(d);
            if (EnvironmentHelpers.isWorldgenReplaceable(level, p)) {
                setBlock(level, p, leafState);
                if (level.getBlockEntity(p) instanceof BerryBushBlockEntity be) {
                    be.setStemPos(stemPos);
                }
            }
        }
    }
}
