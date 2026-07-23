package dev.ethereal.client.ui.mainmenu;

import dev.ethereal.api.system.backend.ClientInfo;
import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.animation.wrap.infinity.InfinityAnimation;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.altmanager.AltManagerScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Cherry extends Screen {
    private static final int BUTTON_WIDTH = 190;
    private static final int BUTTON_HEIGHT = 24;
    private static final int BUTTON_GAP = 8;
    private static final float BUTTON_RADIUS = 12.0f;
    private static final Color BUTTON_BG_COLOR = new Color(8, 4, 16, 215);
    private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

    private final List<MenuButton> buttons = new ArrayList<>();
    private long openTime;

    private RainEffect rainEffect;

    public Cherry() {
        super(Text.of("Cherry"));
    }

    @Override
    protected void init() {
        buttons.clear();
        openTime = Util.getMeasuringTimeMs();

        int count = 5;
        int totalHeight = count * BUTTON_HEIGHT + (count - 1) * BUTTON_GAP;
        int startX = (width - BUTTON_WIDTH) / 2;
        int startY = Math.round(height * 0.5f - totalHeight * 0.5f + 30f);

        buttons.add(new MenuButton("Singleplayer", startX, startY, () -> client.setScreen(new SelectWorldScreen(this))));
        buttons.add(new MenuButton("Multiplayer", startX, startY + (BUTTON_HEIGHT + BUTTON_GAP), () -> client.setScreen(new MultiplayerScreen(this))));
        buttons.add(new MenuButton("Alt Manager", startX, startY + (BUTTON_HEIGHT + BUTTON_GAP) * 2, () -> client.setScreen(new AltManagerScreen(this))));
        buttons.add(new MenuButton("Options", startX, startY + (BUTTON_HEIGHT + BUTTON_GAP) * 3, () -> client.setScreen(new OptionsScreen(this, client.options))));
        buttons.add(new MenuButton("Quit", startX, startY + (BUTTON_HEIGHT + BUTTON_GAP) * 4, () -> client.scheduleStop()));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderMenuBackground(context);

        if (rainEffect == null) rainEffect = new RainEffect();
        rainEffect.renderAndUpdate(context.getMatrices(), width, height);

        renderBrand(context);

        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).render(context, mouseX, mouseY, i);
        }

        renderFooter(context);
    }

    private void renderMenuBackground(DrawContext context) {
        MenuBackgroundRenderer.render(context, width, height);
        RenderUtil.RECT.draw(context.getMatrices(), 0, 0, width, height, 0f, new Color(0, 0, 0, 60));
    }

    private void renderBrand(DrawContext context) {
        long elapsed = Util.getMeasuringTimeMs() - openTime;
        float appear = ease(MathHelper.clamp(elapsed / 520f, 0f, 1f));
        float yOffset = (1f - appear) * -18f;
        float pulse = (float) Math.sin(elapsed / 800.0) * 0.05f;

        String title = "Ethereal";
        float titleSize = 23f;
        float centerX = width / 2f;
        float titleY = Math.max(34f, height * 0.5f - 128f) + yOffset;

        Color primary = UIColors.primary(MathHelper.clamp(Math.round(255 * appear), 0, 255));
        Color secondary = UIColors.secondary(MathHelper.clamp(Math.round(220 * appear), 0, 255));
        Fonts.SF_BOLD.drawCenteredGradientText(context.getMatrices(), title, centerX, titleY, titleSize + pulse, primary, secondary, elapsed / 28f);
        Fonts.SF_REGULAR.drawCenteredText(context.getMatrices(), "made with love by nelxigd and kaiser ^^", centerX, titleY + 29f, 7f,
                UIColors.inactiveTextColor(MathHelper.clamp(Math.round(170 * appear), 0, 255)));

        if (rainEffect != null && appear > 0.05f) {
            rainEffect.setLightSource(centerX, titleY, 60f, primary);
        } else if (rainEffect != null) {
            rainEffect.clearLightSource();
        }
    }

    private void renderFooter(DrawContext context) {
        String text = "Logged as " + client.getSession().getUsername() + "  |  " + ClientInfo.VERSION;
        Fonts.SF_REGULAR.drawCenteredText(context.getMatrices(), text, width / 2f, height - 24f, 7f, new Color(225, 225, 235, 135));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (MenuButton menuButton : buttons) {
                if (menuButton.isHovered(mouseX, mouseY)) {
                    menuButton.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public List<? extends Element> children() {
        return List.of();
    }

    private float ease(float value) {
        float inv = 1f - value;
        return 1f - inv * inv * inv;
    }

    private class MenuButton {
        private final String label;
        private final int x;
        private final int y;
        private final Runnable action;
        private final InfinityAnimation hoverAnimation = new InfinityAnimation().easing(Easing.QUART_OUT);

        private MenuButton(String label, int x, int y, Runnable action) {
            this.label = label;
            this.x = x;
            this.y = y;
            this.action = action;
        }

        private void render(DrawContext context, int mouseX, int mouseY, int index) {
            long elapsed = Util.getMeasuringTimeMs() - openTime - 120L - index * 55L;
            float entry = ease(MathHelper.clamp(elapsed / 480f, 0f, 1f));
            if (entry <= 0.01f) return;

            boolean hovered = isHovered(mouseX, mouseY);
            float hover = hoverAnimation.animate(hovered ? 1.0f : 0.0f, 280);

            float drawX = x;
            float drawY = y + (1f - entry) * 24f;

            float scale = 1.0f + 0.018f * hover;
            float centerX = drawX + BUTTON_WIDTH * 0.5f;
            float centerY = drawY + BUTTON_HEIGHT * 0.5f;

            MatrixStack matrices = context.getMatrices();
            matrices.push();
            matrices.translate(centerX, centerY, 0f);
            matrices.scale(scale, scale, 1f);
            matrices.translate(-centerX, -centerY, 0f);

            Color theme = UIColors.primary();

            int bgAlpha = MathHelper.clamp(Math.round(BUTTON_BG_COLOR.getAlpha() * entry), 0, 255);
            int borderAlpha = MathHelper.clamp(Math.round((38 + 62 * hover) * entry), 0, 255);

            RenderUtil.RECT.drawBorder(matrices, drawX, drawY, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS, 1f,
                    ColorUtil.setAlpha(theme, borderAlpha));

            RenderUtil.RECT.draw(matrices, drawX, drawY, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS,
                    ColorUtil.setAlpha(BUTTON_BG_COLOR, bgAlpha));

            RenderUtil.GRADIENT_RECT.draw(matrices, drawX, drawY, BUTTON_WIDTH, BUTTON_HEIGHT, BUTTON_RADIUS,
                    ColorUtil.setAlpha(theme, Math.round(16 * entry)),
                    ColorUtil.setAlpha(theme, Math.round(10 * entry)),
                    TRANSPARENT_COLOR,
                    ColorUtil.setAlpha(theme, Math.round(12 * entry)));

            String labelText = this.label;
            float fontSize = 7.2f;
            Color textColor = ColorUtil.interpolate(Color.WHITE, theme, 0.5 + 0.5 * hover);

            Fonts.SF_SEMIBOLD.drawCenteredText(matrices, labelText, centerX,
                    drawY + (BUTTON_HEIGHT - Fonts.SF_SEMIBOLD.getHeight(fontSize)) * 0.5f + 0.15f, fontSize,
                    ColorUtil.setAlpha(textColor, MathHelper.clamp(Math.round(255 * entry), 0, 255)));

            matrices.pop();
        }

        private boolean isHovered(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + BUTTON_WIDTH && mouseY >= y && mouseY <= y + BUTTON_HEIGHT;
        }
    }
}