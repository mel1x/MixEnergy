package com.m1x.mixenergy.common;

import com.m1x.mixenergy.common.config.MixEnergyConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;

@AutoRegisterCapability
public class PlayerEnergyData {
    private float energy;
    private float maxEnergy;
    private long lastActionTime;
    private long lastRegenTime;
    
    private float lastKnownEnergy;
    private float lastKnownMaxEnergy;
    private boolean isInitialized = false;

    public PlayerEnergyData() {
        float defaultMaxEnergy = MixEnergyConfig.DEFAULT_MAX_ENERGY.get().floatValue();
        this.energy = defaultMaxEnergy;
        this.maxEnergy = defaultMaxEnergy;
        this.lastActionTime = 0;
        this.lastRegenTime = 0;
        
        this.lastKnownEnergy = defaultMaxEnergy;
        this.lastKnownMaxEnergy = defaultMaxEnergy;
        this.isInitialized = true;
    }

    public float getEnergy() { 
        if (energy <= 0 && lastKnownEnergy > 0 && isInitialized) {
            System.out.println("[MixEnergy] Recovered energy from 0 to " + lastKnownEnergy);
            energy = lastKnownEnergy;
        }
        return energy; 
    }
    
    public float getMaxEnergy() { 
        if (maxEnergy <= 0 && lastKnownMaxEnergy > 0 && isInitialized) {
            System.out.println("[MixEnergy] Recovered maxEnergy from 0 to " + lastKnownMaxEnergy);
            maxEnergy = lastKnownMaxEnergy;
        }
        return maxEnergy; 
    }
    
    public long getLastActionTime() { return lastActionTime; }
    public long getLastRegenTime() { return lastRegenTime; }

    public void setEnergy(float value) {
        if (value > 0) {
            this.lastKnownEnergy = value;
        }
        this.energy = Math.max(0, Math.min(value, maxEnergy));
    }

    public void setMaxEnergy(float value) {
        if (value > 0) {
            this.lastKnownMaxEnergy = value;
        }
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
        
        tag.putFloat("lastKnownEnergy", lastKnownEnergy);
        tag.putFloat("lastKnownMaxEnergy", lastKnownMaxEnergy);
    }

    public void loadNBTData(CompoundTag tag) {
        if (tag.contains("energy")) {
            energy = tag.getFloat("energy");
            if (energy > 0) {
                lastKnownEnergy = energy;
            }
        }
        
        if (tag.contains("maxEnergy")) {
            maxEnergy = tag.getFloat("maxEnergy");
            if (maxEnergy > 0) {
                lastKnownMaxEnergy = maxEnergy;
            }
        }
        
        lastActionTime = tag.getLong("lastActionTime");
        lastRegenTime = tag.getLong("lastRegenTime");
        
        if (tag.contains("lastKnownEnergy")) {
            lastKnownEnergy = tag.getFloat("lastKnownEnergy");
        }
        if (tag.contains("lastKnownMaxEnergy")) {
            lastKnownMaxEnergy = tag.getFloat("lastKnownMaxEnergy");
        }
        
        if (energy <= 0 && lastKnownEnergy > 0) {
            System.out.println("[MixEnergy] Fixed invalid energy value during load");
            energy = lastKnownEnergy;
        }
        
        if (maxEnergy <= 0 && lastKnownMaxEnergy > 0) {
            System.out.println("[MixEnergy] Fixed invalid maxEnergy value during load");
            maxEnergy = lastKnownMaxEnergy;
        }
        
        isInitialized = true;
    }
}