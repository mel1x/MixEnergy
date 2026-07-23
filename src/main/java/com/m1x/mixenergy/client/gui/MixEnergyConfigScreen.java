package com.m1x.mixenergy.client.gui;

import com.m1x.mixenergy.common.config.MixEnergyConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

public class MixEnergyConfigScreen extends Screen {
    private final Screen parentScreen;
    private Button positionButton;
    private Button blockBreakingButton;
    private Button attacksButton;
    private final boolean isOnServer;
    
    public MixEnergyConfigScreen(Screen parentScreen) {
        super(Component.translatable("mixenergy.config.title"));
        this.parentScreen = parentScreen;
        this.isOnServer = this.minecraft != null && this.minecraft.getCurrentServer() != null;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = this.height / 2 - 60;
        int spacing = 35;
        int buttonWidth = 280;
        int buttonHeight = 20;
        
        // Кнопка выбора позиции энергетического бара (более удобная)
        this.positionButton = Button.builder(
                Component.translatable("mixenergy.config.position").append(": ")
                        .append(Component.translatable("mixenergy.config.position." + MixEnergyConfig.ENERGY_BAR_POSITION.get().getName())),
                button -> {
                    // Циклический переход к следующей позиции
                    MixEnergyConfig.EnergyBarPosition[] positions = MixEnergyConfig.EnergyBarPosition.values();
                    MixEnergyConfig.EnergyBarPosition current = MixEnergyConfig.ENERGY_BAR_POSITION.get();
                    int currentIndex = java.util.Arrays.asList(positions).indexOf(current);
                    int nextIndex = (currentIndex + 1) % positions.length;
                    MixEnergyConfig.EnergyBarPosition next = positions[nextIndex];
                    
                    MixEnergyConfig.ENERGY_BAR_POSITION.set(next);
                    MixEnergyConfig.SPEC.save();
                    
                    // Обновляем текст кнопки
                    button.setMessage(Component.translatable("mixenergy.config.position").append(": ")
                            .append(Component.translatable("mixenergy.config.position." + next.getName())));
                })
                .bounds(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight)
                .build();
        
        // Кнопка включения/выключения трат энергии на разрушение блоков
        Component blockBreakingText = isOnServer ? 
                Component.translatable("mixenergy.config.block_breaking").append(": ")
                        .append(Component.translatable("mixenergy.config.server_controlled")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))) :
                Component.translatable("mixenergy.config.block_breaking").append(": ")
                        .append(Component.translatable(MixEnergyConfig.ENERGY_COST_FOR_BREAKING_BLOCKS.get() ? 
                                "mixenergy.config.enabled" : "mixenergy.config.disabled"));
        
        this.blockBreakingButton = Button.builder(
                blockBreakingText,
                button -> {
                    if (!isOnServer) {
                        boolean newValue = !MixEnergyConfig.ENERGY_COST_FOR_BREAKING_BLOCKS.get();
                        MixEnergyConfig.ENERGY_COST_FOR_BREAKING_BLOCKS.set(newValue);
                        MixEnergyConfig.SPEC.save();
                        
                        button.setMessage(Component.translatable("mixenergy.config.block_breaking").append(": ")
                                .append(Component.translatable(newValue ? "mixenergy.config.enabled" : "mixenergy.config.disabled")));
                    }
                })
                .bounds(centerX - buttonWidth / 2, startY + spacing, buttonWidth, buttonHeight)
                .build();
        
        if (isOnServer) {
            this.blockBreakingButton.active = false;
        }
        
        // Кнопка включения/выключения трат энергии на атаки
        Component attacksText = isOnServer ? 
                Component.translatable("mixenergy.config.attacks").append(": ")
                        .append(Component.translatable("mixenergy.config.server_controlled")
                                .setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY))) :
                Component.translatable("mixenergy.config.attacks").append(": ")
                        .append(Component.translatable(MixEnergyConfig.ENERGY_COST_FOR_ATTACKS.get() ? 
                                "mixenergy.config.enabled" : "mixenergy.config.disabled"));
        
        this.attacksButton = Button.builder(
                attacksText,
                button -> {
                    if (!isOnServer) {
                        boolean newValue = !MixEnergyConfig.ENERGY_COST_FOR_ATTACKS.get();
                        MixEnergyConfig.ENERGY_COST_FOR_ATTACKS.set(newValue);
                        MixEnergyConfig.SPEC.save();
                        
                        button.setMessage(Component.translatable("mixenergy.config.attacks").append(": ")
                                .append(Component.translatable(newValue ? "mixenergy.config.enabled" : "mixenergy.config.disabled")));
                    }
                })
                .bounds(centerX - buttonWidth / 2, startY + spacing * 2, buttonWidth, buttonHeight)
                .build();
        
        if (isOnServer) {
            this.attacksButton.active = false;
        }
        
        this.addRenderableWidget(this.positionButton);
        this.addRenderableWidget(this.blockBreakingButton);
        this.addRenderableWidget(this.attacksButton);
        
        // Кнопка "Готово"
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                button -> this.minecraft.setScreen(this.parentScreen))
                .bounds(centerX - 60, startY + spacing * 4, 120, buttonHeight)
                .build());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        
        // Заголовок
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Описание настройки
        guiGraphics.drawCenteredString(this.font, 
                Component.translatable("mixenergy.config.position.description"), 
                this.width / 2, this.height / 2 - 90, 0xAAAAAA);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parentScreen);
    }
    
    // Статический метод для регистрации экрана конфигурации
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parentScreen) -> new MixEnergyConfigScreen(parentScreen)
                )
        );
    }
}