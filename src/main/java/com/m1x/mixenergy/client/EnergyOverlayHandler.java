package com.m1x.mixenergy.client;

import com.m1x.mixenergy.network.EnergyActionPacket;
import com.m1x.mixenergy.network.NetworkHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "mixenergy", value = Dist.CLIENT)
public class EnergyOverlayHandler {
    private static final ResourceLocation CENTER = new ResourceLocation("mixenergy", "textures/gui/energy_bar/center.png");
    private static final ResourceLocation ENERGY_BAR_LEFT = new ResourceLocation("mixenergy", "textures/gui/energy_bar/energy_bar_left.png");
    private static final ResourceLocation ENERGY_BAR_RIGHT = new ResourceLocation("mixenergy", "textures/gui/energy_bar/energy_bar_right.png");
    private static final ResourceLocation ENERGY_BAR_BG_LEFT = new ResourceLocation("mixenergy", "textures/gui/energy_bar/energy_bar_bg_left.png");
    private static final ResourceLocation ENERGY_BAR_BG_RIGHT = new ResourceLocation("mixenergy", "textures/gui/energy_bar/energy_bar_bg_right.png");
    private static final ResourceLocation LEFT_FRAME = new ResourceLocation("mixenergy", "textures/gui/energy_bar/left_frame.png");
    private static final ResourceLocation RIGHT_FRAME = new ResourceLocation("mixenergy", "textures/gui/energy_bar/right_frame.png");

    private static final int CENTER_WIDTH = 11;
    private static final int CENTER_HEIGHT = 10;
    private static final int ENERGY_BAR_WIDTH = 9;
    private static final int ENERGY_BAR_HEIGHT = 10;
    private static final int FRAME_WIDTH = 3;
    private static final int FRAME_HEIGHT = 10;

    private static float ENERGY_VALUE = 27.0f;
    private static float MAX_ENERGY_VALUE = 27.0f;

    private static final ResourceLocation[] CENTER_ANIMATION = new ResourceLocation[18];
    private static boolean isAnimating = false;
    private static long animationStartTime = 0;
    private static final int ANIMATION_FRAME_DURATION = 35;
    private static final int TOTAL_ANIMATION_DURATION = ANIMATION_FRAME_DURATION * 18;

    private static float lastEnergyValue = ENERGY_VALUE;
    private static float overlayAlpha = 0.0f;
    private static long lastEnergyChangeTime = 0;
    private static final float FADE_DURATION = 2000.0f;
    private static final float FADE_SPEED = 0.003f;
    private static final float MIN_ALPHA = 0.0f;
    private static final float MAX_ALPHA = 1.0f;
    private static float targetAlpha = 0.0f;
    private static final float APPEAR_SPEED = 0.003f;

    static {
        for (int i = 0; i < 18; i++) {
            CENTER_ANIMATION[i] = new ResourceLocation("mixenergy",
                    "textures/gui/energy_bar/center_full_" + (i + 1) + ".png");
        }
    }

    public static void playCenterAnimation() {
        isAnimating = true;
        animationStartTime = System.currentTimeMillis();
    }

    private static void renderCenter(GuiGraphics guiGraphics, int x, int y) {
        if (isAnimating) {
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - animationStartTime;

            if (elapsedTime >= TOTAL_ANIMATION_DURATION) {
                isAnimating = false;
                RenderSystem.setShaderTexture(0, CENTER);
                guiGraphics.blit(CENTER, x, y, 0, 0, CENTER_WIDTH, CENTER_HEIGHT, CENTER_WIDTH, CENTER_HEIGHT);
            } else {
                int frameIndex = (int) (elapsedTime / ANIMATION_FRAME_DURATION);
                frameIndex = Math.min(frameIndex, 17);

                RenderSystem.setShaderTexture(0, CENTER_ANIMATION[frameIndex]);
                guiGraphics.blit(CENTER_ANIMATION[frameIndex], x, y, 0, 0,
                        CENTER_WIDTH, CENTER_HEIGHT, CENTER_WIDTH, CENTER_HEIGHT);
            }
        } else {
            RenderSystem.setShaderTexture(0, CENTER);
            guiGraphics.blit(CENTER, x, y, 0, 0, CENTER_WIDTH, CENTER_HEIGHT, CENTER_WIDTH, CENTER_HEIGHT);
        }
    }

