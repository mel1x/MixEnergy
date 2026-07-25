package com.m1x.mixenergy.client;

import com.m1x.mixenergy.MixEnergy;
import com.m1x.mixenergy.client.gui.MixEnergyConfigScreen;
import com.m1x.mixenergy.client.renderer.EnergyOrbRenderer;
import com.m1x.mixenergy.registry.MixEnergyEntities;
//? if bettercombat {
import com.m1x.mixenergy.compat.bettercombat.BetterCombatClientCompat;
//?}
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
// Registers the energy bar into Forge's LayeredDraw on 1.20.5+; see EnergyOverlayHandler
// for why this only exists from that release.
//? if >=1.20.5 {
/*import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
*///?}
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
*///?}

//? if forge {
@Mod.EventBusSubscriber(
        modid = MixEnergy.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
//?} else {
/*// NeoForge picks the bus from the event type, so bus= is deprecated here.
@EventBusSubscriber(
        modid = MixEnergy.MOD_ID,
        value = Dist.CLIENT
)
*///?}
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(MixEnergyEntities.ENERGY_ORB.get(), EnergyOrbRenderer::new);
    }

    //? if forge {
    //? if >=1.20.5 {
    /*@SubscribeEvent
    public static void onAddGuiOverlayLayers(AddGuiOverlayLayersEvent event) {
        event.getLayeredDraw().addAbove(
                ForgeLayeredDraw.HOTBAR,
                MixEnergy.id("energy_bar"),
                EnergyOverlayHandler::renderLayer
        );
    }
    *///?}
    //?}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MixEnergyConfigScreen.registerConfigScreen();
            //? if bettercombat {
            if (ModList.get().isLoaded(BetterCombatClientCompat.MOD_ID)) {
                BetterCombatClientCompat.register();
            }
            //?}
        });
    }
}
