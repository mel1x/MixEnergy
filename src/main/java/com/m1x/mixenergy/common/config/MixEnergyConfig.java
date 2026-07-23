package com.m1x.mixenergy.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class MixEnergyConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.DoubleValue DEFAULT_MAX_ENERGY;

    public static final ForgeConfigSpec.BooleanValue ENERGY_COST_FOR_BREAKING_BLOCKS;
    
    public static final ForgeConfigSpec.BooleanValue ENERGY_COST_FOR_ATTACKS;
    
    public static final ForgeConfigSpec.IntValue ENERGY_REGEN_COOLDOWN;
    
    public static final ForgeConfigSpec.EnumValue<EnergyBarPosition> ENERGY_BAR_POSITION;

    public enum EnergyBarPosition {
        ABOVE_HOTBAR("above_hotbar"),
        TOP_LEFT("top_left"),
        TOP_RIGHT("top_right"),
        TOP_CENTER("top_center"),
        BOTTOM_LEFT("bottom_left"),
        BOTTOM_RIGHT("bottom_right");
        
        private final String name;
        
        EnergyBarPosition(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
    }

    static {
        BUILDER.comment("MixEnergy Configuration");
        BUILDER.push("general");

        DEFAULT_MAX_ENERGY = BUILDER
                .comment("Starting value of maximum energy for players")
                .defineInRange("defaultMaxEnergy", 45.0, 1.0, 1000.0);

        ENERGY_COST_FOR_BREAKING_BLOCKS = BUILDER
                .comment("Whether energy is spent on breaking and placing blocks (true - yes, false - no)")
                .define("energyCostForBreakingBlocks", true);
                
        ENERGY_COST_FOR_ATTACKS = BUILDER
                .comment("Whether energy is spent on attacks (true - yes, false - no)")
                .define("energyCostForAttacks", true);
                
        ENERGY_REGEN_COOLDOWN = BUILDER
                .comment("Energy recovery delay time after action (in milliseconds)")
                .defineInRange("energyRegenCooldown", 1500, 0, 10000);
                
        ENERGY_BAR_POSITION = BUILDER
                .comment("Position of the energy bar on screen")
                .defineEnum("energyBarPosition", EnergyBarPosition.ABOVE_HOTBAR);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC);
    }
}