    public static float getEnergyValue() {
        return ENERGY_VALUE;
    }

    public static float getMaxEnergyValue() {
        return MAX_ENERGY_VALUE;
    }

    private static void updateAlpha() {
        long currentTime = System.currentTimeMillis();
        long timeSinceChange = currentTime - lastEnergyChangeTime;

        if (ENERGY_VALUE != lastEnergyValue) {
            lastEnergyChangeTime = currentTime;
            targetAlpha = MAX_ALPHA;
            lastEnergyValue = ENERGY_VALUE;
        }

        if (ENERGY_VALUE <= 0) {
            targetAlpha = MAX_ALPHA;
        } else if (timeSinceChange > FADE_DURATION) {
            targetAlpha = MIN_ALPHA;
        }

        if (Math.abs(overlayAlpha - targetAlpha) > 0.001f) {
            float speed = FADE_SPEED;
            if (overlayAlpha < targetAlpha) {
                overlayAlpha = Math.min(overlayAlpha + speed, targetAlpha);
            } else {
                overlayAlpha = Math.max(overlayAlpha - speed, targetAlpha);
            }
        }
    }

    public static void setEnergyValue(float value) {
        float oldValue = ENERGY_VALUE;
        ENERGY_VALUE = Math.max(0, Math.min(value, MAX_ENERGY_VALUE));
        
        if (oldValue != ENERGY_VALUE) {
            lastEnergyChangeTime = System.currentTimeMillis();
            targetAlpha = MAX_ALPHA;
        }
        
        if (oldValue < MAX_ENERGY_VALUE && ENERGY_VALUE >= MAX_ENERGY_VALUE) {
            playCenterAnimation();
        }
        
        lastEnergyValue = oldValue;
    }

    public static void setMaxEnergyValue(float value) {
        float oldMaxValue = MAX_ENERGY_VALUE;
        MAX_ENERGY_VALUE = value;
        ENERGY_VALUE = Math.min(ENERGY_VALUE, MAX_ENERGY_VALUE);
        
        if (ENERGY_VALUE >= MAX_ENERGY_VALUE && oldMaxValue != MAX_ENERGY_VALUE) {
            playCenterAnimation();
        }
    }

    @SubscribeEvent
    public static void onRenderGameOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GameType gameMode = mc.gameMode.getPlayerMode();
        if (gameMode != GameType.SURVIVAL && gameMode != GameType.ADVENTURE) {
            return;
        }

        updateAlpha();

