package com.m1x.mixenergy.client;

import com.m1x.mixenergy.common.PlayerEnergyManager;
import com.m1x.mixenergy.network.EnergyActionPacket;
import com.m1x.mixenergy.network.NetworkHandler;
import com.m1x.mixenergy.registry.MixEnergyEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mixenergy", value = Dist.CLIENT)
public final class ClientMovementHandler {
    private static final int FAST_SWIMMING_HEARTBEAT_TICKS = 10;

    private static boolean reportedFastSwimming;
    private static int fastSwimmingHeartbeat;

    private ClientMovementHandler() {
    }

    public static void forceStopFastMovement() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.setSprinting(false);
            player.setSwimming(false);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null) {
            reportedFastSwimming = false;
            fastSwimmingHeartbeat = 0;
            return;
        }

        GameType gameMode = minecraft.gameMode.getPlayerMode();
        if (gameMode != GameType.SURVIVAL && gameMode != GameType.ADVENTURE) {
            reportFastSwimming(false);
            return;
        }

        boolean exhausted = EnergyOverlayHandler.getEnergyValue()
                < PlayerEnergyManager.SPRINT_ENERGY_THRESHOLD;
        boolean fatigued = player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get());
        if (exhausted || fatigued) {
            forceStopFastMovement();
        }

        boolean fastSwimming = !exhausted
                && !fatigued
                && player.isInWater()
                && (player.isSwimming() || player.isSprinting());
        reportFastSwimming(fastSwimming);
    }

    private static void reportFastSwimming(boolean fastSwimming) {
        boolean heartbeatDue = fastSwimming
                && ++fastSwimmingHeartbeat >= FAST_SWIMMING_HEARTBEAT_TICKS;
        if (fastSwimming != reportedFastSwimming || heartbeatDue) {
            NetworkHandler.INSTANCE.sendToServer(new EnergyActionPacket(
                    fastSwimming
                            ? EnergyActionPacket.ActionType.FAST_SWIMMING_START
                            : EnergyActionPacket.ActionType.FAST_SWIMMING_STOP
            ));
            reportedFastSwimming = fastSwimming;
            fastSwimmingHeartbeat = 0;
        } else if (!fastSwimming) {
            fastSwimmingHeartbeat = 0;
        }
    }
}
