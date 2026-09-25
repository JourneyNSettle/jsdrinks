package com.journey.jsdrinks;

import net.neoforged.neoforge.common.ModConfigSpec;

public class JSDConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue TEA_BUSH_TRANSFORM_YEARS;

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

        SPEC = builder.build();
    }
}
