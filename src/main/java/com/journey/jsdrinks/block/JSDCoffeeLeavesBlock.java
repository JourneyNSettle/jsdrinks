package com.journey.jsdrinks.block;

import java.util.function.Supplier;

import com.journey.jsdrinks.block.entity.JSDBerryBushBlockEntity;
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
import net.dries007.tfc.common.blockentities.BerryBushBlockEntity;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.plant.fruit.FruitTreeLeavesBlock;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.climate.ClimateRange;

/**
 * Coffee tree leaves block.
 * <p>
 * TZ §3.4: coffee trees must be at least 2 TFC calendar years old before they
 * can bear fruit.  During the first 2 years FRUITING is suppressed (shows
 * FLOWERING instead).  The age is tracked via the leaf block-entity's own tick
 * counter ({@link BerryBushBlockEntity#getTicksSinceUpdate()}).
 * <p>
 * <b>Architectural note:</b> the stem (trunk) block is a static
 * {@code FruitTreeBranchBlock} which does NOT carry a BlockEntity for mature
 * trees.  Therefore we must NOT try to read age from {@code stemPos}.
 */
public class JSDCoffeeLeavesBlock extends FruitTreeLeavesBlock {

    public JSDCoffeeLeavesBlock(ExtendedProperties properties, Lifecycle[] stages,
                                 Supplier<ClimateRange> climateRange, int flowerColor) {
        super(properties, JSDItems.COFFEE_CHERRY, stages, climateRange, flowerColor);
    }

    // Widen protected → public so worldgen features in another package can call it.
    @Override
    public Lifecycle getLifecycleForCurrentMonth(Level level, BlockPos pos) {
        return super.getLifecycleForCurrentMonth(level, pos);
    }

    // -------------------------------------------------------- Age-gated fruiting

    /**
     * Lifecycle update with 2-year age gate.
     * <p>
     * The leaf BE's tick counter is used as the single source of truth for tree age:
     * <ul>
     *   <li><b>Worldgen trees:</b> {@code WildCoffeeTreeFeature} calls
     *       {@code resetCounter()} + {@code increaseCounter(3yearTicks)} so the
     *       counter reads ≥ 3 years from the moment of generation → fruit immediately.</li>
     *   <li><b>Player trees:</b> leaves are created by {@code addLeaves()} inside
     *       {@code GrowingFruitTreeBranchBlock} with an uninitialised counter
     *       ({@code lastUpdateTick = Integer.MIN_VALUE}).  We detect this here
     *       and reset to "just born" → blocks fruiting for 2 years.</li>
     * </ul>
     */
    @Override
    public void onUpdate(Level level, BlockPos pos, BlockState state) {
        if (state.getValue(PERSISTENT)) return;

        if (level.getBlockEntity(pos) instanceof BerryBushBlockEntity plant) {
            long ageTicks = plant.getTicksSinceUpdate();
            long totalCalendarTicks = Calendars.get(level).getTicks();

            // Detect uninitialised tick counter.
            // Default lastUpdateTick = Integer.MIN_VALUE produces an overflowed age
            // that exceeds the total calendar time elapsed since world creation.
            if (ageTicks < 0 || ageTicks > totalCalendarTicks) {
                plant.resetCounter();   // set "birth" to now
                ageTicks = 0;
            }

            long twoYearsTicks = 2L * Calendars.get(level).getCalendarTicksInYear();
            if (ageTicks < twoYearsTicks) {
                // Young tree: suppress FRUITING → show FLOWERING instead.
                Lifecycle expectedLifecycle = getLifecycleForCurrentMonth(level, pos);
                if (expectedLifecycle == Lifecycle.FRUITING) {
                    if (state.getValue(LIFECYCLE) != Lifecycle.FLOWERING) {
                        level.setBlockAndUpdate(pos, state.setValue(LIFECYCLE, Lifecycle.FLOWERING));
                    }
                    return; // skip super.onUpdate to prevent overwriting with FRUITING
                }
            }
        }

        super.onUpdate(level, pos, state);
    }

    // -------------------------------------------------------------- Harvest (RMB)

    /**
     * TZ §3.5 harvest rules for coffee:
     * <ul>
     *   <li><b>Knife:</b> 2–3 cherries, no {@code quality} tag, −1 durability.</li>
     *   <li><b>Bare hand:</b> 1–2 cherries, always tagged with
     *       {@code quality} 1–5.</li>
     * </ul>
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hitResult) {
        if (state.getValue(LIFECYCLE) == Lifecycle.FRUITING) {
            level.playSound(player, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,
                SoundSource.PLAYERS, 1.0f, level.getRandom().nextFloat() * 0.2f + 0.9f);

            if (!level.isClientSide()) {
                boolean isKnife = stack.is(TFCTags.Items.TOOLS_KNIFE);

                if (isKnife) {
                    // Knife: more cherries, no quality tag, costs durability.
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                    ItemHandlerHelper.giveItemToPlayer(player,
                        new ItemStack(JSDItems.COFFEE_CHERRY.get(), 2 + level.random.nextInt(2)));
                } else {
                    // Bare hand: fewer cherries, always tagged with quality 1–5.
                    ItemStack drop = new ItemStack(JSDItems.COFFEE_CHERRY.get(),
                        1 + (level.random.nextFloat() < 0.35f ? 1 : 0));
                    drop.set(JSDDataComponents.QUALITY.get(), 1 + level.random.nextInt(5));
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
