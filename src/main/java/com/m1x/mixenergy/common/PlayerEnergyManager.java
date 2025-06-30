package com.m1x.mixenergy.common;

import com.m1x.mixenergy.common.config.MixEnergyConfig;
import com.m1x.mixenergy.common.entity.EnergyOrbEntity;
import com.m1x.mixenergy.network.EnergyActionPacket;
import com.m1x.mixenergy.registry.MixEnergyEffects;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.nbt.CompoundTag;
import com.m1x.mixenergy.network.EnergyActionPacket.ActionType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraftforge.common.ForgeMod;

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
    private static final Map<UUID, Long> sprintCooldownMap = new HashMap<>();

    private static final float BASE_ENERGY_REGEN_RATE = 1.0f;
    private static final float MAX_ENERGY_REGEN_RATE = 1.8f;
    private static final int MAX_REGEN_BOOST_TIME = 3000;

    private static final Map<UUID, Float> playerMaxEnergyMap = new HashMap<>();
    private static final Map<UUID, Integer> pendingDimensionSyncs = new HashMap<>();
    private static final Map<UUID, CompoundTag> playerEnergyDataMap = new HashMap<>();

    private static final float SPRINT_ENERGY_THRESHOLD = 0.5f;
    
    private static final UUID SWIM_SPEED_UUID = UUID.fromString("8107DE5E-7CE8-4030-940E-514C1F160892");

    /**
     * Принудительно отключает плавание для игрока
     * @param player Игрок, для которого отключается плавание
     */
    private static void forceStopSwimming(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            // Остановка плавания на сервере
            player.setSwimming(false);
            
            // Попытка доступа к внутренним полям через рефлексию
            try {
                java.lang.reflect.Field isSwimmingField = 
                    net.minecraft.world.entity.LivingEntity.class.getDeclaredField("f_20899_");
                isSwimmingField.setAccessible(true);
                isSwimmingField.set(player, false);
            } catch (Exception e) {
                // Если рефлексия не сработала, применяем резервный метод
                player.setSwimming(false);
            }
            
            // Изменение скорости плавания, если игрок в воде
            if (player.isInWater()) {
                if (player.getAttribute(ForgeMod.SWIM_SPEED.get()) != null) {
                    // Создаем временный модификатор, который снижает скорость плавания
                    AttributeModifier swimModifier = new AttributeModifier(
                        SWIM_SPEED_UUID, 
                        "MixEnergy Swim Neutralizer", 
                        -0.4, // Сильное снижение скорости плавания
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                    );
                    
                    // Удаляем существующий модификатор, если он есть
                    if (player.getAttribute(ForgeMod.SWIM_SPEED.get()).getModifier(SWIM_SPEED_UUID) != null) {
                        player.getAttribute(ForgeMod.SWIM_SPEED.get()).removeModifier(SWIM_SPEED_UUID);
                    }
                    
                    // Добавляем новый модификатор
                    player.getAttribute(ForgeMod.SWIM_SPEED.get()).addTransientModifier(swimModifier);
                }
            }
            
            // Также сбрасываем любые анимации или состояния, связанные с плаванием
            player.stopUsingItem();
            player.setSprinting(false);
            
            // Отправляем пакет синхронизации на клиент
            NetworkHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new EnergyActionPacket(ActionType.STOP_SWIMMING)
            );
        }
    }

    private static void applyMixEnergySlowness(Player player) {
        if (player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get())) {
            return;
        }
        
        MobEffectInstance slowness = new MobEffectInstance(
            MixEnergyEffects.MIX_ENERGY_SLOWNESS.get(),
            SLOWDOWN_DURATION,
            SLOWDOWN_AMPLIFIER,
            false,
            true,
            true
        );
     
        player.addEffect(slowness);
    }
    
    /**
     * Проверяет, может ли игрок начать спринт
     * @param player Игрок
     * @return true если игрок может начать спринт, false если нет
     */
    private static boolean canPlayerSprint(Player player) {
        // Проверяем наличие эффекта усталости
        if (player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get())) {
            return false;
        }
        
        UUID playerUUID = player.getUUID();
        long currentTime = System.currentTimeMillis();
        long sprintCooldownEnd = sprintCooldownMap.getOrDefault(playerUUID, 0L);
        
        // Проверяем, не на кулдауне ли спринт
        if (currentTime < sprintCooldownEnd) {
            return false;
        }
        
        // Проверяем энергию игрока
        PlayerEnergyData energyData = player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).orElse(null);
        if (energyData == null) {
            return true; // если по какой-то причине данных нет, разрешаем спринт
        }
        
        return energyData.getEnergy() >= SPRINT_ENERGY_THRESHOLD;
    }

    /**
     * Проверяет, может ли игрок плавать
     * @param player Игрок
     * @return true если игрок может плавать, false если нет
     */
    private static boolean canPlayerSwim(Player player) {
        // Используем ту же логику, что и для спринта
        return canPlayerSprint(player);
    }

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
            boolean hasSlownessEffect = player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get());
            
            if ((player.isSprinting() || player.isSwimming()) && !canPlayerSprint(player)) {
                player.setSprinting(false);
                if (player.isSwimming()) {
                    forceStopSwimming(player);
                }
                
                if (!hasSlownessEffect) {
                    applyMixEnergySlowness(player);
                }
                
                UUID playerUUID = player.getUUID();
                playerSprintingMap.put(playerUUID, false);
                
                if (currentEnergy < SPRINT_ENERGY_THRESHOLD) {
                    sprintCooldownMap.put(playerUUID, currentTime + MixEnergyConfig.ENERGY_REGEN_COOLDOWN.get());
                }
                
                NetworkHandler.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new EnergyUpdatePacket(energyData.getEnergy(), energyData.getMaxEnergy())
                );
            }
            
            if (player.isSprinting() || player.isSwimming()) {
                UUID playerUUID = player.getUUID();
                
                energyData.setEnergy(currentEnergy - SPRINT_ENERGY_COST);
                energyData.setLastActionTime(currentTime);
                playerSprintingMap.put(playerUUID, true);
                
                if (energyData.getEnergy() < SPRINT_ENERGY_THRESHOLD) {
                    player.setSprinting(false);
                    
                    if (!hasSlownessEffect) {
                        applyMixEnergySlowness(player);
                    }
                    
                    playerSprintingMap.put(playerUUID, false);
                    sprintCooldownMap.put(playerUUID, currentTime + MixEnergyConfig.ENERGY_REGEN_COOLDOWN.get());
                }
            } else {
                UUID playerUUID = player.getUUID();
                boolean wasSprinting = playerSprintingMap.getOrDefault(playerUUID, false);
                
                if (wasSprinting && currentEnergy < SPRINT_ENERGY_THRESHOLD && !hasSlownessEffect) {
                    applyMixEnergySlowness(player);
                }
                playerSprintingMap.put(playerUUID, false);
            }

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
            float newEnergy = currentEnergy - amount;
            energyData.setEnergy(newEnergy);
            
            if (amount > 10.0f || newEnergy < SPRINT_ENERGY_THRESHOLD) {
                if (player.isSprinting()) {
                    player.setSprinting(false);
                    
                    if (!player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get())) {
                        applyMixEnergySlowness(player);
                    }
                    
                    UUID playerUUID = player.getUUID();
                    playerSprintingMap.put(playerUUID, false);
                    
                    if (newEnergy < SPRINT_ENERGY_THRESHOLD) {
                        sprintCooldownMap.put(playerUUID, System.currentTimeMillis() + MixEnergyConfig.ENERGY_REGEN_COOLDOWN.get());
                    }
                }
            }
            
            syncEnergyToClient(player, energyData);
        });
    }

    public static void regenerateEnergy(ServerPlayer player, float amount) {
        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
            float newEnergy = Math.min(energyData.getEnergy() + amount, energyData.getMaxEnergy());
            energyData.setEnergy(newEnergy);
            syncEnergyToClient(player, energyData);
        });
    }

    /**
     * Обрабатывает действия игрока через пакеты
     * @param player Игрок
     * @param actionType Тип действия
     * @param amount Количество энергии для потребления/регенерации
     */
    public static void handleAction(ServerPlayer player, ActionType actionType, float amount) {
        if (player == null) return;
        
        switch (actionType) {
            case CONSUME:
                consumeEnergy(player, amount);
                break;
            case REGENERATE:
                player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
                    float newEnergy = Math.min(energyData.getEnergy() + amount, energyData.getMaxEnergy());
                    energyData.setEnergy(newEnergy);
                    syncEnergyToClient(player, energyData);
                });
                break;
            case STOP_SWIMMING:
                player.setSwimming(false);
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
                
                float blockHardness = event.getState().getDestroySpeed(event.getLevel(), event.getPos());
                if (blockHardness <= 0.0f) {
                    return;
                }
                
                if (currentEnergy >= BLOCK_BREAK_ENERGY_COST) {
                    if (currentEnergy - BLOCK_BREAK_ENERGY_COST < SPRINT_ENERGY_THRESHOLD && player.isSprinting()) {
                        player.setSprinting(false);
                        
                        if (!player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get())) {
                            applyMixEnergySlowness(player);
                        }
                        
                        playerSprintingMap.put(player.getUUID(), false);
                    }
                    
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
        // Если игрок получил урон
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            if (!isValidGameMode(serverPlayer)) return;
            
            serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
                energyData.setLastActionTime(System.currentTimeMillis());
            });
        }
        
        // Если игрок атакует моба
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
    public static void onMobDeath(LivingDeathEvent event) {
        // Only process on the server
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        
        // Don't spawn energy orbs for players
        if (event.getEntity() instanceof Player) {
            return;
        }
        
        // Check if mob was killed by a player
        if (event.getSource().getEntity() instanceof Player player) {
            // Get the position of the killed entity
            double x = event.getEntity().getX();
            double y = event.getEntity().getY() + 0.5D; // Slightly above the entity
            double z = event.getEntity().getZ();
            
            // Determine number of orbs to spawn (2-5 based on entity size)
            int orbCount = 2;
            if (event.getEntity().getBbHeight() > 1.0f || event.getEntity().getBbWidth() > 1.0f) {
                orbCount = 3;
            }
            if (event.getEntity().getBbHeight() > 2.0f || event.getEntity().getBbWidth() > 2.0f) {
                orbCount = 5;
            }
            
            // Spawn energy orbs
            for (int i = 0; i < orbCount; i++) {
                EnergyOrbEntity orb = new EnergyOrbEntity(event.getEntity().level(), x, y, z);
                
                // Add some random movement
                double speedX = event.getEntity().getRandom().nextDouble() * 0.2 - 0.1;
                double speedY = event.getEntity().getRandom().nextDouble() * 0.2 + 0.2; // Always up
                double speedZ = event.getEntity().getRandom().nextDouble() * 0.2 - 0.1;
                
                orb.setDeltaMovement(speedX, speedY, speedZ);
                
                // Add to world
                event.getEntity().level().addFreshEntity(orb);
            }
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
                int ticksLeft = entry.getValue();
                
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
                            });
                            break;
                        }
                    }
                } else {
                    entry.setValue(ticksLeft - 1);
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
            sprintCooldownMap.remove(playerUUID);
        }
    }

    @SubscribeEvent
    public static void onPlayerJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!isValidGameMode(player)) return;
            
            player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
                long currentTime = System.currentTimeMillis();
                UUID playerUUID = player.getUUID();
                
                if (player.isSprinting()) {
                    energyData.setLastActionTime(currentTime);
                    
                    if (!canPlayerSprint(player)) {
                        player.setSprinting(false);
                        
                        if (!player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get())) {
                            applyMixEnergySlowness(player);
                        }
                        
                        playerSprintingMap.put(playerUUID, false);
                        
                        syncEnergyToClient((ServerPlayer)player, energyData);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerSprintCheck(PlayerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.START) {
            Player player = event.player;
            
            if (!(player instanceof ServerPlayer) || !isValidGameMode(player)) {
                return;
            }
            
            boolean hasSlownessEffect = player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get());
            
            if (player.isSprinting() && hasSlownessEffect) {
                player.setSprinting(false);
            }
            
            if (player.isSwimming() && hasSlownessEffect) {
                forceStopSwimming(player);
            }
            
            if (player.isInWater()) {
                if (hasSlownessEffect) {
                    forceStopSwimming(player);
                }
                
                if (!canPlayerSwim(player) && !hasSlownessEffect) {
                    forceStopSwimming(player);
                    applyMixEnergySlowness(player);
                }
            }
            
            if (player.isSwimming() && !canPlayerSwim(player) && !hasSlownessEffect) {
                forceStopSwimming(player);
                applyMixEnergySlowness(player);
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
        return Math.min(1.0f, (float)idleTime / MAX_REGEN_BOOST_TIME);
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