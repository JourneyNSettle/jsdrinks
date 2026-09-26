package com.journey.jsdrinks.registry;

import java.util.function.Supplier;
import com.journey.jsdrinks.JourneyDrinks;
import com.journey.jsdrinks.item.JSDSaplingItem;
import com.journey.jsdrinks.item.TeaLeafPlaceableItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class JSDItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(JourneyDrinks.MODID);

    // Coffee chain
    public static final DeferredHolder<Item, Item> COFFEE_CHERRY = register("coffee_cherries");
    public static final DeferredHolder<Item, Item> COFFEE_CHERRIES = COFFEE_CHERRY;
    public static final DeferredHolder<Item, Item> DRIED_COFFEE_CHERRIES = register("dried_coffee_cherries");
    public static final DeferredHolder<Item, Item> RAW_COFFEE_BEAN = register("raw_coffee_bean");
    public static final DeferredHolder<Item, Item> DRIED_COFFEE_BEAN = register("dried_coffee_bean");
    public static final DeferredHolder<Item, Item> ROASTED_COFFEE_BEAN = register("roasted_coffee_bean");
    public static final DeferredHolder<Item, Item> GROUND_COFFEE = register("ground_coffee");
    public static final DeferredHolder<Item, Item> MANUAL_MILL = register("manual_mill", () -> new Item(new Item.Properties().durability(128)));

    // Tea chain
    public static final DeferredHolder<Item, Item> FRESH_TEA_LEAF = register("fresh_tea_leaf");
    public static final DeferredHolder<Item, Item> WHITE_TEA_LEAF = register("white_tea_leaf");
    public static final DeferredHolder<Item, Item> MOIST_GREEN_TEA_LEAF = register("moist_green_tea_leaf");
    public static final DeferredHolder<Item, Item> GREEN_TEA_LEAF = register("green_tea_leaf");
    public static final DeferredHolder<Item, Item> YELLOW_TEA_LEAF = register("yellow_tea_leaf");
    public static final DeferredHolder<Item, Item> BRUISED_TEA_LEAF = register("bruised_tea_leaf", () -> new TeaLeafPlaceableItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FERMENTED_TEA_LEAF = register("fermented_tea_leaf", () -> new TeaLeafPlaceableItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RED_TEA_LEAF = register("red_tea_leaf");
    public static final DeferredHolder<Item, Item> PUERH_TEA = register("puerh_tea");
    public static final DeferredHolder<Item, Item> PUERH_TEA_LEAF = PUERH_TEA;
    public static final DeferredHolder<Item, Item> STRONG_PUERH_TEA = register("strong_puerh_tea");
    public static final DeferredHolder<Item, Item> STRONG_PUERH_TEA_LEAF = STRONG_PUERH_TEA;

    // Saplings
    public static final DeferredHolder<Item, Item> TEA_SAPLING = register("tea_sapling",
        () -> new JSDSaplingItem(JSDBlocks.TEA_BUSH.get(), JSDClimateRanges.TEA_BUSH, JSDBlocks.TEA_STAGES));
    public static final DeferredHolder<Item, Item> COFFEE_SAPLING = register("coffee_sapling",
        () -> new JSDSaplingItem(JSDBlocks.COFFEE_SAPLING.get(), JSDClimateRanges.COFFEE_TREE, JSDBlocks.COFFEE_STAGES));

    // Leaves BlockItems (for creative/JEI access)
    public static final DeferredHolder<Item, BlockItem> TEA_LEAVES_ITEM = register("plant/tea_leaves",
        () -> new BlockItem(JSDBlocks.TEA_LEAVES.get(), new Item.Properties()));
    public static final DeferredHolder<Item, BlockItem> COFFEE_LEAVES_ITEM = register("plant/coffee_leaves",
        () -> new BlockItem(JSDBlocks.COFFEE_LEAVES.get(), new Item.Properties()));


    public static DeferredHolder<Item, Item> register(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    public static <T extends Item> DeferredHolder<Item, T> register(String name, Supplier<T> supplier) {
        return ITEMS.register(name, supplier);
    }
}
