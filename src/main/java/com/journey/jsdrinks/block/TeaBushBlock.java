package com.journey.jsdrinks.block;

import com.journey.jsdrinks.block.entity.TeaBushBlockEntity;
import com.journey.jsdrinks.registry.JSDBlockEntities;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
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
import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlockStateProperties;
import net.dries007.tfc.common.blocks.plant.ITallPlant;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.common.blocks.plant.fruit.StationaryBerryBushBlock;
import net.dries007.tfc.util.climate.ClimateRange;

import java.util.function.Supplier;

public class TeaBushBlock extends StationaryBerryBushBlock implements ITallPlant, EntityBlock {

    public static final EnumProperty<Part> PART = TFCBlockStateProperties.TALL_PLANT_PART;

    public TeaBushBlock(ExtendedProperties properties, Lifecycle[] lifecycle, Supplier<ClimateRange> climateRange) {
        super(properties, JSDItems.FRESH_TEA_LEAF, lifecycle, climateRange);
        registerDefaultState(getStateDefinition().any().setValue(PART, Part.LOWER).setValue(LIFECYCLE, Lifecycle.HEALTHY));
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
        return state.getValue(PART) == Part.LOWER ? new TeaBushBlockEntity(pos, state) : null;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(PART) == Part.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(PART) == Part.LOWER;
        }
        return super.canSurvive(state, level, pos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        Part part = state.getValue(PART);
        BlockPos otherPos = part == Part.LOWER ? pos.above() : pos.below();
        BlockState otherState = level.getBlockState(otherPos);
        if (otherState.is(this) && otherState.getValue(PART) != part) {
            level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
        }
        // Guaranteed 1 tea sapling on breaking bush (TZ 3.1)
        if (!level.isClientSide() && !player.isCreative()) {
            popResource(level, pos, new ItemStack(JSDItems.TEA_SAPLING.get()));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);

        if (state.getValue(PART) == Part.LOWER) {
            if (level.getBlockEntity(pos) instanceof TeaBushBlockEntity bushBE) {
                bushBE.checkYearlyGrowth(level, pos, state.getValue(LIFECYCLE));
            }
            // Sync upper lifecycle
            BlockPos upperPos = pos.above();
            BlockState upperState = level.getBlockState(upperPos);
            if (upperState.is(this) && upperState.getValue(LIFECYCLE) != state.getValue(LIFECYCLE)) {
                level.setBlockAndUpdate(upperPos, upperState.setValue(LIFECYCLE, state.getValue(LIFECYCLE)));
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos lowerPos = state.getValue(PART) == Part.LOWER ? pos : pos.below();
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
                    // Hand: chance for select tag (TZ 4)
                    if (level.random.nextFloat() < 0.35f) {
                        drop.set(JSDDataComponents.SELECT.get(), true);
                    }
                    ItemHandlerHelper.giveItemToPlayer(player, drop);
                }

                if (level.getBlockEntity(lowerPos) instanceof TeaBushBlockEntity bushBE) {
                    bushBE.resetSeasonsWithoutHarvest();
                }
            }

            BerryBushBlockEntity.resetPickedTick(level, lowerPos);
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
