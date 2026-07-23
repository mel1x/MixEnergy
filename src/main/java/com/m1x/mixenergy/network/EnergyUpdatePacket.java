package com.m1x.mixenergy.network;

import com.m1x.mixenergy.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EnergyUpdatePacket {
    private final float energy;
    private final float maxEnergy;

    public EnergyUpdatePacket(float energy, float maxEnergy) {
        this.energy = energy;
        this.maxEnergy = maxEnergy;
    }

    public static void encode(EnergyUpdatePacket message, FriendlyByteBuf buffer) {
        buffer.writeFloat(message.energy);
        buffer.writeFloat(message.maxEnergy);
    }

    public static EnergyUpdatePacket decode(FriendlyByteBuf buffer) {
        return new EnergyUpdatePacket(buffer.readFloat(), buffer.readFloat());
    }

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
                                message.maxEnergy
                        )
                );
            }
        });
        context.setPacketHandled(true);
    }
}
