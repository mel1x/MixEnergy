package com.m1x.mixenergy.common.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import java.util.UUID;

public class MixEnergySlownessEffect extends MobEffect {
    private static final UUID EFFECT_UUID = UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160890");
    private static final UUID SWIM_EFFECT_UUID = UUID.fromString("7107DE5E-7CE8-4030-940E-514C1F160891");
    private static final double SPEED_MODIFIER = -0.15;
    private static final double SWIM_SPEED_MODIFIER = -0.5;

    public MixEnergySlownessEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A6C81);
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getEffect(this).getDuration() == 1) {
            if (entity.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                AttributeMap attributes = entity.getAttributes();
                if (attributes.hasAttribute(Attributes.MOVEMENT_SPEED)) {
                    attributes.getInstance(Attributes.MOVEMENT_SPEED).removeModifier(EFFECT_UUID);
                }
            }
            
            if (entity.getAttribute(ForgeMod.SWIM_SPEED.get()) != null) {
                AttributeMap attributes = entity.getAttributes();
                if (attributes.hasAttribute(ForgeMod.SWIM_SPEED.get())) {
                    attributes.getInstance(ForgeMod.SWIM_SPEED.get())
                        .removeModifier(SWIM_EFFECT_UUID);
                }
            }
            return;
        }

        if (entity.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            AttributeModifier modifier = new AttributeModifier(EFFECT_UUID, "MixEnergy Slowness", 
                SPEED_MODIFIER * (amplifier + 1), AttributeModifier.Operation.MULTIPLY_TOTAL);
            
            AttributeMap attributes = entity.getAttributes();
            if (attributes.hasAttribute(Attributes.MOVEMENT_SPEED)) {
                if (attributes.getInstance(Attributes.MOVEMENT_SPEED).getModifier(EFFECT_UUID) != null) {
                    attributes.getInstance(Attributes.MOVEMENT_SPEED).removeModifier(EFFECT_UUID);
                }
                attributes.getInstance(Attributes.MOVEMENT_SPEED).addTransientModifier(modifier);
            }
        }
        
        if (entity.getAttribute(ForgeMod.SWIM_SPEED.get()) != null) {
            AttributeModifier swimModifier = new AttributeModifier(
                SWIM_EFFECT_UUID, 
                "MixEnergy Swim Speed Reduction", 
                SWIM_SPEED_MODIFIER,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
            
            AttributeMap attributes = entity.getAttributes();
            if (attributes.hasAttribute(ForgeMod.SWIM_SPEED.get())) {
                if (attributes.getInstance(ForgeMod.SWIM_SPEED.get())
                        .getModifier(SWIM_EFFECT_UUID) != null) {
                    attributes.getInstance(ForgeMod.SWIM_SPEED.get())
                        .removeModifier(SWIM_EFFECT_UUID);
                }
                attributes.getInstance(ForgeMod.SWIM_SPEED.get())
                    .addTransientModifier(swimModifier);
            }
        }
        
        if (entity instanceof Player player) {
            player.setSwimming(false);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true;
    }
} 