        if (overlayAlpha <= MIN_ALPHA && ENERGY_VALUE > 0) {
            return;
        }

        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type()) {
            GuiGraphics guiGraphics = event.getGuiGraphics();
            int screenWidth = mc.getWindow().getGuiScaledWidth();
            int screenHeight = mc.getWindow().getGuiScaledHeight();

            int fullBars = (int) (ENERGY_VALUE / ENERGY_BAR_WIDTH);
            int partialPixels = (int) (ENERGY_VALUE % ENERGY_BAR_WIDTH);

            int maxFullBars = (int) (MAX_ENERGY_VALUE / ENERGY_BAR_WIDTH);
            int maxPartialPixels = (int) (MAX_ENERGY_VALUE % ENERGY_BAR_WIDTH);

            int totalWidth = CENTER_WIDTH + (maxFullBars * 2 * ENERGY_BAR_WIDTH) + (maxPartialPixels * 2) + (2 * FRAME_WIDTH);
            int startX = (screenWidth - totalWidth) / 2;
            
            int yOffset = calculateYOffset(mc.player);
            int startY = screenHeight - 51 - yOffset;
            
            int centerX = startX + FRAME_WIDTH + (maxFullBars * ENERGY_BAR_WIDTH) + maxPartialPixels;

            int initialX = startX;
            int leftEnergyEndX = centerX;

            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, overlayAlpha);

            RenderSystem.setShaderTexture(0, LEFT_FRAME);
            guiGraphics.blit(LEFT_FRAME, startX, startY, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);
            startX += FRAME_WIDTH;

            for (int i = 0; i < maxFullBars; i++) {
                RenderSystem.setShaderTexture(0, ENERGY_BAR_BG_LEFT);
                guiGraphics.blit(ENERGY_BAR_BG_LEFT, startX + (i * ENERGY_BAR_WIDTH), startY, 0, 0,
                        ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);
            }
            startX += maxFullBars * ENERGY_BAR_WIDTH;

            if (maxPartialPixels > 0) {
                RenderSystem.setShaderTexture(0, ENERGY_BAR_BG_LEFT);
                guiGraphics.blit(ENERGY_BAR_BG_LEFT, startX, startY, 0, 0,
                        maxPartialPixels, ENERGY_BAR_HEIGHT, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);
            }

            startX = leftEnergyEndX - (fullBars * ENERGY_BAR_WIDTH) - partialPixels;

            if (partialPixels > 0) {
                RenderSystem.setShaderTexture(0, ENERGY_BAR_LEFT);
                guiGraphics.blit(ENERGY_BAR_LEFT, startX, startY,
                        ENERGY_BAR_WIDTH - partialPixels, 0,
                        partialPixels, ENERGY_BAR_HEIGHT,
                        ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);
                startX += partialPixels;
            }

            for (int i = 0; i < fullBars; i++) {
                RenderSystem.setShaderTexture(0, ENERGY_BAR_LEFT);
                guiGraphics.blit(ENERGY_BAR_LEFT, startX + (i * ENERGY_BAR_WIDTH), startY, 0, 0,
                        ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);
            }

            startX = centerX;
            renderCenter(guiGraphics, startX, startY);
            startX += CENTER_WIDTH;

            int afterCenterX = startX;

            for (int i = 0; i < maxFullBars; i++) {
                RenderSystem.setShaderTexture(0, ENERGY_BAR_BG_RIGHT);
                guiGraphics.blit(ENERGY_BAR_BG_RIGHT, startX + (i * ENERGY_BAR_WIDTH), startY, 0, 0,
                        ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);
            }
            startX += maxFullBars * ENERGY_BAR_WIDTH;

            if (maxPartialPixels > 0) {
                RenderSystem.setShaderTexture(0, ENERGY_BAR_BG_RIGHT);
                guiGraphics.blit(ENERGY_BAR_BG_RIGHT, startX, startY, 0, 0,
                        maxPartialPixels, ENERGY_BAR_HEIGHT, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);
            }

            startX = afterCenterX;

            for (int i = 0; i < fullBars; i++) {
                RenderSystem.setShaderTexture(0, ENERGY_BAR_RIGHT);
                guiGraphics.blit(ENERGY_BAR_RIGHT, startX + (i * ENERGY_BAR_WIDTH), startY, 0, 0,
                        ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);
            }
            startX += fullBars * ENERGY_BAR_WIDTH;

            if (partialPixels > 0) {
                RenderSystem.setShaderTexture(0, ENERGY_BAR_RIGHT);
                guiGraphics.blit(ENERGY_BAR_RIGHT, startX, startY, 0, 0,
                        partialPixels, ENERGY_BAR_HEIGHT, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT);
            }

            startX = initialX + totalWidth - FRAME_WIDTH;
            RenderSystem.setShaderTexture(0, RIGHT_FRAME);
            guiGraphics.blit(RIGHT_FRAME, startX, startY, 0, 0, FRAME_WIDTH, FRAME_HEIGHT, FRAME_WIDTH, FRAME_HEIGHT);

            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
    }

    private static int calculateYOffset(Player player) {
        int offset = 0;
        boolean hasLeftSideElements = false;
        
        // Calculate health rows (each row is up to 10 hearts / 20 health points)
        float healthAndAbsorption = player.getHealth() + player.getAbsorptionAmount();
        int healthRows = (int) Math.ceil(healthAndAbsorption / 20.0f);
        
        // For each extra row beyond the first, add 10px
        if (healthRows > 1) {
            offset += (healthRows - 1) * 10;
            hasLeftSideElements = true;
        }
        
        // Always add offset for armor if present (armor bar is always above health)
        if (player.getArmorValue() > 0) {
            offset += 10;
            hasLeftSideElements = true;
        }
        
        // Add offset for air bar only if no left side elements are pushing the HUD down
        // Air bar is on the right side of the screen
        boolean isUnderwater = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) || 
                              player.getAirSupply() < player.getMaxAirSupply();
        if (isUnderwater && !hasLeftSideElements) {
            offset += 10;
        }
        
        return offset;
    }
}