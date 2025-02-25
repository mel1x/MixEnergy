package com.m1x.mixenergy.common;

import com.m1x.mixenergy.common.config.MixEnergyConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class PlayerEnergyData {
    private float energy;
    private float maxEnergy;
    private long lastActionTime;
    private long lastRegenTime;

    public PlayerEnergyData() {
        float defaultMaxEnergy = MixEnergyConfig.DEFAULT_MAX_ENERGY.get().floatValue();
        this.energy = defaultMaxEnergy;
        this.maxEnergy = defaultMaxEnergy;
        this.lastActionTime = 0;
        this.lastRegenTime = 0;
    }

    public float getEnergy() { return energy; }
    public float getMaxEnergy() { return maxEnergy; }
    public long getLastActionTime() { return lastActionTime; }
    public long getLastRegenTime() { return lastRegenTime; }

    public void setEnergy(float value) {
        this.energy = Math.max(0, Math.min(value, maxEnergy));
    }

    public void setMaxEnergy(float value) {
        this.maxEnergy = value;
        this.energy = Math.min(energy, maxEnergy);
    }

    public void setLastActionTime(long time) { this.lastActionTime = time; }
    public void setLastRegenTime(long time) { this.lastRegenTime = time; }

    public void saveNBTData(CompoundTag tag) {
        tag.putFloat("energy", energy);
        tag.putFloat("maxEnergy", maxEnergy);
        tag.putLong("lastActionTime", lastActionTime);
        tag.putLong("lastRegenTime", lastRegenTime);
    }

    public void loadNBTData(CompoundTag tag) {
        energy = tag.getFloat("energy");
        maxEnergy = tag.getFloat("maxEnergy");
        lastActionTime = tag.getLong("lastActionTime");
        lastRegenTime = tag.getLong("lastRegenTime");
    }
}