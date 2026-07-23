package com.m1x.mixenergy.common.entity;

import com.m1x.mixenergy.common.PlayerEnergyManager;
import com.m1x.mixenergy.registry.MixEnergyEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class EnergyOrbEntity extends Entity {
    public static final float BASE_ENERGY_AMOUNT = 5.0f;

    private static final EntityDataAccessor<Float> ENERGY_AMOUNT =
            SynchedEntityData.defineId(EnergyOrbEntity.class, EntityDataSerializers.FLOAT);
    private static final int MAX_AGE_TICKS = 6000;
    private static final int PLAYER_SEARCH_INTERVAL_TICKS = 20;
    private static final int MERGE_INTERVAL_TICKS = 20;
    private static final double PLAYER_DETECTION_RANGE = 16.0;
    private static final double VERTICAL_ATTRACTION_DISTANCE = 5.0;
    private static final double MERGE_RANGE = 1.0;

    private int age;
    private Player followingPlayer;
    private int playerSearchCooldown;

    public EnergyOrbEntity(EntityType<? extends EnergyOrbEntity> entityType, Level level) {
        super(entityType, level);
    }

    public EnergyOrbEntity(
            Level level,
            double x,
            double y,
            double z,
            float energyAmount
    ) {
        this(MixEnergyEntities.ENERGY_ORB.get(), level);
        setPos(x, y, z);
        setEnergyAmount(energyAmount);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(ENERGY_AMOUNT, BASE_ENERGY_AMOUNT);
    }

    @Override
    public void tick() {
        super.tick();

        xo = getX();
        yo = getY();
        zo = getZ();

        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().multiply(0.98, 0.98, 0.98));

        if (onGround()) {
            Vec3 bouncedMovement = getDeltaMovement().multiply(0.7, -0.5, 0.7);
            if (Math.abs(bouncedMovement.y) < 0.01) {
                bouncedMovement = new Vec3(
                        bouncedMovement.x,
                        0.0,
                        bouncedMovement.z
                );
            }
            setDeltaMovement(bouncedMovement);
        }

        updateFollowingPlayer();
        moveTowardsFollowingPlayer();

        if (!onGround()) {
            double gravity = followingPlayer != null
                    && followingPlayer.getY() > getY() + 2.0
                    ? -0.01
                    : -0.03;
            setDeltaMovement(getDeltaMovement().add(0.0, gravity, 0.0));
        }

        if (!level().isClientSide()
                && tickCount % MERGE_INTERVAL_TICKS == Math.floorMod(getId(), MERGE_INTERVAL_TICKS)) {
            mergeNearbyOrbs();
        }

        age++;
        if (age >= MAX_AGE_TICKS) {
            discard();
        }
    }

    private void updateFollowingPlayer() {
        if (followingPlayer != null
                && (!followingPlayer.isAlive()
                || followingPlayer.isSpectator()
                || distanceToSqr(followingPlayer)
                > PLAYER_DETECTION_RANGE * PLAYER_DETECTION_RANGE)) {
            followingPlayer = null;
            playerSearchCooldown = 0;
        }

        if (followingPlayer != null) {
            return;
        }

        if (playerSearchCooldown > 0) {
            playerSearchCooldown--;
            return;
        }

        followingPlayer = level().getNearestPlayer(this, PLAYER_DETECTION_RANGE);
        playerSearchCooldown = PLAYER_SEARCH_INTERVAL_TICKS;
    }

    private void moveTowardsFollowingPlayer() {
        if (followingPlayer == null || followingPlayer.isSpectator()) {
            return;
        }

        Vec3 offset = new Vec3(
                followingPlayer.getX() - getX(),
                followingPlayer.getY() + followingPlayer.getEyeHeight() / 2.0 - getY(),
                followingPlayer.getZ() - getZ()
        );
        double distanceSquared = offset.lengthSqr();

        if (distanceSquared < 1.0) {
            collect(followingPlayer);
            return;
        }
        if (distanceSquared >= 144.0) {
            return;
        }

        double attraction = 1.0 - Math.sqrt(distanceSquared) / 12.0;
        Vec3 movement = offset.normalize().scale(attraction * 0.035);
        double horizontalDistanceSquared = offset.x * offset.x + offset.z * offset.z;

        if (offset.y > VERTICAL_ATTRACTION_DISTANCE) {
            movement = movement.add(0.0, 0.04 + offset.y / 20.0, 0.0);
        }
        if (horizontalDistanceSquared < 4.0 && offset.y > 2.0) {
            movement = new Vec3(movement.x * 0.8, movement.y * 1.5, movement.z * 0.8);
        }

        setDeltaMovement(getDeltaMovement().add(movement));
    }

    private void collect(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        PlayerEnergyManager.regenerateEnergy(serverPlayer, getEnergyAmount());
        level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS,
                0.1f,
                0.5f * ((random.nextFloat() - random.nextFloat()) * 0.7f + 1.8f)
        );
        discard();
    }

    private void mergeNearbyOrbs() {
        List<EnergyOrbEntity> nearby = level().getEntitiesOfClass(
                EnergyOrbEntity.class,
                getBoundingBox().inflate(MERGE_RANGE),
                other -> other != this && other.isAlive()
        );

        for (EnergyOrbEntity other : nearby) {
            setEnergyAmount(getEnergyAmount() + other.getEnergyAmount());
            age = Math.min(age, other.age);
            other.discard();
        }
    }

    public float getEnergyAmount() {
        return entityData.get(ENERGY_AMOUNT);
    }

    private void setEnergyAmount(float energyAmount) {
        entityData.set(ENERGY_AMOUNT, Math.max(0.0f, energyAmount));
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        age = tag.getInt("Age");
        if (tag.contains("EnergyAmount")) {
            setEnergyAmount(tag.getFloat("EnergyAmount"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", age);
        tag.putFloat("EnergyAmount", getEnergyAmount());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
