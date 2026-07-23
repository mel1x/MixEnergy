package com.m1x.mixenergy.common.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;

public class MixEnergySlownessEffect extends MobEffect {
    private static final String MOVEMENT_SPEED_UUID = "7107DE5E-7CE8-4030-940E-514C1F160890";
    private static final String SWIM_SPEED_UUID = "7107DE5E-7CE8-4030-940E-514C1F160891";

    public MixEnergySlownessEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A6C81);
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
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity instanceof Player player) {
            player.setSprinting(false);
            player.setSwimming(false);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
}
