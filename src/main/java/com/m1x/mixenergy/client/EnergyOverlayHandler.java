package com.m1x.mixenergy.client;

import com.m1x.mixenergy.common.config.MixEnergyConfig;
import com.m1x.mixenergy.common.PlayerEnergyManager;
import com.m1x.mixenergy.registry.MixEnergyEffects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mixenergy", value = Dist.CLIENT)
public final class EnergyOverlayHandler {
    private static final ResourceLocation CENTER =
            texture("textures/gui/energy_bar/center.png");
    private static final ResourceLocation ENERGY_BAR_LEFT =
            texture("textures/gui/energy_bar/energy_bar_left.png");
    private static final ResourceLocation ENERGY_BAR_RIGHT =
            texture("textures/gui/energy_bar/energy_bar_right.png");
    private static final ResourceLocation ENERGY_BAR_BG_LEFT =
            texture("textures/gui/energy_bar/energy_bar_bg_left.png");
    private static final ResourceLocation ENERGY_BAR_BG_RIGHT =
            texture("textures/gui/energy_bar/energy_bar_bg_right.png");
    private static final ResourceLocation LEFT_FRAME =
            texture("textures/gui/energy_bar/left_frame.png");
    private static final ResourceLocation RIGHT_FRAME =
            texture("textures/gui/energy_bar/right_frame.png");
    private static final ResourceLocation LEFT_FRAME_FULL =
            texture("textures/gui/energy_bar/left_frame_full.png");
    private static final ResourceLocation RIGHT_FRAME_FULL =
            texture("textures/gui/energy_bar/right_frame_full.png");
    private static final ResourceLocation[] CENTER_ANIMATION = new ResourceLocation[18];

    private static final int CENTER_WIDTH = 11;
    private static final int BAR_TEXTURE_WIDTH = 9;
    private static final int BAR_HEIGHT = 10;
    private static final int FRAME_WIDTH = 3;
    private static final int MAX_HALF_BAR_WIDTH = 90;
    private static final int ANIMATION_FRAME_DURATION_MILLIS = 35;
    private static final int FADE_DELAY_MILLIS = 2000;
    private static final int FADE_TRANSITION_MILLIS = 260;
    private static final int VISUAL_UPDATE_INTERVAL_TICKS = 4;

    private static float energyValue = 27.0f;
    private static float displayedEnergyValue = 27.0f;
    private static float projectedEnergyValue = 27.0f;
    private static float maxEnergyValue = 27.0f;
    private static float serverEnergyTrendPerTick;
    private static float sprintCostPerTick = 0.25f;
    private static float swimmingCostPerTick = 0.25f;
    private static float overlayAlpha;
    private static long lastEnergyChangeTime = Util.getMillis();
    private static long lastAlphaUpdateTime = Util.getMillis();
    private static long animationStartTime;
    private static int visualUpdateTicker;
    private static boolean animating;
    private static boolean hasServerSnapshot;

    static {
        for (int i = 0; i < CENTER_ANIMATION.length; i++) {
            CENTER_ANIMATION[i] = texture(
                    "textures/gui/energy_bar/center_full_" + (i + 1) + ".png"
            );
        }
    }

    private EnergyOverlayHandler() {
    }

    private static ResourceLocation texture(String path) {
        return new ResourceLocation("mixenergy", path);
    }

    public static float getEnergyValue() {
        return energyValue;
    }

    public static float getMaxEnergyValue() {
        return maxEnergyValue;
    }

    public static void applyServerUpdate(
            float value,
            float maxValue,
            float energyTrendPerTick,
            float serverSprintCostPerTick,
            float serverSwimmingCostPerTick
    ) {
        setMaxEnergyValue(maxValue);
        float previous = energyValue;
        energyValue = Mth.clamp(value, 0.0f, maxEnergyValue);
        projectedEnergyValue = energyValue;
        serverEnergyTrendPerTick = energyTrendPerTick;
        sprintCostPerTick = Math.max(0.0f, serverSprintCostPerTick);
        swimmingCostPerTick = Math.max(0.0f, serverSwimmingCostPerTick);

        if (previous != energyValue) {
            lastEnergyChangeTime = Util.getMillis();
        }
        if (previous < maxEnergyValue && energyValue >= maxEnergyValue) {
            animating = true;
            animationStartTime = Util.getMillis();
        }
        if (!hasServerSnapshot
                || energyValue < PlayerEnergyManager.SPRINT_ENERGY_THRESHOLD) {
            displayedEnergyValue = energyValue;
        }
        hasServerSnapshot = true;
    }

