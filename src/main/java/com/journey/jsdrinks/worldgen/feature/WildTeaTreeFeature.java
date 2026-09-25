package com.journey.jsdrinks.worldgen.feature;

import java.util.HashSet;
import java.util.Set;

import com.journey.jsdrinks.block.JSDTeaLeavesBlock;
import com.journey.jsdrinks.block.entity.JSDBerryBushBlockEntity;
import com.journey.jsdrinks.registry.JSDBlocks;
import com.mojang.serialization.Codec;
import net.dries007.tfc.common.TFCTags;
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

/**
 * Worldgen feature for normal-sized wild tea trees (Camellia sinensis).
 * <p>
 * TZ R23 row 2: Wild tea tree of standard size.
 * Generated with connected multipart fruitwood branches and leaves strictly adjacent
 * to branch blocks (distance 1) so that leaf decay on chunk load never occurs.
 */
public class WildTeaTreeFeature extends Feature<NoneFeatureConfiguration> {

    public WildTeaTreeFeature(Codec<NoneFeatureConfiguration> codec) {
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

        int trunkHeight = 2 + rand.nextInt(2); // 2-3 blocks trunk

        for (int y = 0; y < trunkHeight + 2; y++) {
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
        Set<BlockPos> branchPositions = new HashSet<>();

        // 1. Central trunk
        for (int y = 0; y < trunkHeight; y++) {
            branchPositions.add(pos.above(y));
        }

        // 2. Crown branch
        BlockPos crownPos = pos.above(trunkHeight);
        if (EnvironmentHelpers.isWorldgenReplaceable(level, crownPos)) {
            branchPositions.add(crownPos);
        }

        // 3. Spreading boughs
        BlockPos topTrunkPos = pos.above(trunkHeight - 1);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int branchLen = 1 + rand.nextInt(2); // 1-2 blocks outward
            BlockPos cur = topTrunkPos;
            for (int step = 1; step <= branchLen; step++) {
                cur = cur.relative(dir);
                if (EnvironmentHelpers.isWorldgenReplaceable(level, cur)) {
                    branchPositions.add(cur);
                } else {
                    break;
                }
            }
        }

        // Pass 1: Place all branches
        for (BlockPos bPos : branchPositions) {
            setBlock(level, bPos, branchBlock.defaultBlockState());
        }

        // Pass 2: Connect all branches in 3D using getStateForPlacement
        for (BlockPos bPos : branchPositions) {
            setBlock(level, bPos, branchBlock.getStateForPlacement(level, bPos));
        }

        // Pass 3: Place leaves strictly around existing branch blocks
        BlockState leafState = leavesBlock.defaultBlockState().setValue(JSDTeaLeavesBlock.LIFECYCLE, lifecycle);

        for (BlockPos bPos : branchPositions) {
            for (Direction d : Direction.values()) {
                if (d == Direction.DOWN) {
                    // Only hang leaves below branch if high enough above ground
                    if (bPos.getY() - pos.getY() < 2) {
                        continue;
                    }
                }
                BlockPos leafPos = bPos.relative(d);
                if (!branchPositions.contains(leafPos) && EnvironmentHelpers.isWorldgenReplaceable(level, leafPos)) {
                    setBlock(level, leafPos, leafState);
                    if (level.getBlockEntity(leafPos) instanceof JSDBerryBushBlockEntity be) {
                        be.setStemPos(stemPos);
                        be.resetCounter();
                    }
                }
            }
        }

        return true;
    }
}
