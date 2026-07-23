package dev.ethereal.client.ui.autobuy;

import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.features.modules.other.AutoBuyModule;
import dev.ethereal.client.features.modules.other.autobuy.AutoBuyManager;
import dev.ethereal.client.features.modules.other.autobuy.AutoBuyableItem;
import dev.ethereal.client.features.modules.other.autobuy.FunTimePriceParser;
import dev.ethereal.client.features.modules.other.autobuy.FunTimeProvider;
import dev.ethereal.client.features.modules.other.autobuy.HolyTimeProvider;
import dev.ethereal.client.features.modules.other.autobuy.SpookyTimeProvider;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.awt.Color;
import java.util.*;
import java.util.List;

public class ScreenAutoBuy extends Screen implements QuickImports {
    private static ScreenAutoBuy INSTANCE;
    private float x, y, width = 400, height = 350;
    private float scroll = 0f;
    private float smoothedScroll = 0f;
    private AutoBuyableItem selectedItem = null;
    private String priceInput = "";
    private boolean isEditingPrice = false;
    private int cursorPosition = 0;

    private static final float ITEM_SIZE = 32;
    private static final float ITEM_SPACING = 4;
    private static final int ITEMS_PER_ROW = 8;

    public ScreenAutoBuy() {
        super(Text.of("AutoBuy"));
        AutoBuyManager.getInstance().loadPrices();
    }

    public static ScreenAutoBuy getInstance() {
        if (INSTANCE == null) INSTANCE = new ScreenAutoBuy();
        return INSTANCE;
    }

    public void openGui() {
        AutoBuyManager.getInstance().loadPrices();
        mc.setScreen(this);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (context == null || client == null) return;
        MatrixStack matrix = context.getMatrices();

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        RenderUtil.RECT.draw(matrix, 0, 0, sw, sh, 0f, new Color(0, 0, 0, 100));

        x = Math.round((sw - width) / 2f);
        y = Math.round((sh - height) / 2f);

        RenderUtil.BLUR_RECT.draw(matrix, x, y, width, height, 8f, UIColors.backgroundBlur(240));
        float borderW = 0.5f;
        RenderUtil.RECT.draw(matrix, x - borderW, y - borderW, width + borderW * 2, height + borderW * 2, 8f,
                new Color(255, 255, 255, 15));
        renderHeader(context, matrix, mouseX, mouseY);
        renderItems(context, matrix, mouseX, mouseY, delta);
        renderBottomBar(context, matrix, mouseX, mouseY);
    }

    private void renderHeader(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
        Color accent = UIColors.gradient(0, 255);
        Fonts.SF_MEDIUM.drawText(matrix, "✦ AutoBuy", x + 10, y + 8, 9f, accent);
    }

    private List<AutoBuyableItem> getAllItems() {
        var mode = AutoBuyModule.getInstance().getMode();
        if (mode.is("SpookyTime")) return SpookyTimeProvider.getItems();
        if (mode.is("HolyTime")) return HolyTimeProvider.getItems();
        return FunTimeProvider.getItems();
    }