    public static void setMaxEnergyValue(float value) {
        maxEnergyValue = Math.max(1.0f, value);
        energyValue = Math.min(energyValue, maxEnergyValue);
        displayedEnergyValue = Math.min(displayedEnergyValue, maxEnergyValue);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        float trendPerTick = getClientEnergyTrend();
        projectedEnergyValue = Mth.clamp(
                projectedEnergyValue + trendPerTick,
                0.0f,
                maxEnergyValue
        );
        if (++visualUpdateTicker < VISUAL_UPDATE_INTERVAL_TICKS) {
            return;
        }
        visualUpdateTicker = 0;

        float difference = projectedEnergyValue - displayedEnergyValue;
        float reconciliationDirection = trendPerTick != 0.0f
                ? trendPerTick
                : serverEnergyTrendPerTick;
        if ((reconciliationDirection < 0.0f && difference > 0.0f)
                || (reconciliationDirection > 0.0f && difference < 0.0f)) {
            return;
        }
        if (Math.abs(difference) < 0.05f) {
            displayedEnergyValue = projectedEnergyValue;
            return;
        }

        float expectedStep = Math.abs(trendPerTick) * VISUAL_UPDATE_INTERVAL_TICKS;
        float correctionStep = Math.abs(difference) * 0.5f;
        displayedEnergyValue = Mth.approach(
                displayedEnergyValue,
                projectedEnergyValue,
                Math.max(0.05f, Math.max(expectedStep, correctionStep))
        );
    }

    private static float getClientEnergyTrend() {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.gameMode == null) {
            return 0.0f;
        }

