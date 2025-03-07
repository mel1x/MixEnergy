package com.m1x.mixenergy.common;

import com.m1x.mixenergy.common.config.MixEnergyConfig;
import com.m1x.mixenergy.network.EnergyActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import com.m1x.mixenergy.network.NetworkHandler;
import com.m1x.mixenergy.network.EnergyUpdatePacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Iterator;

@Mod.EventBusSubscriber(modid = "mixenergy")
public class PlayerEnergyManager {
    private static final float SPRINT_ENERGY_COST = 0.25f;
    private static final float BLOCK_BREAK_ENERGY_COST = 2.0f;
    private static final float BLOCK_PLACE_ENERGY_COST = 1.0f;
    private static final float ATTACK_ENERGY_COST = 3.0f;

    private static final int SLOWDOWN_DURATION = 100;
    private static final int SLOWDOWN_AMPLIFIER = 2;
    private static final Map<UUID, Boolean> playerSprintingMap = new HashMap<>();
    private static final Map<UUID, Long> sprintCooldownMap = new HashMap<>(); // Track when players can sprint again

    private static final float BASE_ENERGY_REGEN_RATE = 1.0f;
    private static final float MAX_ENERGY_REGEN_RATE = 1.8f;
    private static final int REGEN_DELAY_TICKS = 30;
    private static final int MAX_REGEN_BOOST_TIME = 3000;

    private static final Map<UUID, Float> playerMaxEnergyMap = new HashMap<>();
    private static final Map<UUID, Integer> pendingDimensionSyncs = new HashMap<>();
    private static final Map<UUID, CompoundTag> playerEnergyDataMap = new HashMap<>();

    private static final float EPSILON = 0.001f; // Small epsilon for floating point comparisons
    private static final float SPRINT_ENERGY_THRESHOLD = 0.5f; // Higher threshold to catch energy depletion earlier

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            Player player = event.player;

            if (event.side != LogicalSide.SERVER) {
                return;
            }

            ServerPlayer serverPlayer = (ServerPlayer) player;
            if (!isValidGameMode(player)) {
                return;
            }

