package com.m1x.mixenergy;

import com.m1x.mixenergy.common.commands.EnergyCommands;
import com.m1x.mixenergy.common.config.MixEnergyConfig;
import com.m1x.mixenergy.network.NetworkHandler;
import com.m1x.mixenergy.registry.MixEnergyEffects;
import com.m1x.mixenergy.registry.MixEnergyEntities;
// ResourceLocation was renamed to Identifier in 1.21.11.
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation;
//?} else {
/*import net.minecraft.resources.Identifier;
*///?}
//? if combatroll {
import com.m1x.mixenergy.compat.combatroll.CombatRollCompat;
//?}
//? if forge {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//?} else {
/*import com.m1x.mixenergy.common.PlayerEnergyProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
*///?}

@Mod(MixEnergy.MOD_ID)
public class MixEnergy {
    public static final String MOD_ID = "mixenergy";

    //? if forge {
    public MixEnergy() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        MixEnergyConfig.register();
        MixEnergyEffects.MOB_EFFECTS.register(modEventBus);
        MixEnergyEntities.ENTITY_TYPES.register(modEventBus);
        NetworkHandler.register();

        MinecraftForge.EVENT_BUS.register(this);
        //? if combatroll {
        if (ModList.get().isLoaded(CombatRollCompat.MOD_ID)) {
            CombatRollCompat.register();
        }
        //?}
    }
    //?} else {
    /*public MixEnergy(IEventBus modEventBus, ModContainer modContainer) {
        MixEnergyConfig.register(modContainer);
        MixEnergyEffects.MOB_EFFECTS.register(modEventBus);
        MixEnergyEntities.ENTITY_TYPES.register(modEventBus);
        PlayerEnergyProvider.ATTACHMENT_TYPES.register(modEventBus);
        modEventBus.addListener(NetworkHandler::register);

        NeoForge.EVENT_BUS.register(this);
        //? if combatroll {
        if (ModList.get().isLoaded(CombatRollCompat.MOD_ID)) {
            CombatRollCompat.register();
        }
        //?}
    }
    *///?}

    /**
     * Builds a resource location in this mod's namespace. The public constructor was
     * replaced by a factory method in 1.21, and the type renamed to Identifier in 1.21.11.
     */
    //? if <1.21 {
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
    //?} elif <1.21.11 {
    /*public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
    *///?} else {
    /*public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
    *///?}

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EnergyCommands.register(event.getDispatcher());
    }
}
