package com.m1x.mixenergy.common.entity;

import com.m1x.mixenergy.common.PlayerEnergyManager;
import com.m1x.mixenergy.registry.MixEnergyEntities;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
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

public class EnergyOrbEntity extends Entity {
    private static final float ENERGY_AMOUNT = 5.0f;
    private int age;
    private int health = 5;
    private int value;
    private Player followingPlayer;
    private int followingTime;
    private static final double PLAYER_DETECTION_RANGE = 16.0D;
    private static final double VERTICAL_ATTRACTION_DISTANCE = 5.0D;

    public EnergyOrbEntity(EntityType<? extends EnergyOrbEntity> entityType, Level level) {
        super(entityType, level);
    }

    public EnergyOrbEntity(Level level, double x, double y, double z) {
        this(MixEnergyEntities.ENERGY_ORB.get(), level);
        setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() {
        // No data to sync
    }

    @Override
    public void tick() {
        super.tick();
        
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        
        // Floating behavior
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, -0.5D, 1.0D));
        }
        
        // Move the entity
        this.move(MoverType.SELF, this.getDeltaMovement());
        
        // Apply friction
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.98D, 0.98D, 0.98D));
        
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
        }
        
        // Check for player to follow
        if (this.followingTime > 0) {
            --this.followingTime;
        } else {
            this.followingPlayer = this.level().getNearestPlayer(this, PLAYER_DETECTION_RANGE);
            if (this.followingPlayer != null) {
                this.followingTime = 20;
            }
        }
        
        // Follow nearby player
        if (this.followingPlayer != null && this.followingPlayer.isSpectator()) {
            this.followingPlayer = null;
        }
        
        if (this.followingPlayer != null) {
            Vec3 playerPos = new Vec3(
                this.followingPlayer.getX() - this.getX(),
                this.followingPlayer.getY() + this.followingPlayer.getEyeHeight() / 2.0D - this.getY(),
                this.followingPlayer.getZ() - this.getZ()
            );
            
            double distance = playerPos.lengthSqr();
            double horizontalDistance = new Vec3(playerPos.x, 0, playerPos.z).lengthSqr();
            
            if (distance < 1.0D) {
                // Player is close enough to pick up the orb
                if (this.followingPlayer instanceof ServerPlayer serverPlayer) {
                    // Regenerate player's energy
                    PlayerEnergyManager.regenerateEnergy(serverPlayer, ENERGY_AMOUNT);
                    
                    // Play pickup sound
                    this.level().playSound(null, this.followingPlayer.getX(), this.followingPlayer.getY(), 
                        this.followingPlayer.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, 
                        SoundSource.PLAYERS, 0.1F, 
                        0.5F * ((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.8F));
                    
                    // Remove the orb
                    this.discard();
                }
            } else if (distance < 144.0D) {
                // Move towards player at a slower rate
                double speed = 1.0D - Math.sqrt(distance) / 12.0D;
                
                // Создаем вектор движения
                Vec3 movement = playerPos.normalize().scale(speed * 0.035D);
                
                // Если игрок высоко над частицей, увеличиваем вертикальную скорость
                if (playerPos.y > VERTICAL_ATTRACTION_DISTANCE) {
                    double verticalBoost = 0.04D + (playerPos.y / 20.0D); // Больше буст для большей высоты
                    movement = new Vec3(movement.x, movement.y + verticalBoost, movement.z);
                }
                
                // Если игрок близко на горизонтальной плоскости, но выше - фокусируемся на вертикальном движении
                if (horizontalDistance < 4.0D && playerPos.y > 2.0D) {
                    movement = new Vec3(movement.x * 0.8D, movement.y * 1.5D, movement.z * 0.8D);
                }
                
                this.setDeltaMovement(this.getDeltaMovement().add(movement));
            }
        }
        
        // Уменьшаем влияние гравитации для лучшего вертикального движения
        if (!this.onGround()) {
            // Проверяем, движется ли сфера к игроку вверх
            if (this.followingPlayer != null && this.followingPlayer.getY() > this.getY() + 2.0D) {
                // Уменьшенная гравитация, когда нужно подниматься к игроку
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.01D, 0.0D));
            } else {
                // Обычная гравитация
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.03D, 0.0D));
            }
        }
        
        // Age the orb
        ++this.age;
        if (this.age >= 6000) { // Approximately 5 minutes
            this.discard();
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
    
    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag nbt) {
        this.age = nbt.getShort("Age");
        this.health = nbt.getShort("Health");
        this.value = nbt.getShort("Value");
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag nbt) {
        nbt.putShort("Age", (short)this.age);
        nbt.putShort("Health", (short)this.health);
        nbt.putShort("Value", (short)this.value);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
} 