    private void renderItems(DrawContext context, MatrixStack matrix, int mouseX, int mouseY, float delta) {
        float listX = x + 6;
        float listY = y + 30;
        float listW = width - 12;
        float listH = height - 78;

        List<AutoBuyableItem> items = getAllItems();
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
            AutoBuyableItem item = items.get(i);
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

    private void renderItemCard(DrawContext context, MatrixStack matrix, AutoBuyableItem item, float cx, float cy, int mouseX, int mouseY) {
        boolean hovered = mouseX >= cx && mouseX <= cx + ITEM_SIZE && mouseY >= cy && mouseY <= cy + ITEM_SIZE;
        boolean isSelected = selectedItem == item;
        boolean enabled = item.isEnabled();
        int price = item.getSettings().getBuyBelow();
        boolean hasPrice = price > 0;
        boolean parserEnabled = FunTimePriceParser.hasItem(item.getDisplayName());

        Color bg;
        if (isSelected) bg = new Color(70, 65, 100, 200);
        else if (hovered) bg = new Color(40, 40, 48, 200);
        else bg = new Color(20, 20, 25, 180);

        if (parserEnabled) {
            Color accent = UIColors.gradient(0, 200);
            RenderUtil.RECT.draw(matrix, cx - 1, cy - 1, ITEM_SIZE + 2, ITEM_SIZE + 2, 5f, new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
        }

        RenderUtil.BLUR_RECT.draw(matrix, cx, cy, ITEM_SIZE, ITEM_SIZE, 4f, bg);

        if (enabled && hasPrice) {
            Color accent = UIColors.gradient(1, 150);
            RenderUtil.RECT.draw(matrix, cx, cy, ITEM_SIZE, 2, 1f, accent);
        }

        try {
            ItemStack stack = item.createItemStack();
            if (stack != null) {
                float ix = cx + (ITEM_SIZE - 16) / 2f;
                float iy = cy + (ITEM_SIZE - 16) / 2f - 3;
                context.drawItem(stack, (int) ix, (int) iy);
            }
        } catch (Exception ignored) {}

        if (hasPrice) {
            String pStr = formatPrice(price);
            float pw = Fonts.SF_REGULAR.getWidth(pStr, 5f);
            Fonts.SF_REGULAR.drawText(matrix, pStr, cx + (ITEM_SIZE - pw) / 2f, cy + ITEM_SIZE - 10, 5f, new Color(180, 180, 100));
        }

        if (enabled) {
            RenderUtil.RECT.draw(matrix, cx + ITEM_SIZE - 6, cy + 2, 4, 4, 1f, new Color(80, 200, 80));
        }
    }

    private void renderBottomBar(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
        float by = y + height - 40;
        float bx = x + 10;
        float bw = width - 20;

        RenderUtil.BLUR_RECT.draw(matrix, bx, by, bw, 32, 6f, new Color(12, 12, 18, 220));

        if (selectedItem == null) {
            Fonts.SF_REGULAR.drawText(matrix, "Выберите предмет", bx + 8, by + 12, 7f, new Color(120, 120, 120));
            return;
        }

        try {
            ItemStack stack = selectedItem.createItemStack();
            if (stack != null) context.drawItem(stack, (int) bx + 5, (int) by + 8);
        } catch (Exception ignored) {}

        String name = selectedItem.getDisplayName();
        Fonts.SF_MEDIUM.drawText(matrix, name != null ? name : "?", bx + 24, by + 5, 7f, new Color(200, 200, 200));

        float inputX = bx + 140;
        float inputW = 90;
        float inputY = by + 6;
        float inputH = 20;

        boolean inputHovered = mouseX >= inputX && mouseX <= inputX + inputW && mouseY >= inputY && mouseY <= inputY + inputH;
        Color inputBg = isEditingPrice ? new Color(35, 35, 45, 200) : inputHovered ? new Color(30, 30, 40, 200) : new Color(20, 20, 30, 200);
        RenderUtil.RECT.draw(matrix, inputX, inputY, inputW, inputH, 4f, inputBg);

        String displayText;
        if (isEditingPrice) {
            displayText = priceInput.isEmpty() ? "0" : priceInput;
            Fonts.SF_REGULAR.drawText(matrix, displayText, inputX + 5, inputY + 5, 7f, new Color(220, 220, 220));
            if (System.currentTimeMillis() % 1000 < 500) {
                String bc = priceInput.substring(0, Math.min(cursorPosition, priceInput.length()));
                float cX = Fonts.SF_REGULAR.getWidth(bc, 7f);
                RenderUtil.RECT.draw(matrix, inputX + 5 + cX, inputY + 4, 1, inputH - 8, 0f, new Color(200, 200, 200));
            }
        } else {
            int cp = selectedItem.getSettings().getBuyBelow();
            displayText = cp > 0 ? formatPrice(cp) : "---";
            Fonts.SF_REGULAR.drawText(matrix, "$ " + displayText, inputX + 5, inputY + 5, 7f,
                    cp > 0 ? new Color(180, 180, 100) : new Color(100, 100, 100));
        }

        boolean parserOn = FunTimePriceParser.isEnabled();
        Color parserColor = parserOn ? new Color(100, 200, 100) : new Color(80, 80, 80);
        float pBtnX = inputX + inputW + 6;
        boolean pBtnHov = mouseX >= pBtnX && mouseX <= pBtnX + 24 && mouseY >= inputY && mouseY <= inputY + inputH;
        RenderUtil.RECT.draw(matrix, pBtnX, inputY, 24, inputH, 4f,
                pBtnHov ? new Color(parserColor.getRed() / 2, parserColor.getGreen() / 2, parserColor.getBlue() / 2, 200) : new Color(20, 20, 30, 200));
        Fonts.SF_REGULAR.drawText(matrix, "↻", pBtnX + 7, inputY + 4, 7f, parserColor);

        float sBtnX = pBtnX + 28;
        boolean sBtnHov = mouseX >= sBtnX && mouseX <= sBtnX + 70 && mouseY >= inputY && mouseY <= inputY + inputH;
        RenderUtil.RECT.draw(matrix, sBtnX, inputY, 70, inputH, 4f,
                sBtnHov ? new Color(40, 40, 55, 200) : new Color(20, 20, 30, 200));
        Fonts.SF_REGULAR.drawText(matrix, "Поиск", sBtnX + 10, inputY + 5, 7f, new Color(160, 160, 200));
    }

    private void renderScrollbar(DrawContext context, MatrixStack matrix, float listY, float listH, float contentH) {
        float sx = x + width - 10;
        RenderUtil.RECT.draw(matrix, sx, listY, 3, listH, 1.5f, new Color(27, 27, 30, 200));
        if (contentH > listH) {
            float maxScroll = contentH - listH;
            float progress = Math.abs(smoothedScroll) / maxScroll;
            float thumbH = Math.max(15, (listH / contentH) * listH);
            float thumbY = listY + progress * (listH - thumbH);
            Color accent = UIColors.gradient(0, 200);
            RenderUtil.RECT.draw(matrix, sx, thumbY, 3, thumbH, 1.5f, accent);
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
        float pBtnX = inputX + inputW + 6;
        float sBtnX = pBtnX + 28;

        if (button == 0) {
            if (selectedItem != null && mouseX >= sBtnX && mouseX <= sBtnX + 70 && mouseY >= inputY && mouseY <= inputY + inputH) {
                String searchName = selectedItem.getSearchName();
                if (searchName != null && mc.player != null && mc.player.networkHandler != null) {
                    mc.player.networkHandler.sendChatCommand("ah search " + searchName);
                }
                return true;
            }

            if (selectedItem != null && mouseX >= pBtnX && mouseX <= pBtnX + 24 && mouseY >= inputY && mouseY <= inputY + inputH) {
                String name = selectedItem.getDisplayName();
                if (name != null) {
                    boolean current = FunTimePriceParser.hasItem(name);
                    FunTimePriceParser.setItemsForParser(current ? Collections.emptyList() : Collections.singletonList(name));
                }
                return true;
            }

            if (selectedItem != null && mouseX >= inputX && mouseX <= inputX + inputW && mouseY >= inputY && mouseY <= inputY + inputH) {
                isEditingPrice = true;
                int cp = selectedItem.getSettings().getBuyBelow();
                priceInput = cp > 0 ? String.valueOf(cp) : "";
                cursorPosition = priceInput.length();
                return true;
            }

            List<AutoBuyableItem> items = getAllItems();
            float listY = y + 30;
            float startX = x + 6;
            float curX = startX;
            float curY = listY + smoothedScroll;

            for (int i = 0; i < items.size(); i++) {
                AutoBuyableItem item = items.get(i);
                if (item != null && mouseX >= curX && mouseX <= curX + ITEM_SIZE && mouseY >= curY && mouseY <= curY + ITEM_SIZE) {
                    if (mouseX >= curX + ITEM_SIZE - 10 && mouseY <= curY + 10) {
                        item.setEnabled(!item.isEnabled());
                    } else {
                        selectedItem = item;
                        isEditingPrice = false;
                    }
                    return true;
                }
                curX += ITEM_SIZE + ITEM_SPACING;
                if ((i + 1) % ITEMS_PER_ROW == 0) { curX = startX; curY += ITEM_SIZE + ITEM_SPACING; }
            }
        }

        if (button == 1) {
            List<AutoBuyableItem> items = getAllItems();
            float listY = y + 30;
            float startX = x + 6;
            float curX = startX;
            float curY = listY + smoothedScroll;

            for (int i = 0; i < items.size(); i++) {
                AutoBuyableItem item = items.get(i);
                if (item != null && mouseX >= curX && mouseX <= curX + ITEM_SIZE && mouseY >= curY && mouseY <= curY + ITEM_SIZE) {
                    String name = item.getDisplayName();
                    if (name != null) {
                        Set<String> current = new HashSet<>(FunTimePriceParser.getParserItemNames());
                        if (current.contains(name.toLowerCase())) current.remove(name.toLowerCase());
                        else current.add(name.toLowerCase());
                        FunTimePriceParser.setItemsForParser(new ArrayList<>(current));
                    }
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
                            selectedItem.getSettings().setBuyBelow(price);
                            AutoBuyManager.getInstance().savePrice(selectedItem.getDisplayName(), price);
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