        GameType gameMode = minecraft.gameMode.getPlayerMode();
        if (gameMode != GameType.SURVIVAL && gameMode != GameType.ADVENTURE) {
            return 0.0f;
        }
        if (player.hasEffect(MixEnergyEffects.MIX_ENERGY_SLOWNESS.get())) {
            return Math.max(0.0f, serverEnergyTrendPerTick);
        }
        if (player.isInWater() && (player.isSwimming() || player.isSprinting())) {
            return -swimmingCostPerTick;
        }
        if (player.isSprinting()) {
            return -sprintCostPerTick;
        }
        return Math.max(0.0f, serverEnergyTrendPerTick);
    }

    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.gameMode == null) {
            return;
        }

        GameType gameMode = minecraft.gameMode.getPlayerMode();
        if (gameMode != GameType.SURVIVAL && gameMode != GameType.ADVENTURE) {
            return;
        }

        updateAlpha();
        if (overlayAlpha <= 0.001f && energyValue > 0.0f) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int availableHalfWidth = Math.max(
                BAR_TEXTURE_WIDTH,
                (screenWidth - CENTER_WIDTH - 2 * FRAME_WIDTH - 20) / 2
        );
        int halfWidth = Mth.clamp(
                Math.round(maxEnergyValue),
                BAR_TEXTURE_WIDTH,
                Math.min(MAX_HALF_BAR_WIDTH, availableHalfWidth)
        );
        int totalWidth = 2 * FRAME_WIDTH + 2 * halfWidth + CENTER_WIDTH;
        int[] position = calculateBarPosition(
                screenWidth,
                screenHeight,
                totalWidth,
                player
        );

        float ratio = Mth.clamp(displayedEnergyValue / maxEnergyValue, 0.0f, 1.0f);
        int filledHalfWidth = Math.round(halfWidth * ratio);
        renderBar(graphics, position[0], position[1], halfWidth, filledHalfWidth);
    }

    private static void updateAlpha() {
        long now = Util.getMillis();
        long elapsed = Math.min(100L, Math.max(0L, now - lastAlphaUpdateTime));
        lastAlphaUpdateTime = now;

        float target = energyValue <= 0.0f
                || now - lastEnergyChangeTime <= FADE_DELAY_MILLIS
                ? 1.0f
                : 0.0f;
        float step = elapsed / (float) FADE_TRANSITION_MILLIS;

        if (overlayAlpha < target) {
            overlayAlpha = Math.min(target, overlayAlpha + step);
        } else if (overlayAlpha > target) {
            overlayAlpha = Math.max(target, overlayAlpha - step);
        }
    }

    private static void renderBar(
            GuiGraphics graphics,
            int startX,
            int y,
            int halfWidth,
            int filledHalfWidth
    ) {
        int leftInnerX = startX + FRAME_WIDTH;
        int centerX = leftInnerX + halfWidth;
        int rightInnerX = centerX + CENTER_WIDTH;
        boolean fullEnergy = energyValue >= maxEnergyValue - 0.001f;
        ResourceLocation leftFrame = fullEnergy ? LEFT_FRAME_FULL : LEFT_FRAME;
        ResourceLocation rightFrame = fullEnergy ? RIGHT_FRAME_FULL : RIGHT_FRAME;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, overlayAlpha);

        graphics.blit(
                leftFrame,
                startX,
                y,
                0,
                0,
                FRAME_WIDTH,
                BAR_HEIGHT,
                FRAME_WIDTH,
                BAR_HEIGHT
        );
        renderTiled(graphics, ENERGY_BAR_BG_LEFT, leftInnerX, y, halfWidth);
        renderLeftFill(graphics, centerX, y, filledHalfWidth);
        renderCenter(graphics, centerX, y);
        renderTiled(graphics, ENERGY_BAR_BG_RIGHT, rightInnerX, y, halfWidth);
        renderTiled(graphics, ENERGY_BAR_RIGHT, rightInnerX, y, filledHalfWidth);
        graphics.blit(
                rightFrame,
                rightInnerX + halfWidth,
                y,
                0,
                0,
                FRAME_WIDTH,
                BAR_HEIGHT,
                FRAME_WIDTH,
                BAR_HEIGHT
        );

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static void renderLeftFill(
            GuiGraphics graphics,
            int centerX,
            int y,
            int width
    ) {
        int fullSegments = width / BAR_TEXTURE_WIDTH;
        int partialWidth = width % BAR_TEXTURE_WIDTH;
        int x = centerX - width;

        if (partialWidth > 0) {
            graphics.blit(
                    ENERGY_BAR_LEFT,
                    x,
                    y,
                    BAR_TEXTURE_WIDTH - partialWidth,
                    0,
                    partialWidth,
                    BAR_HEIGHT,
                    BAR_TEXTURE_WIDTH,
                    BAR_HEIGHT
            );
            x += partialWidth;
        }

        for (int segment = 0; segment < fullSegments; segment++) {
            graphics.blit(
                    ENERGY_BAR_LEFT,
                    x + segment * BAR_TEXTURE_WIDTH,
                    y,
                    0,
                    0,
                    BAR_TEXTURE_WIDTH,
                    BAR_HEIGHT,
                    BAR_TEXTURE_WIDTH,
                    BAR_HEIGHT
            );
        }
    }

    private static void renderTiled(
            GuiGraphics graphics,
            ResourceLocation texture,
            int x,
            int y,
            int width
    ) {
        int fullSegments = width / BAR_TEXTURE_WIDTH;
        int partialWidth = width % BAR_TEXTURE_WIDTH;

        for (int segment = 0; segment < fullSegments; segment++) {
            graphics.blit(
                    texture,
                    x + segment * BAR_TEXTURE_WIDTH,
                    y,
                    0,
                    0,
                    BAR_TEXTURE_WIDTH,
                    BAR_HEIGHT,
                    BAR_TEXTURE_WIDTH,
                    BAR_HEIGHT
            );
        }

        if (partialWidth > 0) {
            graphics.blit(
                    texture,
                    x + fullSegments * BAR_TEXTURE_WIDTH,
                    y,
                    0,
                    0,
                    partialWidth,
                    BAR_HEIGHT,
                    BAR_TEXTURE_WIDTH,
                    BAR_HEIGHT
            );
        }
    }

    private static void renderCenter(GuiGraphics graphics, int x, int y) {
        ResourceLocation texture = CENTER;
        if (animating) {
            long elapsed = Util.getMillis() - animationStartTime;
            int frame = (int) (elapsed / ANIMATION_FRAME_DURATION_MILLIS);
            if (frame >= CENTER_ANIMATION.length) {
                animating = false;
            } else {
                texture = CENTER_ANIMATION[frame];
            }
        }

        graphics.blit(
                texture,
                x,
                y,
                0,
                0,
                CENTER_WIDTH,
                BAR_HEIGHT,
                CENTER_WIDTH,
                BAR_HEIGHT
        );
    }

    private static int[] calculateBarPosition(
            int screenWidth,
            int screenHeight,
            int totalWidth,
            Player player
    ) {
        int margin = 10;
        int x;
        int y;

        switch (MixEnergyConfig.ENERGY_BAR_POSITION.get()) {
            case TOP_LEFT -> {
                x = margin;
                y = 8;
            }
            case TOP_RIGHT -> {
                x = screenWidth - totalWidth - margin;
                y = player.getActiveEffects().isEmpty() ? 8 : 40;
            }
            case TOP_CENTER -> {
                x = (screenWidth - totalWidth) / 2;
                y = 25;
            }
            case BOTTOM_LEFT -> {
                x = margin;
                y = screenHeight - 20;
            }
            case BOTTOM_RIGHT -> {
                x = screenWidth - totalWidth - margin;
                y = screenHeight - 20;
            }
            case ABOVE_HOTBAR -> {
                x = (screenWidth - totalWidth) / 2;
                y = screenHeight - 51 - calculateHotbarOffset(player);
            }
            default -> throw new IllegalStateException("Unknown energy bar position");
        }

        return new int[]{x, y};
    }

    private static int calculateHotbarOffset(Player player) {
        int leftOffset = 0;
        boolean leftHudOccupied = false;
        int healthRows = Mth.ceil((player.getHealth() + player.getAbsorptionAmount()) / 20.0f);

        if (healthRows > 1) {
            leftOffset += (healthRows - 1) * 10;
            leftHudOccupied = true;
        }
        if (player.getArmorValue() > 0) {
            leftOffset += 10;
            leftHudOccupied = true;
        }

        int rightOffset = 0;
        if (player.getVehicle() instanceof LivingEntity vehicle && vehicle.isAlive()) {
            int vehicleHealthRows = Mth.clamp(
                    Mth.ceil(vehicle.getMaxHealth() / 20.0f),
                    1,
                    2
            );
            rightOffset = (vehicleHealthRows - 1) * 10;
        }

        boolean underwater = player.isEyeInFluid(FluidTags.WATER)
                || player.getAirSupply() < player.getMaxAirSupply();
        if (underwater && !leftHudOccupied) {
            rightOffset = Math.max(rightOffset, 10);
        }

        return Math.max(leftOffset, rightOffset);
    }
}
