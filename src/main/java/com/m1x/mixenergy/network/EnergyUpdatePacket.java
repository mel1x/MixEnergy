package com.m1x.mixenergy.network;

import com.m1x.mixenergy.client.EnergyOverlayHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class EnergyUpdatePacket {
    private final float energy;
    private final float maxEnergy;

    public EnergyUpdatePacket(float energy, float maxEnergy) {
        this.energy = energy;
        this.maxEnergy = maxEnergy;
    }

    public static void encode(EnergyUpdatePacket msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.energy);
        buf.writeFloat(msg.maxEnergy);
    }

    public static EnergyUpdatePacket decode(FriendlyByteBuf buf) {
        return new EnergyUpdatePacket(buf.readFloat(), buf.readFloat());
    }

    public static void handle(EnergyUpdatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                EnergyOverlayHandler.setEnergyValue(msg.energy);
                EnergyOverlayHandler.setMaxEnergyValue(msg.maxEnergy);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}