package com.journey.jsdrinks.registry;

import com.journey.jsdrinks.JourneyDrinks;
import com.journey.jsdrinks.item.JSDSaplingItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class JSDItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, JourneyDrinks.MODID);

    // --- Coffee Chain ---
    public static final DeferredHolder<Item, Item> COFFEE_CHERRY = register("coffee_cherry");
    public static final DeferredHolder<Item, Item> DRIED_COFFEE_CHERRY = register("dried_coffee_cherry");
    public static final DeferredHolder<Item, Item> RAW_COFFEE_BEANS = register("raw_coffee_beans");
    public static final DeferredHolder<Item, Item> DRIED_COFFEE_BEANS = register("dried_coffee_beans");
    public static final DeferredHolder<Item, Item> ROASTED_COFFEE_BEANS = register("roasted_coffee_beans");
    public static final DeferredHolder<Item, Item> GROUND_COFFEE = register("ground_coffee");

    // --- Tea Chain ---
    public static final DeferredHolder<Item, Item> FRESH_TEA_LEAF = register("fresh_tea_leaf");
    public static final DeferredHolder<Item, Item> DRIED_WHITE_TEA = register("dried_white_tea");
    public static final DeferredHolder<Item, Item> MOIST_GREEN_TEA_LEAF = register("moist_green_tea_leaf");
    public static final DeferredHolder<Item, Item> DRIED_GREEN_TEA = register("dried_green_tea");
    public static final DeferredHolder<Item, Item> DRIED_YELLOW_TEA = register("dried_yellow_tea");
    public static final DeferredHolder<Item, Item> BRUISED_TEA_LEAF = register("bruised_tea_leaf", () -> new com.journey.jsdrinks.item.TeaLeafPlaceableItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> FERMENTED_TEA_LEAF = register("fermented_tea_leaf", () -> new com.journey.jsdrinks.item.TeaLeafPlaceableItem(new Item.Properties()));
    public static final DeferredHolder<Item, Item> DRIED_RED_TEA = register("dried_red_tea");
    public static final DeferredHolder<Item, Item> PUERH_TEA = register("puerh_tea");
    public static final DeferredHolder<Item, Item> STRONG_PUERH_TEA = register("strong_puerh_tea");

    // --- Saplings & Plants (uses JSDSaplingItem with TFC PlantableInfo climate overlay) ---
    public static final DeferredHolder<Item, Item> TEA_SAPLING = register("plant/tea_sapling", () ->
            new JSDSaplingItem(JSDBlocks.TEA_SAPLING.get(), JSDClimateRanges.TEA_TREE, JSDBlocks.TEA_STAGES));

    public static final DeferredHolder<Item, Item> COFFEE_SAPLING = register("plant/coffee_sapling", () ->
            new JSDSaplingItem(JSDBlocks.COFFEE_SAPLING.get(), JSDClimateRanges.COFFEE_TREE, JSDBlocks.COFFEE_STAGES));

    // --- Tools ---
    public static final DeferredHolder<Item, Item> MANUAL_MILL = register("manual_mill", () -> new Item(new Item.Properties().stacksTo(1)));

    private static DeferredHolder<Item, Item> register(String name) {
        return register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredHolder<Item, Item> register(String name, Supplier<Item> supplier) {
        return ITEMS.register(name, supplier);
    }
}
