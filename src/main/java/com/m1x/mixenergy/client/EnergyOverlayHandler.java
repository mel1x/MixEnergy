package com.m1x.mixenergy.client;

import com.m1x.mixenergy.MixEnergy;
import com.m1x.mixenergy.common.config.MixEnergyConfig;
import com.m1x.mixenergy.common.PlayerEnergyManager;
import com.m1x.mixenergy.registry.MixEnergyEffects;
// Util moved from the root package into net.minecraft.util in 1.21.11.
//? if <1.21.11 {
import net.minecraft.Util;
//?} else {
/*import net.minecraft.util.Util;
*///?}
import net.minecraft.client.Minecraft;
// GuiGraphics was renamed to GuiGraphicsExtractor in 26.2, when GUI drawing became a
// two-step extract-then-render pass. The drawing calls this class uses are unchanged.
//? if <26 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?}
//? if <1.21.11 {
import net.minecraft.resources.ResourceLocation;
//?} else {
/*import net.minecraft.resources.Identifier;
*///?}
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
//? if forge {
import net.minecraftforge.api.distmarker.Dist;
// Forge replaced the named-overlay event with a LayeredDraw the mod registers into once,
// in the same 1.20.5 release that reworked the vanilla GUI rendering pipeline.
//? if <1.20.5 {
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
//?}
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
*///?}
//? if <1.21.2 {
import com.mojang.blaze3d.systems.RenderSystem;
//?} elif <1.21.6 {
/*import net.minecraft.client.renderer.RenderType;
*///?} else {
/*import net.minecraft.client.renderer.RenderPipelines;
*///?}

import java.util.EnumMap;
import java.util.Map;

//? if forge {
@Mod.EventBusSubscriber(modid = MixEnergy.MOD_ID, value = Dist.CLIENT)
//?} else {
/*@EventBusSubscriber(modid = MixEnergy.MOD_ID, value = Dist.CLIENT)
*///?}
public final class EnergyOverlayHandler {
    private static final int CENTER_WIDTH = 11;
    private static final int BAR_TEXTURE_WIDTH = 9;
    private static final int BAR_HEIGHT = 10;
    private static final int FRAME_WIDTH = 3;
    private static final int MAX_HALF_BAR_WIDTH = 90;
    private static final int ANIMATION_FRAME_DURATION_MILLIS = 35;
    private static final int FADE_DELAY_MILLIS = 2000;
    private static final int FADE_TRANSITION_MILLIS = 260;
    /**
     * Time constant of the bar easing towards the value the client predicts. The bar is
     * advanced once per frame rather than on a tick interval, so the motion stays smooth
     * at any frame rate and a spent action reads on screen within roughly 200 ms.
     */
    private static final float VISUAL_RESPONSE_MILLIS = 70.0f;
    /** Below this the bar is considered settled and is pinned to the predicted value. */
    private static final float VISUAL_SETTLE_EPSILON = 0.01f;
    /** Longest frame the bar is advanced by, so a stall does not make it jump. */
    private static final long MAX_VISUAL_STEP_MILLIS = 250L;
    /**
     * How long the client keeps predicting a moving value on its own without the server
     * confirming it. A value the server is changing is pushed at least every other tick,
     * so anything past a second of silence means the two disagree about whether energy is
     * being spent at all - most likely because the server does not consider the player to
     * be sprinting. The bar then stops running away from the last known value and waits,
     * instead of draining to empty and snapping back to full on the next real update.
     */
    private static final int PREDICTION_GRACE_TICKS = 20;
    private static final int CENTER_ANIMATION_FRAMES = 18;

    /** Half-bar width the config screen's skin preview is drawn at. */
    public static final int PREVIEW_HALF_WIDTH = 24;
    public static final int PREVIEW_WIDTH =
            2 * FRAME_WIDTH + 2 * PREVIEW_HALF_WIDTH + CENTER_WIDTH;
    public static final int PREVIEW_HEIGHT = BAR_HEIGHT;

