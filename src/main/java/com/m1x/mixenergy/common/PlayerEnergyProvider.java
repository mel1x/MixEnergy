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

    private final PlayerEnergyData data = new PlayerEnergyData();
    private final LazyOptional<PlayerEnergyData> optional = LazyOptional.of(() -> data);

    @SubscribeEvent
    public static void attachCapability(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerEnergyProvider provider = new PlayerEnergyProvider();
            event.addCapability(IDENTIFIER, provider);
            event.addListener(provider::invalidate);
        }
    }

    private void invalidate() {
        optional.invalidate();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return PLAYER_ENERGY.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        data.saveNBTData(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        data.loadNBTData(tag);
    }
}