            PlayerEnergyData energyData = player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).orElse(null);
            
            if (energyData == null) {
                if (!player.level().isClientSide()) {
                    ensureCapabilityIntegrity(serverPlayer);
                    energyData = player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).orElse(null);
                    if (energyData == null) return;
                } else {
                    return;
                }
            }

            long currentTime = System.currentTimeMillis();
            float currentEnergy = energyData.getEnergy();
            
            // FORCIBLY CHECK AND HANDLE SPRINTING
            // This direct approach should catch all cases including when energy depletes in one go
            if (player.isSprinting()) {
                // If energy is below threshold or player is on cooldown, force stop sprint and apply slowness
                UUID playerUUID = player.getUUID();
                long sprintCooldownEnd = sprintCooldownMap.getOrDefault(playerUUID, 0L);
                
                if (currentEnergy < SPRINT_ENERGY_THRESHOLD || currentTime < sprintCooldownEnd) {
                    // AGGRESSIVE SPRINT STOPPING
                    player.setSprinting(false);
                    // Force apply slowness with particle effects
                    MobEffectInstance slowness = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 
                                                                     SLOWDOWN_DURATION, 
                                                                     SLOWDOWN_AMPLIFIER, 
                                                                     false, // ambient
                                                                     true,  // visible
                                                                     true); // show icon
                    player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                    player.addEffect(slowness);
                    
                    // Network sync to ensure client sees the sprint stop
                    NetworkHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> serverPlayer),
                        new EnergyUpdatePacket(energyData.getEnergy(), energyData.getMaxEnergy())
                    );
                    
                    // Mark this player as no longer sprinting and set cooldown if energy is too low
                    playerSprintingMap.put(playerUUID, false);
                    if (currentEnergy < SPRINT_ENERGY_THRESHOLD) {
                        // Set cooldown equal to regeneration delay
                        sprintCooldownMap.put(playerUUID, currentTime + MixEnergyConfig.ENERGY_REGEN_COOLDOWN.get());
                    }
                } else {
                    // Consume energy for sprinting
                    energyData.setEnergy(currentEnergy - SPRINT_ENERGY_COST);
                    energyData.setLastActionTime(currentTime);
                    playerSprintingMap.put(playerUUID, true);
                    
                    // Double-check if this tick's consumption depleted energy
                    if (energyData.getEnergy() < SPRINT_ENERGY_THRESHOLD) {
                        player.setSprinting(false);
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 
                                                              SLOWDOWN_DURATION, 
                                                              SLOWDOWN_AMPLIFIER, 
                                                              false, true, true));
                        playerSprintingMap.put(playerUUID, false);
                        // Set cooldown equal to regeneration delay
                        sprintCooldownMap.put(playerUUID, currentTime + MixEnergyConfig.ENERGY_REGEN_COOLDOWN.get());
                    }
                }
            } else {
                // Not sprinting, but check if player just stopped sprinting
                UUID playerUUID = player.getUUID();
                boolean wasSprinting = playerSprintingMap.getOrDefault(playerUUID, false);
                
                if (wasSprinting && currentEnergy < SPRINT_ENERGY_THRESHOLD) {
                    // Reapply slowness to ensure effect persists
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 
                                                          SLOWDOWN_DURATION, 
                                                          SLOWDOWN_AMPLIFIER, 
                                                          false, true, true));
                }
                playerSprintingMap.put(playerUUID, false);
            }

            // Energy regeneration
            if (canRegenerate(currentTime, energyData)) {
                if (currentTime - energyData.getLastRegenTime() >= 120) {
                    float regenMultiplier = calculateRegenMultiplier(currentTime, energyData);
                    float regenAmount = BASE_ENERGY_REGEN_RATE +
                            (MAX_ENERGY_REGEN_RATE - BASE_ENERGY_REGEN_RATE) * regenMultiplier;

                    float newEnergy = Math.min(energyData.getEnergy() + regenAmount, energyData.getMaxEnergy());
                    energyData.setEnergy(newEnergy);
                    energyData.setLastRegenTime(currentTime);
                }
            }

            // LAST RESORT - Force stop sprint if energy is too low or player is on cooldown
            UUID playerUUID = player.getUUID();
            long sprintCooldownEnd = sprintCooldownMap.getOrDefault(playerUUID, 0L);
            
            if (player.isSprinting() && (energyData.getEnergy() < SPRINT_ENERGY_THRESHOLD || currentTime < sprintCooldownEnd)) {
                player.setSprinting(false);
                
                // Apply slowness and also mining fatigue to indicate sprint is on cooldown
                if (currentTime < sprintCooldownEnd) {
                    // Calculate remaining cooldown time
                    int remainingCooldown = (int)(sprintCooldownEnd - currentTime) / 50; // Convert to ticks
                    
                    // Apply a mining fatigue effect as a visual indicator of sprint cooldown
                    player.addEffect(new MobEffectInstance(
                        MobEffects.DIG_SLOWDOWN, 
                        Math.min(remainingCooldown, 100), // Use remaining time but cap at 5 seconds
                        0, // Level 1 effect
                        false, true, true
                    ));
                }
                
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 
                                                     SLOWDOWN_DURATION, 
                                                     SLOWDOWN_AMPLIFIER, 
                                                     false, true, true));
            }

            syncEnergyToClient(serverPlayer, energyData);
        }
    }

    public static void syncEnergyToClient(ServerPlayer player, PlayerEnergyData energyData) {
        NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new EnergyUpdatePacket(energyData.getEnergy(), energyData.getMaxEnergy())
        );
    }

    public static void consumeEnergy(ServerPlayer player, float amount) {
        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
            float currentEnergy = energyData.getEnergy();
            if (currentEnergy >= amount) {
                float newEnergy = currentEnergy - amount;
                energyData.setEnergy(newEnergy);
                
                // CRITICAL PATH: Check if we're depleting a large amount of energy at once
                // This helps catch cases where a large cost depletes energy from full to zero
                if (amount > 10.0f || newEnergy < SPRINT_ENERGY_THRESHOLD) {
                    // If player was sprinting and energy is now depleted, forcibly stop sprint
                    if (player.isSprinting()) {
                        player.setSprinting(false);
                        
                        // Apply a strong slowness effect with particles
                        MobEffectInstance slowness = new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN, 
                            SLOWDOWN_DURATION, 
                            SLOWDOWN_AMPLIFIER, 
                            false,  // ambient 
                            true,   // visible
                            true    // show icon
                        );
                        
                        // First remove any existing effect then add new one
                        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        player.addEffect(slowness);
                        
                        // Update player state tracking and set sprint cooldown
                        UUID playerUUID = player.getUUID();
                        playerSprintingMap.put(playerUUID, false);
                        
                        if (newEnergy < SPRINT_ENERGY_THRESHOLD) {
                            // Set sprint cooldown equal to regeneration delay
                            sprintCooldownMap.put(playerUUID, System.currentTimeMillis() + MixEnergyConfig.ENERGY_REGEN_COOLDOWN.get());
                        }
                    }
                }
                
                syncEnergyToClient(player, energyData);
            }
        });
    }

    public static void regenerateEnergy(ServerPlayer player, float amount) {
        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
            float newEnergy = Math.min(energyData.getEnergy() + amount, energyData.getMaxEnergy());
            energyData.setEnergy(newEnergy);
            syncEnergyToClient(player, energyData);
        });
    }

    public static void handleAction(ServerPlayer player, EnergyActionPacket.ActionType actionType, float amount) {
        switch (actionType) {
            case CONSUME:
                consumeEnergy(player, amount);
                break;
            case REGENERATE:
                regenerateEnergy(player, amount);
                break;
        }
    }

    @SubscribeEvent
    public static void onPlayerLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getSide() != LogicalSide.SERVER) {
            return;
        }
        
        Player player = event.getEntity();
        if (!isValidGameMode(player)) return;
        
        if (player instanceof ServerPlayer serverPlayer) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
                energyData.setLastActionTime(System.currentTimeMillis());
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() != LogicalSide.SERVER) {
            return;
        }
        
        Player player = event.getEntity();
        if (!isValidGameMode(player)) return;
        
        if (player instanceof ServerPlayer serverPlayer) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
                energyData.setLastActionTime(System.currentTimeMillis());
            });
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!MixEnergyConfig.ENERGY_COST_FOR_BREAKING_BLOCKS.get()) {
            return;
        }
        
        Player player = event.getPlayer();
        if (!isValidGameMode(player)) return;
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerEnergyData energyData = player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).orElse(null);
            if (energyData != null) {
                float currentEnergy = energyData.getEnergy();
                
                // First check if we have enough energy
                if (currentEnergy >= BLOCK_BREAK_ENERGY_COST) {
                    // CRITICAL CHECK: Would this deplete energy to zero?
                    if (currentEnergy - BLOCK_BREAK_ENERGY_COST < SPRINT_ENERGY_THRESHOLD && player.isSprinting()) {
                        // Preemptively stop sprinting before consuming energy
                        player.setSprinting(false);
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 
                                                             SLOWDOWN_DURATION, 
                                                             SLOWDOWN_AMPLIFIER, 
                                                             false, true, true));
                        playerSprintingMap.put(player.getUUID(), false);
                    }
                    
                    // Now consume energy normally
                    consumeEnergy(serverPlayer, BLOCK_BREAK_ENERGY_COST);
                    energyData.setLastActionTime(System.currentTimeMillis());
                } else {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!MixEnergyConfig.ENERGY_COST_FOR_BREAKING_BLOCKS.get()) {
            return;
        }
        
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (!isValidGameMode(serverPlayer)) return;
            PlayerEnergyData energyData = serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).orElse(null);
            if (energyData != null) {
                if (energyData.getEnergy() >= BLOCK_PLACE_ENERGY_COST) {
                    consumeEnergy(serverPlayer, BLOCK_PLACE_ENERGY_COST);
                    energyData.setLastActionTime(System.currentTimeMillis());
                } else {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
            if (!isValidGameMode(serverPlayer)) return;
            
            serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
                energyData.setLastActionTime(System.currentTimeMillis());
            });

            if (!MixEnergyConfig.ENERGY_COST_FOR_ATTACKS.get()) {
                return;
            }
            
            consumeEnergy(serverPlayer, ATTACK_ENERGY_COST);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
                playerMaxEnergyMap.put(player.getUUID(), energyData.getMaxEnergy());
            });
        }
    }
    
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUUID = player.getUUID();

            if (playerMaxEnergyMap.containsKey(playerUUID)) {
                float maxEnergy = playerMaxEnergyMap.get(playerUUID);
                
                player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
                    energyData.setMaxEnergy(maxEnergy);
                    syncEnergyToClient(player, energyData);
                });
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        try {
            Player originalPlayer = event.getOriginal();
            Player newPlayer = event.getEntity();
            UUID playerUUID = newPlayer.getUUID();
            
            CompoundTag energyData = new CompoundTag();
            
            originalPlayer.reviveCaps();

            originalPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(oldData -> {
                energyData.putFloat("energy", oldData.getEnergy());
                energyData.putFloat("maxEnergy", oldData.getMaxEnergy());
                energyData.putLong("lastActionTime", oldData.getLastActionTime());
                energyData.putLong("lastRegenTime", oldData.getLastRegenTime());

                playerEnergyDataMap.put(playerUUID, energyData.copy());
                
                System.out.println("[MixEnergy] Stored energy data for " + playerUUID + ": energy=" + 
                    oldData.getEnergy() + ", maxEnergy=" + oldData.getMaxEnergy());
            });

            originalPlayer.invalidateCaps();

            if (!energyData.isEmpty()) {
                newPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(newData -> {
                    newData.setEnergy(energyData.getFloat("energy"));
                    newData.setMaxEnergy(energyData.getFloat("maxEnergy"));
                    newData.setLastActionTime(energyData.getLong("lastActionTime"));
                    newData.setLastRegenTime(energyData.getLong("lastRegenTime"));
                    
                    System.out.println("[MixEnergy] Applied energy data for " + playerUUID + ": energy=" + 
                        newData.getEnergy() + ", maxEnergy=" + newData.getMaxEnergy());

                    if (!event.isWasDeath() && newPlayer instanceof ServerPlayer serverPlayer) {
                        pendingDimensionSyncs.put(serverPlayer.getUUID(), 2);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[MixEnergy] Error in player clone handling: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        try {
            Player player = event.getEntity();
            UUID playerUUID = player.getUUID();
            
            if (player instanceof ServerPlayer serverPlayer) {
                if (playerEnergyDataMap.containsKey(playerUUID)) {
                    CompoundTag storedData = playerEnergyDataMap.get(playerUUID);
                    
                    player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
                        energyData.setEnergy(storedData.getFloat("energy"));
                        energyData.setMaxEnergy(storedData.getFloat("maxEnergy"));
                        energyData.setLastActionTime(storedData.getLong("lastActionTime"));
                        energyData.setLastRegenTime(storedData.getLong("lastRegenTime"));
                        
                        System.out.println("[MixEnergy] Restored energy data on dimension change for " + 
                            playerUUID + ": energy=" + energyData.getEnergy() + 
                            ", maxEnergy=" + energyData.getMaxEnergy());
                    });
                }
                
                pendingDimensionSyncs.put(playerUUID, 10);
            }
        } catch (Exception e) {
            System.err.println("[MixEnergy] Error in dimension change handling: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            Iterator<Map.Entry<UUID, Integer>> iterator = pendingDimensionSyncs.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Integer> entry = iterator.next();
                UUID playerUUID = entry.getKey();
                int ticksLeft = entry.getValue() - 1;
                
                if (ticksLeft <= 0) {
                    iterator.remove();

                    for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                        if (player.getUUID().equals(playerUUID)) {
                            if (playerEnergyDataMap.containsKey(playerUUID)) {
                                CompoundTag storedData = playerEnergyDataMap.get(playerUUID);

                                player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
                                    if (energyData.getEnergy() != storedData.getFloat("energy") ||
                                        energyData.getMaxEnergy() != storedData.getFloat("maxEnergy")) {
                                        
                                        energyData.setEnergy(storedData.getFloat("energy"));
                                        energyData.setMaxEnergy(storedData.getFloat("maxEnergy"));
                                        
                                        System.out.println("[MixEnergy] Fixed reset values during tick sync for " + 
                                            playerUUID + ": energy=" + energyData.getEnergy() + 
                                            ", maxEnergy=" + energyData.getMaxEnergy());
                                    }
                                });
                            }
                            
                            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
                                syncEnergyToClient(player, energyData);
                                System.out.println("[MixEnergy] Synced energy to client for " + playerUUID + 
                                    ": energy=" + energyData.getEnergy() + 
                                    ", maxEnergy=" + energyData.getMaxEnergy());
                            });
                            break;
                        }
                    }
                } else {
                    entry.setValue(ticksLeft);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player != null) {
            UUID playerUUID = player.getUUID();
            playerSprintingMap.remove(playerUUID);
            sprintCooldownMap.remove(playerUUID); // Clean up sprint cooldown map
        }
    }

    @SubscribeEvent
    public static void onPlayerJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!isValidGameMode(player)) return;
            
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
                long currentTime = System.currentTimeMillis();
                UUID playerUUID = player.getUUID();
                long sprintCooldownEnd = sprintCooldownMap.getOrDefault(playerUUID, 0L);
                
                // Check if the player is sprinting and record the action
                if (player.isSprinting()) {
                    energyData.setLastActionTime(currentTime);
                    
                    // If energy is below threshold or player is on cooldown, force stop sprint
                    if (energyData.getEnergy() < SPRINT_ENERGY_THRESHOLD || currentTime < sprintCooldownEnd) {
                        player.setSprinting(false);
                        
                        // Apply a strong slowness effect
                        MobEffectInstance slowness = new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN, 
                            SLOWDOWN_DURATION, 
                            SLOWDOWN_AMPLIFIER, 
                            false, true, true
                        );
                        
                        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        player.addEffect(slowness);
                        
                        // Apply a visual indicator if on cooldown
                        if (currentTime < sprintCooldownEnd) {
                            int remainingCooldown = (int)(sprintCooldownEnd - currentTime) / 50; // Convert to ticks
                            player.addEffect(new MobEffectInstance(
                                MobEffects.DIG_SLOWDOWN,
                                Math.min(remainingCooldown, 100), // Cap at 5 seconds
                                0, false, true, true
                            ));
                        }
                        
                        playerSprintingMap.put(playerUUID, false);
                        
                        // Use network packet to ensure client sees sprint stop
                        syncEnergyToClient((ServerPlayer)player, energyData);
                    }
                }
            });
        }
    }

    private static boolean isValidGameMode(Player player) {
        GameType gameMode = player.isCreative() ? GameType.CREATIVE :
                player.isSpectator() ? GameType.SPECTATOR : GameType.SURVIVAL;
        return gameMode == GameType.SURVIVAL || gameMode == GameType.ADVENTURE;
    }

    private static boolean canRegenerate(long currentTime, PlayerEnergyData energyData) {
        return (currentTime - energyData.getLastActionTime()) > MixEnergyConfig.ENERGY_REGEN_COOLDOWN.get();
    }

    private static float calculateRegenMultiplier(long currentTime, PlayerEnergyData energyData) {
        long idleTime = currentTime - energyData.getLastActionTime() - MixEnergyConfig.ENERGY_REGEN_COOLDOWN.get();
        if (idleTime <= 0) return 0;
        return Math.min((float) idleTime / MAX_REGEN_BOOST_TIME, 1.0f);
    }

    private static void ensureCapabilityIntegrity(ServerPlayer player) {
        PlayerEnergyData energyData = player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).orElse(null);
        if (energyData == null) {
            PlayerEnergyProvider provider = new PlayerEnergyProvider();
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(data -> {
                data.setEnergy(MixEnergyConfig.DEFAULT_MAX_ENERGY.get().floatValue());
                data.setMaxEnergy(MixEnergyConfig.DEFAULT_MAX_ENERGY.get().floatValue());
                data.setLastActionTime(System.currentTimeMillis());
                data.setLastRegenTime(System.currentTimeMillis());
                syncEnergyToClient(player, data);
            });
        }
    }

    // Helper method to check if energy is zero or very close to zero
    private static boolean isEnergyZero(float energy) {
        return energy < EPSILON;
    }
}