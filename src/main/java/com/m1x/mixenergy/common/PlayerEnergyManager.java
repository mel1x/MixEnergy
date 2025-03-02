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

    private static final int SLOWDOWN_DURATION = 80;
    private static boolean wasSprintingLastTick = false;

    private static final float BASE_ENERGY_REGEN_RATE = 1.0f;
    private static final float MAX_ENERGY_REGEN_RATE = 1.8f;
    private static final int REGEN_DELAY_TICKS = 30;
    private static final int MAX_REGEN_BOOST_TIME = 3000;

    private static final Map<UUID, Float> playerMaxEnergyMap = new HashMap<>();
    private static final Map<UUID, Integer> pendingDimensionSyncs = new HashMap<>();
    private static final Map<UUID, CompoundTag> playerEnergyDataMap = new HashMap<>();

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

            if (player.isSprinting()) {
                if (energyData.getEnergy() >= SPRINT_ENERGY_COST) {
                    consumeEnergy(serverPlayer, SPRINT_ENERGY_COST);
                    energyData.setLastActionTime(currentTime);
                    player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                } else {
                    player.setSprinting(false);
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWDOWN_DURATION, 1, false, false));
                }
                wasSprintingLastTick = true;
            } else {
                if (wasSprintingLastTick && energyData.getEnergy() < SPRINT_ENERGY_COST) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOWDOWN_DURATION, 1, false, false));
                }
                wasSprintingLastTick = false;
            }

            if (canRegenerate(currentTime, energyData)) {
                if (currentTime - energyData.getLastRegenTime() >= 120) {
                    float regenMultiplier = calculateRegenMultiplier(currentTime, energyData);
                    float regenAmount = BASE_ENERGY_REGEN_RATE +
                            (MAX_ENERGY_REGEN_RATE - BASE_ENERGY_REGEN_RATE) * regenMultiplier;

                    regenerateEnergy(serverPlayer, regenAmount);
                    energyData.setLastRegenTime(currentTime);
                }
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
            if (energyData.getEnergy() >= amount) {
                energyData.setEnergy(energyData.getEnergy() - amount);
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
                if (energyData.getEnergy() >= BLOCK_BREAK_ENERGY_COST) {
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
}