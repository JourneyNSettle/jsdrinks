package com.journey.jsdrinks.block;

import com.journey.jsdrinks.registry.JSDDataComponents;
import com.journey.jsdrinks.registry.JSDItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import net.dries007.tfc.common.TFCTags;
import com.journey.jsdrinks.block.entity.JSDBerryBushBlockEntity;
import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blockentities.TickCounterBlockEntity;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeLeavesBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.ClimateRange;

import java.util.function.Supplier;

public class JSDCoffeeLeavesBlock extends FruitTreeLeavesBlock {

    @Override
    public Lifecycle getLifecycleForCurrentMonth(Level level, BlockPos pos) {
        return super.getLifecycleForCurrentMonth(level, pos);
    }

    public JSDCoffeeLeavesBlock(ExtendedProperties properties, Lifecycle[] stages, Supplier<ClimateRange> climateRange, int flowerColor) {
        super(properties, JSDItems.COFFEE_CHERRY, stages, climateRange, flowerColor);
    }

    @Override
    public void onUpdate(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(PERSISTENT)) return;

        if (level.getBlockEntity(pos) instanceof BerryBushBlockEntity plant) {
            BlockPos stemPos = plant.getStemPos();
            long totalAgeTicks = plant.getTicksSinceUpdate();
            if (level.getBlockEntity(stemPos) instanceof TickCounterBlockEntity stemEntity) {
                totalAgeTicks = Math.max(totalAgeTicks, stemEntity.getTicksSinceUpdate());
            }

            // In world: 2 years without fruiting (from TZ 3.2)
            long twoYearsTicks = 2L * Calendars.get(level).getCalendarTicksInYear();
            if (totalAgeTicks < twoYearsTicks) {
                Lifecycle expectedLifecycle = getLifecycleForCurrentMonth(level, pos);
                if (expectedLifecycle == Lifecycle.FRUITING) {
                    state = state.setValue(LIFECYCLE, Lifecycle.FLOWERING);
                    level.setBlockAndUpdate(pos, state);
                    return;
                }
            }
        }

        super.onUpdate(level, pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (state.getValue(LIFECYCLE) == Lifecycle.FRUITING) {
            level.playSound(player, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.PLAYERS, 1.0f, level.getRandom().nextFloat() * 0.2f + 0.9f);
            if (!level.isClientSide()) {
                boolean isKnife = stack.is(TFCTags.Items.TOOLS_KNIFE);
                if (isKnife) {
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                    ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(JSDItems.COFFEE_CHERRY.get(), 2 + level.random.nextInt(2)));
                } else {
                    ItemStack drop = new ItemStack(JSDItems.COFFEE_CHERRY.get(), 1 + (level.random.nextFloat() < 0.35f ? 1 : 0));
                    int quality = 1 + level.random.nextInt(5);
                    drop.set(JSDDataComponents.QUALITY.get(), quality);
                    ItemHandlerHelper.giveItemToPlayer(player, drop);
                }
            }
            JSDBerryBushBlockEntity.resetPickedTick(level, pos);
            level.setBlockAndUpdate(pos, state.setValue(LIFECYCLE, Lifecycle.HEALTHY));
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
