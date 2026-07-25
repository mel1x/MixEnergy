package com.m1x.mixenergy.registry;

import com.m1x.mixenergy.MixEnergy;
import com.m1x.mixenergy.common.entity.EnergyOrbEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
//? if forge {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
//?} else {
/*import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}
//? if >=1.21.2 {
/*import net.minecraft.resources.ResourceKey;
*///?}

public final class MixEnergyEntities {
    //? if forge {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MixEnergy.MOD_ID);

    public static final RegistryObject<EntityType<EnergyOrbEntity>> ENERGY_ORB =
            ENTITY_TYPES.register("energy_orb", MixEnergyEntities::buildEnergyOrb);
    //?} else {
    /*public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, MixEnergy.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<EnergyOrbEntity>> ENERGY_ORB =
            ENTITY_TYPES.register("energy_orb", () -> buildEnergyOrb());
    *///?}

    private MixEnergyEntities() {
    }

    private static EntityType<EnergyOrbEntity> buildEnergyOrb() {
        EntityType.Builder<EnergyOrbEntity> builder =
                EntityType.Builder.<EnergyOrbEntity>of(EnergyOrbEntity::new, MobCategory.MISC)
                        .sized(0.5F, 0.5F) // Size of the entity hitbox
                        .clientTrackingRange(6) // Range at which clients will track this entity
                        .updateInterval(2); // Smooth movement while the orb is attracted to a player

        // EntityType.Builder#build takes the registry key instead of a plain id from 1.21.2.
        //? if >=1.21.2 {
        /*return builder.build(ResourceKey.create(Registries.ENTITY_TYPE, MixEnergy.id("energy_orb")));
        *///?} else {
        return builder.build("energy_orb");
        //?}
    }
}
