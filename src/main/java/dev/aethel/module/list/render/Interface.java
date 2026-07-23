package dev.aethel.module.list.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.Aethel;
import dev.aethel.event.list.EventHUD;
import dev.aethel.event.list.EventTick;
import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;
import dev.aethel.module.list.combat.Aura;
import dev.aethel.module.settings.*;
import dev.aethel.ui.hud.*;
import dev.aethel.util.base.Instance;
import dev.aethel.util.draggable.DragManager;
import dev.aethel.util.draggable.Draggable;
import dev.aethel.util.render.builders.Builder;
import dev.aethel.util.render.builders.states.QuadColorState;
import dev.aethel.util.render.builders.states.QuadRadiusState;
import dev.aethel.util.render.builders.states.SizeState;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import dev.aethel.util.render.renderers.impl.BuiltGlow;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@ModuleInformation(moduleName = "Interface", moduleCategory = ModuleCategory.RENDER, moduleDesc = "Интерфейс клиента")
public class Interface extends Module {

    private final ModeListSetting elements = new ModeListSetting("Элементы",
            new BooleanSetting("Ватермарка", true),
            new BooleanSetting("Координаты", false),
            new BooleanSetting("Активный таргет", true),
            new BooleanSetting("Бинды", true),
            new BooleanSetting("Активные модераторы", true),
            new BooleanSetting("Бафы", true),
            new BooleanSetting("Скорость", false),
            new BooleanSetting("Счетчик тотемов", false),
            new BooleanSetting("Уведомления", true),
            new BooleanSetting("Серверные бинды", false),
            new BooleanSetting("Кулдауны", false)
    );
    private final SliderSetting backgroundIntensity =
            new SliderSetting("Интенсивность фона", 0.15f, 0.05f, 1.0f, 0.01f);


    private final Draggable watermarkDrag = DragManager.installDrag(this, "Watermark", 4, 4);
    private final Draggable keyBindsDrag = DragManager.installDrag(this, "HotKeys", 100, 50);
    private final Draggable staffListDrag = DragManager.installDrag(this, "StaffList", 200, 50);
    private final Draggable potionsDrag = DragManager.installDrag(this, "Potions", 300, 50);
    private final Draggable targetHUDDrag = DragManager.installDrag(this, "TargetHUD", 130, 130);
    private final Draggable totemCounterDrag = DragManager.installDrag(this, "TotemCounter", 200, 200);
    private final Draggable notificationsDrag = DragManager.installDrag(this, "Notifications", 10, 10);
    private final Draggable serverBindingsDrag = DragManager.installDrag(this, "ServerBindings", 400, 50);
    private final Draggable cooldownsDrag = DragManager.installDrag(this, "Cooldowns", 500, 50);

    private final WatermarkRenderer watermarkRenderer;
    private final KeyBindsRenderer keyBindsRenderer;
    private final StaffListRenderer staffListRenderer;
    private final PotionsRenderer potionsRenderer;
    private final TargetHUDRenderer targetHUDRenderer;
    private final TotemCounterRenderer totemCounterRenderer;
    private final SpeedRenderer speedRenderer;
    private final ServerBindingsRenderer serverBindingsRenderer;
    private final CooldownsRenderer cooldownsRenderer;

    private final List<Staff> staffPlayers = new ArrayList<>();
    final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    final Pattern prefixMatches = Pattern.compile(".*(ꔷ|ꔳ|ꔩ|ꔥ|ꔡ|ꔗ|ꔓ|\\bmod\\b|\\badm\\b|\\bhelp\\b|\\bwne\\b|модер|хелп|помощ|админ|владел|отриц|\\btaf\\b|\\bcurat\\b|куратор|\\bdev\\b|разраб|\\bsupp\\b|саппорт|\\byt\\b|\\[yt\\]|ютуб|стажер|сотрудник).*");

    public Interface() {
        this.watermarkRenderer = new WatermarkRenderer(this);
        this.keyBindsRenderer = new KeyBindsRenderer(this);
        this.staffListRenderer = new StaffListRenderer(this);
        this.potionsRenderer = new PotionsRenderer(this);
        this.targetHUDRenderer = new TargetHUDRenderer(this);
        this.totemCounterRenderer = new TotemCounterRenderer(this);
        this.speedRenderer = new SpeedRenderer();
        this.serverBindingsRenderer = new ServerBindingsRenderer(this);
        this.cooldownsRenderer = new CooldownsRenderer(this);
    }

