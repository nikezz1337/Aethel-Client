package dev.aethel.ui.hud;

import dev.aethel.config.FriendManager;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

public class ChatFriendPanel implements IMinecraft {

    private static final ChatFriendPanel INSTANCE = new ChatFriendPanel();

    private String playerName = null;
    private float panelX = 0;
    private float panelY = 0;
    private boolean visible = false;

    private final Animation showAnim = new Animation(Easing.EXPO_OUT, 200);
    private final Animation hoverAdd = new Animation(Easing.QUINTIC_OUT, 150);
    private final Animation hoverRemove = new Animation(Easing.QUINTIC_OUT, 150);

    private static final float BTN_HEIGHT = 12f;
    private static final float BTN_PADDING_X = 6f;
    private static final float BTN_TEXT_SIZE = 6.5f;
    private static final float BTN_RADIUS = 3f;
    private static final float PANEL_GAP = 2f;

    private ChatFriendPanel() {}

    public static ChatFriendPanel getInstance() {
        return INSTANCE;
    }

    public void show(String name, float x, float y) {
        if (name == null || name.isEmpty()) return;
        this.playerName = name;
        this.panelX = x;
        this.panelY = y - BTN_HEIGHT - PANEL_GAP;
        this.visible = true;
        this.showAnim.run(true);
    }

    public void hide() {
        this.visible = false;
        this.playerName = null;
        this.showAnim.run(false);
    }

    public boolean isVisible() {
        return visible && playerName != null;
    }

    public boolean isHoveringAdd(double mouseX, double mouseY) {
        if (!isVisible()) return false;
        float anim = (float) showAnim.getValue();
        if (anim < 0.1f) return false;
        float w = getAddButtonWidth();
        return mouseX >= panelX && mouseX <= panelX + w
                && mouseY >= panelY && mouseY <= panelY + BTN_HEIGHT;
    }

    public boolean isHoveringRemove(double mouseX, double mouseY) {
        if (!isVisible()) return false;
        if (!FriendManager.isFriend(playerName)) return false;
        float anim = (float) showAnim.getValue();
        if (anim < 0.1f) return false;
        float addW = getAddButtonWidth();
        float removeX = panelX + addW + PANEL_GAP;
        float removeW = getRemoveButtonWidth();
        return mouseX >= removeX && mouseX <= removeX + removeW
                && mouseY >= panelY && mouseY <= panelY + BTN_HEIGHT;
    }

    public boolean handleClick(double mouseX, double mouseY) {
        if (!isVisible()) return false;

        if (isHoveringAdd(mouseX, mouseY)) {
            if (FriendManager.add(playerName)) {
                dev.aethel.config.ChatUtils.send("§a" + playerName + " §fдобавлен в друзья");
            }
            return true;
        }

        if (isHoveringRemove(mouseX, mouseY)) {
            if (FriendManager.remove(playerName)) {
                dev.aethel.config.ChatUtils.send("§c" + playerName + " §fудален из друзей");
            }
            return true;
        }

        return false;
    }

    public void render(DrawContext context) {
        if (!isVisible() || playerName == null) return;

        float anim = (float) showAnim.getValue();
        if (anim < 0.05f) return;

        boolean isFriend = FriendManager.isFriend(playerName);

        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();
        int aInt = MathHelper.clamp((int) (255f * anim), 0, 255);

        Matrix4f m2 = context.getMatrices().peek().getPositionMatrix();

        if (isFriend) {
            float removeW = getRemoveButtonWidth();
            float removeX = panelX;

            int[] glow = ColorProvider.getOrbitalRect(t1, t2, 800.0, aInt);
            int tmp = glow[1]; glow[1] = glow[3]; glow[3] = tmp;
            DrawUtil.drawRound(removeX - 0.5f, panelY - 0.5f, removeW + 1f, BTN_HEIGHT + 1f, BTN_RADIUS, glow[0], glow[1], glow[2], glow[3]);

            int bgColor = ColorProvider.rgba(180, 40, 40, Math.min(160, aInt));
            DrawUtil.drawRound(removeX, panelY, removeW, BTN_HEIGHT, BTN_RADIUS, bgColor);

            float textX = removeX + (removeW - Fonts.SFBOLD.get().getWidth("Удалить", BTN_TEXT_SIZE)) / 2f;
            float textY = panelY + (BTN_HEIGHT - BTN_TEXT_SIZE) / 2f + 0.5f;
            DrawUtil.drawText(Fonts.SFBOLD.get(), "Удалить", textX, textY, ColorProvider.rgba(255, 255, 255, aInt), BTN_TEXT_SIZE);
        } else {
            float addW = getAddButtonWidth();

            int[] glow = ColorProvider.getOrbitalRect(t1, t2, 800.0, aInt);
            int tmp = glow[1]; glow[1] = glow[3]; glow[3] = tmp;
            DrawUtil.drawRound(panelX - 0.5f, panelY - 0.5f, addW + 1f, BTN_HEIGHT + 1f, BTN_RADIUS, glow[0], glow[1], glow[2], glow[3]);

            int bgColor = ColorProvider.rgba(
                    ((t1 >> 16) & 0xFF) >> 2,
                    ((t1 >> 8) & 0xFF) >> 2,
                    (t1 & 0xFF) >> 2,
                    Math.min(160, aInt)
            );
            DrawUtil.drawRound(panelX, panelY, addW, BTN_HEIGHT, BTN_RADIUS, bgColor);

            float textX = panelX + (addW - Fonts.SFBOLD.get().getWidth("В друзья", BTN_TEXT_SIZE)) / 2f;
            float textY = panelY + (BTN_HEIGHT - BTN_TEXT_SIZE) / 2f + 0.5f;
            DrawUtil.drawText(Fonts.SFBOLD.get(), "В друзья", textX, textY, ColorProvider.rgba(255, 255, 255, aInt), BTN_TEXT_SIZE);
        }
    }

    private float getAddButtonWidth() {
        return Fonts.SFBOLD.get().getWidth("В друзья", BTN_TEXT_SIZE) + BTN_PADDING_X * 2f;
    }

    private float getRemoveButtonWidth() {
        return Fonts.SFBOLD.get().getWidth("Удалить", BTN_TEXT_SIZE) + BTN_PADDING_X * 2f;
    }
}
