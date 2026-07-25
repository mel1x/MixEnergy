package com.m1x.mixenergy.common.commands;

import com.m1x.mixenergy.common.PlayerEnergyData;
import com.m1x.mixenergy.common.PlayerEnergyManager;
import com.m1x.mixenergy.common.PlayerEnergyProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

public final class EnergyCommands {
    private EnergyCommands() {
    }

    /**
     * Restricts the energy commands to operators. Numeric permission levels were replaced
     * by named permission sets in 1.21.11, where level 2 is the gamemaster set.
     */
    private static Predicate<CommandSourceStack> operatorOnly() {
        //? if <1.21.11 {
        return source -> source.hasPermission(2);
        //?} else {
        /*return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
        *///?}
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("setEnergy")
                .requires(operatorOnly())
                .then(Commands.argument("value", FloatArgumentType.floatArg(0))
                        .executes(context -> setEnergy(
                                context,
                                context.getSource().getPlayerOrException(),
                                FloatArgumentType.getFloat(context, "value")
                        ))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setEnergy(
                                        context,
                                        EntityArgument.getPlayer(context, "player"),
                                        FloatArgumentType.getFloat(context, "value")
                                )))));

        dispatcher.register(Commands.literal("setMaxEnergy")
                .requires(operatorOnly())
                .then(Commands.argument("value", FloatArgumentType.floatArg(1, 1000))
                        .executes(context -> setMaxEnergy(
                                context,
                                context.getSource().getPlayerOrException(),
                                FloatArgumentType.getFloat(context, "value")
                        ))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> setMaxEnergy(
                                        context,
                                        EntityArgument.getPlayer(context, "player"),
                                        FloatArgumentType.getFloat(context, "value")
                                )))));
    }

    private static int setEnergy(
            CommandContext<CommandSourceStack> context,
            ServerPlayer player,
            float value
    ) {
        PlayerEnergyData energyData = PlayerEnergyProvider.get(player);
        if (energyData == null) {
            return 0;
        }

        energyData.setEnergy(value);
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "commands.mixenergy.set_energy.success",
                        value,
                        player.getDisplayName()
                ),
                true
        );
        PlayerEnergyManager.syncEnergyToClient(player, energyData);
        return 1;
    }

    private static int setMaxEnergy(
            CommandContext<CommandSourceStack> context,
            ServerPlayer player,
            float value
    ) {
        PlayerEnergyData energyData = PlayerEnergyProvider.get(player);
        if (energyData == null) {
            return 0;
        }

        energyData.setMaxEnergy(value);
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "commands.mixenergy.set_max_energy.success",
                        value,
                        player.getDisplayName()
                ),
                true
        );
        PlayerEnergyManager.syncEnergyToClient(player, energyData);
        return 1;
    }
}
