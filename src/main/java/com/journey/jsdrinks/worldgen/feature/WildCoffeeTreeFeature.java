package com.journey.jsdrinks.worldgen.feature;

import java.util.HashSet;
import java.util.Set;

import com.journey.jsdrinks.block.JSDCoffeeLeavesBlock;
import com.journey.jsdrinks.block.entity.JSDBerryBushBlockEntity;
import com.journey.jsdrinks.registry.JSDBlocks;
import com.mojang.serialization.Codec;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeBranchBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.EnvironmentHelpers;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Worldgen feature for wild coffee trees.
 * <p>
 * Ensures all non-persistent leaves are placed directly adjacent to a branch block
 * (distance 1) so that TFC leaf validation succeeds and leaves never break on chunk load.
 * Branches use TFC's multipart connection logic (getStateForPlacement) to ensure
 * slender, organic 3D branch connections rather than full 16x16 blocks.
 * <p>
 * TZ R23: Wild coffee trees spawn with age >= 3 years so ripe coffee cherries are available.
 */
public class WildCoffeeTreeFeature extends Feature<NoneFeatureConfiguration> {

    public WildCoffeeTreeFeature(Codec<NoneFeatureConfiguration> codec) {
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

        if (pos.getY() + trunkHeight + 3 >= level.getMaxBuildHeight()) {
            return false;
        }

        for (int y = 0; y < trunkHeight + 2; y++) {
            if (!isDryReplaceable(level, pos.above(y))) {
                return false;
            }
        }

        FruitTreeBranchBlock branchBlock = JSDBlocks.COFFEE_BRANCH.get();
        JSDCoffeeLeavesBlock leavesBlock = JSDBlocks.COFFEE_LEAVES.get();
        Lifecycle lifecycle = leavesBlock.getLifecycleForCurrentMonth(level.getLevel(), pos);
        if (lifecycle == Lifecycle.DORMANT) {
            lifecycle = Lifecycle.HEALTHY;
        }

        BlockPos stemPos = pos;
        long threeYearsTicks = 3L * Calendars.get(level.getLevel()).getCalendarTicksInYear();

        Set<BlockPos> branchPositions = new HashSet<>();

        // 1. Trunk
        for (int y = 0; y < trunkHeight; y++) {
            branchPositions.add(pos.above(y));
        }

        // 2. Crown central branch above trunk
        BlockPos crownPos = pos.above(trunkHeight);
        if (isDryReplaceable(level, crownPos)) {
            branchPositions.add(crownPos);
        }

        // 3. Horizontal spreading branches
        BlockPos topTrunkPos = pos.above(trunkHeight - 1);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            int branchLen = 1 + rand.nextInt(2); // 1-2 blocks outward
            BlockPos cur = topTrunkPos;
            for (int step = 1; step <= branchLen; step++) {
                cur = cur.relative(dir);
                if (isDryReplaceable(level, cur)) {
                    branchPositions.add(cur);
                } else {
                    break;
                }
            }
        }

        // Pass 1: Place all branch blocks
        for (BlockPos bPos : branchPositions) {
            setBlock(level, bPos, branchBlock.defaultBlockState());
        }

        // Pass 2: Update all branch blockstates using getStateForPlacement
        // so directional connections (UP, DOWN, NORTH, SOUTH, EAST, WEST) match adjacent branches
        for (BlockPos bPos : branchPositions) {
            setBlock(level, bPos, branchBlock.getStateForPlacement(level, bPos));
        }

        // Pass 3: Place leaves ONLY directly around existing branch blocks.
        // Every leaf is strictly adjacent (distance 1) to a branch, so isValid() is always true.
        BlockState leafState = leavesBlock.defaultBlockState().setValue(JSDCoffeeLeavesBlock.LIFECYCLE, lifecycle);
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
                        // Backdate placedTick by 3 years so the tree is immediately mature
                        // (passes the 2-year age gate in JSDCoffeeLeavesBlock.onUpdate()).
                        be.setPlacedTick(Calendars.get(level.getLevel()).getTicks() - threeYearsTicks);
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
