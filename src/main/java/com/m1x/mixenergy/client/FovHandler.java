package com.m1x.mixenergy.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.m1x.mixenergy.registry.MixEnergyEffects;

@Mod.EventBusSubscriber(modid = "mixenergy", value = Dist.CLIENT)
public class FovHandler {
    private static final float FOV_MODIFIER = 0.96f;

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get())) {
            event.setNewFovModifier(event.getFovModifier() * FOV_MODIFIER);
        }
    }
} 