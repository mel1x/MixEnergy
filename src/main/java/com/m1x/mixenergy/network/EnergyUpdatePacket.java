package com.m1x.mixenergy.network;

import com.m1x.mixenergy.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
//? if forge {
//? if <1.20.2 {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
//?} else {
/*import net.minecraftforge.event.network.CustomPayloadEvent;
*///?}
//?} else {
/*import com.m1x.mixenergy.MixEnergy;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
*///?}

//? if forge {
public class EnergyUpdatePacket {
//?} else {
/*public class EnergyUpdatePacket implements CustomPacketPayload {
*///?}
    //? if neoforge {
    /*public static final CustomPacketPayload.Type<EnergyUpdatePacket> TYPE =
            new CustomPacketPayload.Type<>(MixEnergy.id("energy_update"));

    public static final StreamCodec<FriendlyByteBuf, EnergyUpdatePacket> STREAM_CODEC =
            StreamCodec.of(EnergyUpdatePacket::encode, EnergyUpdatePacket::decode);
    *///?}

    private final float energy;
    private final float maxEnergy;
    private final float energyTrendPerTick;
    private final float sprintCostPerTick;
    private final float swimmingCostPerTick;
    private final boolean instantVisual;

    public EnergyUpdatePacket(
            float energy,
            float maxEnergy,
            float energyTrendPerTick,
            float sprintCostPerTick,
            float swimmingCostPerTick,
            boolean instantVisual
    ) {
        this.energy = energy;
        this.maxEnergy = maxEnergy;
        this.energyTrendPerTick = energyTrendPerTick;
        this.sprintCostPerTick = sprintCostPerTick;
        this.swimmingCostPerTick = swimmingCostPerTick;
        this.instantVisual = instantVisual;
    }

    //? if forge {
    public static void encode(EnergyUpdatePacket message, FriendlyByteBuf buffer) {
    //?} else {
    /*private static void encode(FriendlyByteBuf buffer, EnergyUpdatePacket message) {
    *///?}
        buffer.writeFloat(message.energy);
        buffer.writeFloat(message.maxEnergy);
        buffer.writeFloat(message.energyTrendPerTick);
        buffer.writeFloat(message.sprintCostPerTick);
        buffer.writeFloat(message.swimmingCostPerTick);
        buffer.writeBoolean(message.instantVisual);
    }

    //? if forge {
    public static EnergyUpdatePacket decode(FriendlyByteBuf buffer) {
    //?} else {
    /*private static EnergyUpdatePacket decode(FriendlyByteBuf buffer) {
    *///?}
        return new EnergyUpdatePacket(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean()
        );
    }

    //? if forge {
    // Forge 1.20.2 replaced NetworkEvent (and the DistExecutor side-guard) with
    // CustomPayloadEvent, whose Context reports the side directly.
    //? if <1.20.2 {
    public static void handle(
            EnergyUpdatePacket message,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> () -> ClientPacketHandler.updateEnergy(
                                message.energy,
                                message.maxEnergy,
                                message.energyTrendPerTick,
                                message.sprintCostPerTick,
                                message.swimmingCostPerTick,
                                message.instantVisual
                        )
                );
            }
        });
        context.setPacketHandled(true);
    }
    //?} else {
    /*public static void handle(EnergyUpdatePacket message, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            if (context.isClientSide()) {
                ClientPacketHandler.updateEnergy(
                        message.energy,
                        message.maxEnergy,
                        message.energyTrendPerTick,
                        message.sprintCostPerTick,
                        message.swimmingCostPerTick,
                        message.instantVisual
                );
            }
        });
        context.setPacketHandled(true);
    }
    *///?}
    //?} else {
    /*@Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /^* Registered with {@code playToClient}, so this only ever runs on the client. ^/
    public static void handle(EnergyUpdatePacket message, IPayloadContext context) {
        context.enqueueWork(() -> ClientPacketHandler.updateEnergy(
                message.energy,
                message.maxEnergy,
                message.energyTrendPerTick,
                message.sprintCostPerTick,
                message.swimmingCostPerTick,
                message.instantVisual
        ));
    }
    *///?}
}