    private static final Map<MixEnergyConfig.EnergyBarSkin, SkinTextures> SKIN_TEXTURES =
            new EnumMap<>(MixEnergyConfig.EnergyBarSkin.class);

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
    private static long lastVisualUpdateTime = Util.getMillis();
    private static long animationStartTime;
    private static boolean animating;
    private static boolean hasServerSnapshot;
    private static int unconfirmedPredictionTicks;
    /**
     * Alpha every quad in the current draw is multiplied by. The HUD sets it from the fade
     * and the config screen's preview draws fully opaque, so both paths share the drawing
     * code below without either having to thread the value through every call.
     */
    private static float renderAlpha = 1.0f;

    static {
        for (MixEnergyConfig.EnergyBarSkin skin : MixEnergyConfig.EnergyBarSkin.values()) {
            SKIN_TEXTURES.put(skin, new SkinTextures(skin));
        }
    }

    private EnergyOverlayHandler() {
    }

    //? if <1.21.11 {
    private static ResourceLocation texture(String path) {
        return MixEnergy.id(path);
    }
    //?} else {
    /*private static Identifier texture(String path) {
        return MixEnergy.id(path);
    }
    *///?}

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
            float serverSwimmingCostPerTick,
            boolean instantVisual
    ) {
        setMaxEnergyValue(maxValue);
        float previous = energyValue;
        energyValue = Mth.clamp(value, 0.0f, maxEnergyValue);
        projectedEnergyValue = energyValue;
        serverEnergyTrendPerTick = energyTrendPerTick;
        unconfirmedPredictionTicks = 0;
        sprintCostPerTick = Math.max(0.0f, serverSprintCostPerTick);
        swimmingCostPerTick = Math.max(0.0f, serverSwimmingCostPerTick);

        if (previous != energyValue) {
            lastEnergyChangeTime = Util.getMillis();
        }
        if (previous < maxEnergyValue && energyValue >= maxEnergyValue) {
            animating = true;
            animationStartTime = Util.getMillis();
        }
        if (instantVisual
                || !hasServerSnapshot
                || energyValue < PlayerEnergyManager.SPRINT_ENERGY_THRESHOLD) {
            displayedEnergyValue = energyValue;
        }
        hasServerSnapshot = true;
    }

    public static void setMaxEnergyValue(float value) {
        maxEnergyValue = Math.max(1.0f, value);
        energyValue = Math.min(energyValue, maxEnergyValue);
        displayedEnergyValue = Math.min(displayedEnergyValue, maxEnergyValue);
        projectedEnergyValue = Math.min(projectedEnergyValue, maxEnergyValue);
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

    /**
     * Carries the prediction forward by one tick. Continuous drain is predicted locally so
     * the bar keeps moving between server updates instead of waiting for the next packet.
     */
    private static void tick() {
        if (Minecraft.getInstance().player == null) {
            // Leaving a world keeps the last world's values in these fields, so drop the
            // snapshot and let the next server update be adopted as-is.
            hasServerSnapshot = false;
            animating = false;
            unconfirmedPredictionTicks = 0;
            return;
        }

        float trend = getClientEnergyTrend();
        if (trend == 0.0f) {
            unconfirmedPredictionTicks = 0;
            return;
        }
        if (unconfirmedPredictionTicks >= PREDICTION_GRACE_TICKS) {
            // Nothing has confirmed this prediction for a second. Fall back on the last
            // value the server actually sent rather than leaving the bar parked at one the
            // client invented and has no way of correcting.
            projectedEnergyValue = energyValue;
            return;
        }

        unconfirmedPredictionTicks++;
        projectedEnergyValue = Mth.clamp(
                projectedEnergyValue + trend,
                0.0f,
                maxEnergyValue
        );
    }

    /**
     * Eases the drawn value towards the prediction. This runs per frame off the wall clock,
     * so the bar moves at the display's refresh rate and closes any gap in a fixed amount
     * of time no matter which direction it is in.
     */
    private static void advanceDisplayedEnergy() {
        long now = Util.getMillis();
        long elapsed = Math.min(MAX_VISUAL_STEP_MILLIS, Math.max(0L, now - lastVisualUpdateTime));
        lastVisualUpdateTime = now;

        float difference = projectedEnergyValue - displayedEnergyValue;
        if (elapsed <= 0L) {
            return;
        }
        if (Math.abs(difference) <= VISUAL_SETTLE_EPSILON) {
            displayedEnergyValue = projectedEnergyValue;
            return;
        }

        // Fraction of the remaining gap to close this frame. Independent of frame time, so
        // the bar takes the same wall-clock time to catch up at any frame rate: about 94%
        // of the gap within 200 ms, and the rest lands shortly after.
        float closedFraction = 1.0f - (float) Math.exp(-elapsed / VISUAL_RESPONSE_MILLIS);
        float previous = displayedEnergyValue;
        displayedEnergyValue = Mth.approach(
                displayedEnergyValue,
                projectedEnergyValue,
                Math.abs(difference) * closedFraction
        );

        // A bar that is still visibly moving counts as a change, so the fade waits for the
        // motion to finish. The server only reports whole changes of its own value, and
        // predicted drain between two of those reports would otherwise let the bar fade
        // out while it is still sliding.
        if (Math.abs(displayedEnergyValue - previous) > VISUAL_SETTLE_EPSILON) {
            lastEnergyChangeTime = now;
        }
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
        // The same two conditions under which ClientMovementHandler stops fast movement:
        // while either holds the server charges nothing, so neither may the prediction.
        if (MixEnergyEffects.isFatigued(player)
                || energyValue < PlayerEnergyManager.SPRINT_ENERGY_THRESHOLD) {
            return Math.max(0.0f, serverEnergyTrendPerTick);
        }
        // A drain is only ever predicted while the server's own last word was that it is
        // charging for one. Predicting it from the local sprint flag alone is what let the
        // bar drain against a server that had stopped the sprint on its side: the value
        // never moved there, so no update came to correct the bar either, and it emptied
        // and faded out while the server still held a full one.
        if (serverEnergyTrendPerTick >= 0.0f) {
            return serverEnergyTrendPerTick;
        }
        if (player.isInWater() && (player.isSwimming() || player.isSprinting())) {
            return -swimmingCostPerTick;
        }
        if (player.isSprinting()) {
            return -sprintCostPerTick;
        }
        // Still charged for a movement the player has just stopped; the update that says so
        // is a tick or two out, and until then the bar holds instead of draining.
        return 0.0f;
    }

    // Forge 1.20.1 draws overlays through a named vanilla overlay fired every frame on the
    // Forge event bus. Forge 1.20.5+ replaced that with a LayeredDraw the mod registers a
    // layer into once (see ClientModEvents#onAddGuiOverlayLayers, forge-only), matching how
    // NeoForge moved to named GUI layers fired every frame with the same hotbar identifier.
    //? if forge {
    //? if <1.20.5 {
    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        render(event.getGuiGraphics());
    }
    //?} else {
    /*static void renderLayer(GuiGraphics graphics, float partialTick) {
        render(graphics);
    }
    *///?}
    //?} else {
    /*@SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!VanillaGuiLayers.HOTBAR.equals(event.getName())) {
            return;
        }
        render(event.getGuiGraphics());
    }
    *///?}

    //? if <26 {
    private static void render(GuiGraphics graphics) {
    //?} else {
    /*private static void render(GuiGraphicsExtractor graphics) {
    *///?}
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.gameMode == null) {
            return;
        }

        GameType gameMode = minecraft.gameMode.getPlayerMode();
        if (gameMode != GameType.SURVIVAL && gameMode != GameType.ADVENTURE) {
            return;
        }

        advanceDisplayedEnergy();
        updateAlpha();
        if (overlayAlpha <= 0.001f && energyValue > 0.0f) {
            return;
        }

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
        renderAlpha = overlayAlpha;
        renderBar(
                graphics,
                SKIN_TEXTURES.get(MixEnergyConfig.ENERGY_BAR_SKIN.get()),
                position[0],
                position[1],
                halfWidth,
                filledHalfWidth,
                // Matched against the drawn value, not the last one the server sent, so the
                // frames never claim a full bar while the fill is still draining towards
                // the prediction.
                displayedEnergyValue >= maxEnergyValue - 0.001f,
                true
        );
    }

    /**
     * Draws one skin at a fixed size and fill for the config screen, so the preview there
     * is the same bar the HUD paints rather than a stand-in for it. Always fully opaque and
     * never animated: the preview must not blink out with the HUD's fade or flash the
     * "energy full" animation while a skin is only being looked at.
     */
    //? if <26 {
    public static void renderSkinPreview(
            GuiGraphics graphics,
    //?} else {
    /*public static void renderSkinPreview(
            GuiGraphicsExtractor graphics,
    *///?}
            MixEnergyConfig.EnergyBarSkin skin,
            int x,
            int y,
            float ratio
    ) {
        renderAlpha = 1.0f;
        renderBar(
                graphics,
                SKIN_TEXTURES.get(skin),
                x,
                y,
                PREVIEW_HALF_WIDTH,
                Math.round(PREVIEW_HALF_WIDTH * Mth.clamp(ratio, 0.0f, 1.0f)),
                ratio >= 1.0f,
                false
        );
        renderAlpha = overlayAlpha;
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

    /**
     * Draws one texture quad.
     *
     * <p>Up to 1.21.1 the fade is applied with a global shader colour; from 1.21.2 blit
     * takes a render type and a per-call tint instead, and 1.21.6 replaced the render type
     * with a render pipeline.
     */
    private static void drawTexture(
            //? if <26 {
            GuiGraphics graphics,
            //?} else {
            /*GuiGraphicsExtractor graphics,
            *///?}
            //? if <1.21.11 {
            ResourceLocation texture,
            //?} else {
            /*Identifier texture,
            *///?}
            int x,
            int y,
            int u,
            int v,
            int width,
            int height,
            int textureWidth,
            int textureHeight
    ) {
        //? if <1.21.2 {
        graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        //?} elif <1.21.6 {
        /*graphics.blit(
                RenderType::guiTextured,
                texture,
                x,
                y,
                (float) u,
                (float) v,
                width,
                height,
                textureWidth,
                textureHeight,
                overlayTint()
        );
        *///?} else {
        /*graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture,
                x,
                y,
                (float) u,
                (float) v,
                width,
                height,
                textureWidth,
                textureHeight,
                overlayTint()
        );
        *///?}
    }

    //? if >=1.21.2 {
    /*private static int overlayTint() {
        int alpha = Mth.clamp(Math.round(renderAlpha * 255.0f), 0, 255);
        return (alpha << 24) | 0x00FFFFFF;
    }
    *///?}

    private static void renderBar(
            //? if <26 {
            GuiGraphics graphics,
            //?} else {
            /*GuiGraphicsExtractor graphics,
            *///?}
            SkinTextures skin,
            int startX,
            int y,
            int halfWidth,
            int filledHalfWidth,
            boolean fullEnergy,
            boolean allowAnimation
    ) {
        int leftInnerX = startX + FRAME_WIDTH;
        int centerX = leftInnerX + halfWidth;
        int rightInnerX = centerX + CENTER_WIDTH;
        var leftFrame = fullEnergy ? skin.leftFrameFull : skin.leftFrame;
        var rightFrame = fullEnergy ? skin.rightFrameFull : skin.rightFrame;

        //? if <1.21.2 {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, renderAlpha);
        //?}

        drawTexture(
                graphics,
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
        renderTiled(graphics, skin.backgroundLeft, leftInnerX, y, halfWidth);
        renderLeftFill(graphics, skin, centerX, y, filledHalfWidth);
        renderCenter(graphics, skin, centerX, y, allowAnimation);
        renderTiled(graphics, skin.backgroundRight, rightInnerX, y, halfWidth);
        renderTiled(graphics, skin.fillRight, rightInnerX, y, filledHalfWidth);
        drawTexture(
                graphics,
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

        //? if <1.21.2 {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        //?}
    }

    private static void renderLeftFill(
            //? if <26 {
            GuiGraphics graphics,
            //?} else {
            /*GuiGraphicsExtractor graphics,
            *///?}
            SkinTextures skin,
            int centerX,
            int y,
            int width
    ) {
        int fullSegments = width / BAR_TEXTURE_WIDTH;
        int partialWidth = width % BAR_TEXTURE_WIDTH;
        int x = centerX - width;

        if (partialWidth > 0) {
            drawTexture(
                    graphics,
                    skin.fillLeft,
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
            drawTexture(
                    graphics,
                    skin.fillLeft,
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
            //? if <26 {
            GuiGraphics graphics,
            //?} else {
            /*GuiGraphicsExtractor graphics,
            *///?}
            //? if <1.21.11 {
            ResourceLocation texture,
            //?} else {
            /*Identifier texture,
            *///?}
            int x,
            int y,
            int width
    ) {
        int fullSegments = width / BAR_TEXTURE_WIDTH;
        int partialWidth = width % BAR_TEXTURE_WIDTH;

        for (int segment = 0; segment < fullSegments; segment++) {
            drawTexture(
                    graphics,
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
            drawTexture(
                    graphics,
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

    //? if <26 {
    private static void renderCenter(
            GuiGraphics graphics,
    //?} else {
    /*private static void renderCenter(
            GuiGraphicsExtractor graphics,
    *///?}
            SkinTextures skin,
            int x,
            int y,
            boolean allowAnimation
    ) {
        var texture = skin.center;
        if (allowAnimation && animating) {
            long elapsed = Util.getMillis() - animationStartTime;
            int frame = (int) (elapsed / ANIMATION_FRAME_DURATION_MILLIS);
            if (frame >= skin.centerAnimation.length) {
                animating = false;
            } else {
                texture = skin.centerAnimation[frame];
            }
        }

        drawTexture(
                graphics,
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

    /**
     * The nine textures plus animation strip one skin is drawn from. Every skin ships the
     * same file names under its own directory, so a skin is nothing more than the prefix
     * those names are resolved against.
     */
    private static final class SkinTextures {
        //? if <1.21.11 {
        private final ResourceLocation center;
        private final ResourceLocation fillLeft;
        private final ResourceLocation fillRight;
        private final ResourceLocation backgroundLeft;
        private final ResourceLocation backgroundRight;
        private final ResourceLocation leftFrame;
        private final ResourceLocation rightFrame;
        private final ResourceLocation leftFrameFull;
        private final ResourceLocation rightFrameFull;
        private final ResourceLocation[] centerAnimation =
                new ResourceLocation[CENTER_ANIMATION_FRAMES];
        //?} else {
        /*private final Identifier center;
        private final Identifier fillLeft;
        private final Identifier fillRight;
        private final Identifier backgroundLeft;
        private final Identifier backgroundRight;
        private final Identifier leftFrame;
        private final Identifier rightFrame;
        private final Identifier leftFrameFull;
        private final Identifier rightFrameFull;
        private final Identifier[] centerAnimation =
                new Identifier[CENTER_ANIMATION_FRAMES];
        *///?}

        private SkinTextures(MixEnergyConfig.EnergyBarSkin skin) {
            String directory = "textures/gui/energy_bar/" + skin.getTextureDirectory();
            center = texture(directory + "center.png");
            fillLeft = texture(directory + "energy_bar_left.png");
            fillRight = texture(directory + "energy_bar_right.png");
            backgroundLeft = texture(directory + "energy_bar_bg_left.png");
            backgroundRight = texture(directory + "energy_bar_bg_right.png");
            leftFrame = texture(directory + "left_frame.png");
            rightFrame = texture(directory + "right_frame.png");
            leftFrameFull = texture(directory + "left_frame_full.png");
            rightFrameFull = texture(directory + "right_frame_full.png");
            for (int frame = 0; frame < centerAnimation.length; frame++) {
                centerAnimation[frame] =
                        texture(directory + "center_full_" + (frame + 1) + ".png");
            }
        }
    }
}
