package com.m1x.mixenergy.common;

import com.m1x.mixenergy.MixEnergy;
import com.m1x.mixenergy.common.config.MixEnergyConfig;
import com.m1x.mixenergy.common.entity.EnergyOrbEntity;
import com.m1x.mixenergy.network.EnergyActionPacket;
import com.m1x.mixenergy.network.EnergyUpdatePacket;
import com.m1x.mixenergy.network.NetworkHandler;
import com.m1x.mixenergy.registry.MixEnergyEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
//? if forge {
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
// BlockEvent.BreakEvent became a top-level BreakBlockEvent in 26.2.
//? if >=26 {
/^import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
^///?}
*///?}

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//? if forge {
@Mod.EventBusSubscriber(modid = MixEnergy.MOD_ID)
//?} else {
/*@EventBusSubscriber(modid = MixEnergy.MOD_ID)
*///?}
public final class PlayerEnergyManager {
    public static final float SPRINT_ENERGY_THRESHOLD = 0.5f;

    private static final int FATIGUE_DURATION_TICKS = 100;
    /**
     * How often a still-changing value is pushed to the client. The client predicts the
     * continuous drain itself, so this only bounds how far that prediction may drift; at
     * half a second the correction was large enough to be visible as the bar catching up.
     * Nothing is sent while the value is unchanged, so an idle player still costs nothing.
     */
    private static final int CLIENT_SYNC_INTERVAL_TICKS = 2;
    private static final long REGEN_INTERVAL_TICKS = 3L;
    private static final long MAX_REGEN_BOOST_TIME_TICKS = 60L;
    private static final long BETTER_COMBAT_ATTACK_TIMEOUT_TICKS = 60L;
    /**
     * How long a reported sprint is acted on without being refreshed. The client repeats
     * the report every ten ticks, so this only decides how quickly a player stops being
     * charged if those reports stop arriving.
     */
    private static final long CLIENT_SPRINT_TIMEOUT_TICKS = 20L;

    private static final Map<UUID, SyncState> SYNC_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, BetterCombatAttackState> BETTER_COMBAT_ATTACKS =
            new ConcurrentHashMap<>();
    private static final Set<UUID> CLIENT_FAST_SWIMMING = ConcurrentHashMap.newKeySet();
    /** Game time each reported sprint stops counting at, by player. */
    private static final Map<UUID, Long> CLIENT_SPRINTING = new ConcurrentHashMap<>();

    private PlayerEnergyManager() {
    }

