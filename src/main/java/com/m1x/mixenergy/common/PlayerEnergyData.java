package com.m1x.mixenergy.common;

import com.m1x.mixenergy.common.config.MixEnergyConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class PlayerEnergyData {
    private static final String ENERGY_TAG = "energy";
    private static final String MAX_ENERGY_TAG = "maxEnergy";
    private static final String LAST_ACTION_TICK_TAG = "lastActionTick";
    private static final String LAST_REGEN_TICK_TAG = "lastRegenTick";

    private float energy;
    private float maxEnergy;
    private long lastActionTick;
    private long lastRegenTick;

    public PlayerEnergyData() {
        maxEnergy = MixEnergyConfig.DEFAULT_MAX_ENERGY.get().floatValue();
        energy = maxEnergy;
    }

    public float getEnergy() {
        return energy;
    }

    public float getMaxEnergy() {
        return maxEnergy;
    }

    public long getLastActionTick() {
        return lastActionTick;
    }

    public long getLastRegenTick() {
        return lastRegenTick;
    }

    public void setEnergy(float value) {
        energy = Mth.clamp(value, 0.0f, maxEnergy);
    }

    public void setMaxEnergy(float value) {
        maxEnergy = Math.max(1.0f, value);
        energy = Math.min(energy, maxEnergy);
    }

    public void setLastActionTick(long tick) {
        lastActionTick = Math.max(0L, tick);
    }

    public void setLastRegenTick(long tick) {
        lastRegenTick = Math.max(0L, tick);
    }

    public void saveNBTData(CompoundTag tag) {
        tag.putFloat(ENERGY_TAG, energy);
        tag.putFloat(MAX_ENERGY_TAG, maxEnergy);
        tag.putLong(LAST_ACTION_TICK_TAG, lastActionTick);
        tag.putLong(LAST_REGEN_TICK_TAG, lastRegenTick);
    }

    public void loadNBTData(CompoundTag tag) {
        float loadedMaxEnergy = tag.contains(MAX_ENERGY_TAG)
                ? tag.getFloat(MAX_ENERGY_TAG)
                : MixEnergyConfig.DEFAULT_MAX_ENERGY.get().floatValue();

        setMaxEnergy(loadedMaxEnergy);
        setEnergy(tag.contains(ENERGY_TAG) ? tag.getFloat(ENERGY_TAG) : maxEnergy);
        setLastActionTick(tag.getLong(LAST_ACTION_TICK_TAG));
        setLastRegenTick(tag.getLong(LAST_REGEN_TICK_TAG));
    }

    public void copyFrom(PlayerEnergyData source) {
        setMaxEnergy(source.getMaxEnergy());
        setEnergy(source.getEnergy());
        setLastActionTick(source.getLastActionTick());
        setLastRegenTick(source.getLastRegenTick());
    }
}
