package com.m1x.mixenergy.common;

import com.m1x.mixenergy.common.config.MixEnergyConfig;
import com.m1x.mixenergy.common.entity.EnergyOrbEntity;
import com.m1x.mixenergy.network.EnergyActionPacket;
import com.m1x.mixenergy.network.EnergyUpdatePacket;
import com.m1x.mixenergy.network.NetworkHandler;
import com.m1x.mixenergy.registry.MixEnergyEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = "mixenergy")
public final class PlayerEnergyManager {
    public static final float SPRINT_ENERGY_THRESHOLD = 0.5f;

    private static final int FATIGUE_DURATION_TICKS = 100;
    private static final int CLIENT_SYNC_INTERVAL_TICKS = 10;
    private static final long REGEN_INTERVAL_TICKS = 3L;
    private static final long MAX_REGEN_BOOST_TIME_TICKS = 60L;

    private static final Map<UUID, SyncState> SYNC_STATES = new ConcurrentHashMap<>();
    private static final Set<UUID> CLIENT_FAST_SWIMMING = ConcurrentHashMap.newKeySet();

    private PlayerEnergyManager() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.side != LogicalSide.SERVER
                || !(event.player instanceof ServerPlayer player)
                || !usesEnergy(player)) {
            return;
        }

        PlayerEnergyData energyData = player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY)
                .orElse(null);
        if (energyData == null) {
            return;
        }

        long gameTime = player.serverLevel().getGameTime();
        boolean movementBlocked = energyData.getEnergy() < SPRINT_ENERGY_THRESHOLD
                || player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get());
        boolean forceSync = enforceExhaustion(player, energyData);

        if (!movementBlocked) {
            float movementCost = getMovementCost(player);
            if (movementCost > 0.0f) {
                float previousEnergy = energyData.getEnergy();
                energyData.setEnergy(previousEnergy - movementCost);
                energyData.setLastActionTick(gameTime);
                forceSync = previousEnergy >= SPRINT_ENERGY_THRESHOLD
                        && energyData.getEnergy() < SPRINT_ENERGY_THRESHOLD;

                if (forceSync) {
                    applyFatigue(player);
                    forceStopFastMovement(player);
                }
            }
        }

        regenerateEnergyIfReady(gameTime, energyData);
        syncEnergyToClient(player, energyData, forceSync);
    }

    private static float getMovementCost(ServerPlayer player) {
        if (isFastSwimming(player)) {
            return MixEnergyConfig.ENERGY_COST_FOR_SWIMMING.get()
                    ? MixEnergyConfig.FAST_SWIMMING_ENERGY_COST.get().floatValue()
                    : 0.0f;
        }
        if (player.isSprinting()) {
            return MixEnergyConfig.ENERGY_COST_FOR_SPRINTING.get()
                    ? MixEnergyConfig.SPRINT_ENERGY_COST.get().floatValue()
                    : 0.0f;
        }
        return 0.0f;
    }

    private static boolean isFastSwimming(Player player) {
        if (!player.isInWater()) {
            CLIENT_FAST_SWIMMING.remove(player.getUUID());
            return false;
        }
        return player.isSwimming()
                || player.isSprinting()
                || CLIENT_FAST_SWIMMING.contains(player.getUUID());
    }

    private static boolean enforceExhaustion(ServerPlayer player, PlayerEnergyData energyData) {
        boolean exhausted = energyData.getEnergy() < SPRINT_ENERGY_THRESHOLD;
        boolean fatigued = player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get());

        if (!exhausted && !fatigued) {
            return false;
        }

        boolean fatigueApplied = false;
        if (exhausted && !fatigued) {
            applyFatigue(player);
            fatigueApplied = true;
        }

        boolean clientReportedFastSwimming =
                CLIENT_FAST_SWIMMING.remove(player.getUUID());
        if (player.isSprinting() || player.isSwimming() || clientReportedFastSwimming) {
            forceStopFastMovement(player);
            return true;
        }
        return fatigueApplied;
    }

    private static void regenerateEnergyIfReady(long gameTime, PlayerEnergyData energyData) {
        float amount = getRegenerationPulseAmount(gameTime, energyData);
        if (amount <= 0.0f
                || gameTime - energyData.getLastRegenTick() < REGEN_INTERVAL_TICKS) {
            return;
        }

        energyData.setEnergy(energyData.getEnergy() + amount);
        energyData.setLastRegenTick(gameTime);
    }

    private static float getRegenerationPulseAmount(
            long gameTime,
            PlayerEnergyData energyData
    ) {
        if (energyData.getEnergy() >= energyData.getMaxEnergy()) {
            return 0.0f;
        }

        long idleTicks = Math.max(0L, gameTime - energyData.getLastActionTick());
        if (idleTicks <= MixEnergyConfig.ENERGY_REGEN_COOLDOWN_TICKS.get()) {
            return 0.0f;
        }

        long boostedIdleTicks = idleTicks - MixEnergyConfig.ENERGY_REGEN_COOLDOWN_TICKS.get();
        float multiplier = Math.min(
                1.0f,
                (float) boostedIdleTicks / MAX_REGEN_BOOST_TIME_TICKS
        );
        float baseRate = MixEnergyConfig.BASE_ENERGY_REGEN_RATE.get().floatValue();
        float maxRate = Math.max(
                baseRate,
                MixEnergyConfig.MAX_ENERGY_REGEN_RATE.get().floatValue()
        );
        float speedMultiplier =
                MixEnergyConfig.ENERGY_REGEN_SPEED_MULTIPLIER.get().floatValue();
        if (speedMultiplier <= 0.0f) {
            return 0.0f;
        }
        return (baseRate + (maxRate - baseRate) * multiplier)
                * speedMultiplier;
    }

    private static void applyFatigue(Player player) {
        if (player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get())) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MixEnergyEffects.MIX_ENERGY_SLOWNESS.get(),
                FATIGUE_DURATION_TICKS,
                0,
                false,
                true,
                true
        ));
    }

    private static void forceStopFastMovement(ServerPlayer player) {
        CLIENT_FAST_SWIMMING.remove(player.getUUID());
        player.setSprinting(false);
        player.setSwimming(false);
        NetworkHandler.sendToPlayer(
                player,
                new EnergyActionPacket(EnergyActionPacket.ActionType.STOP_SWIMMING)
        );
    }

    public static void setClientFastSwimming(ServerPlayer player, boolean fastSwimming) {
        if (!fastSwimming
                || !usesEnergy(player)
                || !player.isInWater()
                || player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get())
                || !hasEnoughEnergyForFastMovement(player)) {
            CLIENT_FAST_SWIMMING.remove(player.getUUID());
            return;
        }
        CLIENT_FAST_SWIMMING.add(player.getUUID());
    }

    private static boolean hasEnoughEnergyForFastMovement(ServerPlayer player) {
        PlayerEnergyData data = player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY)
                .orElse(null);
        return data != null && data.getEnergy() >= SPRINT_ENERGY_THRESHOLD;
    }

    public static void consumeEnergy(ServerPlayer player, float amount) {
        if (amount <= 0.0f || !usesEnergy(player)) {
            return;
        }

        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
            energyData.setEnergy(energyData.getEnergy() - amount);
            energyData.setLastActionTick(player.serverLevel().getGameTime());

            if (energyData.getEnergy() < SPRINT_ENERGY_THRESHOLD) {
                applyFatigue(player);
                forceStopFastMovement(player);
            }

            syncEnergyToClient(player, energyData);
        });
    }

    public static void consumeCombatRollEnergy(ServerPlayer player) {
        if (!MixEnergyConfig.ENERGY_COST_FOR_COMBAT_ROLL.get()) {
            return;
        }

        float threeSecondsOfSprinting =
                MixEnergyConfig.SPRINT_ENERGY_COST.get().floatValue() * 60.0f;
        consumeEnergy(player, threeSecondsOfSprinting);
    }

    public static void regenerateEnergy(ServerPlayer player, float amount) {
        if (amount <= 0.0f) {
            return;
        }

        player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).ifPresent(energyData -> {
            float previousEnergy = energyData.getEnergy();
            energyData.setEnergy(energyData.getEnergy() + amount);
            if (energyData.getEnergy() != previousEnergy) {
                syncEnergyToClient(player, energyData);
            }
        });
    }

    private static boolean tryConsumeEnergy(ServerPlayer player, float amount) {
        PlayerEnergyData energyData = player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).orElse(null);
        if (energyData == null) {
            return true;
        }

        if (energyData.getEnergy() < amount) {
            energyData.setLastActionTick(player.serverLevel().getGameTime());
            if (energyData.getEnergy() < SPRINT_ENERGY_THRESHOLD) {
                applyFatigue(player);
                forceStopFastMovement(player);
            }
            syncEnergyToClient(player, energyData);
            return false;
        }

        consumeEnergy(player, amount);
        return true;
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!MixEnergyConfig.ENERGY_COST_FOR_BREAKING_BLOCKS.get()) {
            return;
        }

        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer) || !usesEnergy(serverPlayer)) {
            return;
        }

        float hardness = event.getState().getDestroySpeed(event.getLevel(), event.getPos());
        float configuredCost = MixEnergyConfig.BLOCK_BREAK_ENERGY_COST.get().floatValue();
        if (hardness > 0.0f && !tryConsumeEnergy(serverPlayer, configuredCost)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!MixEnergyConfig.ENERGY_COST_FOR_PLACING_BLOCKS.get()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !usesEnergy(player)) {
            return;
        }

        if (!tryConsumeEnergy(
                player,
                MixEnergyConfig.BLOCK_PLACE_ENERGY_COST.get().floatValue()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!MixEnergyConfig.ENERGY_COST_FOR_ATTACKS.get()
                || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !usesEnergy(player)) {
            return;
        }

        if (!tryConsumeEnergy(
                player,
                MixEnergyConfig.ATTACK_ENERGY_COST.get().floatValue()
        )) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!MixEnergyConfig.ENERGY_COST_FOR_JUMPING.get()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !usesEnergy(player)) {
            return;
        }

        if (!tryConsumeEnergy(
                player,
                MixEnergyConfig.JUMP_ENERGY_COST.get().floatValue()
        )) {
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(movement.x, Math.min(0.0, movement.y), movement.z);
            player.hurtMarked = true;
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()
                || event.getEntity() instanceof Player
                || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !usesEnergy(player)
                || !playerNeedsEnergy(player)) {
            return;
        }

        int orbCount = 2;
        if (event.getEntity().getBbHeight() > 1.0f || event.getEntity().getBbWidth() > 1.0f) {
            orbCount = 3;
        }
        if (event.getEntity().getBbHeight() > 2.0f || event.getEntity().getBbWidth() > 2.0f) {
            orbCount = 5;
        }

        EnergyOrbEntity orb = new EnergyOrbEntity(
                event.getEntity().level(),
                event.getEntity().getX(),
                event.getEntity().getY() + 0.5,
                event.getEntity().getZ(),
                orbCount * EnergyOrbEntity.BASE_ENERGY_AMOUNT
        );
        orb.setDeltaMovement(
                event.getEntity().getRandom().nextDouble() * 0.2 - 0.1,
                event.getEntity().getRandom().nextDouble() * 0.2 + 0.2,
                event.getEntity().getRandom().nextDouble() * 0.2 - 0.1
        );
        event.getEntity().level().addFreshEntity(orb);
    }

    private static boolean playerNeedsEnergy(ServerPlayer player) {
        PlayerEnergyData data = player.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).orElse(null);
        return data != null && data.getEnergy() < data.getMaxEnergy();
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        original.reviveCaps();
        try {
            PlayerEnergyData oldData = original.getCapability(PlayerEnergyProvider.PLAYER_ENERGY).orElse(null);
            PlayerEnergyData newData = event.getEntity()
                    .getCapability(PlayerEnergyProvider.PLAYER_ENERGY)
                    .orElse(null);

            if (oldData == null || newData == null) {
                return;
            }

            newData.copyFrom(oldData);
            if (event.isWasDeath()) {
                newData.setEnergy(newData.getMaxEnergy());
                long gameTime = event.getEntity().level().getGameTime();
                newData.setLastActionTick(gameTime);
                newData.setLastRegenTick(gameTime);
            }
            SYNC_STATES.remove(event.getEntity().getUUID());
            CLIENT_FAST_SWIMMING.remove(event.getEntity().getUUID());
        } finally {
            original.invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncPlayer(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncPlayer(event.getEntity());
    }

    private static void syncPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.getCapability(PlayerEnergyProvider.PLAYER_ENERGY)
                    .ifPresent(data -> syncEnergyToClient(serverPlayer, data));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SYNC_STATES.remove(event.getEntity().getUUID());
        CLIENT_FAST_SWIMMING.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SYNC_STATES.clear();
        CLIENT_FAST_SWIMMING.clear();
    }

    public static void syncEnergyToClient(ServerPlayer player, PlayerEnergyData energyData) {
        syncEnergyToClient(player, energyData, true);
    }

    private static void syncEnergyToClient(
            ServerPlayer player,
            PlayerEnergyData energyData,
            boolean force
    ) {
        long gameTime = player.serverLevel().getGameTime();
        SyncState previous = SYNC_STATES.get(player.getUUID());
        boolean maxChanged = previous == null || previous.maxEnergy != energyData.getMaxEnergy();
        boolean energyChanged = previous == null || previous.energy != energyData.getEnergy();
        boolean intervalElapsed = previous == null
                || gameTime - previous.gameTime >= CLIENT_SYNC_INTERVAL_TICKS;

        if (!force && !maxChanged && (!energyChanged || !intervalElapsed)) {
            return;
        }

        NetworkHandler.sendToPlayer(
                player,
                new EnergyUpdatePacket(
                        energyData.getEnergy(),
                        energyData.getMaxEnergy(),
                        getVisualEnergyTrend(player, energyData, gameTime),
                        MixEnergyConfig.ENERGY_COST_FOR_SPRINTING.get()
                                ? MixEnergyConfig.SPRINT_ENERGY_COST.get().floatValue()
                                : 0.0f,
                        MixEnergyConfig.ENERGY_COST_FOR_SWIMMING.get()
                                ? MixEnergyConfig.FAST_SWIMMING_ENERGY_COST.get().floatValue()
                                : 0.0f
                )
        );
        SYNC_STATES.put(
                player.getUUID(),
                new SyncState(gameTime, energyData.getEnergy(), energyData.getMaxEnergy())
        );
    }

    private static boolean usesEnergy(Player player) {
        return !player.isCreative() && !player.isSpectator();
    }

    private static float getVisualEnergyTrend(
            ServerPlayer player,
            PlayerEnergyData energyData,
            long gameTime
    ) {
        boolean movementBlocked = energyData.getEnergy() < SPRINT_ENERGY_THRESHOLD
                || player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get());
        if (!movementBlocked) {
            float movementCost = getMovementCost(player);
            if (movementCost > 0.0f) {
                return -movementCost;
            }
        }
        return getRegenerationPulseAmount(gameTime, energyData)
                / REGEN_INTERVAL_TICKS;
    }

    private record SyncState(long gameTime, float energy, float maxEnergy) {
    }
}
