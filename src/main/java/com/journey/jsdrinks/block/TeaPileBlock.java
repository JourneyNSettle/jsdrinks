package com.journey.jsdrinks.block;

import com.journey.jsdrinks.block.entity.TeaPileBlockEntity;
import com.journey.jsdrinks.registry.JSDBlockEntities;
import com.journey.jsdrinks.registry.JSDItems;
import net.dries007.tfc.common.component.food.FoodCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

public class TeaPileBlock extends Block implements EntityBlock {

    // 0: Bruised (green), 1: Fermented (red-brown), 2: Pu-erh (dark), 3: Rotten
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 3);
    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);

    public TeaPileBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canSupportRigidBlock(level, pos.below());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TeaPileBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide() ? null : (lvl, pos, st, be) -> {
            if (be instanceof TeaPileBlockEntity pileBE) {
                TeaPileBlockEntity.serverTick(lvl, pos, st, pileBE);
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof TeaPileBlockEntity pileBE) {
                int stage = state.getValue(STAGE);
                ItemStack drop;
                if (stage == 3) {
                    drop = pileBE.getStoredItem();
                    if (drop.isEmpty()) {
                        drop = new ItemStack(JSDItems.FERMENTED_TEA_LEAF.get());
                    }
                    FoodCapability.setRotten(drop);
                    ItemHandlerHelper.giveItemToPlayer(player, drop);
                    level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 1.0f, 0.8f);
                } else {
                    drop = pileBE.getStoredItem();
                    if (!drop.isEmpty()) {
                        if (FoodCapability.isRotten(drop)) {
                            FoodCapability.setRotten(drop);
                        }
                        ItemHandlerHelper.giveItemToPlayer(player, drop);
                    }
                    level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
                pileBE.setStoredItem(ItemStack.EMPTY);
            }
            level.removeBlock(pos, false);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof TeaPileBlockEntity pileBE) {
                ItemStack drop = pileBE.getStoredItem();
                if (!drop.isEmpty()) {
                    if (state.getValue(STAGE) == 3 || FoodCapability.isRotten(drop)) {
                        FoodCapability.setRotten(drop);
                    }
                    popResource(level, pos, drop);
                    pileBE.setStoredItem(ItemStack.EMPTY);
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