    public Draggable getWatermarkDrag() { return watermarkDrag; }
    public Draggable getKeyBindsDrag() { return keyBindsDrag; }
    public Draggable getStaffListDrag() { return staffListDrag; }
    public Draggable getPotionsDrag() { return potionsDrag; }
    public Draggable getTargetHUDDrag() { return targetHUDDrag; }
    public Draggable getTotemCounterDrag() { return totemCounterDrag; }
    public Draggable getNotificationsDrag() { return notificationsDrag; }
    public Draggable getServerBindingsDrag() { return serverBindingsDrag; }
    public Draggable getCooldownsDrag() { return cooldownsDrag; }
    public List<Staff> getStaffPlayers() { return staffPlayers; }

    public void drawBackground(float x, float y, float w, float h, float radius, int alpha) {
        if (elements.isEnabled("Блюр фона")) {
            int color = ColorProvider.rgba(25, 25, 25, (int) (alpha * backgroundIntensity.getFloatValue()));

            DrawUtil.drawRoundBlur(x, y, w, h, radius, ColorProvider.rgba(200, 200, 200, alpha), 12);
            DrawUtil.drawRound(x, y, w, h, radius, color);

        } else {
            int color = ColorProvider.rgba(25, 25, 25, (int) (alpha * backgroundIntensity.getFloatValue()));
            DrawUtil.drawRound(x, y, w, h, radius, color);
        }

        if (elements.isEnabled("Задний фон от темы")) {
            DrawUtil.drawRound(x, y, w, h, radius, getThemeTint(alpha));
        }
    }

    public boolean isWidgetEnabled(String name) {
        return elements.isEnabled(name);
    }

    public int getThemeTint(int alpha) {
        int themeColor = ColorProvider.getThemeColor();
        return ColorProvider.setAlpha(themeColor, (int) (100 * (alpha / 255f) * backgroundIntensity.getFloatValue()));
    }

    public void drawGlow(Matrix4f matrix, float x, float y, float w, float h, float radius, float anim) {
        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();

        float glow = 7.0f;
        int a = (int) (110 * anim);

        int[] c = ColorProvider.getOrbitalRect(t1, t2, 300.0, a);

        Builder.glow()
                .size(new SizeState(w + glow * 2f - 6, h + glow * 2f - 6))
                .radius(new QuadRadiusState(radius))
                .color(new QuadColorState(c[0], c[1], c[2], c[3]))
                .glowRadius(glow)
                .softness(0f)
                .intensity(2.0f)
                .additive(true)
                .build()
                .render(matrix, x - glow + 3, y - glow + 3, 0);
    }
    

