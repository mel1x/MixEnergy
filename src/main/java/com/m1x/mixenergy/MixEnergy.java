package com.m1x.mixenergy;

import com.m1x.mixenergy.common.commands.EnergyCommands;
import com.m1x.mixenergy.compat.combatroll.CombatRollCompat;
import com.m1x.mixenergy.common.config.MixEnergyConfig;
import com.m1x.mixenergy.network.NetworkHandler;
import com.m1x.mixenergy.registry.MixEnergyEffects;
import com.m1x.mixenergy.registry.MixEnergyEntities;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MixEnergy.MOD_ID)
public class MixEnergy {
    public static final String MOD_ID = "mixenergy";

    public MixEnergy() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        MixEnergyConfig.register();
        MixEnergyEffects.MOB_EFFECTS.register(modEventBus);
        MixEnergyEntities.ENTITY_TYPES.register(modEventBus);
        NetworkHandler.register();

        MinecraftForge.EVENT_BUS.register(this);
        if (ModList.get().isLoaded(CombatRollCompat.MOD_ID)) {
            CombatRollCompat.register();
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EnergyCommands.register(event.getDispatcher());
    }
}
