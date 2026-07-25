package com.m1x.mixenergy.compat.combatroll;

// Compiled only for targets whose gradle.properties declares deps_combatroll.
//? if combatroll {
import com.m1x.mixenergy.common.PlayerEnergyManager;
// Combat Roll renamed its API package from net.combatroll to net.combat_roll in 2.0,
// which is the first release for the NeoForge targets.
//? if forge {
import net.combatroll.api.event.ServerSideRollEvents;
//?} else {
/*import net.combat_roll.api.event.ServerSideRollEvents;
*///?}

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
//?}
