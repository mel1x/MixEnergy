package com.m1x.mixenergy.common.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
//? if forge {
import net.minecraftforge.common.ForgeMod;
//?} else {
/*import com.m1x.mixenergy.MixEnergy;
import net.neoforged.neoforge.common.NeoForgeMod;
*///?}
//? if >=1.21.2 {
/*import net.minecraft.server.level.ServerLevel;
*///?}

public class MixEnergySlownessEffect extends MobEffect {
    // Still needed up to 1.20.6: the String these calls take is parsed with
    // UUID.fromString, so it must be a UUID and not a readable name. Only 1.21 moved to
    // ResourceLocation keys.
    //? if <1.21 {
    private static final String MOVEMENT_SPEED_UUID = "7107DE5E-7CE8-4030-940E-514C1F160890";
    private static final String SWIM_SPEED_UUID = "7107DE5E-7CE8-4030-940E-514C1F160891";
    //?}

    public MixEnergySlownessEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A6C81);
        // Attribute modifiers are keyed by a UUID string up to 1.20.6 and by a
        // ResourceLocation from 1.21 - NeoForge only exists from 1.21 on, so it always
        // uses the ResourceLocation form. What changed in 1.20.5 is the attribute itself
        // becoming a Holder (hence RegistryObject#getHolder for the Forge attribute) and
        // MULTIPLY_TOTAL being renamed to ADD_MULTIPLIED_TOTAL.
        //? if <1.20.5 {
        addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_SPEED_UUID,
                -0.45,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        addAttributeModifier(
                ForgeMod.SWIM_SPEED.get(),
                SWIM_SPEED_UUID,
                -0.5,
                AttributeModifier.Operation.MULTIPLY_TOTAL
        );
        //?} elif <1.21 {
        /*addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MOVEMENT_SPEED_UUID,
                -0.45,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        addAttributeModifier(
                ForgeMod.SWIM_SPEED.getHolder().orElseThrow(),
                SWIM_SPEED_UUID,
                -0.5,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        *///?} else {
        /*addAttributeModifier(
                Attributes.MOVEMENT_SPEED,
                MixEnergy.id("mix_energy_slowness_movement"),
                -0.45,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        addAttributeModifier(
                NeoForgeMod.SWIM_SPEED,
                MixEnergy.id("mix_energy_slowness_swim"),
                -0.5,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        );
        *///?}
    }

    // applyEffectTick only started returning a boolean in 1.20.5, but the "should this
    // tick run" hook was already renamed from isDurationEffectTick to
    // shouldApplyEffectTickThisTick one release earlier, in 1.20.2.
    //? if <1.20.2 {
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        stopFastMovement(entity);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
    //?} elif <1.20.5 {
    /*@Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        stopFastMovement(entity);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
    *///?} elif <1.21.2 {
    /*@Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        stopFastMovement(entity);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
    *///?} else {
    /*@Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        stopFastMovement(entity);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
    *///?}

    private static void stopFastMovement(LivingEntity entity) {
        if (entity instanceof Player player) {
            player.setSprinting(false);
            player.setSwimming(false);
        }
    }
}
