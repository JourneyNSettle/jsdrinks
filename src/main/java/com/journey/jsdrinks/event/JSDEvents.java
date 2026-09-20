package com.journey.jsdrinks.event;

import com.journey.jsdrinks.registry.JSDItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.component.heat.IHeat;

import static com.journey.jsdrinks.JourneyDrinks.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class JSDEvents {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide) return;

        // Check every 30 ticks (~1.5s)
        if (level.getGameTime() % 30 != 0) return;

        BlockPos playerPos = player.blockPosition();
        // Check 3x3x3 around player for any block entity cooking coffee beans
        for (BlockPos pos : BlockPos.betweenClosed(playerPos.offset(-3, -2, -3), playerPos.offset(3, 2, 3))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be != null) {
                IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
                if (handler != null) {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        var stack = handler.getStackInSlot(i);
                        if (stack.is(JSDItems.DRIED_COFFEE_BEAN.get()) || stack.is(JSDItems.ROASTED_COFFEE_BEAN.get())) {
                            IHeat heat = HeatCapability.get(stack);
                            if (heat != null && heat.getTemperature() >= 180.0f) {
                                // Play coffee roasting crackle near player
                                level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.45f, 1.2f + level.random.nextFloat() * 0.4f);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
