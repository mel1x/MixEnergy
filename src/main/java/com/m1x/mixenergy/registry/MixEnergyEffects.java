package com.m1x.mixenergy.registry;

import com.m1x.mixenergy.common.effects.MixEnergySlownessEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MixEnergyEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = 
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, "mixenergy");

    public static final RegistryObject<MobEffect> MIX_ENERGY_SLOWNESS = MOB_EFFECTS.register("mix_energy_slowness",
        MixEnergySlownessEffect::new);
} 