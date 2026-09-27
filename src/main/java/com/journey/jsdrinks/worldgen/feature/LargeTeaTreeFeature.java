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
 * Worldgen feature for rare large ancient tea trees (Camellia sinensis var. assamica).
 * <p>
 * Generates an ancient tea tree with spreading boughs, natural multipart fruitwood branches,
 * and leaves strictly attached directly to branch blocks (distance 1) so edge decay never occurs.
 */
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

        if (pos.getY() + trunkHeight + 5 >= level.getMaxBuildHeight()) {
            return false;
        }

        for (int y = 0; y < trunkHeight + 3; y++) {
            if (!isDryReplaceable(level, pos.above(y))) {
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

        // 2. Crown vertical extension
        BlockPos crown1 = pos.above(trunkHeight);
        if (isDryReplaceable(level, crown1)) {
            branchPositions.add(crown1);
            BlockPos crown2 = crown1.above();
            if (isDryReplaceable(level, crown2)) {
                branchPositions.add(crown2);
            }
        }

        // 3. Spreading boughs from upper trunk
        BlockPos topTrunkPos = pos.above(trunkHeight - 1);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int branchLen = 2 + rand.nextInt(2); // 2-3 blocks outward
            BlockPos cur = topTrunkPos;
            for (int step = 1; step <= branchLen; step++) {
                BlockPos next = cur.relative(dir);
                if (step == 2 && rand.nextBoolean()) {
                    // Ascend: place horizontal connector first, then rise vertically to prevent diagonal disconnect
                    if (isDryReplaceable(level, next)) {
                        branchPositions.add(next);
                        BlockPos risen = next.above();
                        if (isDryReplaceable(level, risen)) {
                            branchPositions.add(risen);
                            cur = risen;
                        } else {
                            cur = next;
                        }
                    } else {
                        break;
                    }
                } else {
                    if (isDryReplaceable(level, next)) {
                        branchPositions.add(next);
                        cur = next;
                    } else {
                        break;
                    }
                }
            }
        }

        // 4. Secondary lower boughs (if trunk is tall enough)
        if (trunkHeight >= 4) {
            BlockPos midTrunkPos = pos.above(trunkHeight - 2);
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (rand.nextBoolean()) {
                    BlockPos cur = midTrunkPos.relative(dir);
                    if (isDryReplaceable(level, cur)) {
                        branchPositions.add(cur);
                    }
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
        Set<BlockPos> placedLeaves = new HashSet<>();

        for (BlockPos bPos : branchPositions) {
            // Keep the ground-level trunk base clean of leaves
            if (bPos.equals(pos)) {
                continue;
            }
            for (Direction d : Direction.values()) {
                if (d == Direction.DOWN) {
                    // Only hang leaves below branch if high enough above ground
                    if (bPos.getY() - pos.getY() < 2) {
                        continue;
                    }
                }
                BlockPos leafPos = bPos.relative(d);
                if (!branchPositions.contains(leafPos) && !placedLeaves.contains(leafPos) && isDryReplaceable(level, leafPos)) {
                    setBlock(level, leafPos, leafState);
                    placedLeaves.add(leafPos);
                    if (level.getBlockEntity(leafPos) instanceof JSDBerryBushBlockEntity be) {
                        be.setStemPos(stemPos);
                        be.resetCounter();
                    }
                }
            }
        }

        return true;
    }

    private static boolean isDryReplaceable(WorldGenLevel level, BlockPos pos) {
        return level.getFluidState(pos).isEmpty() && EnvironmentHelpers.isWorldgenReplaceable(level, pos);
    }
}
