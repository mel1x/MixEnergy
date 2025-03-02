package com.m1x.mixenergy.common;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mixenergy")
public class PlayerEnergyProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<PlayerEnergyData> PLAYER_ENERGY = CapabilityManager.get(new CapabilityToken<>(){});
    public static final ResourceLocation IDENTIFIER = new ResourceLocation("mixenergy", "player_energy");

    private PlayerEnergyData data;
    private LazyOptional<PlayerEnergyData> optional;
    
    private boolean invalidated = false;

    private CompoundTag lastSavedData = new CompoundTag();

    public PlayerEnergyProvider() {
        this.data = new PlayerEnergyData();
        this.optional = LazyOptional.of(() -> this.data);
    }

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerEnergyProvider provider = new PlayerEnergyProvider();
            event.addCapability(IDENTIFIER, provider);
            event.addListener(provider::invalidate);
        }
    }

    private void invalidate() {
        if (!invalidated) {
            this.lastSavedData = serializeNBT();
            optional.invalidate();
            invalidated = true;
        }
    }
    
    public void revive() {
        if (invalidated) {
            this.data = new PlayerEnergyData();
            this.optional = LazyOptional.of(() -> this.data);
            invalidated = false;
            
            if (!lastSavedData.isEmpty()) {
                deserializeNBT(lastSavedData);
                System.out.println("[MixEnergy] Revived capability with saved data: energy=" + 
                    data.getEnergy() + ", maxEnergy=" + data.getMaxEnergy());
            }
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (invalidated) {
            revive();
        }
        return PLAYER_ENERGY.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if (data != null) {
            data.saveNBTData(tag);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        if (data != null) {
            data.loadNBTData(tag);
        } else {
            this.data = new PlayerEnergyData();
            this.data.loadNBTData(tag);
            this.optional = LazyOptional.of(() -> this.data);
        }
        this.lastSavedData = tag.copy();
    }
}