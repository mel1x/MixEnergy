package com.m1x.mixenergy.client;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void updateEnergy(
            float energy,
            float maxEnergy,
            float energyTrendPerTick,
            float sprintCostPerTick,
            float swimmingCostPerTick,
            boolean instantVisual
    ) {
        EnergyOverlayHandler.applyServerUpdate(
                energy,
                maxEnergy,
                energyTrendPerTick,
                sprintCostPerTick,
                swimmingCostPerTick,
                instantVisual
        );
    }
}
