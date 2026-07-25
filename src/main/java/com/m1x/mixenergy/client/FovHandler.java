package com.m1x.mixenergy.client;

import com.m1x.mixenergy.MixEnergy;
import com.m1x.mixenergy.registry.MixEnergyEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
*///?}

//? if forge {
@Mod.EventBusSubscriber(modid = MixEnergy.MOD_ID, value = Dist.CLIENT)
//?} else {
/*@EventBusSubscriber(modid = MixEnergy.MOD_ID, value = Dist.CLIENT)
*///?}
public class FovHandler {
    private static final float FOV_MODIFIER = 0.96f;

    // The class-level Dist.CLIENT subscriber already keeps this off the server; the
    // @OnlyIn annotation that used to be here no longer strips members at runtime.
    @SubscribeEvent
    public static void onComputeFovModifier(ComputeFovModifierEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && MixEnergyEffects.isFatigued(player)) {
            event.setNewFovModifier(event.getFovModifier() * FOV_MODIFIER);
        }
    }
}
