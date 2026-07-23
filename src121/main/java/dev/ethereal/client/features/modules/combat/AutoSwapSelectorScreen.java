package dev.ethereal.client.features.modules.combat;

import dev.ethereal.api.utils.animation.Easing;
import dev.ethereal.api.utils.animation.wrap.WrapAnimation;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.math.MouseUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.ScissorUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class AutoSwapSelectorScreen extends Screen {
    private static final float CARD_WIDTH = 90f;
    private static final float CARD_HEIGHT = 72f;
    private static final float CARD_GAP = 12f;
    private static final float CARD_RADIUS = 8f;
    private static final float CARD_INNER_PADDING = 6f;
    private static final float TEXT_AREA_HEIGHT = 14f;
    private static final float ITEM_SCALE = 2.5f;
    private static final float MAGNET_RANGE = 34f;
    private static final float FONT_SIZE = 6.2f;

    private final ItemSwapModule module;
    private final WrapAnimation openAnimation = new WrapAnimation()
            .setSpeed(220).setSize(1f).setEasing(Easing.BACK_OUT);
    private final List<WrapAnimation> hoverAnimations = new ArrayList<>();

    private List<ItemSwapModule.SwapCandidate> candidates;

    private boolean closing;
    private int previewIndex;
    private int pendingSlotId = -1;
    private long openedAt;

    public AutoSwapSelectorScreen(ItemSwapModule module, List<ItemSwapModule.SwapCandidate> candidates) {
        super(Text.empty());
        this.module = module;
        this.candidates = new ArrayList<>(candidates);
    }

    @Override
    protected void init() {
        openAnimation.reset();
        openAnimation.setForward(true);
        closing = false;
        openedAt = System.currentTimeMillis();
        previewIndex = MathHelper.clamp(previewIndex, 0, Math.max(0, candidates.size() - 1));
        syncHoverAnimations();
    }

    @Override
    public void tick() {
        if (module == null || client == null || client.player == null || !module.isEnabled()) {
            beginClose(-1);
            return;
        }
        updateMovementKeys();

        if (!closing && candidates.isEmpty()) {
            reloadCandidates();
            if (candidates.isEmpty()) {
                beginClose(-1);
                return;
            }
        }

        if (!closing && System.currentTimeMillis() - openedAt > 60L && !isBindHeld()) {
            int hoveredIndex = resolveHoveredIndex(getUiMouseX(), getUiMouseY(), buildLayout());
            beginClose(hoveredIndex == -1 ? -1 : candidates.get(hoveredIndex).getSlotId());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (closing) {
            openAnimation.setForward(false);
        }
        float open = MathHelper.clamp(openAnimation.get(), 0f, 1f);
        if (closing && open <= 0.01f) {
            finishClose();
            return;
        }

        if (candidates.isEmpty()) return;

        Layout layout = buildLayout();
        float uiMouseX = getUiMouseX();
        float uiMouseY = getUiMouseY();
        int hoveredIndex = resolveHoveredIndex(uiMouseX, uiMouseY, layout);
        if (hoveredIndex != -1) previewIndex = hoveredIndex;

        syncHoverAnimations();
        for (int i = 0; i < hoverAnimations.size(); i++) {
            hoverAnimations.get(i).setForward(i == hoveredIndex);
        }

        float scale = getScreenScale();
        float centerX = width * 0.5f;
        float centerY = height * 0.5f;

        context.getMatrices().push();
        context.getMatrices().translate(centerX, centerY, 0f);
        context.getMatrices().scale(scale, scale, 1f);
        context.getMatrices().translate(-centerX, -centerY, 0f);

        renderCards(context, layout, hoveredIndex, open);

        context.getMatrices().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E) {
            beginClose(-1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void renderCards(DrawContext context, Layout layout, int hoveredIndex, float open) {
        for (int i = 0; i < candidates.size(); i++) {
            Rect rect = getCardRect(layout, i);
            float hover = hoverAnimations.get(i).get();
            float cardX = rect.x;
            float cardY = rect.y;

            Color accent = UIColors.gradient((int) (130 + hover * 60f));
            Color idleBorder = new Color(255, 255, 255, 0);
            Color hoverBorder = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int) (175 * open));
            Color border = ColorUtil.interpolate(idleBorder, hoverBorder, hover);

            RenderUtil.BLUR_RECT.draw(context.getMatrices(), cardX, cardY, rect.width, rect.height, CARD_RADIUS, new Color(0, 0, 0, 200));
            RenderUtil.drawGradientBorder(context.getMatrices(), cardX, cardY, rect.width, rect.height, 1f, border, border);

            ItemSwapModule.SwapCandidate candidate = candidates.get(i);
            float itemSize = 16f * ITEM_SCALE;
            float itemX = cardX + rect.width * 0.5f - itemSize * 0.5f;
            float itemY = cardY + CARD_INNER_PADDING + 0.5f;
            context.getMatrices().push();
            context.getMatrices().translate(itemX, itemY, 1f);
            context.getMatrices().scale(ITEM_SCALE, ITEM_SCALE, 1f);
            context.drawItem(candidate.getStack(), 0, 0);
            context.getMatrices().pop();

            float textBoxY = cardY + rect.height - CARD_INNER_PADDING - TEXT_AREA_HEIGHT;
            float textCenterY = textBoxY + TEXT_AREA_HEIGHT * 0.5f - FONT_SIZE * 0.5f + 0.2f;
            float scissorX = cardX + CARD_INNER_PADDING;
            float scissorWidth = rect.width - CARD_INNER_PADDING * 2f;
            float nameWidth = Fonts.PS_REGULAR.getWidth(candidate.getDisplayName(), FONT_SIZE);
            float textX = cardX + rect.width * 0.5f - nameWidth * 0.5f;

            ScissorUtil.start(context.getMatrices(), scissorX, textBoxY, scissorWidth, TEXT_AREA_HEIGHT);
            Fonts.PS_REGULAR.drawText(context.getMatrices(), candidate.getStack().getName(), textX, textCenterY, FONT_SIZE);
            ScissorUtil.stop(context.getMatrices());
        }
    }

    private void reloadCandidates() {
        candidates = new ArrayList<>(module.collectSelectorCandidates());
        previewIndex = MathHelper.clamp(previewIndex, 0, Math.max(0, candidates.size() - 1));
        syncHoverAnimations();
    }

    private void syncHoverAnimations() {
        while (hoverAnimations.size() < candidates.size()) {
            hoverAnimations.add(new WrapAnimation().setSpeed(180).setSize(1f).setEasing(Easing.SINE_BOTH));
        }
        while (hoverAnimations.size() > candidates.size()) {
            hoverAnimations.remove(hoverAnimations.size() - 1);
        }
    }

    private int resolveHoveredIndex(float mouseX, float mouseY, Layout layout) {
        int bestIndex = -1;
        double bestScore = Double.MAX_VALUE;

        for (int i = 0; i < candidates.size(); i++) {
            Rect rect = getCardRect(layout, i);
            float centerX = rect.x + rect.width * 0.5f;
            float centerY = rect.y + rect.height * 0.5f;
            double distance = Math.hypot(mouseX - centerX, mouseY - centerY);

            boolean directHover = MouseUtil.isHovered(mouseX, mouseY, rect.x, rect.y, rect.width, rect.height);
            boolean nearHover = MouseUtil.isHovered(
                    mouseX, mouseY,
                    rect.x - MAGNET_RANGE * 0.5f,
                    rect.y - MAGNET_RANGE * 0.5f,
                    rect.width + MAGNET_RANGE,
                    rect.height + MAGNET_RANGE
            );

            if (!directHover && !nearHover && distance > MAGNET_RANGE) continue;

            double score = distance - (directHover ? 20d : 0d);
            if (score < bestScore) {
                bestScore = score;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private boolean isBindHeld() {
        if (client == null || module == null) return false;
        int bindValue = module.bind.getValue();
        if (bindValue == -1 || bindValue == -999) return false;

        long handle = client.getWindow().getHandle();
        if (bindValue < 0) {
            int mouseButton = bindValue + 100;
            return GLFW.glfwGetMouseButton(handle, mouseButton) == GLFW.GLFW_PRESS;
        }
        return GLFW.glfwGetKey(handle, bindValue) == GLFW.GLFW_PRESS;
    }

    private float getUiMouseX() {
        return transformMouseX(getScaledMouseX());
    }

    private float getUiMouseY() {
        return transformMouseY(getScaledMouseY());
    }

    private float getScaledMouseX() {
        if (client == null) return 0f;
        return (float) (client.mouse.getX() * width / client.getWindow().getWidth());
    }

    private float getScaledMouseY() {
        if (client == null) return 0f;
        return (float) (client.mouse.getY() * height / client.getWindow().getHeight());
    }

    private float getScreenScale() {
        return 0.86f + openAnimation.get() * 0.14f;
    }

    private float transformMouseX(float mouseX) {
        float scale = getScreenScale();
        float centerX = width * 0.5f;
        return centerX + (mouseX - centerX) / scale;
    }

    private float transformMouseY(float mouseY) {
        float scale = getScreenScale();
        float centerY = height * 0.5f;
        return centerY + (mouseY - centerY) / scale;
    }

    private void beginClose(int slotId) {
        if (closing) return;
        closing = true;
        pendingSlotId = slotId;
    }

    private void finishClose() {
        if (client != null && client.currentScreen == this) {
            client.setScreen(null);
        }

        if (pendingSlotId != -1) {
            int slotId = pendingSlotId;
            pendingSlotId = -1;
            if (client != null) {
                client.execute(() -> module.startSwapBySlotId(slotId));
            } else {
                module.startSwapBySlotId(slotId);
            }
        }
    }

    private void updateMovementKeys() {
        if (client == null) return;
        long handle = client.getWindow().getHandle();
        client.options.forwardKey.setPressed(GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS);
        client.options.backKey.setPressed(GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS);
        client.options.leftKey.setPressed(GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS);
        client.options.rightKey.setPressed(GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS);
        client.options.jumpKey.setPressed(GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS);
        client.options.sprintKey.setPressed(GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS);
    }

    private Layout buildLayout() {
        int visibleCount = Math.max(1, candidates.size());
        int rows = Math.max(1, (visibleCount + 4) / 5);
        float layoutWidth = getRowWidth(Math.min(5, visibleCount));
        float layoutHeight = rows * CARD_HEIGHT + (rows - 1) * CARD_GAP;
        float x = this.width * 0.5f - layoutWidth * 0.5f;
        float y = this.height * 0.5f - layoutHeight * 0.5f;
        return new Layout(x, y, layoutWidth, layoutHeight);
    }

    private Rect getCardRect(Layout layout, int index) {
        int row = index / 5;
        int indexInRow = index % 5;
        int itemsInRow = Math.min(5, candidates.size() - row * 5);
        float rowWidth = getRowWidth(itemsInRow);
        float rowX = layout.x + layout.width * 0.5f - rowWidth * 0.5f;
        float x = rowX + indexInRow * (CARD_WIDTH + CARD_GAP);
        float y = layout.y + row * (CARD_HEIGHT + CARD_GAP);
        return new Rect(x, y, CARD_WIDTH, CARD_HEIGHT);
    }

    private float getRowWidth(int itemsInRow) {
        return itemsInRow * CARD_WIDTH + Math.max(0, itemsInRow - 1) * CARD_GAP;
    }

    private record Layout(float x, float y, float width, float height) {}
    private record Rect(float x, float y, float width, float height) {}
}
