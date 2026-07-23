package dev.aethel.ui.autobuy;

import dev.aethel.util.IMinecraft;
import dev.aethel.util.auction.ab.BuyableItem;
import dev.aethel.util.auction.ab.BuyableRegistry;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public class ScreenAutoBuy extends Screen implements IMinecraft {
    private static ScreenAutoBuy INSTANCE;
    private float x, y, width = 400, height = 350;
    private float scroll = 0f;
    private float smoothedScroll = 0f;
    private BuyableItem selectedItem = null;
    private String priceInput = "";
    private boolean isEditingPrice = false;
    private int cursorPosition = 0;

    private static final float ITEM_SIZE = 32;
    private static final float ITEM_SPACING = 4;
    private static final int ITEMS_PER_ROW = 8;

    public ScreenAutoBuy() {
        super(Text.of("AutoBuy"));
    }

    public static ScreenAutoBuy getInstance() {
        if (INSTANCE == null) INSTANCE = new ScreenAutoBuy();
        return INSTANCE;
    }

    public void openGui() {
        BuyableRegistry.init();
        mc.setScreen(this);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (context == null || mc == null) return;
        MatrixStack matrix = context.getMatrices();

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        DrawUtil.drawRound(0, 0, sw, sh, 0, 0x64000000);

        x = Math.round((sw - width) / 2f);
        y = Math.round((sh - height) / 2f);

        DrawUtil.drawRoundBlur(x, y, width, height, 8f, 0xF0121218, 20f);
        float borderW = 0.5f;
        DrawUtil.drawRound(x - borderW, y - borderW, width + borderW * 2, height + borderW * 2, 8f, 0x0FFFFFFF);
        renderHeader(context, matrix, mouseX, mouseY);
        renderItems(context, matrix, mouseX, mouseY, delta);
        renderBottomBar(context, matrix, mouseX, mouseY);
    }

    private void renderHeader(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
        int accent = ColorProvider.setAlpha(ColorProvider.getThemeColor(), 255);
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), "✦ AutoBuy", x + 10, y + 8, accent, 9f);
    }

    private void renderItems(DrawContext context, MatrixStack matrix, int mouseX, int mouseY, float delta) {
        float listX = x + 6;
        float listY = y + 30;
        float listW = width - 12;
        float listH = height - 78;

        List<BuyableItem> items = BuyableRegistry.getAll();
        if (items.isEmpty()) return;

        int rows = (int) Math.ceil((double) items.size() / ITEMS_PER_ROW);
        float contentH = rows * (ITEM_SIZE + ITEM_SPACING);
        float maxScroll = Math.max(0, contentH - listH);
        scroll = MathHelper.clamp(scroll, -maxScroll, 0f);
        smoothedScroll += (scroll - smoothedScroll) * 0.12f;

        context.enableScissor((int) listX, (int) listY, (int) (listX + listW), (int) (listY + listH));

        float startX = listX;
        float curX = startX;
        float curY = listY + smoothedScroll;

        for (int i = 0; i < items.size(); i++) {
            BuyableItem item = items.get(i);
            if (item == null) continue;
            if (curY + ITEM_SIZE >= listY && curY <= listY + listH) {
                renderItemCard(context, matrix, item, curX, curY, mouseX, mouseY);
            }
            curX += ITEM_SIZE + ITEM_SPACING;
            if ((i + 1) % ITEMS_PER_ROW == 0) {
                curX = startX;
                curY += ITEM_SIZE + ITEM_SPACING;
            }
        }

        context.disableScissor();
        renderScrollbar(context, matrix, listY, listH, contentH);
    }

    private void renderItemCard(DrawContext context, MatrixStack matrix, BuyableItem item, float cx, float cy, int mouseX, int mouseY) {
        boolean hovered = mouseX >= cx && mouseX <= cx + ITEM_SIZE && mouseY >= cy && mouseY <= cy + ITEM_SIZE;
        boolean isSelected = selectedItem == item;
        boolean enabled = item.isEnabled();
        int price = item.getMaxPrice();
        boolean hasPrice = price > 0;

        int bg;
        if (isSelected) bg = 0xC8464164;
        else if (hovered) bg = 0xC8282830;
        else bg = 0xB4141419;

        DrawUtil.drawRoundBlur(cx, cy, ITEM_SIZE, ITEM_SIZE, 4f, bg, 20f);

        if (enabled && hasPrice) {
            int accent = ColorProvider.setAlpha(ColorProvider.getThemeColor(), 150);
            DrawUtil.drawRound(cx, cy, ITEM_SIZE, 2, 1f, accent);
        }

        try {
            ItemStack stack = new ItemStack(item.getDisplayItem());
            if (!stack.isEmpty()) {
                float ix = cx + (ITEM_SIZE - 16) / 2f;
                float iy = cy + (ITEM_SIZE - 16) / 2f - 3;
                context.drawItem(stack, (int) ix, (int) iy);
            }
        } catch (Exception ignored) {}

        if (hasPrice) {
            String pStr = formatPrice(price);
            float pw = Fonts.SFREGULAR.get().getWidth(pStr, 5f);
            DrawUtil.drawText(Fonts.SFREGULAR.get(), pStr, cx + (ITEM_SIZE - pw) / 2f, cy + ITEM_SIZE - 10, 0xFFB4B464, 5f);
        }

        if (enabled) {
            DrawUtil.drawRound(cx + ITEM_SIZE - 6, cy + 2, 4, 4, 1f, 0x50C85032);
        }
    }

    private void renderBottomBar(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
        float by = y + height - 40;
        float bx = x + 10;
        float bw = width - 20;

        DrawUtil.drawRoundBlur(bx, by, bw, 32, 6f, 0xDC0C0C12, 20f);

        if (selectedItem == null) {
            DrawUtil.drawText(Fonts.SFREGULAR.get(), "Выберите предмет", bx + 8, by + 12, 0xFF787878, 7f);
            return;
        }

        try {
            ItemStack stack = new ItemStack(selectedItem.getDisplayItem());
            if (!stack.isEmpty()) context.drawItem(stack, (int) bx + 5, (int) by + 8);
        } catch (Exception ignored) {}

        String name = selectedItem.getName();
        DrawUtil.drawText(Fonts.SFMEDIUM.get(), name != null ? name : "?", bx + 24, by + 5, 0xFFC8C8C8, 7f);

        float inputX = bx + 140;
        float inputW = 90;
        float inputY = by + 6;
        float inputH = 20;

        boolean inputHovered = mouseX >= inputX && mouseX <= inputX + inputW && mouseY >= inputY && mouseY <= inputY + inputH;
        int inputBg = isEditingPrice ? 0xC823232D : inputHovered ? 0xC81E1E28 : 0xC814141E;
        DrawUtil.drawRound(inputX, inputY, inputW, inputH, 4f, inputBg);

        String displayText;
        if (isEditingPrice) {
            displayText = priceInput.isEmpty() ? "0" : priceInput;
            DrawUtil.drawText(Fonts.SFREGULAR.get(), displayText, inputX + 5, inputY + 5, 0xFFDCDCDC, 7f);
            if (System.currentTimeMillis() % 1000 < 500) {
                String bc = priceInput.substring(0, Math.min(cursorPosition, priceInput.length()));
                float cX = Fonts.SFREGULAR.get().getWidth(bc, 7f);
                DrawUtil.drawRound(inputX + 5 + cX, inputY + 4, 1, inputH - 8, 0f, 0xFFC8C8C8);
            }
        } else {
            int cp = selectedItem.getMaxPrice();
            displayText = cp > 0 ? formatPrice(cp) : "---";
            DrawUtil.drawText(Fonts.SFREGULAR.get(), "$ " + displayText, inputX + 5, inputY + 5, cp > 0 ? 0xFFB4B464 : 0xFF646464, 7f);
        }
    }

    private void renderScrollbar(DrawContext context, MatrixStack matrix, float listY, float listH, float contentH) {
        float sx = x + width - 10;
        DrawUtil.drawRound(sx, listY, 3, listH, 1.5f, 0xC81B1B1E);
        if (contentH > listH) {
            float maxScroll = contentH - listH;
            float progress = Math.abs(smoothedScroll) / maxScroll;
            float thumbH = Math.max(15, (listH / contentH) * listH);
            float thumbY = listY + progress * (listH - thumbH);
            int accent = ColorProvider.setAlpha(ColorProvider.getThemeColor(), 200);
            DrawUtil.drawRound(sx, thumbY, 3, thumbH, 1.5f, accent);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float by = y + height - 40;
        float bx = x + 10;
        float inputX = bx + 140;
        float inputW = 90;
        float inputY = by + 6;
        float inputH = 20;

        if (button == 0) {
            if (selectedItem != null && mouseX >= inputX && mouseX <= inputX + inputW && mouseY >= inputY && mouseY <= inputY + inputH) {
                isEditingPrice = true;
                int cp = selectedItem.getMaxPrice();
                priceInput = cp > 0 ? String.valueOf(cp) : "";
                cursorPosition = priceInput.length();
                return true;
            }

            List<BuyableItem> items = BuyableRegistry.getAll();
            float listY = y + 30;
            float startX = x + 6;
            float curX = startX;
            float curY = listY + smoothedScroll;

            for (int i = 0; i < items.size(); i++) {
                BuyableItem item = items.get(i);
                if (item != null && mouseX >= curX && mouseX <= curX + ITEM_SIZE && mouseY >= curY && mouseY <= curY + ITEM_SIZE) {
                    selectedItem = item;
                    isEditingPrice = false;
                    return true;
                }
                curX += ITEM_SIZE + ITEM_SPACING;
                if ((i + 1) % ITEMS_PER_ROW == 0) { curX = startX; curY += ITEM_SIZE + ITEM_SPACING; }
            }
        }

        if (button == 1) {
            List<BuyableItem> items = BuyableRegistry.getAll();
            float listY = y + 30;
            float startX = x + 6;
            float curX = startX;
            float curY = listY + smoothedScroll;

            for (int i = 0; i < items.size(); i++) {
                BuyableItem item = items.get(i);
                if (item != null && mouseX >= curX && mouseX <= curX + ITEM_SIZE && mouseY >= curY && mouseY <= curY + ITEM_SIZE) {
                    item.setEnabled(!item.isEnabled());
                    return true;
                }
                curX += ITEM_SIZE + ITEM_SPACING;
                if ((i + 1) % ITEMS_PER_ROW == 0) { curX = startX; curY += ITEM_SIZE + ITEM_SPACING; }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll += verticalAmount * 20;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isEditingPrice) {
            if (keyCode == 257 || keyCode == 335) {
                if (selectedItem != null && !priceInput.isEmpty()) {
                    try {
                        int price = Integer.parseInt(priceInput);
                        if (price > 0) {
                            selectedItem.setMaxPrice(price);
                        }
                    } catch (NumberFormatException ignored) {}
                }
                isEditingPrice = false;
                return true;
            }
            if (keyCode == 256) { isEditingPrice = false; return true; }
            if (keyCode == 259) {
                if (cursorPosition > 0 && !priceInput.isEmpty()) {
                    priceInput = priceInput.substring(0, cursorPosition - 1) + priceInput.substring(cursorPosition);
                    cursorPosition--;
                }
                return true;
            }
            if (keyCode == 263) { if (cursorPosition > 0) cursorPosition--; return true; }
            if (keyCode == 262) { if (cursorPosition < priceInput.length()) cursorPosition++; return true; }
            if (keyCode == 268) { cursorPosition = 0; return true; }
            if (keyCode == 269) { cursorPosition = priceInput.length(); return true; }
            if ((keyCode >= 48 && keyCode <= 57) || (keyCode >= 320 && keyCode <= 329)) {
                char c = (char) (keyCode >= 320 ? '0' + (keyCode - 320) : '0' + (keyCode - 48));
                priceInput = priceInput.substring(0, cursorPosition) + c + priceInput.substring(cursorPosition);
                cursorPosition++;
                return true;
            }
            return true;
        }
        if (keyCode == 256) { mc.setScreen(null); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String formatPrice(int price) {
        StringBuilder sb = new StringBuilder();
        String s = String.valueOf(price);
        int c = 0;
        for (int i = s.length() - 1; i >= 0; i--) {
            if (c > 0 && c % 3 == 0) sb.insert(0, '.');
            sb.insert(0, s.charAt(i));
            c++;
        }
        return sb.toString();
    }
}
