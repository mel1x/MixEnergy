package com.m1x.mixenergy.network;

import com.m1x.mixenergy.common.PlayerEnergyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class EnergyActionPacket {
    private final ActionType actionType;
    private final float amount;

    public enum ActionType {
        CONSUME,
        REGENERATE
    }

    public EnergyActionPacket(ActionType actionType, float amount) {
        this.actionType = actionType;
        this.amount = amount;
    }

    public static void encode(EnergyActionPacket msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.actionType);
        buf.writeFloat(msg.amount);
    }

    public static EnergyActionPacket decode(FriendlyByteBuf buf) {
        return new EnergyActionPacket(buf.readEnum(ActionType.class), buf.readFloat());
    }

    public static void handle(EnergyActionPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    PlayerEnergyManager.handleAction(player, msg.actionType, msg.amount);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}