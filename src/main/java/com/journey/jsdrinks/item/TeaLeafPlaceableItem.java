package com.journey.jsdrinks.item;

import com.journey.jsdrinks.block.TeaPileBlock;
import com.journey.jsdrinks.block.entity.TeaPileBlockEntity;
import com.journey.jsdrinks.registry.JSDBlocks;
import com.journey.jsdrinks.registry.JSDItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class TeaLeafPlaceableItem extends Item {

    public TeaLeafPlaceableItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos placePos = clickedPos.relative(face);

        if (face == Direction.UP && level.getBlockState(placePos).canBeReplaced() && level.getBlockState(clickedPos).isFaceSturdy(level, clickedPos, Direction.UP)) {
            if (!level.isClientSide()) {
                ItemStack stack = context.getItemInHand();
                boolean isBruised = stack.is(JSDItems.BRUISED_TEA_LEAF.get());
                int stage = isBruised ? 0 : 1;

                BlockState state = JSDBlocks.TEA_PILE.get().defaultBlockState().setValue(TeaPileBlock.STAGE, stage);
                level.setBlockAndUpdate(placePos, state);

                if (level.getBlockEntity(placePos) instanceof TeaPileBlockEntity pileBE) {
                    pileBE.setStoredItem(stack.copyWithCount(1));
                }

                level.playSound(null, placePos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
                stack.shrink(1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useOn(context);
    }
}
