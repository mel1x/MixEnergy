package com.m1x.mixenergy.compat.combatroll;

import com.m1x.mixenergy.common.PlayerEnergyManager;
import net.combatroll.api.event.ServerSideRollEvents;

public final class CombatRollCompat {
    public static final String MOD_ID = "combatroll";

    private CombatRollCompat() {
    }

    public static void register() {
        ServerSideRollEvents.PLAYER_START_ROLLING.register(
                (player, velocity) -> PlayerEnergyManager.consumeCombatRollEnergy(player)
        );
    }
}
