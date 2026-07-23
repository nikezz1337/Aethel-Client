package dev.aethel.ui;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import dev.aethel.module.Module;
import dev.aethel.module.settings.*;
import dev.aethel.ui.component.Component;
import dev.aethel.ui.component.impl.*;
import dev.aethel.util.cursor.CursorManager;
import dev.aethel.util.render.helper.HoverUtil;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.msdf.Fonts;

import java.util.HashMap;
import java.util.Map;

public class ModuleComponent extends Component {
    private final Module module;
    private final Panel panel;

    private final Animation animation = new Animation(Easing.QUINTIC_OUT, 320);
    private final Animation hoverAnim = new Animation(Easing.QUINTIC_OUT, 300);
    private final Animation enabledAnim = new Animation(Easing.QUINTIC_OUT, 400);

    public boolean open;
    private boolean isHovered;
    private boolean binding;

    private final ObjectArrayList<Component> components = new ObjectArrayList<>();
    private final Map<Component, Float> frozenHeights = new HashMap<>();
    private final Map<Component, Float> prevAlphas = new HashMap<>();

    public ModuleComponent(Module module, Panel panel) {
        this.module = module;
        this.panel = panel;
        for (Setting setting : module.getSettings()) {
            switch (setting) {
                case BooleanSetting option -> components.add(new BooleanComponent(option));
                case ModeSetting option -> components.add(new ModeComponent(option));
                case ModeListSetting option -> components.add(new ModeListComponent(option));
                case SliderSetting option -> components.add(new SliderComponent(option));
                case BindSetting option -> components.add(new BindComponent(option));
                case ThemeSetting option -> components.add(new ThemeComponent(option));
                case ColorSetting option -> components.add(new ColorPickerComponent(option));
                case MultiBooleanSetting option -> components.add(new MultiBooleanComponent(option));
                case TextSetting option -> components.add(new TextComponent(option));
                default -> {}
            }
        }

        if (module.getName().equals("Interface")) {
            components.add(new ThemeActionComponent(this));
        }
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        isHovered = HoverUtil.isHovered(mouseX, mouseY, x, y, width, 20);

        hoverAnim.run(isHovered);
        animation.run(open);
        enabledAnim.run(module.isEnabled());

        if (HoverUtil.isHovered(mouseX, mouseY, x, y, width, 20)) CursorManager.requestHand();

        float alpha = Math.max(Math.min(panel.getAnimationAlpha().getValue(), 1), 0);
        float enabled = enabledAnim.getValue();

        int textColor = ColorProvider.interpolateColor(
                ColorProvider.setAlpha(ColorProvider.getColorInactiveText(), (int)(255 * alpha)),
                ColorProvider.setAlpha(ColorProvider.getColorText(), (int)(255 * alpha)),
                enabled
        );

        float currentHeight = 16 + ((height - 16) * animation.getValue());
        float moduleRound = 1.8f;

        int inactiveBase = ColorProvider.rgba(23, 23, 23, 255);
        if (enabled > 0.01f) {
            int baseColor = ColorProvider.getColorVisualModules();
            float cycle = (float)((System.currentTimeMillis() % 2400) / 2400.0) * (float)Math.PI * 2;
            float phase1 = (float)(Math.sin(cycle) * 0.35 + 0.65);
            float phase2 = (float)(Math.sin(cycle + Math.PI / 2) * 0.35 + 0.65);
            float phase3 = (float)(Math.sin(cycle + Math.PI) * 0.35 + 0.65);
            float phase4 = (float)(Math.sin(cycle + Math.PI * 1.5) * 0.35 + 0.65);
            int maxA = 140;
            int tl = ColorProvider.interpolateColor(inactiveBase, ColorProvider.setAlpha(baseColor, (int)(maxA * phase1 * alpha * enabled)), enabled);
            int bl = ColorProvider.interpolateColor(inactiveBase, ColorProvider.setAlpha(baseColor, (int)(maxA * phase2 * alpha * enabled)), enabled);
            int br = ColorProvider.interpolateColor(inactiveBase, ColorProvider.setAlpha(baseColor, (int)(maxA * phase3 * alpha * enabled)), enabled);
            int tr = ColorProvider.interpolateColor(inactiveBase, ColorProvider.setAlpha(baseColor, (int)(maxA * phase4 * alpha * enabled)), enabled);

            // Theme-colored shadow behind active module — статичная, строго по контуру
            float shadowSoftness = 5f;
            int shadowColor = ColorProvider.setAlpha(baseColor, 65);
            Matrix4f mat = matrixStack.peek().getPositionMatrix();
            DrawUtil.drawShadow(mat, x + shadowSoftness, y + shadowSoftness,
                    width - shadowSoftness * 2f, currentHeight - 0.5f - shadowSoftness * 2f,
                    moduleRound, shadowSoftness, shadowColor);

            DrawUtil.drawRound(x, y, width, currentHeight - 0.5f, moduleRound, tl, bl, br, tr);

            float glowInset = 0.7f;
            int glowA = (int)(110 * alpha * enabled);
            int glow1 = ColorProvider.interpolateColor(0, ColorProvider.setAlpha(baseColor, (int)(glowA * phase1)), enabled);
            int glow2 = ColorProvider.interpolateColor(0, ColorProvider.setAlpha(baseColor, (int)(glowA * phase2)), enabled);
            int glow3 = ColorProvider.interpolateColor(0, ColorProvider.setAlpha(baseColor, (int)(glowA * phase3)), enabled);
            int glow4 = ColorProvider.interpolateColor(0, ColorProvider.setAlpha(baseColor, (int)(glowA * phase4)), enabled);
            DrawUtil.drawRound(x - glowInset, y - glowInset, width + glowInset * 2f, currentHeight - 0.5f + glowInset * 2f, moduleRound + 0.5f, glow1, glow2, glow3, glow4);
        } else {
            // Standard dark shadow behind inactive module — статичная, строго по контуру
            float shadowSoftness = 5f;
            Matrix4f mat = matrixStack.peek().getPositionMatrix();
            DrawUtil.drawShadow(mat, x + shadowSoftness, y + shadowSoftness,
                    width - shadowSoftness * 2f, currentHeight - 0.5f - shadowSoftness * 2f,
                    moduleRound, shadowSoftness, ColorProvider.rgba(0, 0, 0, 55));

            DrawUtil.drawRound(x, y, width, currentHeight - 0.5f, moduleRound, inactiveBase, inactiveBase);
        }

        if (binding) {
            String hint = "Нажмите клавишу...";
            DrawUtil.drawText(Fonts.SFREGULAR.get(), hint,
                x + width / 2f - Fonts.SFREGULAR.get().getWidth(hint, 7.5f) / 2f,
                y + 4.25f, ColorProvider.rgba(255, 255, 255, (int)(255 * alpha)), 7.5f);
        } else {
            float textW = Fonts.SFBOLD.get().getWidth(module.getName(), 8f);
            DrawUtil.drawText(Fonts.SFBOLD.get(), module.getName(), x + width / 2f - textW / 2f, y + 3.75f, textColor, 8f);

            if (!components.isEmpty()) {
                float chevronSize = 9.5f;
                float chevronX = x + width - 8f;
                float chevronY = y + 2.25f;
                int chevronColor = textColor;

                float openAnim = (float) animation.getValue();
                float rotation = openAnim * 90f;
                matrixStack.push();
                matrixStack.translate(chevronX + chevronSize / 2f, chevronY + chevronSize / 2f, 0);
                matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
                matrixStack.translate(-(chevronX + chevronSize / 2f), -(chevronY + chevronSize / 2f), 0);
                DrawUtil.drawText(Fonts.SFMEDIUM.get(), ">", chevronX, chevronY, chevronColor, chevronSize);
                matrixStack.pop();
            }
        }

        if (animation.getValue() > 0.01f) {
            float compY = y + 17f;
            float panelTop = panel.getY() + 20;
            float panelBottom = panel.getY() + panel.getHeight() - 4;
            float settingsY = y + 17f;
            float settingsBottom = y + currentHeight;

            float intersectY = Math.max(settingsY, panelTop);
            float intersectBottom = Math.min(settingsBottom, panelBottom);
            float intersectHeight = Math.max(0, intersectBottom - intersectY);

            float darkHeight = currentHeight - 17;
            if (darkHeight > 0) {
                DrawUtil.drawRound(x + 1f, y + 17, width - 2f, darkHeight, 0f, ColorProvider.rgba(0, 0, 0, (int)(30 * alpha * animation.getValue())));
            }

            for (Component component : components) {
                component.getAlphaAnim().setValue(Math.min(panel.getAnimationAlpha().getValue(), 1));
                component.getAlphaAnimSetting().run(component.isVisible());

                float rawAlpha = component.getAlphaAnimSetting().getValue();
                float visibleProgress = MathHelper.clamp(rawAlpha, 0f, 1f);

                if (component.isVisible() || visibleProgress > 0) {
                    component.setX(x);
                    component.setY(compY);
                    component.setWidth(width - 4);

                    dev.aethel.util.render.math.Scissor.push();
                    dev.aethel.util.render.math.Scissor.setFromComponentCoordinates(x, intersectY, width, intersectHeight);

                    component.render(matrixStack, mouseX, mouseY, partialTicks);

                    dev.aethel.util.render.math.Scissor.unset();
                    dev.aethel.util.render.math.Scissor.pop();

                    // Анти-тряска: при fade-out (alpha уменьшается) — замораживаем высоту
                    float compHeight = component.getHeight();
                    float prevAlpha = prevAlphas.getOrDefault(component, 1f);
                    prevAlphas.put(component, rawAlpha);

                    if (rawAlpha < prevAlpha && rawAlpha < 0.95f) {
                        // Фаза исчезновения — используем замороженную высоту
                        compHeight = frozenHeights.getOrDefault(component, compHeight);
                    } else {
                        // Фаза появления или стабильного состояния — обновляем заморозку
                        frozenHeights.put(component, compHeight);
                    }

                    compY += compHeight * visibleProgress;
                }
            }
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, 20)) {
            if (button == 0) module.setEnabled(!module.isEnabled());
            if (button == 1 && !components.isEmpty()) open = !open;
            if (button == 2) binding = !binding;
        }

