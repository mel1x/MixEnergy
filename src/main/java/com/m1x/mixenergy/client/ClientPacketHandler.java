package com.m1x.mixenergy.client;

public final class ClientPacketHandler {
    private ClientPacketHandler() {
    }

    public static void updateEnergy(float energy, float maxEnergy) {
        EnergyOverlayHandler.setMaxEnergyValue(maxEnergy);
        EnergyOverlayHandler.setEnergyValue(energy);
    }
}
