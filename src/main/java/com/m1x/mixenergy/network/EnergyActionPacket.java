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
        REGENERATE,
        STOP_SWIMMING
    }

    public EnergyActionPacket(ActionType actionType, float amount) {
        this.actionType = actionType;
        this.amount = amount;
    }
    
    public EnergyActionPacket(ActionType actionType) {
        this(actionType, 0.0f);
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
            } else if (ctx.get().getDirection().getReceptionSide().isClient()) {
                if (msg.actionType == ActionType.STOP_SWIMMING) {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.player != null) {
                        // Полное отключение плавания на клиенте
                        mc.player.setSwimming(false);
                        
                        // Сбрасываем флаг плавания с помощью рефлексии
                        try {
                            java.lang.reflect.Field isSwimmingField = 
                                net.minecraft.world.entity.LivingEntity.class.getDeclaredField("f_20899_");
                            isSwimmingField.setAccessible(true);
                            isSwimmingField.set(mc.player, false);
                        } catch (Exception e) {
                            // Запасной вариант, если рефлексия не сработала
                            mc.player.setSwimming(false);
                        }
                        
                        // Сбрасываем состояние спринта и использования предметов
                        mc.player.setSprinting(false);
                        mc.player.stopUsingItem();
                        
                        // Если игрок в воде, можно дополнительно изменить атрибуты на клиенте
                        if (mc.player.isInWater()) {
                            try {
                                // Попытка вызвать метод плавания с false
                                java.lang.reflect.Method swimMethod = 
                                    net.minecraft.client.player.LocalPlayer.class.getDeclaredMethod("setSwimming", boolean.class);
                                swimMethod.setAccessible(true);
                                swimMethod.invoke(mc.player, false);
                            } catch (Exception e) {
                                // Если метод не найден, используем стандартный
                                mc.player.setSwimming(false);
                            }
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}