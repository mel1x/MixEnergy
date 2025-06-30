package com.m1x.mixenergy;

import com.m1x.mixenergy.client.renderer.EnergyOrbRenderer;
import com.m1x.mixenergy.common.PlayerEnergyManager;
import com.m1x.mixenergy.common.commands.EnergyCommands;
import com.m1x.mixenergy.common.config.MixEnergyConfig;
import com.m1x.mixenergy.common.entity.EnergyOrbEntity;
import com.m1x.mixenergy.network.NetworkHandler;
import com.m1x.mixenergy.registry.MixEnergyEffects;
import com.m1x.mixenergy.registry.MixEnergyEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("mixenergy")
public class MixEnergy {
    public MixEnergy() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        MixEnergyConfig.register();
        MixEnergyEffects.MOB_EFFECTS.register(modEventBus);
        MixEnergyEntities.ENTITY_TYPES.register(modEventBus);
        
        NetworkHandler.register();
        MinecraftForge.EVENT_BUS.register(PlayerEnergyManager.class);
        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(MixEnergyEntities.ENERGY_ORB.get(), EnergyOrbRenderer::new);
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        EnergyCommands.register(event.getDispatcher());
    }
}