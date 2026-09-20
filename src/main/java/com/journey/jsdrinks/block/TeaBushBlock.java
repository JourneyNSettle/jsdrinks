package com.journey.jsdrinks.block;

import com.journey.jsdrinks.block.entity.TeaBushBlockEntity;
import com.journey.jsdrinks.registry.JSDBlocks;
import com.journey.jsdrinks.registry.JSDDataComponents;
import com.journey.jsdrinks.registry.JSDItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.plant.ITallPlant;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.blocks.plant.fruit.SeasonalPlantBlock;
import net.dries007.tfc.util.climate.ClimateRange;

import java.util.function.Supplier;

public class TeaBushBlock extends SeasonalPlantBlock {

    public static final EnumProperty<ITallPlant.Part> PART = TFCBlockStateProperties.TALL_PLANT_PART;

    @Override
    public Lifecycle getLifecycleForCurrentMonth(Level level, BlockPos pos) {
        return super.getLifecycleForCurrentMonth(level, pos);
    }

    public TeaBushBlock(ExtendedProperties properties, Lifecycle[] lifecycle, Supplier<ClimateRange> climateRange) {
        super(properties, climateRange, JSDItems.FRESH_TEA_LEAF, lifecycle);
        registerDefaultState(getStateDefinition().any().setValue(PART, ITallPlant.Part.LOWER).setValue(LIFECYCLE, Lifecycle.HEALTHY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == ITallPlant.Part.LOWER ? new TeaBushBlockEntity(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> givenType) {
        return null;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxBuildHeight() - 1 && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return defaultBlockState().setValue(PART, ITallPlant.Part.LOWER);
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlockAndUpdate(pos.above(), defaultBlockState().setValue(PART, ITallPlant.Part.UPPER).setValue(LIFECYCLE, state.getValue(LIFECYCLE)));
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(PART) == ITallPlant.Part.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(PART) == ITallPlant.Part.LOWER;
        }
        return super.canSurvive(state, level, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        ITallPlant.Part part = state.getValue(PART);
        if (facing.getAxis() != Direction.Axis.Y || part == ITallPlant.Part.LOWER != (facing == Direction.UP) || (facingState.is(this) && facingState.getValue(PART) != part)) {
            return part == ITallPlant.Part.LOWER && facing == Direction.DOWN && !state.canSurvive(level, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
        } else {
            return Blocks.AIR.defaultBlockState();
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        ITallPlant.Part part = state.getValue(PART);
        BlockPos otherPos = part == ITallPlant.Part.LOWER ? pos.above() : pos.below();
        BlockState otherState = level.getBlockState(otherPos);
        if (otherState.is(this) && otherState.getValue(PART) != part) {
            level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
        }
        if (!level.isClientSide() && !player.isCreative() && part == ITallPlant.Part.LOWER) {
            popResource(level, pos, new ItemStack(JSDItems.TEA_SAPLING.get()));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);

        if (state.getValue(PART) == ITallPlant.Part.LOWER) {
            if (level.getBlockEntity(pos) instanceof TeaBushBlockEntity bushBE) {
                bushBE.checkYearlyGrowth(level, pos, state.getValue(LIFECYCLE));
            }
            BlockPos upperPos = pos.above();
            BlockState upperState = level.getBlockState(upperPos);
            if (upperState.is(this) && upperState.getValue(LIFECYCLE) != state.getValue(LIFECYCLE)) {
                level.setBlockAndUpdate(upperPos, upperState.setValue(LIFECYCLE, state.getValue(LIFECYCLE)));
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos lowerPos = state.getValue(PART) == ITallPlant.Part.LOWER ? pos : pos.below();
        BlockState lowerState = level.getBlockState(lowerPos);

        if (lowerState.is(this) && lowerState.getValue(LIFECYCLE) == Lifecycle.FRUITING) {
            level.playSound(player, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 1.0f, level.getRandom().nextFloat() * 0.2f + 0.9f);
            if (!level.isClientSide()) {
                boolean isKnife = stack.is(TFCTags.Items.TOOLS_KNIFE);
                if (isKnife) {
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                    ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(JSDItems.FRESH_TEA_LEAF.get(), 2 + level.random.nextInt(2)));
                } else {
                    ItemStack drop = new ItemStack(JSDItems.FRESH_TEA_LEAF.get(), 1 + (level.random.nextFloat() < 0.35f ? 1 : 0));
                    if (level.random.nextFloat() < 0.35f) {
                        drop.set(JSDDataComponents.SELECT.get(), true);
                    }
                    ItemHandlerHelper.giveItemToPlayer(player, drop);
                }

                if (level.getBlockEntity(lowerPos) instanceof TeaBushBlockEntity bushBE) {
                    bushBE.resetSeasonsWithoutHarvest();
                    bushBE.resetLastPickedCounter();
                }
            }

            level.setBlockAndUpdate(lowerPos, lowerState.setValue(LIFECYCLE, Lifecycle.HEALTHY));

            BlockPos upperPos = lowerPos.above();
            BlockState upperState = level.getBlockState(upperPos);
            if (upperState.is(this)) {
                level.setBlockAndUpdate(upperPos, upperState.setValue(LIFECYCLE, Lifecycle.HEALTHY));
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
