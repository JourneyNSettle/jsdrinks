package com.journey.jsdrinks.client;

import com.journey.jsdrinks.JourneyDrinks;
import com.journey.jsdrinks.registry.JSDDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = JourneyDrinks.MODID, value = Dist.CLIENT)
public class JSDClientEvents {

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // Select tag
        if (Boolean.TRUE.equals(stack.get(JSDDataComponents.SELECT.get()))) {
            event.getToolTip().add(Component.translatable("tooltip.jsdrinks.select").withStyle(ChatFormatting.GREEN));
        }

        // Large leaf tag
        if (Boolean.TRUE.equals(stack.get(JSDDataComponents.LARGE_LEAF.get()))) {
            event.getToolTip().add(Component.translatable("tooltip.jsdrinks.large_leaf").withStyle(ChatFormatting.AQUA));
        }

        // Burnt tag
        if (Boolean.TRUE.equals(stack.get(JSDDataComponents.BURNT.get()))) {
            event.getToolTip().add(Component.translatable("tooltip.jsdrinks.burnt").withStyle(ChatFormatting.RED));
        }

        // Quality rating
        Integer quality = stack.get(JSDDataComponents.QUALITY.get());
        if (quality != null && quality > 0) {
            event.getToolTip().add(Component.translatable("tooltip.jsdrinks.quality", quality).withStyle(ChatFormatting.GOLD));
        }
    }
}
