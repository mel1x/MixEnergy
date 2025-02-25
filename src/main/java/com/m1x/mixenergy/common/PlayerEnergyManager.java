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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
            if (energyData == null) return;

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
}