    //? if forge {
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.side != LogicalSide.SERVER
                || !(event.player instanceof ServerPlayer player)
                || !usesEnergy(player)) {
            return;
        }
        tickPlayer(player);
    }
    //?} else {
    /*@SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !usesEnergy(player)) {
            return;
        }
        tickPlayer(player);
    }
    *///?}

    private static void tickPlayer(ServerPlayer player) {
        PlayerEnergyData energyData = PlayerEnergyProvider.get(player);
        if (energyData == null) {
            return;
        }

        long gameTime = player.level().getGameTime();
        boolean movementBlocked = energyData.getEnergy() < SPRINT_ENERGY_THRESHOLD
                || MixEnergyEffects.isFatigued(player);
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
        if (isSprinting(player)) {
            return MixEnergyConfig.ENERGY_COST_FOR_SPRINTING.get()
                    ? MixEnergyConfig.SPRINT_ENERGY_COST.get().floatValue()
                    : 0.0f;
        }
        return 0.0f;
    }

    /**
     * Whether the player is sprinting for energy purposes. The vanilla flag alone is not
     * enough: the client only reports a sprint when it notices the flag change, and a
     * sprint this mod cancels on the client is one of the changes it does not notice, so a
     * resumed sprint can otherwise stay invisible here for as long as the key is held.
     */
    private static boolean isSprinting(ServerPlayer player) {
        if (player.isSprinting()) {
            return true;
        }

        Long reportedUntil = CLIENT_SPRINTING.get(player.getUUID());
        if (reportedUntil == null) {
            return false;
        }
        if (player.level().getGameTime() > reportedUntil) {
            CLIENT_SPRINTING.remove(player.getUUID(), reportedUntil);
            return false;
        }
        return true;
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
        boolean fatigued = MixEnergyEffects.isFatigued(player);

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
        boolean clientReportedSprinting =
                CLIENT_SPRINTING.remove(player.getUUID()) != null;
        if (player.isSprinting()
                || player.isSwimming()
                || clientReportedFastSwimming
                || clientReportedSprinting) {
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
        if (MixEnergyEffects.isFatigued(player)) {
            return;
        }

        player.addEffect(MixEnergyEffects.fatigue(FATIGUE_DURATION_TICKS));
    }

    private static void forceStopFastMovement(ServerPlayer player) {
        CLIENT_FAST_SWIMMING.remove(player.getUUID());
        CLIENT_SPRINTING.remove(player.getUUID());
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
                || MixEnergyEffects.isFatigued(player)
                || !hasEnoughEnergyForFastMovement(player)) {
            CLIENT_FAST_SWIMMING.remove(player.getUUID());
            return;
        }
        CLIENT_FAST_SWIMMING.add(player.getUUID());
    }

    public static void setClientSprinting(ServerPlayer player, boolean sprinting) {
        if (!sprinting
                || !usesEnergy(player)
                || MixEnergyEffects.isFatigued(player)
                || !hasEnoughEnergyForFastMovement(player)) {
            CLIENT_SPRINTING.remove(player.getUUID());
            return;
        }
        CLIENT_SPRINTING.put(
                player.getUUID(),
                player.level().getGameTime() + CLIENT_SPRINT_TIMEOUT_TICKS
        );
    }

    private static boolean hasEnoughEnergyForFastMovement(ServerPlayer player) {
        PlayerEnergyData data = PlayerEnergyProvider.get(player);
        return data != null && data.getEnergy() >= SPRINT_ENERGY_THRESHOLD;
    }

    public static void consumeEnergy(ServerPlayer player, float amount) {
        consumeEnergy(player, amount, false);
    }

    private static void consumeEnergy(
            ServerPlayer player,
            float amount,
            boolean instantVisual
    ) {
        if (amount <= 0.0f || !usesEnergy(player)) {
            return;
        }

        PlayerEnergyData energyData = PlayerEnergyProvider.get(player);
        if (energyData == null) {
            return;
        }

        energyData.setEnergy(energyData.getEnergy() - amount);
        energyData.setLastActionTick(player.level().getGameTime());

        if (energyData.getEnergy() < SPRINT_ENERGY_THRESHOLD) {
            applyFatigue(player);
            forceStopFastMovement(player);
        }

        syncEnergyToClient(player, energyData, true, instantVisual);
    }

    public static void consumeCombatRollEnergy(ServerPlayer player) {
        if (!MixEnergyConfig.ENERGY_COST_FOR_COMBAT_ROLL.get()) {
            return;
        }

        consumeEnergy(
                player,
                MixEnergyConfig.COMBAT_ROLL_ENERGY_COST.get().floatValue(),
                true
        );
    }

    public static void beginBetterCombatAttack(ServerPlayer player) {
        if (!ModList.get().isLoaded("bettercombat")) {
            return;
        }

        long gameTime = player.level().getGameTime();
        BetterCombatAttackState previous = BETTER_COMBAT_ATTACKS.get(player.getUUID());
        if (previous != null && previous.gameTime() == gameTime) {
            return;
        }

        boolean allowed = true;
        if (MixEnergyConfig.ENERGY_COST_FOR_BETTER_COMBAT.get()) {
            allowed = tryConsumeEnergy(
                    player,
                    MixEnergyConfig.BETTER_COMBAT_ATTACK_ENERGY_COST.get().floatValue(),
                    true
            );
        }
        BETTER_COMBAT_ATTACKS.put(
                player.getUUID(),
                new BetterCombatAttackState(gameTime, allowed)
        );
    }

    public static void regenerateEnergy(ServerPlayer player, float amount) {
        if (amount <= 0.0f) {
            return;
        }

        PlayerEnergyData energyData = PlayerEnergyProvider.get(player);
        if (energyData == null) {
            return;
        }

        float previousEnergy = energyData.getEnergy();
        energyData.setEnergy(energyData.getEnergy() + amount);
        if (energyData.getEnergy() != previousEnergy) {
            syncEnergyToClient(player, energyData);
        }
    }

    private static boolean tryConsumeEnergy(ServerPlayer player, float amount) {
        return tryConsumeEnergy(player, amount, false);
    }

    private static boolean tryConsumeEnergy(
            ServerPlayer player,
            float amount,
            boolean instantVisual
    ) {
        if (amount <= 0.0f || !usesEnergy(player)) {
            return true;
        }

        PlayerEnergyData energyData = PlayerEnergyProvider.get(player);
        if (energyData == null) {
            return true;
        }

        if (energyData.getEnergy() < amount) {
            energyData.setLastActionTick(player.level().getGameTime());
            if (energyData.getEnergy() < SPRINT_ENERGY_THRESHOLD) {
                applyFatigue(player);
                forceStopFastMovement(player);
            }
            syncEnergyToClient(player, energyData, true, instantVisual);
            return false;
        }

        consumeEnergy(player, amount, instantVisual);
        return true;
    }

    @SubscribeEvent
    //? if <26 {
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
    //?} else {
    /*public static void onBlockBreak(BreakBlockEvent event) {
    *///?}
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

    // LivingAttackEvent was split up in 1.21; LivingIncomingDamageEvent is the cancellable
    // pre-damage phase that matches the old behaviour.
    //? if forge {
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
    //?} else {
    /*@SubscribeEvent
    public static void onLivingAttack(LivingIncomingDamageEvent event) {
    *///?}
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)
                || !usesEnergy(player)) {
            return;
        }

        if (ModList.get().isLoaded("bettercombat")) {
            BetterCombatAttackState attackState = BETTER_COMBAT_ATTACKS.get(player.getUUID());
            if (attackState != null) {
                long age = player.level().getGameTime() - attackState.gameTime();
                if (age >= 0L && age <= BETTER_COMBAT_ATTACK_TIMEOUT_TICKS) {
                    if (!attackState.allowed()) {
                        event.setCanceled(true);
                    }
                    return;
                }
                BETTER_COMBAT_ATTACKS.remove(player.getUUID(), attackState);
            }
        }

        if (!MixEnergyConfig.ENERGY_COST_FOR_ATTACKS.get()) {
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
        PlayerEnergyData data = PlayerEnergyProvider.get(player);
        return data != null && data.getEnergy() < data.getMaxEnergy();
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        //? if forge {
        original.reviveCaps();
        //?}
        try {
            PlayerEnergyData oldData = PlayerEnergyProvider.get(original);
            PlayerEnergyData newData = PlayerEnergyProvider.get(event.getEntity());

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
            CLIENT_SPRINTING.remove(event.getEntity().getUUID());
            BETTER_COMBAT_ATTACKS.remove(event.getEntity().getUUID());
        } finally {
            //? if forge {
            original.invalidateCaps();
            //?}
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
            PlayerEnergyData data = PlayerEnergyProvider.get(serverPlayer);
            if (data != null) {
                syncEnergyToClient(serverPlayer, data);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SYNC_STATES.remove(event.getEntity().getUUID());
        CLIENT_FAST_SWIMMING.remove(event.getEntity().getUUID());
        CLIENT_SPRINTING.remove(event.getEntity().getUUID());
        BETTER_COMBAT_ATTACKS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SYNC_STATES.clear();
        CLIENT_FAST_SWIMMING.clear();
        CLIENT_SPRINTING.clear();
        BETTER_COMBAT_ATTACKS.clear();
    }

    public static void syncEnergyToClient(ServerPlayer player, PlayerEnergyData energyData) {
        syncEnergyToClient(player, energyData, true);
    }

    private static void syncEnergyToClient(
            ServerPlayer player,
            PlayerEnergyData energyData,
            boolean force
    ) {
        syncEnergyToClient(player, energyData, force, false);
    }

    private static void syncEnergyToClient(
            ServerPlayer player,
            PlayerEnergyData energyData,
            boolean force,
            boolean instantVisual
    ) {
        long gameTime = player.level().getGameTime();
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
                                : 0.0f,
                        instantVisual
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
                || MixEnergyEffects.isFatigued(player);
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

    private record BetterCombatAttackState(long gameTime, boolean allowed) {
    }
}
