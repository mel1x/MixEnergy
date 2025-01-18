package com.m1x.mixenergy.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("mixenergy", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.registerMessage(packetId++,
                EnergyUpdatePacket.class,
                EnergyUpdatePacket::encode,
                EnergyUpdatePacket::decode,
                EnergyUpdatePacket::handle);

        INSTANCE.registerMessage(packetId++,
                EnergyActionPacket.class,
                EnergyActionPacket::encode,
                EnergyActionPacket::decode,
                EnergyActionPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}