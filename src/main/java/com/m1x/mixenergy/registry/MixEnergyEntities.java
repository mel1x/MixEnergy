package com.m1x.mixenergy.registry;

import com.m1x.mixenergy.MixEnergy;
import com.m1x.mixenergy.common.entity.EnergyOrbEntity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MixEnergyEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "mixenergy");
        
    public static final RegistryObject<EntityType<EnergyOrbEntity>> ENERGY_ORB = ENTITY_TYPES.register("energy_orb",
        () -> EntityType.Builder.<EnergyOrbEntity>of(EnergyOrbEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F) // Size of the entity hitbox
            .clientTrackingRange(6) // Range at which clients will track this entity
            .updateInterval(20) // How often the entity sends update packets
            .build("energy_orb"));
} 