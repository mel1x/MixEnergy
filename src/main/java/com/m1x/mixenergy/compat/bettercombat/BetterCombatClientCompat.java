package com.m1x.mixenergy.compat.bettercombat;

import com.m1x.mixenergy.network.EnergyActionPacket;
import com.m1x.mixenergy.network.NetworkHandler;
import net.bettercombat.api.client.BetterCombatClientEvents;

public final class BetterCombatClientCompat {
    public static final String MOD_ID = "bettercombat";

    private static boolean registered;

    private BetterCombatClientCompat() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        BetterCombatClientEvents.ATTACK_START.register((player, attackHand) ->
                NetworkHandler.INSTANCE.sendToServer(new EnergyActionPacket(
                        EnergyActionPacket.ActionType.BETTER_COMBAT_ATTACK_START
                ))
        );
    }
}
