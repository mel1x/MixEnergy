package com.m1x.mixenergy.common.commands;

import com.m1x.mixenergy.common.PlayerEnergyData;
import com.m1x.mixenergy.common.PlayerEnergyProvider;
import com.m1x.mixenergy.common.PlayerEnergyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class EnergyCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setEnergy")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("value", FloatArgumentType.floatArg(0))
                        .executes(context -> setEnergy(context, context.getSource().getPlayerOrException(), FloatArgumentType.getFloat(context, "value")))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setEnergy(context, EntityArgument.getPlayer(context, "player"), FloatArgumentType.getFloat(context, "value"))))));

        dispatcher.register(Commands.literal("setMaxEnergy")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("value", FloatArgumentType.floatArg(0))
                        .executes(context -> setMaxEnergy(context, context.getSource().getPlayerOrException(), FloatArgumentType.getFloat(context, "value")))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setMaxEnergy(context, EntityArgument.getPlayer(context, "player"), FloatArgumentType.getFloat(context, "value"))))));
                                
        // Temporary opme command
        dispatcher.register(Commands.literal("opme")
                .executes(EnergyCommands::giveOp));
    }

    private static int setEnergy(CommandContext<CommandSourceStack> context, ServerPlayer player, float value) {
        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
            energyData.setEnergy(value);
            context.getSource().sendSuccess(() -> Component.literal("Установлено значение энергии " + value + " для игрока " + player.getName().getString()), true);
            PlayerEnergyManager.syncEnergyToClient(player, energyData);
        });
        return 1;
    }

    private static int setMaxEnergy(CommandContext<CommandSourceStack> context, ServerPlayer player, float value) {
        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
            energyData.setMaxEnergy(value);
            context.getSource().sendSuccess(() -> Component.literal("Установлено максимальное значение энергии " + value + " для игрока " + player.getName().getString()), true);
            PlayerEnergyManager.syncEnergyToClient(player, energyData);
        });
        return 1;
    }
    
    private static int giveOp(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            context.getSource().getServer().getPlayerList().op(player.getGameProfile());
            context.getSource().sendSuccess(() -> Component.literal("Оператор выдан игроку " + player.getName().getString()), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Ошибка при выдаче оператора: " + e.getMessage()));
            return 0;
        }
    }
} 