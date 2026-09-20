package com.journey.jsdrinks.block;

import com.journey.jsdrinks.block.entity.JSDTickingPlantBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.TickPriority;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeSaplingBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.climate.ClimateRange;

import java.util.function.Supplier;

public class JSDFruitTreeSaplingBlock extends FruitTreeSaplingBlock {
    public JSDFruitTreeSaplingBlock(ExtendedProperties properties, Supplier<? extends Block> block, Supplier<Integer> growthTicks, Supplier<ClimateRange> climateRange, Lifecycle[] stages) {
        super(properties, block, growthTicks, climateRange, stages);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        final BlockPos downPos = pos.below();
        final BlockState downState = level.getBlockState(downPos);
        if (Helpers.isBlock(downState, TFCTags.Blocks.FRUIT_TREE_BRANCH)) {
            JSDTickingPlantBlockEntity.setStemPos(level, pos, findBaseOfTree(level, downPos, downState));
        }
        JSDTickingPlantBlockEntity.reset(level, pos);
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    public void createTree(Level level, BlockPos pos, BlockState state, RandomSource random, long ticksToAdd, BlockPos stemPos) {
        final boolean onBranch = Helpers.isBlock(level.getBlockState(pos.below()), TFCTags.Blocks.FRUIT_TREE_BRANCH);
        int internalSapling = onBranch ? 3 : state.getValue(TFCBlockStateProperties.SAPLINGS);
        if (internalSapling == 1 && random.nextBoolean()) internalSapling += 1;
        level.setBlockAndUpdate(pos, block.get().defaultBlockState().setValue(PipeBlock.DOWN, true).setValue(TFCBlockStateProperties.SAPLINGS, internalSapling).setValue(TFCBlockStateProperties.STAGE_3, onBranch ? 1 : 0));
        if (level.getBlockEntity(pos) instanceof JSDTickingPlantBlockEntity branch) {
            branch.resetCounter();
            branch.increaseCounter(ticksToAdd);
            branch.setStemPos(stemPos);
        }
        level.scheduleTick(pos, block.get(), 20, TickPriority.NORMAL);
    }
}
