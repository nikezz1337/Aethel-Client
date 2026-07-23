package dev.ethereal.client.ui.altmanager;

import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.ui.mainmenu.MenuBackgroundRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class AltManagerScreen extends Screen implements QuickImports {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 280;
    private static final int ROW_HEIGHT = 25;
    private static final Random RANDOM = new Random();

    private final Screen parent;
    private TextFieldWidget usernameField;
    private AltAccount selected;
    private float scroll;
    private int x;
    private int y;

    public AltManagerScreen(Screen parent) {
        super(Text.of("Alt Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        x = (width - PANEL_WIDTH) / 2;
        y = (height - PANEL_HEIGHT) / 2;

        usernameField = new TextFieldWidget(textRenderer, x + 18, y + 45, 245, 22, Text.of("Nickname"));
        usernameField.setMaxLength(16);
        usernameField.setText("");
        addDrawableChild(usernameField);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        MenuBackgroundRenderer.render(context, width, height);
        RenderUtil.RECT.draw(matrix, 0, 0, width, height, 0f, new Color(0, 0, 0, 90));

        RenderUtil.RECT.draw(matrix, x - 0.5f, y - 0.5f, PANEL_WIDTH + 1, PANEL_HEIGHT + 1, 8f, new Color(255, 255, 255, 18));
        RenderUtil.RECT.draw(matrix, x, y, PANEL_WIDTH, PANEL_HEIGHT, 8f, new Color(11, 11, 18, 238));

        Fonts.SF_SEMIBOLD.drawText(matrix, "Alt Manager", x + 18, y + 15, 11f, UIColors.textColor());
        Fonts.SF_REGULAR.drawText(matrix, "Current: " + client.getSession().getUsername(), x + 112, y + 17, 7f, UIColors.inactiveTextColor());

        super.render(context, mouseX, mouseY, delta);

        renderButton(matrix, "Add", x + 272, y + 45, 58, 22, mouseX, mouseY, true);
        renderButton(matrix, "Random", x + 336, y + 45, 66, 22, mouseX, mouseY, true);
        renderList(matrix, mouseX, mouseY);
        renderButton(matrix, "Login", x + 18, y + 242, 90, 24, mouseX, mouseY, selected != null);
        renderButton(matrix, "Delete", x + 116, y + 242, 90, 24, mouseX, mouseY, selected != null);
        renderButton(matrix, "Back", x + 312, y + 242, 90, 24, mouseX, mouseY, true);

        String status = AltManager.getInstance().getStatus();
        if (status != null && !status.isEmpty()) {
            Fonts.SF_REGULAR.drawText(matrix, status, x + 18, y + 222, 7f, UIColors.inactiveTextColor());
        }
    }

    private void renderList(MatrixStack matrix, int mouseX, int mouseY) {
        float listX = x + 18;
        float listY = y + 78;
        float listW = PANEL_WIDTH - 36;
        float listH = 132;

        RenderUtil.RECT.draw(matrix, listX, listY, listW, listH, 5f, new Color(10, 10, 16, 185));

        List<AltAccount> accounts = AltManager.getInstance().getAccounts();
        if (accounts.isEmpty()) {
            Fonts.SF_REGULAR.drawCenteredText(matrix, "No alts yet", listX + listW / 2f, listY + 54, 8f, UIColors.inactiveTextColor());
            return;
        }

        int visibleRows = (int) (listH / ROW_HEIGHT);
        int maxScroll = Math.max(0, accounts.size() - visibleRows);
        scroll = MathHelper.clamp(scroll, 0, maxScroll);
        int start = (int) scroll;

        for (int row = 0; row < visibleRows && start + row < accounts.size(); row++) {
            AltAccount account = accounts.get(start + row);
            float rowY = listY + row * ROW_HEIGHT;
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT;
            boolean current = account.username().equalsIgnoreCase(client.getSession().getUsername());
            boolean isSelected = account.equals(selected);

            Color bg = isSelected ? new Color(68, 68, 96, 185) : hovered ? new Color(35, 35, 47, 180) : new Color(0, 0, 0, 0);
            if (bg.getAlpha() > 0) {
                RenderUtil.RECT.draw(matrix, listX + 3, rowY + 3, listW - 6, ROW_HEIGHT - 5, 4f, bg);
            }

            Color dot = current ? new Color(90, 215, 105) : UIColors.gradient(row, 180);
            RenderUtil.RECT.draw(matrix, listX + 12, rowY + 10, 5, 5, 2.5f, dot);
            Fonts.SF_MEDIUM.drawText(matrix, account.username(), listX + 25, rowY + 8, 8f, UIColors.textColor());
            if (current) {
                Fonts.SF_REGULAR.drawText(matrix, "active", listX + listW - 46, rowY + 8, 7f, new Color(90, 215, 105));
            }
        }
    }

    private void renderButton(MatrixStack matrix, String label, float bx, float by, float bw, float bh, int mouseX, int mouseY, boolean enabled) {
        boolean hovered = enabled && mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
        Color bg = !enabled ? new Color(20, 20, 27, 115) : hovered ? new Color(45, 45, 62, 210) : new Color(23, 23, 32, 205);
        RenderUtil.RECT.draw(matrix, bx, by, bw, bh, 5f, bg);
        RenderUtil.RECT.draw(matrix, bx - 0.4f, by - 0.4f, bw + 0.8f, bh + 0.8f, 5f, new Color(255, 255, 255, hovered ? 28 : 12));
        Fonts.SF_MEDIUM.drawCenteredText(matrix, label, bx + bw / 2f, by + 7, 7f, enabled ? UIColors.textColor() : UIColors.inactiveTextColor());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (inside(mouseX, mouseY, x + 272, y + 45, 58, 22)) {
                AltAccount added = AltManager.getInstance().add(usernameField.getText());
                if (added != null) {
                    selected = added;
                    usernameField.setText("");
                }
                return true;
            }

            if (inside(mouseX, mouseY, x + 336, y + 45, 66, 22)) {
                usernameField.setText(randomName());
                return true;
            }

            if (inside(mouseX, mouseY, x + 18, y + 242, 90, 24) && selected != null) {
                AltManager.getInstance().login(selected);
                selected = AltManager.getInstance().getAccounts().isEmpty() ? null : AltManager.getInstance().getAccounts().get(0);
                return true;
            }

            if (inside(mouseX, mouseY, x + 116, y + 242, 90, 24) && selected != null) {
                AltManager.getInstance().remove(selected);
                selected = null;
                return true;
            }

            if (inside(mouseX, mouseY, x + 312, y + 242, 90, 24)) {
                close();
                return true;
            }

            AltAccount clicked = getClickedAccount(mouseX, mouseY);
            if (clicked != null) {
                selected = clicked;
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inside(mouseX, mouseY, x + 18, y + 78, PANEL_WIDTH - 36, 132)) {
            scroll -= (float) verticalAmount;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            AltAccount added = AltManager.getInstance().add(usernameField.getText());
            if (added != null) {
                selected = added;
                usernameField.setText("");
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private AltAccount getClickedAccount(double mouseX, double mouseY) {
        float listX = x + 18;
        float listY = y + 78;
        float listW = PANEL_WIDTH - 36;
        float listH = 132;
        if (!inside(mouseX, mouseY, listX, listY, listW, listH)) return null;

        int index = (int) scroll + (int) ((mouseY - listY) / ROW_HEIGHT);
        List<AltAccount> accounts = AltManager.getInstance().getAccounts();
        return index >= 0 && index < accounts.size() ? accounts.get(index) : null;
    }

    private boolean inside(double mouseX, double mouseY, float bx, float by, float bw, float bh) {
        return mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
    }

    private String randomName() {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder builder = new StringBuilder("Ethereal");
        for (int i = 0; i < 4; i++) {
            builder.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString().substring(0, Math.min(16, builder.length())).toLowerCase(Locale.ROOT);
    }
}
