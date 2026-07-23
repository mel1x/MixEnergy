package com.m1x.mixenergy.client;

import com.m1x.mixenergy.MixEnergy;
import com.m1x.mixenergy.client.gui.MixEnergyConfigScreen;
import com.m1x.mixenergy.client.renderer.EnergyOrbRenderer;
import com.m1x.mixenergy.registry.MixEnergyEntities;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = MixEnergy.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            EntityRenderers.register(MixEnergyEntities.ENERGY_ORB.get(), EnergyOrbRenderer::new);
            MixEnergyConfigScreen.registerConfigScreen();
        });
    }
}
