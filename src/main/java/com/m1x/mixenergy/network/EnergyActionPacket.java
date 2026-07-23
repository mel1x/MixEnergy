package com.m1x.mixenergy.network;

import com.m1x.mixenergy.client.ClientMovementHandler;
import com.m1x.mixenergy.common.PlayerEnergyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class EnergyActionPacket {
    private final ActionType actionType;

    public enum ActionType {
        STOP_SWIMMING,
        FAST_SWIMMING_START,
        FAST_SWIMMING_STOP
    }

    public EnergyActionPacket(ActionType actionType) {
        this.actionType = actionType;
    }

    public static void encode(EnergyActionPacket message, FriendlyByteBuf buffer) {
        buffer.writeEnum(message.actionType);
    }

    public static EnergyActionPacket decode(FriendlyByteBuf buffer) {
        return new EnergyActionPacket(buffer.readEnum(ActionType.class));
    }

    public static void handle(EnergyActionPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                if (message.actionType != ActionType.STOP_SWIMMING) {
                    return;
                }
                DistExecutor.unsafeRunWhenOn(
                        Dist.CLIENT,
                        () -> ClientMovementHandler::forceStopFastMovement
                );
                return;
            }

            if (context.getSender() != null) {
                switch (message.actionType) {
                    case FAST_SWIMMING_START ->
                            PlayerEnergyManager.setClientFastSwimming(context.getSender(), true);
                    case FAST_SWIMMING_STOP ->
                            PlayerEnergyManager.setClientFastSwimming(context.getSender(), false);
                    case STOP_SWIMMING -> {
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