    @Subscribe
    public void onEventHUD(EventHUD e) {
        if (mc.player == null || mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;

        if (elements.isEnabled("Уведомления")) {
            NotificationRenderer.render(e.getDrawContext());
        }

        if (elements.isEnabled("Счетчик тотемов")) {
            totemCounterRenderer.render(e.getDrawContext());
        }
        if (elements.isEnabled("Ватермарка")) {
            watermarkRenderer.render(e.getDrawContext());
        }
        if (elements.isEnabled("Активный таргет")) {
            targetHUDRenderer.render(e.getDrawContext());
        }
        if (elements.isEnabled("Бинды")) {
            keyBindsRenderer.render(e.getDrawContext());
        }
        if (elements.isEnabled("Активные модераторы")) {
            staffListRenderer.render(e.getDrawContext());
        }
        if (elements.isEnabled("Бафы")) {
            potionsRenderer.render(e.getDrawContext());
        }
        if (elements.isEnabled("Скорость")) {
            speedRenderer.render(e.getDrawContext());
        }
        if (elements.isEnabled("Серверные бинды")) {
            serverBindingsRenderer.render(e.getDrawContext());
        }
        if (elements.isEnabled("Кулдауны")) {
            cooldownsRenderer.render(e.getDrawContext());
        }
    }

    @Subscribe
    private void onUpdate(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        if (elements.isEnabled("Активные модераторы")) {
            updateStaff();
        }
        if (elements.isEnabled("Бафы")) {
            potionsRenderer.update();
        }
    }

    public void updateStaff() {
        for (Staff staff : staffPlayers) {
            staff.isOnServer = false;
        }

        for (var playerListEntry : mc.getNetworkHandler().getPlayerList()) {
            String name = playerListEntry.getProfile().getName().replaceAll("[\\[\\]]", "");
            var info = mc.getNetworkHandler().getPlayerListEntry(name);
            boolean vanish = info == null;
            boolean isGM3 = info != null && info.getGameMode() == net.minecraft.world.GameMode.SPECTATOR;

            boolean matchesPrefix = prefixMatches.matcher(playerListEntry.getDisplayName() != null ? playerListEntry.getDisplayName().getString().toLowerCase(Locale.ROOT) : "").matches();
            boolean isValidName = namePattern.matcher(name).matches();
            boolean notSelf = !name.equals(mc.player.getName().getString());

            if ((isValidName && notSelf && matchesPrefix) || (isValidName && notSelf && vanish)) {
                var existingStaff = staffPlayers.stream().filter(s -> s.name.equals(name)).findFirst();

                Status status = vanish ? Status.VANISHED : (isGM3 ? Status.VANISHED : Status.NONE);

                if (existingStaff.isPresent()) {
                    Staff s = existingStaff.get();
                    s.isOnServer = true;
                    s.status = status;
                } else {
                    Text originalPrefix = playerListEntry.getDisplayName();
                    Text prefix = originalPrefix;
                    if (prefix != null) {
                        String fullString = prefix.getString();
                        int nickIndex = fullString.indexOf(name);
                        if (nickIndex != -1) {
                            Text newText = extractPrefixBeforeName(prefix, nickIndex);
                            prefix = newText;
                        }
                    }
                    Staff staff = new Staff(prefix == null ? Text.of(name) : prefix, name, vanish || isGM3, status);
                    staff.isOnServer = true;
                    staffPlayers.add(staff);
                }
            }
        }

        staffPlayers.removeIf(staff -> !staff.isOnServer && staff.animation.getValue() == 0);
    }

    private Text extractPrefixBeforeName(Text prefix, int nickIndex) {
        net.minecraft.text.MutableText newText = Text.empty();
        int currentLength = 0;

        net.minecraft.text.MutableText baseCopy = prefix.copy();
        baseCopy.getSiblings().clear();
        String mainContent = baseCopy.getString();

        if (!mainContent.isEmpty() && currentLength < nickIndex) {
            int takeLength = Math.min(mainContent.length(), nickIndex - currentLength);
            newText.append(Text.literal(mainContent.substring(0, takeLength)).setStyle(prefix.getStyle()));
            currentLength += takeLength;
        }

        for (Text sibling : prefix.getSiblings()) {
            if (currentLength >= nickIndex) break;
            net.minecraft.text.MutableText siblingCopy = sibling.copy();
            siblingCopy.getSiblings().clear();
            String siblingContent = siblingCopy.getString();

            int takeLength = Math.min(siblingContent.length(), nickIndex - currentLength);
            if (takeLength > 0) {
                newText.append(Text.literal(siblingContent.substring(0, takeLength)).setStyle(sibling.getStyle()));
                currentLength += takeLength;
            }
        }

        return newText;
    }

    public enum Status {
        NONE("", -1),
        VANISHED("SPEC", ColorProvider.rgba(229, 0, 63, 255));

        public final String string;
        public final int color;

        Status(String string, int color) {
            this.string = string;
            this.color = color;
        }
    }

    public static class Staff {
        public Text prefix;
        public String name;
        public boolean isSpec;
        public Status status;
        public boolean isOnServer;
        public Animation animation;
        public long mills;

        public Staff(Text prefix, String name, boolean isSpec, Status status) {
            this.prefix = prefix;
            this.name = name;
            this.isSpec = isSpec;
            this.status = status;
            animation = new Animation(Easing.EXPO_OUT, 233);
            mills = System.currentTimeMillis();
        }
    }
}
