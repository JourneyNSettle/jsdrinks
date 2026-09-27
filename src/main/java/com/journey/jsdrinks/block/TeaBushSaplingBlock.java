package com.journey.jsdrinks.block;

import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.plant.ITallPlant;
import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeSaplingBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.climate.ClimateRange;

import com.journey.jsdrinks.block.entity.JSDTickingPlantBlockEntity;
import com.journey.jsdrinks.block.entity.TeaBushBlockEntity;
import com.journey.jsdrinks.registry.JSDBlocks;
import com.journey.jsdrinks.registry.JSDItems;

/**
 * 1-block sapling that grows into a 2-block adult {@link TeaBushBlock} over 8 days.
 */
public class TeaBushSaplingBlock extends FruitTreeSaplingBlock {

    public TeaBushSaplingBlock(ExtendedProperties properties, Supplier<? extends Block> block,
                              Supplier<Integer> growthTicks, Supplier<ClimateRange> climateRange,
                              Lifecycle[] stages) {
        super(properties, block, growthTicks, climateRange, stages);
        registerDefaultState(getStateDefinition().any().setValue(TFCBlockStateProperties.SAPLINGS, 1));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Helpers.isBlock(level.getBlockState(pos.below()), TFCTags.Blocks.BUSH_PLANTABLE_ON);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof JSDTickingPlantBlockEntity be) {
            be.setStemPos(pos);
            be.resetCounter();
        }
    }

    @Override
    public void createTree(Level level, BlockPos pos, BlockState state, RandomSource random, long ticksToAdd, BlockPos stemPos) {
        BlockPos upperPos = pos.above();
        if (level.getBlockState(upperPos).canBeReplaced()) {
            TeaBushBlock bushBlock = JSDBlocks.TEA_BUSH.get();
            Lifecycle currentLifecycle = bushBlock.getLifecycleForCurrentMonth(level, pos);
            BlockState lowerState = bushBlock.defaultBlockState()
                    .setValue(TeaBushBlock.PART, ITallPlant.Part.LOWER)
                    .setValue(TeaBushBlock.LIFECYCLE, currentLifecycle);
            BlockState upperState = bushBlock.defaultBlockState()
                    .setValue(TeaBushBlock.PART, ITallPlant.Part.UPPER)
                    .setValue(TeaBushBlock.LIFECYCLE, currentLifecycle);

            level.setBlockAndUpdate(pos, lowerState);
            level.setBlockAndUpdate(upperPos, upperState);

            if (level.getBlockEntity(pos) instanceof TeaBushBlockEntity bush) {
                bush.resetCounter();
                bush.increaseCounter(ticksToAdd);
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(JSDItems.TEA_SAPLING.get());
    }
}
