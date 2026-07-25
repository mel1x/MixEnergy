package com.m1x.mixenergy.common;

import com.m1x.mixenergy.MixEnergy;
import net.minecraft.world.entity.player.Player;
//? if forge {
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;
*///?}

/**
 * Storage of {@link PlayerEnergyData} on a player.
 *
 * <p>Forge 1.20.1 attaches it as a capability; NeoForge stores it as a data attachment.
 * Call sites only use {@link #get(Player)} so the difference stays inside this class.
 */
//? if forge {
@Mod.EventBusSubscriber(modid = MixEnergy.MOD_ID)
public class PlayerEnergyProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<PlayerEnergyData> PLAYER_ENERGY =
            CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation IDENTIFIER = MixEnergy.id("player_energy");

    private final PlayerEnergyData data = new PlayerEnergyData();
    private final LazyOptional<PlayerEnergyData> optional = LazyOptional.of(() -> data);

    /** Returns the player's energy data, or {@code null} when the capability is absent. */
    public static PlayerEnergyData get(Player player) {
        return player.getCapability(PLAYER_ENERGY).orElse(null);
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
        optional.invalidate();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        return PLAYER_ENERGY.orEmpty(capability, optional);
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
//?} else {
/*public final class PlayerEnergyProvider {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MixEnergy.MOD_ID);

    // AttachmentType.Builder#serialize took a Codec until 1.21.5 and a MapCodec from 1.21.6.
    //? if <1.21.6 {
    public static final Supplier<AttachmentType<PlayerEnergyData>> PLAYER_ENERGY =
            ATTACHMENT_TYPES.register("player_energy", () -> AttachmentType
                    .builder(PlayerEnergyData::new)
                    .serialize(PlayerEnergyData.MAP_CODEC.codec())
                    .build());
    //?} else {
    /^public static final Supplier<AttachmentType<PlayerEnergyData>> PLAYER_ENERGY =
            ATTACHMENT_TYPES.register("player_energy", () -> AttachmentType
                    .builder(PlayerEnergyData::new)
                    .serialize(PlayerEnergyData.MAP_CODEC)
                    .build());
    ^///?}

    private PlayerEnergyProvider() {
    }

    /^* Returns the player's energy data, creating the default value on first access. ^/
    public static PlayerEnergyData get(Player player) {
        return player.getData(PLAYER_ENERGY);
    }
}
*///?}
