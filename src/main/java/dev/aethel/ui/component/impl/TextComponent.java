package dev.aethel.ui.component.impl;

import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;
import dev.aethel.module.settings.TextSetting;
import dev.aethel.ui.component.Component;
import dev.aethel.util.cursor.CursorManager;
import dev.aethel.util.render.helper.HoverUtil;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;

public class TextComponent extends Component {
    private final TextSetting setting;
    private boolean editing;

    public TextComponent(TextSetting setting) {
        this.setting = setting;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        float alpha = Math.max(Math.min(getAlphaAnimSetting().getValue(), 1), 0);
        int alphaInt = (int) (255 * alpha);

        String text = setting.getValue();
        String display = editing ? text + "_" : text;

        float textWidth = Fonts.SFSEMIBOLD.get().getWidth(display, 6.5f) + 6f;
        float boxX = x + width - textWidth - 4;
        float boxY = y + 2;
        float boxHeight = 9;

        if (HoverUtil.isHovered(mouseX, mouseY, boxX, boxY, textWidth, boxHeight)) {
            CursorManager.requestHand();
        }

        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), setting.getName(), x + 4.5f, y + 3f, ColorProvider.rgba(255, 255, 255, alphaInt), 6.5f);

        DrawUtil.drawRound(boxX - 0.5f, boxY - 0.5f, textWidth + 1, boxHeight + 1, 2f, ColorProvider.rgba(60, 60, 65, (int)(150 * alpha)));
        DrawUtil.drawRound(boxX, boxY, textWidth, boxHeight, 2f, ColorProvider.rgba(20, 20, 25, alphaInt));
        DrawUtil.drawText(Fonts.SFSEMIBOLD.get(), display, boxX + 4, boxY + 0.5f, ColorProvider.rgba(200, 200, 200, alphaInt), 6.5f);

        setHeight(13);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        String text = setting.getValue();
        float textWidth = Fonts.SFSEMIBOLD.get().getWidth(text, 6.5f) + 6f;
        float boxX = x + width - textWidth - 4;
        float boxY = y + 2;

        if (HoverUtil.isHovered(mouseX, mouseY, boxX, boxY, textWidth, 9) && button == 0) {
            editing = !editing;
        }
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!editing) return;

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            editing = false;
        } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            String val = setting.getValue();
            if (!val.isEmpty()) setting.setValue(val.substring(0, val.length() - 1));
        }
    }

    @Override
    public void charTyped(char chr, int modifiers) {
        if (!editing) return;
        String val = setting.getValue();
        if (val.length() < setting.getMaxLength()) {
            setting.setValue(val + chr);
        }
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
