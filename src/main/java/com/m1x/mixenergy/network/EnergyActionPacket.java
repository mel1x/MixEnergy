package com.m1x.mixenergy.network;

import com.m1x.mixenergy.client.ClientMovementHandler;
import com.m1x.mixenergy.common.PlayerEnergyManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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
public class EnergyActionPacket {
//?} else {
/*public class EnergyActionPacket implements CustomPacketPayload {
*///?}
    //? if neoforge {
    /*public static final CustomPacketPayload.Type<EnergyActionPacket> TYPE =
            new CustomPacketPayload.Type<>(MixEnergy.id("energy_action"));

    public static final StreamCodec<FriendlyByteBuf, EnergyActionPacket> STREAM_CODEC =
            StreamCodec.of(EnergyActionPacket::encode, EnergyActionPacket::decode);
    *///?}

    private final ActionType actionType;

    public enum ActionType {
        STOP_SWIMMING,
        FAST_SWIMMING_START,
        FAST_SWIMMING_STOP,
        BETTER_COMBAT_ATTACK_START
    }

    public EnergyActionPacket(ActionType actionType) {
        this.actionType = actionType;
    }

    //? if forge {
    public static void encode(EnergyActionPacket message, FriendlyByteBuf buffer) {
    //?} else {
    /*private static void encode(FriendlyByteBuf buffer, EnergyActionPacket message) {
    *///?}
        buffer.writeEnum(message.actionType);
    }

    //? if forge {
    public static EnergyActionPacket decode(FriendlyByteBuf buffer) {
    //?} else {
    /*private static EnergyActionPacket decode(FriendlyByteBuf buffer) {
    *///?}
        return new EnergyActionPacket(buffer.readEnum(ActionType.class));
    }

    //? if forge {
    // Forge 1.20.2 replaced NetworkEvent (and the DistExecutor side-guard) with
    // CustomPayloadEvent, whose Context reports the side directly.
    //? if <1.20.2 {
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
                handleOnServer(message, context.getSender());
            }
        });
        context.setPacketHandled(true);
    }
    //?} else {
    /*public static void handle(EnergyActionPacket message, CustomPayloadEvent.Context context) {
        context.enqueueWork(() -> {
            if (context.isClientSide()) {
                if (message.actionType == ActionType.STOP_SWIMMING) {
                    ClientMovementHandler.forceStopFastMovement();
                }
                return;
            }

            if (context.getSender() != null) {
                handleOnServer(message, context.getSender());
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

    /^* Registered with {@code playBidirectional}, so this runs on both sides. ^/
    public static void handle(EnergyActionPacket message, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender) {
                handleOnServer(message, sender);
                return;
            }

            if (message.actionType == ActionType.STOP_SWIMMING) {
                ClientMovementHandler.forceStopFastMovement();
            }
        });
    }
    *///?}

    private static void handleOnServer(EnergyActionPacket message, ServerPlayer sender) {
        switch (message.actionType) {
            case FAST_SWIMMING_START ->
                    PlayerEnergyManager.setClientFastSwimming(sender, true);
            case FAST_SWIMMING_STOP ->
                    PlayerEnergyManager.setClientFastSwimming(sender, false);
            case BETTER_COMBAT_ATTACK_START ->
                    PlayerEnergyManager.beginBetterCombatAttack(sender);
            case STOP_SWIMMING -> {
            }
        }
    }
}
