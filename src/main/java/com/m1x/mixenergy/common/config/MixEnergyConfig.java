package com.m1x.mixenergy.common.config;

//? if forge {
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.DoubleValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
//?} else {
/*import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
*///?}

/**
 * Config definition shared by every supported version. Forge calls the spec type
 * {@code ForgeConfigSpec} and NeoForge calls it {@code ModConfigSpec}; the nested value
 * types are identical, so they are imported directly and used unqualified below.
 */
public final class MixEnergyConfig {
    public static final String COMMON_FILE_NAME = "mixenergy-common.toml";
    public static final String CLIENT_FILE_NAME = "mixenergy-client.toml";

    //? if forge {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec CLIENT_SPEC;
    //?} else {
    /*public static final ModConfigSpec COMMON_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;
    *///?}

    public static final DoubleValue DEFAULT_MAX_ENERGY;
    public static final IntValue ENERGY_REGEN_COOLDOWN_TICKS;

    public static final BooleanValue ENERGY_COST_FOR_SPRINTING;
    public static final BooleanValue ENERGY_COST_FOR_SWIMMING;
    public static final BooleanValue ENERGY_COST_FOR_BREAKING_BLOCKS;
    public static final BooleanValue ENERGY_COST_FOR_PLACING_BLOCKS;
    public static final BooleanValue ENERGY_COST_FOR_ATTACKS;
    public static final BooleanValue ENERGY_COST_FOR_JUMPING;
    public static final BooleanValue ENERGY_COST_FOR_COMBAT_ROLL;
    public static final BooleanValue ENERGY_COST_FOR_BETTER_COMBAT;

    public static final DoubleValue SPRINT_ENERGY_COST;
    public static final DoubleValue FAST_SWIMMING_ENERGY_COST;
    public static final DoubleValue BLOCK_BREAK_ENERGY_COST;
    public static final DoubleValue BLOCK_PLACE_ENERGY_COST;
    public static final DoubleValue ATTACK_ENERGY_COST;
    public static final DoubleValue JUMP_ENERGY_COST;
    public static final DoubleValue COMBAT_ROLL_ENERGY_COST;
    public static final DoubleValue BETTER_COMBAT_ATTACK_ENERGY_COST;
    public static final DoubleValue BASE_ENERGY_REGEN_RATE;
    public static final DoubleValue MAX_ENERGY_REGEN_RATE;
    public static final DoubleValue ENERGY_REGEN_SPEED_MULTIPLIER;
    public static final DoubleValue CONSUMABLE_ENERGY_RESTORE;

    public static final EnumValue<EnergyBarPosition> ENERGY_BAR_POSITION;
    public static final EnumValue<EnergyBarSkin> ENERGY_BAR_SKIN;

    static {
        Builder commonBuilder = new Builder();
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

        CONSUMABLE_ENERGY_RESTORE = commonBuilder
                .comment("Energy restored when a food item, potion or other drink is consumed.")
                .comment("Set to 0 to disable energy restoration from consumables.")
                .defineInRange("consumableRestore", 8.0, 0.0, 1000.0);

        commonBuilder.pop();
        COMMON_SPEC = commonBuilder.build();

        Builder clientBuilder = new Builder();
        clientBuilder
                .comment("MixEnergy client settings. These values only affect the local HUD.")
                .push("hud");

        ENERGY_BAR_POSITION = clientBuilder
                .comment("Position of the energy bar on screen.")
                .defineEnum("energyBarPosition", EnergyBarPosition.ABOVE_HOTBAR);

        ENERGY_BAR_SKIN = clientBuilder
                .comment("Texture set the energy bar is drawn with.")
                .defineEnum("energyBarSkin", EnergyBarSkin.DEFAULT);

        clientBuilder.pop();
        CLIENT_SPEC = clientBuilder.build();
    }

    private MixEnergyConfig() {
    }

    //? if forge {
    public static void register() {
        MixEnergyConfigMigration.migrate();
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, COMMON_FILE_NAME);
        context.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, CLIENT_FILE_NAME);
    }
    //?} else {
    /*public static void register(ModContainer container) {
        MixEnergyConfigMigration.migrate();
        container.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC, COMMON_FILE_NAME);
        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC, CLIENT_FILE_NAME);
    }
    *///?}

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

    /**
     * Texture sets the energy bar can be drawn with. Every skin ships the same file names
     * with the same pixel geometry, so the renderer only swaps the directory it loads them
     * from; {@link #DEFAULT} is the set that lives directly in {@code gui/energy_bar}.
     */
    public enum EnergyBarSkin {
        DEFAULT("default", ""),
        AQUA("aqua", "aqua/"),
        AMETHYST("amethyst", "amethyst/");

        private final String serializedName;
        private final String textureDirectory;

        EnergyBarSkin(String serializedName, String textureDirectory) {
            this.serializedName = serializedName;
            this.textureDirectory = textureDirectory;
        }

        public String getName() {
            return serializedName;
        }

        public String getTextureDirectory() {
            return textureDirectory;
        }
    }
}
