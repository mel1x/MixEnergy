package com.m1x.mixenergy.client;

import com.m1x.mixenergy.MixEnergy;
import com.m1x.mixenergy.common.PlayerEnergyManager;
import com.m1x.mixenergy.network.EnergyActionPacket;
import com.m1x.mixenergy.network.NetworkHandler;
import com.m1x.mixenergy.registry.MixEnergyEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.level.GameType;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
*///?}

//? if forge {
@Mod.EventBusSubscriber(modid = MixEnergy.MOD_ID, value = Dist.CLIENT)
//?} else {
/*@EventBusSubscriber(modid = MixEnergy.MOD_ID, value = Dist.CLIENT)
*///?}
public final class ClientMovementHandler {
    /**
     * How often an unchanged movement state is repeated to the server. The reports are
     * edge triggered, so the refresh is what keeps a lost packet - or a server that has
     * dropped the state on its own - from leaving the two sides disagreeing for good.
     */
    private static final int MOVEMENT_HEARTBEAT_TICKS = 10;
    /**
     * How long a sprint stop the server asked for keeps the mod from reporting the sprint
     * again. The two sides can decide a tick apart whether the player may sprint, so
     * without the delay a stop that arrives while the client still thinks it has energy
     * would be answered with an immediate resume, and the two would trade those two
     * packets every tick for as long as the sprint key is held.
     */
    private static final int SPRINT_RESUME_DELAY_TICKS = 2;

    /**
     * The sprint the player is spending energy on, as the client sees it.
     *
     * <p>The server cannot rely on its own sprint flag for this: it is set by a vanilla
     * command the client only sends on a change it notices, and the mod cancels sprints
     * behind that bookkeeping. Reporting the state the client is actually in - the same one
     * the energy bar predicts from - keeps the charge and the bar from ever disagreeing,
     * the way the swimming report below already does for a state vanilla never sends at all.
     */
    private static final MovementReport SPRINT_REPORT = new MovementReport(
            EnergyActionPacket.ActionType.SPRINT_START,
            EnergyActionPacket.ActionType.SPRINT_STOP
    );
    private static final MovementReport FAST_SWIMMING_REPORT = new MovementReport(
            EnergyActionPacket.ActionType.FAST_SWIMMING_START,
            EnergyActionPacket.ActionType.FAST_SWIMMING_STOP
    );

    /**
     * Whether the sprint the server last heard about was stopped by this handler rather
     * than by vanilla.
     *
     * <p>{@link LocalPlayer} reports a sprint only when the flag differs from the one it
     * last sent, and it does that before this handler runs. Clearing the flag here
     * therefore leaves it convinced the server still knows about a sprint the mod has
     * since cancelled: while the sprint key stays held, vanilla re-sets the flag every
     * tick, sees no change and stays quiet, so the server never learns that the player is
     * sprinting again once the exhaustion passes. It keeps the player at walking cost with
     * a bar that only the client is draining, until the sprint is toggled by hand. Both
     * the stop and the resume are therefore sent from here.
     */
    private static boolean sprintStoppedByMod;
    private static int sprintResumeDelay;

    private ClientMovementHandler() {
    }

    public static void forceStopFastMovement() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        sprintResumeDelay = SPRINT_RESUME_DELAY_TICKS;
        stopFastMovement(player);
    }

    //? if forge {
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tick();
    }
    //?} else {
    /*@SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        tick();
    }
    *///?}

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null) {
            SPRINT_REPORT.reset();
            FAST_SWIMMING_REPORT.reset();
            sprintStoppedByMod = false;
            sprintResumeDelay = 0;
            return;
        }

        if (sprintResumeDelay > 0) {
            sprintResumeDelay--;
        }

        GameType gameMode = minecraft.gameMode.getPlayerMode();
        if (gameMode != GameType.SURVIVAL && gameMode != GameType.ADVENTURE) {
            resumeSprintIfStoppedByMod(player);
            SPRINT_REPORT.update(false);
            FAST_SWIMMING_REPORT.update(false);
            return;
        }

        boolean exhausted = EnergyOverlayHandler.getEnergyValue()
                < PlayerEnergyManager.SPRINT_ENERGY_THRESHOLD;
        boolean fatigued = MixEnergyEffects.isFatigued(player);
        if (exhausted || fatigued) {
            stopFastMovement(player);
        } else {
            resumeSprintIfStoppedByMod(player);
        }

        boolean fastMovementAllowed = !exhausted && !fatigued;
        boolean fastSwimming = fastMovementAllowed
                && player.isInWater()
                && (player.isSwimming() || player.isSprinting());
        SPRINT_REPORT.update(fastMovementAllowed && player.isSprinting());
        FAST_SWIMMING_REPORT.update(fastSwimming);
    }

    private static void stopFastMovement(LocalPlayer player) {
        player.setSwimming(false);
        if (!player.isSprinting()) {
            // Vanilla saw the sprint end itself and told the server, so both sides agree
            // and the resume below has nothing left to repair.
            sprintStoppedByMod = false;
            return;
        }

        player.setSprinting(false);
        if (!sprintStoppedByMod) {
            sendSprintCommand(player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING);
            sprintStoppedByMod = true;
        }
    }

    private static void resumeSprintIfStoppedByMod(LocalPlayer player) {
        if (!sprintStoppedByMod || sprintResumeDelay > 0) {
            return;
        }

        sprintStoppedByMod = false;
        if (player.isSprinting()) {
            sendSprintCommand(player, ServerboundPlayerCommandPacket.Action.START_SPRINTING);
        }
        // When the player is not sprinting any more, vanilla notices the flag differs from
        // the sprint it last reported and sends the stop itself on the next tick.
    }

    private static void sendSprintCommand(
            LocalPlayer player,
            ServerboundPlayerCommandPacket.Action action
    ) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundPlayerCommandPacket(player, action));
        }
    }

    /** One reported movement state: sent when it changes and refreshed while it holds. */
    private static final class MovementReport {
        private final EnergyActionPacket.ActionType startAction;
        private final EnergyActionPacket.ActionType stopAction;
        private boolean reported;
        private int heartbeat;

        private MovementReport(
                EnergyActionPacket.ActionType startAction,
                EnergyActionPacket.ActionType stopAction
        ) {
            this.startAction = startAction;
            this.stopAction = stopAction;
        }

        private void update(boolean active) {
            boolean heartbeatDue = active && ++heartbeat >= MOVEMENT_HEARTBEAT_TICKS;
            if (active != reported || heartbeatDue) {
                NetworkHandler.sendToServer(
                        new EnergyActionPacket(active ? startAction : stopAction)
                );
                reported = active;
                heartbeat = 0;
            } else if (!active) {
                heartbeat = 0;
            }
        }

        private void reset() {
            reported = false;
            heartbeat = 0;
        }
    }
}
