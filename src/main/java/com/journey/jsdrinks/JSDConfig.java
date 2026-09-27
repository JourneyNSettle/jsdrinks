package com.journey.jsdrinks;

import net.neoforged.neoforge.common.ModConfigSpec;

public class JSDConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue TEA_BUSH_TRANSFORM_YEARS;
    public static final ModConfigSpec.IntValue TEA_FRUIT_REGROWTH_DAYS;
    public static final ModConfigSpec.IntValue COFFEE_FRUIT_REGROWTH_DAYS;
    public static final ModConfigSpec.DoubleValue TEA_FROST_KILL_TEMP;
    public static final ModConfigSpec.DoubleValue COFFEE_FROST_KILL_TEMP;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("tea_bush");
        TEA_BUSH_TRANSFORM_YEARS = builder
            .comment(
                "Number of TFC calendar years without any harvest before a tea bush",
                "irreversibly transforms into a tea tree.",
                "With defaultMonthLength=30 one year ≈ 360 game-days."
            )
            .defineInRange("transformYears", 2, 1, 100);
        builder.pop();

        builder.push("fruit_regrowth");
        TEA_FRUIT_REGROWTH_DAYS = builder
            .comment(
                "Number of TFC calendar days after harvest before a tea plant",
                "can transition from FLOWERING back to FRUITING.",
                "Applies to both tea bushes and tea tree leaves."
            )
            .defineInRange("teaRegrowthDays", 3, 1, 60);
        COFFEE_FRUIT_REGROWTH_DAYS = builder
            .comment(
                "Number of TFC calendar days after harvest before a coffee tree",
                "can transition from FLOWERING back to FRUITING."
            )
            .defineInRange("coffeeRegrowthDays", 8, 1, 60);
        builder.pop();

        builder.push("frost");
        TEA_FROST_KILL_TEMP = builder
            .comment(
                "Instant temperature (°C) at or below which tea bushes and trees die.",
                "Default: -5.0 (tolerates light frost)."
            )
            .defineInRange("teaFrostKillTemp", -5.0, -50.0, 10.0);
        COFFEE_FROST_KILL_TEMP = builder
            .comment(
                "Instant temperature (°C) at or below which coffee trees die.",
                "Default: 0.0 (cannot survive any sub-zero temperature)."
            )
            .defineInRange("coffeeFrostKillTemp", 0.0, -50.0, 10.0);
        builder.pop();

        SPEC = builder.build();
    }
}
