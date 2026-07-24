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

    public static void encode(EnergyUpdatePacket message, FriendlyByteBuf buffer) {
        buffer.writeFloat(message.energy);
        buffer.writeFloat(message.maxEnergy);
        buffer.writeFloat(message.energyTrendPerTick);
        buffer.writeFloat(message.sprintCostPerTick);
        buffer.writeFloat(message.swimmingCostPerTick);
        buffer.writeBoolean(message.instantVisual);
    }

    public static EnergyUpdatePacket decode(FriendlyByteBuf buffer) {
        return new EnergyUpdatePacket(
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readFloat(),
                buffer.readBoolean()
        );
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
}
