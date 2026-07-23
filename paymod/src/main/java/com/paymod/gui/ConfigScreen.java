package com.paymod.gui;

import com.paymod.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget keepAmountField;
    private TextFieldWidget targetPlayerField;

    public ConfigScreen(Screen parent) {
        super(Text.literal("PayMod"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        keepAmountField = new TextFieldWidget(this.textRenderer, centerX - 75, 50, 150, 20, Text.literal(""));
        keepAmountField.setText(String.valueOf((long) ModConfig.getKeepAmount()));
        keepAmountField.setChangedListener(s -> {
            try {
                double val = Double.parseDouble(s);
                ModConfig.setKeepAmount(val);
                ModConfig.save();
            } catch (NumberFormatException ignored) {}
        });
        this.addDrawableChild(keepAmountField);

        targetPlayerField = new TextFieldWidget(this.textRenderer, centerX - 75, 95, 150, 20, Text.literal(""));
        targetPlayerField.setText(ModConfig.getTargetPlayer());
        targetPlayerField.setChangedListener(s -> {
            ModConfig.setTargetPlayer(s);
            ModConfig.save();
        });
        this.addDrawableChild(targetPlayerField);

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Сохранить"), button -> {
            this.close();
        }).dimensions(centerX - 50, 135, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawText(this.textRenderer, "PayMod - Настройки", this.width / 2 - 55, 15, 0xFFFFFF, false);
        context.drawText(this.textRenderer, "Сохранять сумму:", this.width / 2 - 75, 38, 0xAAAAAA, false);
        context.drawText(this.textRenderer, "Игрок для перевода:", this.width / 2 - 75, 83, 0xAAAAAA, false);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        ModConfig.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
