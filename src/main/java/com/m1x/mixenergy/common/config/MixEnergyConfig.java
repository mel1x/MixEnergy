package com.m1x.mixenergy.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class MixEnergyConfig {
    public static final String COMMON_FILE_NAME = "mixenergy-common.toml";
    public static final String CLIENT_FILE_NAME = "mixenergy-client.toml";

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;

    public static final ForgeConfigSpec.DoubleValue DEFAULT_MAX_ENERGY;
    public static final ForgeConfigSpec.IntValue ENERGY_REGEN_COOLDOWN_TICKS;

    public static final ForgeConfigSpec.BooleanValue ENERGY_COST_FOR_SPRINTING;
    public static final ForgeConfigSpec.BooleanValue ENERGY_COST_FOR_SWIMMING;
    public static final ForgeConfigSpec.BooleanValue ENERGY_COST_FOR_BREAKING_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue ENERGY_COST_FOR_PLACING_BLOCKS;
    public static final ForgeConfigSpec.BooleanValue ENERGY_COST_FOR_ATTACKS;
    public static final ForgeConfigSpec.BooleanValue ENERGY_COST_FOR_JUMPING;
    public static final ForgeConfigSpec.BooleanValue ENERGY_COST_FOR_COMBAT_ROLL;
    public static final ForgeConfigSpec.BooleanValue ENERGY_COST_FOR_BETTER_COMBAT;

    public static final ForgeConfigSpec.DoubleValue SPRINT_ENERGY_COST;
    public static final ForgeConfigSpec.DoubleValue FAST_SWIMMING_ENERGY_COST;
    public static final ForgeConfigSpec.DoubleValue BLOCK_BREAK_ENERGY_COST;
    public static final ForgeConfigSpec.DoubleValue BLOCK_PLACE_ENERGY_COST;
    public static final ForgeConfigSpec.DoubleValue ATTACK_ENERGY_COST;
    public static final ForgeConfigSpec.DoubleValue JUMP_ENERGY_COST;
    public static final ForgeConfigSpec.DoubleValue COMBAT_ROLL_ENERGY_COST;
    public static final ForgeConfigSpec.DoubleValue BETTER_COMBAT_ATTACK_ENERGY_COST;
    public static final ForgeConfigSpec.DoubleValue BASE_ENERGY_REGEN_RATE;
    public static final ForgeConfigSpec.DoubleValue MAX_ENERGY_REGEN_RATE;
    public static final ForgeConfigSpec.DoubleValue ENERGY_REGEN_SPEED_MULTIPLIER;

    public static final ForgeConfigSpec.EnumValue<EnergyBarPosition> ENERGY_BAR_POSITION;

    static {
        ForgeConfigSpec.Builder commonBuilder = new ForgeConfigSpec.Builder();
        commonBuilder
                .comment("MixEnergy gameplay settings. These values are authoritative on the server.")
                .push("general");

        DEFAULT_MAX_ENERGY = commonBuilder
                .comment("Maximum energy assigned to new players.")
                .defineInRange("defaultMaxEnergy", 45.0, 1.0, 1000.0);

        ENERGY_REGEN_COOLDOWN_TICKS = commonBuilder
                .comment("Delay before energy starts regenerating after a charged action, in server ticks.")
                .comment("20 ticks are one second at the normal server tick rate.")
                .defineInRange("energyRegenCooldownTicks", 30, 0, 200);

        commonBuilder.pop();
        commonBuilder
                .comment("Enable or disable individual sources of energy consumption.")
                .push("energy_sources");

        ENERGY_COST_FOR_SPRINTING = commonBuilder
                .comment("Spend energy while sprinting on land.")
                .define("sprinting", true);

        ENERGY_COST_FOR_SWIMMING = commonBuilder
                .comment("Spend energy while fast-swimming.")
                .define("fastSwimming", true);

        ENERGY_COST_FOR_BREAKING_BLOCKS = commonBuilder
                .comment("Spend energy when breaking blocks.")
                .define("breakingBlocks", true);

        ENERGY_COST_FOR_PLACING_BLOCKS = commonBuilder
                .comment("Spend energy when placing blocks.")
                .define("placingBlocks", true);

        ENERGY_COST_FOR_ATTACKS = commonBuilder
                .comment("Spend energy when attacking entities.")
                .define("attacks", true);

        ENERGY_COST_FOR_JUMPING = commonBuilder
                .comment("Spend energy when jumping.")
                .define("jumping", false);

        ENERGY_COST_FOR_COMBAT_ROLL = commonBuilder
                .comment("Spend energy for each Combat Roll.")
                .comment("This setting is used only when the Combat Roll mod is installed.")
                .define("combatRoll", true);

        ENERGY_COST_FOR_BETTER_COMBAT = commonBuilder
                .comment("Spend energy once for each Better Combat attack, including missed attacks.")
                .comment("This setting is used only when the Better Combat mod is installed.")
                .define("betterCombat", true);

        commonBuilder.pop();
        commonBuilder
                .comment("Energy spent by each enabled action.")
                .push("energy_costs");

        SPRINT_ENERGY_COST = commonBuilder
                .comment("Energy spent per server tick while sprinting.")
                .defineInRange("sprintingPerTick", 0.25, 0.0, 1000.0);

        FAST_SWIMMING_ENERGY_COST = commonBuilder
                .comment("Energy spent per server tick while fast-swimming.")
                .defineInRange("fastSwimmingPerTick", 0.25, 0.0, 1000.0);

        BLOCK_BREAK_ENERGY_COST = commonBuilder
                .comment("Energy spent for breaking a block with positive hardness.")
                .defineInRange("breakingBlock", 2.0, 0.0, 1000.0);

        BLOCK_PLACE_ENERGY_COST = commonBuilder
                .comment("Energy spent for placing a block.")
                .defineInRange("placingBlock", 1.0, 0.0, 1000.0);

        ATTACK_ENERGY_COST = commonBuilder
                .comment("Energy spent for an attack.")
                .defineInRange("attack", 3.0, 0.0, 1000.0);

        JUMP_ENERGY_COST = commonBuilder
                .comment("Energy spent for a jump when the jumping source is enabled.")
                .defineInRange("jump", 1.0, 0.0, 1000.0);

        COMBAT_ROLL_ENERGY_COST = commonBuilder
                .comment("Energy spent instantly for each Combat Roll.")
                .defineInRange("combatRoll", 15.0, 0.0, 1000.0);

        BETTER_COMBAT_ATTACK_ENERGY_COST = commonBuilder
                .comment("Energy spent instantly for each Better Combat attack or combo step.")
                .comment("The cost is charged once per swing, not once per target hit.")
                .defineInRange("betterCombatAttack", 3.0, 0.0, 1000.0);

        commonBuilder.pop();
        commonBuilder
                .comment("Energy regeneration balance.")
                .push("regeneration");

        BASE_ENERGY_REGEN_RATE = commonBuilder
                .comment("Energy restored per regeneration pulse after the cooldown.")
                .comment("A regeneration pulse normally occurs every 3 server ticks.")
                .defineInRange("baseRate", 1.0, 0.0, 1000.0);

        MAX_ENERGY_REGEN_RATE = commonBuilder
                .comment("Maximum energy restored per pulse after the idle-time boost.")
                .comment("Values below baseRate are treated as equal to baseRate.")
                .defineInRange("maxRate", 1.8, 0.0, 1000.0);

        ENERGY_REGEN_SPEED_MULTIPLIER = commonBuilder
                .comment("Multiplier applied to passive energy regeneration.")
                .comment("Set to 0 to disable passive regeneration.")
                .defineInRange("speedMultiplier", 1.0, 0.0, 5.0);

        commonBuilder.pop();
        COMMON_SPEC = commonBuilder.build();

        ForgeConfigSpec.Builder clientBuilder = new ForgeConfigSpec.Builder();
        clientBuilder
                .comment("MixEnergy client settings. These values only affect the local HUD.")
                .push("hud");

        ENERGY_BAR_POSITION = clientBuilder
                .comment("Position of the energy bar on screen.")
                .defineEnum("energyBarPosition", EnergyBarPosition.ABOVE_HOTBAR);

        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();
    }

    private MixEnergyConfig() {
    }

    public static void register() {
        MixEnergyConfigMigration.migrate();
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, COMMON_FILE_NAME);
        context.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, CLIENT_FILE_NAME);
    }

    public static void saveCommon() {
        COMMON_SPEC.save();
    }

    public static void saveClient() {
        CLIENT_SPEC.save();
    }

    public enum EnergyBarPosition {
        ABOVE_HOTBAR("above_hotbar"),
        TOP_LEFT("top_left"),
        TOP_RIGHT("top_right"),
        TOP_CENTER("top_center"),
        BOTTOM_LEFT("bottom_left"),
        BOTTOM_RIGHT("bottom_right");

        private final String serializedName;

        EnergyBarPosition(String serializedName) {
            this.serializedName = serializedName;
        }

        public String getName() {
            return serializedName;
        }
    }
}