        if (open) {
            for (Component component : components) {
                if (component.isVisible() && component.getAlphaAnimSetting().getValue() > 0.5f) {
                    component.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (open) {
            for (Component component : components) {
                component.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE) {
                module.setKey(-1);
            } else {
                module.setKey(keyCode);
            }
            binding = false;
            return;
        }

        if (open) {
            for (Component component : components) {
                component.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    public void charTyped(char chr, int modifiers) {
        if (open) {
            for (Component component : components) {
                component.charTyped(chr, modifiers);
            }
        }
    }

    private boolean isHovered(double mouseX, double mouseY, float heightCheck) {
        return HoverUtil.isHovered(mouseX, mouseY, x, y, width, heightCheck);
    }

    public Module getModule() {
        return module;
    }

    public Panel getPanel() {
        return panel;
    }

    public Animation getAnimation() {
        return animation;
    }

    public Animation getHoverAnim() {
        return hoverAnim;
    }

    public Animation getEnabledAnim() {
        return enabledAnim;
    }

    public boolean isOpen() {
        return open;
    }

    public boolean isHovered() {
        return isHovered;
    }

    public boolean isBinding() {
        return binding;
    }

    public ObjectArrayList<Component> getComponents() {
        return components;
    }

    public static class ThemeActionComponent extends Component {
        private final ModuleComponent parent;
        private final Animation hoverAnim = new Animation(Easing.QUINTIC_OUT, 300);

        public ThemeActionComponent(ModuleComponent parent) {
            this.parent = parent;
        }

        @Override
        public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
            float btnX = x + 2f;
            float btnY = y + 1f;
            float btnW = width - 4f;
            float btnH = 16;

            boolean isHovered = HoverUtil.isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH);
            hoverAnim.run(isHovered);
            if (isHovered) CursorManager.requestHand();

            float alpha = (float) getAlphaAnim().getValue();

            int outlineAlpha = (int) ((25 + (40 * hoverAnim.getValue())) * alpha);
            int outlineColor = ColorProvider.rgba(255, 255, 255, outlineAlpha);
            int innerColor = ColorProvider.rgba(44, 44, 44, (int)(140 * alpha));

            DrawUtil.drawRound(btnX - 0.5f, btnY - 0.5f, btnW + 1f, btnH + 0.5f, 3.5f, outlineColor);
            DrawUtil.drawRoundBlur(btnX, btnY, btnW, btnH - 0.5f, 3f, innerColor, 16);

            int textColor = ColorProvider.rgba(255, 255, 255, (int)(255 * alpha));
            DrawUtil.drawText(Fonts.SFREGULAR.get(), "Открыть менеджер тем", btnX + 3.5f, btnY + 3.25f, textColor, 7.35f);
        }

        @Override
        public void mouseClicked(double mouseX, double mouseY, int button) {
            float btnX = x + 2f;
            float btnY = y + 1f;
            float btnW = width - 4f;
            float btnH = 14f;

            if (HoverUtil.isHovered(mouseX, mouseY, btnX, btnY, btnW, btnH) && button == 0) {
                ThemeManagerWindow tm = parent.getPanel().getParent().getThemeManager();
                tm.setOpen(!tm.isOpen());
            }
        }

        @Override
        public void mouseReleased(double mouseX, double mouseY, int button) {}

        @Override
        public void keyPressed(int keyCode, int scanCode, int modifiers) {}

        @Override
        public float getHeight() {
            return 16f;
        }

        @Override
        public boolean isVisible() {
            return true;
        }
    }
}