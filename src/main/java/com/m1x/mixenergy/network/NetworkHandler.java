package com.m1x.mixenergy.network;

import com.m1x.mixenergy.MixEnergy;
import net.minecraft.server.level.ServerPlayer;
//? if forge {
import net.minecraftforge.network.PacketDistributor;
// Forge rewrote its networking for 1.20.2, dropping net.minecraftforge.network.simple in
// favor of a ChannelBuilder in the base package, alongside NetworkRegistry.newSimpleChannel
// and NetworkEvent. Forge 1.20.1 - the only release this project still targets from before
// that rewrite - needs the old imports.
//? if <1.20.2 {
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
//?} else {
/*import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
*///?}
//?} else {
/*import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
// NeoForge's payload-handler split (this class, and playBidirectional taking two
// handlers) landed in the 21.7.x line, one Minecraft patch after the GUI rendering
// overhaul that most of the other 1.21.6 predicates in this project key off of. NeoForge
// 21.6.20-beta - the only release for Minecraft 1.21.6 - still uses the older API.
//? if >=1.21.7 {
/^import net.neoforged.neoforge.client.network.ClientPacketDistributor;
^///?}
*///?}

/**
 * Packet registration and dispatch.
 *
 * <p>Forge 1.20.1 uses a {@code SimpleChannel} built through {@code NetworkRegistry};
 * Forge 1.20.2+ moved the same class to a {@code ChannelBuilder}; NeoForge replaced it
 * with typed payloads registered through {@code RegisterPayloadHandlersEvent}. All three
 * expose the same {@link #sendToPlayer} / {@link #sendToServer} pair to the rest of the mod.
 */
public final class NetworkHandler {
    // 6 added the sprint reports to EnergyActionPacket.ActionType. The enum travels as an
    // ordinal, so a client and a server that disagree about its contents must not connect.
    private static final String PROTOCOL_VERSION = "6";

    //? if forge {
    //? if <1.20.2 {
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            MixEnergy.id("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    //?} else {
    /*public static final SimpleChannel INSTANCE = ChannelBuilder
            .named(MixEnergy.id("main"))
            .networkProtocolVersion(Integer.parseInt(PROTOCOL_VERSION))
            .acceptedVersions(Channel.VersionTest.exact(Integer.parseInt(PROTOCOL_VERSION)))
            .simpleChannel();
    *///?}

    private static int packetId = 0;

    private NetworkHandler() {
    }

    public static void register() {
        //? if <1.20.2 {
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
        //?} else {
        /*INSTANCE.messageBuilder(EnergyUpdatePacket.class, packetId++)
                .encoder(EnergyUpdatePacket::encode)
                .decoder(EnergyUpdatePacket::decode)
                .consumerMainThread(EnergyUpdatePacket::handle)
                .add();

        INSTANCE.messageBuilder(EnergyActionPacket.class, packetId++)
                .encoder(EnergyActionPacket::encode)
                .decoder(EnergyActionPacket::decode)
                .consumerMainThread(EnergyActionPacket::handle)
                .add();
        *///?}
    }

    //? if <1.20.2 {
    public static void sendToPlayer(ServerPlayer player, Object packet) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
    //?} else {
    /*public static void sendToPlayer(ServerPlayer player, Object packet) {
        INSTANCE.send(packet, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToServer(Object packet) {
        INSTANCE.send(packet, PacketDistributor.SERVER.noArg());
    }
    *///?}
    //?} else {
    /*private NetworkHandler() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                EnergyUpdatePacket.TYPE,
                EnergyUpdatePacket.STREAM_CODEC,
                EnergyUpdatePacket::handle);

        // Carries STOP_SWIMMING to the client and the movement/attack reports back.
        // Up to Minecraft 1.21.6 (NeoForge 21.6.x) a single handler covered both
        // directions. From NeoForge 21.7.x the three-argument form registers the
        // serverbound direction only and leaves the clientbound one unhandled, which fails
        // the loader's startup check, so both are passed.
        //? if <1.21.7 {
        registrar.playBidirectional(
                EnergyActionPacket.TYPE,
                EnergyActionPacket.STREAM_CODEC,
                EnergyActionPacket::handle);
        //?} else {
        /^registrar.playBidirectional(
                EnergyActionPacket.TYPE,
                EnergyActionPacket.STREAM_CODEC,
                EnergyActionPacket::handle,
                EnergyActionPacket::handle);
        ^///?}
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    // Sending to the server moved to a client-only distributor in NeoForge 21.7.x.
    //? if <1.21.7 {
    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }
    //?} else {
    /^public static void sendToServer(CustomPacketPayload packet) {
        ClientPacketDistributor.sendToServer(packet);
    }
    ^///?}
    *///?}
}
