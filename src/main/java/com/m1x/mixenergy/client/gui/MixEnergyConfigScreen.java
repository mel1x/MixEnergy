package com.m1x.mixenergy.client.gui;

import com.m1x.mixenergy.common.config.MixEnergyConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MixEnergyConfigScreen extends Screen {
    private static final int PANEL_MAX_WIDTH = 440;
    private static final int ROW_HEIGHT = 23;
    private static final int COLOR_PANEL = 0xD914181E;
    private static final int COLOR_ROW = 0xA6222830;
    private static final int COLOR_ROW_ALT = 0xA61D232A;
    private static final int COLOR_BORDER = 0xFF52636A;
    private static final int COLOR_ACCENT = 0xFF78AAA6;
    private static final int COLOR_PRIMARY_TEXT = 0xFFE8EEF0;
    private static final int COLOR_SECONDARY_TEXT = 0xFFA9B5B9;
    private static final String[] SOURCE_LABEL_KEYS = {
            "mixenergy.config.source.sprinting",
            "mixenergy.config.source.swimming",
            "mixenergy.config.source.breaking",
            "mixenergy.config.source.placing",
            "mixenergy.config.source.attacks"
    };
    private static final String[] SOURCE_DESCRIPTION_KEYS = {
            "mixenergy.config.source.sprinting.description",
            "mixenergy.config.source.swimming.description",
            "mixenergy.config.source.breaking.description",
            "mixenergy.config.source.placing.description",
            "mixenergy.config.source.attacks.description"
    };

    private final Screen parentScreen;
    private final List<AbstractWidget> interfaceWidgets = new ArrayList<>();
    private final List<AbstractWidget> gameplayWidgets = new ArrayList<>();
    private final Map<Button, ForgeConfigSpec.BooleanValue> sourceButtons = new LinkedHashMap<>();
    private final Map<Button, PositionChoice> positionButtons = new LinkedHashMap<>();

    private Tab activeTab = Tab.INTERFACE;
    private boolean remoteServer;
    private int panelX;
    private int panelWidth;
    private int contentTop;
    private Button interfaceTabButton;
    private Button gameplayTabButton;
    private Button resetButton;

    public MixEnergyConfigScreen(Screen parentScreen) {
        super(Component.translatable("mixenergy.config.title"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        remoteServer = minecraft.getConnection() != null && !minecraft.hasSingleplayerServer();
        panelWidth = Math.min(PANEL_MAX_WIDTH, width - 24);
        panelX = (width - panelWidth) / 2;
        contentTop = 80;

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
        int buttonWidth = 48;
        int buttonHeight = 24;
        int gap = 6;
        int gridWidth = buttonWidth * 3 + gap * 2;
        int startX = width / 2 - gridWidth / 2;
        int startY = contentTop + 28;

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
        addSourceToggle(
                0,
                "mixenergy.config.source.sprinting",
                "mixenergy.config.source.sprinting.description",
                MixEnergyConfig.ENERGY_COST_FOR_SPRINTING
        );
        addSourceToggle(
                1,
                "mixenergy.config.source.swimming",
                "mixenergy.config.source.swimming.description",
                MixEnergyConfig.ENERGY_COST_FOR_SWIMMING
        );
        addSourceToggle(
                2,
                "mixenergy.config.source.breaking",
                "mixenergy.config.source.breaking.description",
                MixEnergyConfig.ENERGY_COST_FOR_BREAKING_BLOCKS
        );
        addSourceToggle(
                3,
                "mixenergy.config.source.placing",
                "mixenergy.config.source.placing.description",
                MixEnergyConfig.ENERGY_COST_FOR_PLACING_BLOCKS
        );
        addSourceToggle(
                4,
                "mixenergy.config.source.attacks",
                "mixenergy.config.source.attacks.description",
                MixEnergyConfig.ENERGY_COST_FOR_ATTACKS
        );
    }

    private void addSourceToggle(
            int row,
            String labelKey,
            String descriptionKey,
            ForgeConfigSpec.BooleanValue value
    ) {
        int buttonWidth = 88;
        int y = contentTop + 15 + row * ROW_HEIGHT;
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
        ).bounds(panelX + panelWidth - buttonWidth - 8, y + 2, buttonWidth, 19).build();
        toggle.setTooltip(Tooltip.create(Component.translatable(descriptionKey)));
        toggle.active = !remoteServer;

        gameplayWidgets.add(addRenderableWidget(toggle));
        sourceButtons.put(toggle, value);
    }

    private Component sourceState(ForgeConfigSpec.BooleanValue value) {
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
            MixEnergyConfig.saveClient();
            updatePositionButtons();
            return;
        }

        if (remoteServer) {
            return;
        }

        sourceButtons.values().forEach(value -> value.set(true));
        MixEnergyConfig.saveCommon();
        sourceButtons.forEach((button, value) -> button.setMessage(sourceState(value)));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        graphics.fill(panelX - 6, 36, panelX + panelWidth + 6, height - 32, COLOR_PANEL);
        graphics.fill(panelX - 6, 36, panelX + panelWidth + 6, 37, COLOR_ACCENT);

        graphics.drawCenteredString(font, title, width / 2, 12, COLOR_PRIMARY_TEXT);
        Component subtitle = Component.translatable(
                remoteServer
                        ? "mixenergy.config.subtitle.multiplayer"
                        : "mixenergy.config.subtitle.local"
        );
        graphics.drawCenteredString(
                font,
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

        super.render(graphics, mouseX, mouseY, partialTick);

        if (activeTab == Tab.INTERFACE) {
            renderPositionArrows(graphics);
        } else {
            renderGameplayTooltip(graphics, mouseX, mouseY);
        }
    }

    private void renderInterfaceTab(GuiGraphics graphics) {
        Component description = Component.translatable(
                "mixenergy.config.position.description"
        );
        graphics.drawCenteredString(
                font,
                font.plainSubstrByWidth(description.getString(), panelWidth - 24),
                width / 2,
                contentTop + 2,
                COLOR_SECONDARY_TEXT
        );
        graphics.drawCenteredString(
                font,
                positionName(MixEnergyConfig.ENERGY_BAR_POSITION.get()),
                width / 2,
                contentTop + 91,
                COLOR_ACCENT
        );
    }

    private void renderPositionArrows(GuiGraphics graphics) {
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
            GuiGraphics graphics,
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
            GuiGraphics graphics,
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

    private void renderGameplayTab(GuiGraphics graphics) {
        Component sectionDescription = Component.translatable(
                remoteServer
                        ? "mixenergy.config.server.description"
                        : "mixenergy.config.sources.description"
        );
        graphics.drawString(
                font,
                font.plainSubstrByWidth(sectionDescription.getString(), panelWidth - 16),
                panelX + 8,
                contentTop,
                remoteServer ? 0xFFE0BD72 : COLOR_SECONDARY_TEXT,
                false
        );

        int labelMaxWidth = panelWidth - 116;
        for (int row = 0; row < SOURCE_LABEL_KEYS.length; row++) {
            int y = contentTop + 15 + row * ROW_HEIGHT;
            graphics.fill(
                    panelX,
                    y,
                    panelX + panelWidth,
                    y + ROW_HEIGHT - 1,
                    row % 2 == 0 ? COLOR_ROW : COLOR_ROW_ALT
            );

            Component label = Component.translatable(SOURCE_LABEL_KEYS[row]);
            String clipped = font.plainSubstrByWidth(label.getString(), labelMaxWidth);
            graphics.drawString(
                    font,
                    clipped,
                    panelX + 8,
                    y + 7,
                    COLOR_PRIMARY_TEXT,
                    false
            );

        }
    }

    private void renderGameplayTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (mouseX < panelX || mouseX >= panelX + panelWidth - 100) {
            return;
        }

        int firstRowY = contentTop + 15;
        int row = (mouseY - firstRowY) / ROW_HEIGHT;
        if (mouseY < firstRowY || row < 0 || row >= SOURCE_DESCRIPTION_KEYS.length) {
            return;
        }

        graphics.renderTooltip(
                font,
                Component.translatable(SOURCE_DESCRIPTION_KEYS[row]),
                mouseX,
                mouseY
        );
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parentScreen);
    }

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parentScreen) -> new MixEnergyConfigScreen(parentScreen)
                )
        );
    }

    private enum Tab {
        INTERFACE,
        GAMEPLAY
    }

    private record PositionChoice(
            MixEnergyConfig.EnergyBarPosition position,
            int directionX,
            int directionY
    ) {
    }
}
