package com.m1x.mixenergy.registry;

import com.m1x.mixenergy.MixEnergy;
import com.m1x.mixenergy.common.effects.MixEnergySlownessEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
//? if forge {
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
//?} else {
/*import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
*///?}

public final class MixEnergyEffects {
    //? if forge {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, MixEnergy.MOD_ID);

    public static final RegistryObject<MobEffect> MIX_ENERGY_SLOWNESS =
            MOB_EFFECTS.register("mix_energy_slowness", () -> new MixEnergySlownessEffect());
    //?} else {
    /*public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MixEnergy.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> MIX_ENERGY_SLOWNESS =
            MOB_EFFECTS.register("mix_energy_slowness", () -> new MixEnergySlownessEffect());
    *///?}

    private MixEnergyEffects() {
    }

    /**
     * From Minecraft 1.20.5 the effect APIs take a {@code Holder<MobEffect>} instead of
     * the effect itself, so callers go through these two helpers instead of touching
     * {@link #MIX_ENERGY_SLOWNESS} directly. NeoForge only exists from that point on, so
     * its {@code DeferredHolder} is always Holder-shaped already; Forge 1.20.1 still wants
     * the plain effect, while Forge 1.20.6 needs the same Holder as NeoForge.
     */
    public static boolean isFatigued(LivingEntity entity) {
        //? if forge {
        //? if <1.20.5 {
        return entity.hasEffect(MIX_ENERGY_SLOWNESS.get());
        //?} else {
        /*return entity.hasEffect(MIX_ENERGY_SLOWNESS.getHolder().orElseThrow());
        *///?}
        //?} else {
        /*return entity.hasEffect(MIX_ENERGY_SLOWNESS);
        *///?}
    }

    public static MobEffectInstance fatigue(int durationTicks) {
        //? if forge {
        //? if <1.20.5 {
        return new MobEffectInstance(MIX_ENERGY_SLOWNESS.get(), durationTicks, 0, false, true, true);
        //?} else {
        /*return new MobEffectInstance(
                MIX_ENERGY_SLOWNESS.getHolder().orElseThrow(), durationTicks, 0, false, true, true
        );
        *///?}
        //?} else {
        /*return new MobEffectInstance(MIX_ENERGY_SLOWNESS, durationTicks, 0, false, true, true);
        *///?}
    }
}
