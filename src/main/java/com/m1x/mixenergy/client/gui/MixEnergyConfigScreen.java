package com.m1x.mixenergy.client.gui;

import com.m1x.mixenergy.MixEnergy;
import com.m1x.mixenergy.client.EnergyOverlayHandler;
import com.m1x.mixenergy.common.config.MixEnergyConfig;
import net.minecraft.ChatFormatting;
// GuiGraphics was renamed to GuiGraphicsExtractor in 26.2, when screen drawing became a
// two-step extract-then-render pass.
//? if <26 {
import net.minecraft.client.gui.GuiGraphics;
//?} else {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?}
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
//? if forge {
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
//?} else {
/*import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
*///?}

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MixEnergyConfigScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 440;
    private static final int ROW_HEIGHT = 16;
    private static final double MAX_REGEN_SPEED_MULTIPLIER = 5.0;
    // The config itself accepts far more, but a slider that has to be readable needs a
    // range players will actually use; larger amounts stay editable in the toml file.
    private static final double MAX_CONSUMABLE_ENERGY_RESTORE = 30.0;
    /** Fill the bar preview is drawn at, so both the fill and its background are visible. */
    private static final float PREVIEW_FILL_RATIO = 0.6f;
    // Fully opaque: the panel used to let a sliver of the world (blurred, when the
    // "Menu Background Blurriness" option is on) show through its ~85% alpha, which
    // softened the edges of the text drawn on top of it.
    private static final int COLOR_PANEL = 0xFF14181E;
    private static final int COLOR_ROW = 0xA6222830;
    private static final int COLOR_ROW_ALT = 0xA61D232A;
    private static final int COLOR_BORDER = 0xFF52636A;
    private static final int COLOR_ACCENT = 0xFF78AAA6;
    private static final int COLOR_PRIMARY_TEXT = 0xFFE8EEF0;
    private static final int COLOR_SECONDARY_TEXT = 0xFFA9B5B9;
    private final Screen parentScreen;
    private final List<AbstractWidget> interfaceWidgets = new ArrayList<>();
    private final List<AbstractWidget> gameplayWidgets = new ArrayList<>();
    private final List<String> gameplayLabelKeys = new ArrayList<>();
    private final List<String> gameplayDescriptionKeys = new ArrayList<>();
    private final Map<Button, BooleanValue> sourceButtons = new LinkedHashMap<>();
    private final Map<Button, PositionChoice> positionButtons = new LinkedHashMap<>();
    private final Map<Button, MixEnergyConfig.EnergyBarSkin> skinButtons = new LinkedHashMap<>();

    private Tab activeTab = Tab.INTERFACE;
    private boolean remoteServer;
    private boolean combatRollLoaded;
    private boolean betterCombatLoaded;
    private int panelX;
    private int panelWidth;
    private int contentTop;
    private Button interfaceTabButton;
    private Button gameplayTabButton;
    private Button resetButton;
    private RegenSpeedSlider regenSpeedSlider;
    private ConsumableRestoreSlider consumableRestoreSlider;

    public MixEnergyConfigScreen(Screen parentScreen) {
        super(Component.translatable("mixenergy.config.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        interfaceWidgets.clear();
        gameplayWidgets.clear();
        gameplayLabelKeys.clear();
        gameplayDescriptionKeys.clear();
        sourceButtons.clear();
        positionButtons.clear();
        skinButtons.clear();

        remoteServer = minecraft.getConnection() != null && !minecraft.hasSingleplayerServer();
        combatRollLoaded = ModList.get().isLoaded("combatroll");
        betterCombatLoaded = ModList.get().isLoaded("bettercombat");
        panelWidth = Math.min(PANEL_MAX_WIDTH, width - 24);
        panelX = (width - panelWidth) / 2;
        contentTop = 66;

        int tabGap = 4;
        int tabWidth = (panelWidth - tabGap) / 2;
        interfaceTabButton = addRenderableWidget(Button.builder(
                Component.translatable("mixenergy.config.tab.interface"),
                button -> setActiveTab(Tab.INTERFACE)
        ).bounds(panelX, 43, tabWidth, 20).build());
        gameplayTabButton = addRenderableWidget(Button.builder(
                Component.translatable("mixenergy.config.tab.energy_sources"),
                button -> setActiveTab(Tab.GAMEPLAY)
        ).bounds(panelX + tabWidth + tabGap, 43, tabWidth, 20).build());

        createInterfaceWidgets();
        createGameplayWidgets();

        int footerY = height - 27;
        resetButton = addRenderableWidget(Button.builder(
                Component.translatable("controls.reset"),
                button -> resetCurrentTab()
        ).bounds(width / 2 - 104, footerY, 100, 20).build());
        addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> onClose()
        ).bounds(width / 2 + 4, footerY, 100, 20).build());

        updateTabState();
    }

    private void createInterfaceWidgets() {
        int buttonWidth = 56;
        int buttonHeight = 22;
        int gap = 4;
        int gridWidth = buttonWidth * 3 + gap * 2;
        int startX = width / 2 - gridWidth / 2;
        int startY = contentTop + 14;

        addPositionButton(startX, startY, buttonWidth, buttonHeight, -1, -1,
                MixEnergyConfig.EnergyBarPosition.TOP_LEFT);
        addPositionButton(startX + buttonWidth + gap, startY, buttonWidth, buttonHeight, 0, -1,
                MixEnergyConfig.EnergyBarPosition.TOP_CENTER);
        addPositionButton(startX + (buttonWidth + gap) * 2, startY, buttonWidth, buttonHeight, 1, -1,
                MixEnergyConfig.EnergyBarPosition.TOP_RIGHT);
        addPositionButton(startX, startY + buttonHeight + gap, buttonWidth, buttonHeight, -1, 1,
                MixEnergyConfig.EnergyBarPosition.BOTTOM_LEFT);
        addPositionButton(startX + buttonWidth + gap, startY + buttonHeight + gap,
                buttonWidth, buttonHeight, 0, 1,
                MixEnergyConfig.EnergyBarPosition.ABOVE_HOTBAR);
        addPositionButton(startX + (buttonWidth + gap) * 2, startY + buttonHeight + gap,
                buttonWidth, buttonHeight, 1, 1,
                MixEnergyConfig.EnergyBarPosition.BOTTOM_RIGHT);
        updatePositionButtons();

        // One button per skin on the row below the position grid, aligned to its columns.
        MixEnergyConfig.EnergyBarSkin[] skins = MixEnergyConfig.EnergyBarSkin.values();
        int skinY = startY + (buttonHeight + gap) * 2 + 2;
        for (int index = 0; index < skins.length; index++) {
            addSkinButton(
                    startX + (buttonWidth + gap) * index,
                    skinY,
                    buttonWidth,
                    20,
                    skins[index]
            );
        }
    }

    private void addSkinButton(
            int x,
            int y,
            int width,
            int height,
            MixEnergyConfig.EnergyBarSkin skin
    ) {
        Button button = Button.builder(
                skinName(skin),
                pressed -> {
                    MixEnergyConfig.ENERGY_BAR_SKIN.set(skin);
                    MixEnergyConfig.saveClient();
                }
        ).bounds(x, y, width, height).build();
        button.setTooltip(Tooltip.create(skinName(skin)));
        interfaceWidgets.add(addRenderableWidget(button));
        skinButtons.put(button, skin);
    }

    private void addPositionButton(
            int x,
            int y,
            int width,
            int height,
            int directionX,
            int directionY,
            MixEnergyConfig.EnergyBarPosition position
    ) {
        PositionChoice choice = new PositionChoice(position, directionX, directionY);
        Button button = Button.builder(
                Component.empty(),
                pressed -> {
                    MixEnergyConfig.ENERGY_BAR_POSITION.set(position);
                    MixEnergyConfig.saveClient();
                    updatePositionButtons();
                }
        ).bounds(x, y, width, height).build();
        button.setTooltip(Tooltip.create(positionName(position)));
        interfaceWidgets.add(addRenderableWidget(button));
        positionButtons.put(button, choice);
    }

    private void updatePositionButtons() {
        positionButtons.keySet().forEach(button -> button.setMessage(Component.empty()));
    }

    private void createGameplayWidgets() {
        int row = 0;
        addSourceToggle(
                row++,
                "mixenergy.config.source.sprinting",
                "mixenergy.config.source.sprinting.description",
                MixEnergyConfig.ENERGY_COST_FOR_SPRINTING
        );
        addSourceToggle(
                row++,
                "mixenergy.config.source.swimming",
                "mixenergy.config.source.swimming.description",
                MixEnergyConfig.ENERGY_COST_FOR_SWIMMING
        );
        addSourceToggle(
                row++,
                "mixenergy.config.source.breaking",
                "mixenergy.config.source.breaking.description",
                MixEnergyConfig.ENERGY_COST_FOR_BREAKING_BLOCKS
        );
        addSourceToggle(
                row++,
                "mixenergy.config.source.placing",
                "mixenergy.config.source.placing.description",
                MixEnergyConfig.ENERGY_COST_FOR_PLACING_BLOCKS
        );
        addSourceToggle(
                row++,
                "mixenergy.config.source.attacks",
                "mixenergy.config.source.attacks.description",
                MixEnergyConfig.ENERGY_COST_FOR_ATTACKS
        );
        addSourceToggle(
                row++,
                "mixenergy.config.source.jumping",
                "mixenergy.config.source.jumping.description",
                MixEnergyConfig.ENERGY_COST_FOR_JUMPING
        );
        if (combatRollLoaded) {
            addSourceToggle(
                    row++,
                    "mixenergy.config.source.combat_roll",
                    "mixenergy.config.source.combat_roll.description",
                    MixEnergyConfig.ENERGY_COST_FOR_COMBAT_ROLL
            );
        }
        if (betterCombatLoaded) {
            addSourceToggle(
                    row++,
                    "mixenergy.config.source.better_combat",
                    "mixenergy.config.source.better_combat.description",
                    MixEnergyConfig.ENERGY_COST_FOR_BETTER_COMBAT
            );
        }

        int sliderWidth = 104;
        int sliderX = panelX + panelWidth - sliderWidth - 8;

        regenSpeedSlider = new RegenSpeedSlider(sliderX, gameplayRowY(row++), sliderWidth);
        addSlider(
                "mixenergy.config.regeneration_speed",
                "mixenergy.config.regeneration_speed.description",
                regenSpeedSlider
        );

        consumableRestoreSlider =
                new ConsumableRestoreSlider(sliderX, gameplayRowY(row++), sliderWidth);
        addSlider(
                "mixenergy.config.consumable_restore",
                "mixenergy.config.consumable_restore.description",
                consumableRestoreSlider
        );
    }

    private void addSlider(
            String labelKey,
            String descriptionKey,
            AbstractSliderButton slider
    ) {
        gameplayLabelKeys.add(labelKey);
        gameplayDescriptionKeys.add(descriptionKey);
        slider.active = !remoteServer;
        slider.setTooltip(Tooltip.create(Component.translatable(descriptionKey)));
        gameplayWidgets.add(addRenderableWidget(slider));
    }

    private void addSourceToggle(
            int row,
            String labelKey,
            String descriptionKey,
            BooleanValue value
    ) {
        gameplayLabelKeys.add(labelKey);
        gameplayDescriptionKeys.add(descriptionKey);
        int buttonWidth = 88;
        int y = gameplayRowY(row);
        Button toggle = Button.builder(
                sourceState(value),
                button -> {
                    if (remoteServer) {
                        return;
                    }
                    value.set(!value.get());
                    MixEnergyConfig.saveCommon();
                    button.setMessage(sourceState(value));
                }
        ).bounds(panelX + panelWidth - buttonWidth - 8, y, buttonWidth, ROW_HEIGHT).build();
        toggle.setTooltip(Tooltip.create(Component.translatable(descriptionKey)));
        toggle.active = !remoteServer;

        gameplayWidgets.add(addRenderableWidget(toggle));
        sourceButtons.put(toggle, value);
    }

    private int gameplayRowY(int row) {
        return contentTop + 10 + row * ROW_HEIGHT;
    }

    private Component sourceState(BooleanValue value) {
        if (remoteServer) {
            return Component.translatable("mixenergy.config.server_controlled")
                    .withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable(
                value.get() ? "mixenergy.config.enabled" : "mixenergy.config.disabled"
        ).withStyle(value.get() ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private Component positionName(MixEnergyConfig.EnergyBarPosition position) {
        return Component.translatable(
                "mixenergy.config.position."
                        + position.getName()
        );
    }

    private Component skinName(MixEnergyConfig.EnergyBarSkin skin) {
        return Component.translatable("mixenergy.config.skin." + skin.getName());
    }

    private void setActiveTab(Tab tab) {
        if (activeTab != tab) {
            activeTab = tab;
            updateTabState();
        }
    }

    private void updateTabState() {
        boolean showInterface = activeTab == Tab.INTERFACE;
        interfaceTabButton.active = !showInterface;
        gameplayTabButton.active = showInterface;

        interfaceWidgets.forEach(widget -> widget.visible = showInterface);
        gameplayWidgets.forEach(widget -> widget.visible = !showInterface);

        resetButton.active = showInterface || !remoteServer;
        resetButton.setTooltip(remoteServer && !showInterface
                ? Tooltip.create(Component.translatable("mixenergy.config.server.tooltip"))
                : null);
    }

    private void resetCurrentTab() {
        if (activeTab == Tab.INTERFACE) {
            MixEnergyConfig.ENERGY_BAR_POSITION.set(
                    MixEnergyConfig.EnergyBarPosition.ABOVE_HOTBAR
            );
            MixEnergyConfig.ENERGY_BAR_SKIN.set(MixEnergyConfig.EnergyBarSkin.DEFAULT);
            MixEnergyConfig.saveClient();
            updatePositionButtons();
            return;
        }

        if (remoteServer) {
            return;
        }

        sourceButtons.values().forEach(value ->
                value.set(value != MixEnergyConfig.ENERGY_COST_FOR_JUMPING)
        );
        MixEnergyConfig.ENERGY_REGEN_SPEED_MULTIPLIER.set(1.0);
        MixEnergyConfig.CONSUMABLE_ENERGY_RESTORE.set(8.0);
        MixEnergyConfig.saveCommon();
        sourceButtons.forEach((button, value) -> button.setMessage(sourceState(value)));
        regenSpeedSlider.setConfigValue(1.0);
        consumableRestoreSlider.setConfigValue(8.0);
    }

    // Screen#render was replaced by extractRenderState in 26.2: the screen now records
    // what to draw and the GUI renderer submits it later in the frame.
    //? if <26 {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    //?} else {
    /*@Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
    *///?}
        // Below 1.21.6 this screen still chooses its own background, so it uses
        // renderTransparentBackground: a plain full-screen dim with no world blur, meant
        // for exactly this "one screen layered over the game" case. The alternative,
        // renderBackground, triggers the "Menu Background Blurriness" shader, which then
        // shows through this screen's own (opaque) panel at the edges and, on some
        // versions, has been documented to corrupt state for content drawn after it (see
        // the "Neo:" fix in Screen#renderBlurredBackground for NeoForge issue #1504).
        //
        // From 1.21.6 the vanilla wrapper that calls this method (renderWithTooltip, later
        // renderWithTooltipAndSubtitles, and extractRenderStateWithTooltipAndSubtitles on
        // 26.2) already calls renderBackground/extractBackground itself before invoking
        // this method - calling it again here throws "Can only blur once per frame" - so
        // for those versions the world behind is blurred regardless of what happens here.
        // renderTransparentBackground was only added in 1.20.2, so 1.20.1 uses its own
        // renderBackground - which predates the blur entirely and is therefore safe.
        //? if <1.20.2 {
        renderBackground(graphics);
        //?} elif <1.21.6 {
        /*renderTransparentBackground(graphics);
        *///?}

        graphics.fill(panelX - 6, 36, panelX + panelWidth + 6, height - 32, COLOR_PANEL);
        graphics.fill(panelX - 6, 36, panelX + panelWidth + 6, 37, COLOR_ACCENT);

        centeredText(graphics, title, width / 2, 12, COLOR_PRIMARY_TEXT);
        Component subtitle = Component.translatable(
                remoteServer
                        ? "mixenergy.config.subtitle.multiplayer"
                        : "mixenergy.config.subtitle.local"
        );
        centeredText(
                graphics,
                font.plainSubstrByWidth(subtitle.getString(), width - 24),
                width / 2,
                25,
                COLOR_SECONDARY_TEXT
        );

        if (activeTab == Tab.INTERFACE) {
            renderInterfaceTab(graphics);
        } else {
            renderGameplayTab(graphics);
        }

        graphics.fill(panelX, height - 32, panelX + panelWidth, height - 31, COLOR_BORDER);

        // Draw the widgets by walking the list directly instead of calling super. On
        // Minecraft 1.20.2 - 1.21.5, Screen#render paints the background itself before
        // iterating the widgets, so calling super here would re-run the background - and
        // with it the "Menu Background Blurriness" shader - *after* the panel and text
        // above were already drawn, blurring them while leaving the widgets that follow
        // sharp. This loop is exactly what Screen#render does minus that background call,
        // and it behaves identically on every supported version.
        //? if <26 {
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        //?} else {
        /*for (Renderable renderable : renderables) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        *///?}

        if (activeTab == Tab.INTERFACE) {
            renderPositionArrows(graphics);
            renderSkinSelection(graphics);
        } else {
            renderGameplayTooltip(graphics, mouseX, mouseY);
        }
    }

    //? if <26 {
    private void renderSkinSelection(GuiGraphics graphics) {
    //?} else {
    /*private void renderSkinSelection(GuiGraphicsExtractor graphics) {
    *///?}
        MixEnergyConfig.EnergyBarSkin selected = MixEnergyConfig.ENERGY_BAR_SKIN.get();
        skinButtons.forEach((button, skin) -> {
            if (button.visible && skin == selected) {
                drawSelectionBorder(graphics, button, COLOR_ACCENT);
            }
        });
    }

    // The text drawing methods were renamed in 26.2: drawCenteredString became
    // centeredText and drawString became text. These wrappers keep the call sites shared.
    //? if <26 {
    private void centeredText(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.drawCenteredString(font, text, x, y, color);
    }

    private void centeredText(GuiGraphics graphics, String text, int x, int y, int color) {
        graphics.drawCenteredString(font, text, x, y, color);
    }

    private void text(GuiGraphics graphics, String value, int x, int y, int color, boolean shadow) {
        graphics.drawString(font, value, x, y, color, shadow);
    }
    //?} else {
    /*private void centeredText(GuiGraphicsExtractor graphics, Component text, int x, int y, int color) {
        graphics.centeredText(font, text, x, y, color);
    }

    private void centeredText(GuiGraphicsExtractor graphics, String text, int x, int y, int color) {
        graphics.centeredText(font, text, x, y, color);
    }

    private void text(GuiGraphicsExtractor graphics, String value, int x, int y, int color, boolean shadow) {
        graphics.text(font, value, x, y, color, shadow);
    }
    *///?}

    //? if <26 {
    private void renderInterfaceTab(GuiGraphics graphics) {
    //?} else {
    /*private void renderInterfaceTab(GuiGraphicsExtractor graphics) {
    *///?}
        Component description = Component.translatable(
                "mixenergy.config.appearance.description"
        );
        centeredText(
                graphics,
                font.plainSubstrByWidth(description.getString(), panelWidth - 24),
                width / 2,
                contentTop + 2,
                COLOR_SECONDARY_TEXT
        );

        String selection = positionName(MixEnergyConfig.ENERGY_BAR_POSITION.get()).getString()
                + " · "
                + skinName(MixEnergyConfig.ENERGY_BAR_SKIN.get()).getString();
        centeredText(
                graphics,
                font.plainSubstrByWidth(selection, panelWidth - 24),
                width / 2,
                contentTop + 92,
                COLOR_ACCENT
        );
        renderPositionPreview(graphics);
    }

    //? if <26 {
    private void renderPositionPreview(GuiGraphics graphics) {
    //?} else {
    /*private void renderPositionPreview(GuiGraphicsExtractor graphics) {
    *///?}
        // Wide enough that the bar drawn at its real size still has room to sit against
        // either edge, so the left and right positions stay distinguishable.
        int previewWidth = Math.max(
                EnergyOverlayHandler.PREVIEW_WIDTH + 16,
                Math.min(190, panelWidth - 32)
        );
        int previewHeight = 34;
        int previewX = width / 2 - previewWidth / 2;
        int previewY = contentTop + 102;
        int previewRight = previewX + previewWidth;
        int previewBottom = previewY + previewHeight;

        graphics.fill(previewX, previewY, previewRight, previewBottom, COLOR_BORDER);
        graphics.fill(
                previewX + 1,
                previewY + 1,
                previewRight - 1,
                previewBottom - 1,
                0xE8101419
        );

        int hotbarWidth = 44;
        int hotbarX = width / 2 - hotbarWidth / 2;
        graphics.fill(
                hotbarX,
                previewBottom - 6,
                hotbarX + hotbarWidth,
                previewBottom - 3,
                0xFF39434A
        );

        int barWidth = EnergyOverlayHandler.PREVIEW_WIDTH;
        int barHeight = EnergyOverlayHandler.PREVIEW_HEIGHT;
        int margin = 4;
        int barX;
        int barY;
        switch (MixEnergyConfig.ENERGY_BAR_POSITION.get()) {
            case TOP_LEFT -> {
                barX = previewX + margin;
                barY = previewY + margin;
            }
            case TOP_RIGHT -> {
                barX = previewRight - margin - barWidth;
                barY = previewY + margin;
            }
            case TOP_CENTER -> {
                barX = width / 2 - barWidth / 2;
                barY = previewY + margin;
            }
            case BOTTOM_LEFT -> {
                barX = previewX + margin;
                barY = previewBottom - margin - barHeight;
            }
            case BOTTOM_RIGHT -> {
                barX = previewRight - margin - barWidth;
                barY = previewBottom - margin - barHeight;
            }
            case ABOVE_HOTBAR -> {
                barX = width / 2 - barWidth / 2;
                barY = previewBottom - 7 - barHeight;
            }
            default -> throw new IllegalStateException("Unknown energy bar position");
        }

        EnergyOverlayHandler.renderSkinPreview(
                graphics,
                MixEnergyConfig.ENERGY_BAR_SKIN.get(),
                barX,
                barY,
                PREVIEW_FILL_RATIO
        );
    }

    //? if <26 {
    private void renderPositionArrows(GuiGraphics graphics) {
    //?} else {
    /*private void renderPositionArrows(GuiGraphicsExtractor graphics) {
    *///?}
        MixEnergyConfig.EnergyBarPosition selected =
                MixEnergyConfig.ENERGY_BAR_POSITION.get();
        positionButtons.forEach((button, choice) -> {
            if (!button.visible) {
                return;
            }

            boolean isSelected = choice.position == selected;
            int color = isSelected ? COLOR_ACCENT : COLOR_PRIMARY_TEXT;
            if (isSelected) {
                drawSelectionBorder(graphics, button, color);
            }

            int centerX = button.getX() + button.getWidth() / 2;
            int centerY = button.getY() + button.getHeight() / 2;
            int tipX = centerX + choice.directionX * 6;
            int tipY = centerY + choice.directionY * 6;
            int tailX = centerX - choice.directionX * 5;
            int tailY = centerY - choice.directionY * 5;
            drawThickLine(graphics, tailX, tailY, tipX, tipY, color);

            int baseX = tipX - choice.directionX * 4;
            int baseY = tipY - choice.directionY * 4;
            int perpendicularX = -choice.directionY * 3;
            int perpendicularY = choice.directionX * 3;
            drawThickLine(
                    graphics,
                    tipX,
                    tipY,
                    baseX + perpendicularX,
                    baseY + perpendicularY,
                    color
            );
            drawThickLine(
                    graphics,
                    tipX,
                    tipY,
                    baseX - perpendicularX,
                    baseY - perpendicularY,
                    color
            );
        });
    }

    private static void drawSelectionBorder(
            //? if <26 {
            GuiGraphics graphics,
            //?} else {
            /*GuiGraphicsExtractor graphics,
            *///?}
            Button button,
            int color
    ) {
        int left = button.getX() + 2;
        int top = button.getY() + 2;
        int right = button.getX() + button.getWidth() - 2;
        int bottom = button.getY() + button.getHeight() - 2;
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    private static void drawThickLine(
            //? if <26 {
            GuiGraphics graphics,
            //?} else {
            /*GuiGraphicsExtractor graphics,
            *///?}
            int startX,
            int startY,
            int endX,
            int endY,
            int color
    ) {
        int deltaX = Math.abs(endX - startX);
        int stepX = startX < endX ? 1 : -1;
        int deltaY = -Math.abs(endY - startY);
        int stepY = startY < endY ? 1 : -1;
        int error = deltaX + deltaY;

        while (true) {
            graphics.fill(startX - 1, startY - 1, startX + 1, startY + 1, color);
            if (startX == endX && startY == endY) {
                break;
            }

            int doubledError = error * 2;
            if (doubledError >= deltaY) {
                error += deltaY;
                startX += stepX;
            }
            if (doubledError <= deltaX) {
                error += deltaX;
                startY += stepY;
            }
        }
    }

    //? if <26 {
    private void renderGameplayTab(GuiGraphics graphics) {
    //?} else {
    /*private void renderGameplayTab(GuiGraphicsExtractor graphics) {
    *///?}
        Component sectionDescription = Component.translatable(
                remoteServer
                        ? "mixenergy.config.server.description"
                        : "mixenergy.config.sources.description"
        );
        text(
                graphics,
                font.plainSubstrByWidth(sectionDescription.getString(), panelWidth - 16),
                panelX + 8,
                contentTop,
                remoteServer ? 0xFFE0BD72 : COLOR_SECONDARY_TEXT,
                false
        );

        int labelMaxWidth = panelWidth - 128;
        for (int row = 0; row < gameplayLabelKeys.size(); row++) {
            int y = gameplayRowY(row);
            graphics.fill(
                    panelX,
                    y,
                    panelX + panelWidth,
                    y + ROW_HEIGHT - 1,
                    row % 2 == 0 ? COLOR_ROW : COLOR_ROW_ALT
            );

            Component label = Component.translatable(gameplayLabelKeys.get(row));
            String clipped = font.plainSubstrByWidth(label.getString(), labelMaxWidth);
            text(
                    graphics,
                    clipped,
                    panelX + 8,
                    y + 4,
                    COLOR_PRIMARY_TEXT,
                    false
            );

        }
    }

    //? if <26 {
    private void renderGameplayTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
    //?} else {
    /*private void renderGameplayTooltip(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY
    ) {
    *///?}
        if (mouseX < panelX || mouseX >= panelX + panelWidth - 100) {
            return;
        }

        int firstRowY = gameplayRowY(0);
        int row = (mouseY - firstRowY) / ROW_HEIGHT;
        if (mouseY < firstRowY || row < 0 || row >= gameplayDescriptionKeys.size()) {
            return;
        }

        // Tooltips became a deferred, once-per-frame request in 1.21.6.
        //? if <1.21.6 {
        graphics.renderTooltip(
                font,
                Component.translatable(gameplayDescriptionKeys.get(row)),
                mouseX,
                mouseY
        );
        //?} else {
        /*graphics.setTooltipForNextFrame(
                font,
                Component.translatable(gameplayDescriptionKeys.get(row)),
                mouseX,
                mouseY
        );
        *///?}
    }

    @Override
    public void onClose() {
        // Minecraft#setScreen was renamed to setScreenAndShow in 26.2.
        //? if <26 {
        minecraft.setScreen(parentScreen);
        //?} else {
        /*minecraft.setScreenAndShow(parentScreen);
        *///?}
    }

    public static void registerConfigScreen() {
        //? if forge {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parentScreen) -> new MixEnergyConfigScreen(parentScreen)
                )
        );
        //?} else {
        /*ModList.get().getModContainerById(MixEnergy.MOD_ID).ifPresent(container ->
                container.registerExtensionPoint(
                        IConfigScreenFactory.class,
                        (modContainer, parentScreen) -> new MixEnergyConfigScreen(parentScreen)
                )
        );
        *///?}
    }

    private enum Tab {
        INTERFACE,
        GAMEPLAY
    }

    private final class RegenSpeedSlider extends AbstractSliderButton {
        private RegenSpeedSlider(int x, int y, int width) {
            super(
                    x,
                    y,
                    width,
                    ROW_HEIGHT,
                    Component.empty(),
                    Mth.clamp(
                            MixEnergyConfig.ENERGY_REGEN_SPEED_MULTIPLIER.get()
                                    / MAX_REGEN_SPEED_MULTIPLIER,
                            0.0,
                            1.0
                    )
            );
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (remoteServer) {
                setMessage(Component.translatable("mixenergy.config.server_controlled")
                        .withStyle(ChatFormatting.GRAY));
                return;
            }

            double configuredValue = getConfiguredValue();
            if (configuredValue <= 0.0) {
                setMessage(Component.translatable("mixenergy.config.disabled")
                        .withStyle(ChatFormatting.RED));
            } else {
                setMessage(Component.literal(String.format(
                                Locale.ROOT,
                                "×%.1f",
                                configuredValue
                        ))
                        .withStyle(ChatFormatting.GREEN));
            }
        }

        @Override
        protected void applyValue() {
            if (remoteServer) {
                return;
            }
            MixEnergyConfig.ENERGY_REGEN_SPEED_MULTIPLIER.set(getConfiguredValue());
            MixEnergyConfig.saveCommon();
        }

        private double getConfiguredValue() {
            return Math.round(value * MAX_REGEN_SPEED_MULTIPLIER * 10.0) / 10.0;
        }

        private void setConfigValue(double configuredValue) {
            value = Mth.clamp(
                    configuredValue / MAX_REGEN_SPEED_MULTIPLIER,
                    0.0,
                    1.0
            );
            updateMessage();
        }
    }

    private final class ConsumableRestoreSlider extends AbstractSliderButton {
        private ConsumableRestoreSlider(int x, int y, int width) {
            super(
                    x,
                    y,
                    width,
                    ROW_HEIGHT,
                    Component.empty(),
                    Mth.clamp(
                            MixEnergyConfig.CONSUMABLE_ENERGY_RESTORE.get()
                                    / MAX_CONSUMABLE_ENERGY_RESTORE,
                            0.0,
                            1.0
                    )
            );
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (remoteServer) {
                setMessage(Component.translatable("mixenergy.config.server_controlled")
                        .withStyle(ChatFormatting.GRAY));
                return;
            }

            double configuredValue = getConfiguredValue();
            if (configuredValue <= 0.0) {
                setMessage(Component.translatable("mixenergy.config.disabled")
                        .withStyle(ChatFormatting.RED));
            } else {
                setMessage(Component.literal(String.format(
                                Locale.ROOT,
                                "+%.1f",
                                configuredValue
                        ))
                        .withStyle(ChatFormatting.GREEN));
            }
        }

        @Override
        protected void applyValue() {
            if (remoteServer) {
                return;
            }
            MixEnergyConfig.CONSUMABLE_ENERGY_RESTORE.set(getConfiguredValue());
            MixEnergyConfig.saveCommon();
        }

        /** Rounded to half points, so the slider can still land on the small values. */
        private double getConfiguredValue() {
            return Math.round(value * MAX_CONSUMABLE_ENERGY_RESTORE * 2.0) / 2.0;
        }

        private void setConfigValue(double configuredValue) {
            value = Mth.clamp(configuredValue / MAX_CONSUMABLE_ENERGY_RESTORE, 0.0, 1.0);
            updateMessage();
        }
    }

    private record PositionChoice(
            MixEnergyConfig.EnergyBarPosition position,
            int directionX,
            int directionY
    ) {
    }
}
