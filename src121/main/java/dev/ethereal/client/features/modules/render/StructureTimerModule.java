package dev.ethereal.client.features.modules.render;

import dev.ethereal.api.event.orbit.EventHandler;
import dev.ethereal.api.event.events.client.PacketEvent;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;
import dev.ethereal.api.module.setting.ModeSetting;
import dev.ethereal.api.utils.color.ColorUtil;
import dev.ethereal.api.utils.math.ProjectionUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.fonts.Fonts;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleRegister(name = "StructureTimer", category = Category.RENDER)
public class StructureTimerModule extends Module {
    @Getter private static final StructureTimerModule instance = new StructureTimerModule();

    @AllArgsConstructor
    public enum Mode implements ModeSetting.NamedChoice {
        FunTime("ФанТайм"),
        SpookyTime("СпукиТайм"),
        Holyworld("ХолиВорлд");
        private final String name;
        @Override
        public String getName() { return name; }
    }

    private static final String TRAPKA_SOUND = "block.piston.extend";
    private static final String PLAST_SOUND = "block.anvil.place";
    private static final String DRAGON_SOUND = "entity.ender_dragon.growl";

    private static final long TRAPKA_DURATION = 15000L;
    private static final long PLAST_DURATION = 20000L;
    private static final long DRAGON_TRAP_DURATION = 30000L;
    private static final long DRAGON_PLAST_DURATION = 20000L;

    private static final float TAG_FADE_IN_PER_SEC = 6.2f;
    private static final float TAG_FADE_OUT_PER_SEC = 4.0f;
    private static final long TAG_FADE_MAX_STEP_MS = 75L;

    private static final Color OWNER_TEXT_COLOR = new Color(240, 244, 255, 255);
    private static final Color TIMER_TEXT_COLOR = new Color(250, 252, 255, 245);

    private static final float NAME_FONT_SIZE = 7.3f;
    private static final float TIMER_FONT_SIZE = 6.8f;

    enum TrapType {
        TRAPKA("Трапка"),
        PLAST("Пласт"),
        DRAGON_TRAP("Драконья трапка"),
        DRAGON_PLAST("Драконий пласт");

        private final String displayName;
        TrapType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }

        public ItemStack getIcon() {
            return switch (this) {
                case TRAPKA, DRAGON_TRAP -> new ItemStack(Items.NETHERITE_SCRAP);
                case PLAST, DRAGON_PLAST -> new ItemStack(Items.DRIED_KELP);
            };
        }
    }

    private static class TrapEntry {
        final Vec3d position;
        final long creationTime;
        TrapType type;
        long duration;
        boolean typeDetermined;
        boolean needsNetheriteCheck;
        Item item;

        float alpha = 0f;
        boolean alive = true;
        long lastUpdateMs = System.currentTimeMillis();

        TrapEntry(Vec3d position, TrapType type, long duration) {
            this(position, type, duration, null);
        }

        TrapEntry(Vec3d position, TrapType type, long duration, Item item) {
            this.position = position;
            this.creationTime = System.currentTimeMillis();
            this.type = type;
            this.duration = duration;
            this.typeDetermined = true;
            this.item = item;
        }
    }

    private final ModeSetting mode = new ModeSetting("Режим")
            .values(Mode.values())
            .value(Mode.FunTime);

    private final List<TrapEntry> entries = new CopyOnWriteArrayList<>();
    private int prevNetheriteScrapCount = -1;

    public StructureTimerModule() {
        addSettings(mode);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        entries.clear();
        prevNetheriteScrapCount = -1;
    }

    @Override
    public void onDisable() {
        entries.clear();
        prevNetheriteScrapCount = -1;
        super.onDisable();
    }

    public void addTrapTimer(Vec3d position) {
        if (!isEnabled()) return;
        if (mode.is(Mode.SpookyTime)) return;
        entries.add(new TrapEntry(position, TrapType.TRAPKA, TRAPKA_DURATION));
    }

    public void addTrapTimer(Vec3d position, Item item) {
        if (!isEnabled()) return;
        entries.add(new TrapEntry(position, TrapType.TRAPKA, TRAPKA_DURATION, item));
    }

    public void addPlastTimer(Vec3d position) {
        if (!isEnabled()) return;
        if (mode.is(Mode.SpookyTime)) return;
        entries.add(new TrapEntry(position, TrapType.PLAST, PLAST_DURATION));
    }

    public void addPlastTimer(Vec3d position, Item item) {
        if (!isEnabled()) return;
        entries.add(new TrapEntry(position, TrapType.PLAST, PLAST_DURATION, item));
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null) return;

        int current = countNetheriteScrap();
        if (prevNetheriteScrapCount == -1) {
            prevNetheriteScrapCount = current;
            return;
        }
        if (prevNetheriteScrapCount > 0 && current < prevNetheriteScrapCount) {
            entries.add(new TrapEntry(mc.player.getPos(), TrapType.TRAPKA, TRAPKA_DURATION));
        }
        prevNetheriteScrapCount = current;
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (!event.isReceive()) return;
        if (!(event.packet() instanceof PlaySoundS2CPacket packet)) return;
        if (mc.world == null || mc.player == null) return;

        String soundId = packet.getSound().value().id().getPath();
        float pitch = packet.getPitch();
        float volume = packet.getVolume();
        Vec3d pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());

        if (mode.is(Mode.FunTime)) {
            if (soundId.equals(PLAST_SOUND)
                    && Math.abs(pitch - 1.1f) < 0.01f
                    && Math.abs(volume - 0.7f) < 0.01f) {
                entries.add(new TrapEntry(pos, TrapType.PLAST, PLAST_DURATION));
                return;
            }
            if (soundId.equals(TRAPKA_SOUND)
                    && Math.abs(pitch - 0.5f) < 0.01f
                    && Math.abs(volume - 0.7f) < 0.01f) {
                entries.add(new TrapEntry(pos, TrapType.TRAPKA, TRAPKA_DURATION));
                return;
            }
            if (soundId.equals(DRAGON_SOUND)
                    && Math.abs(pitch - 1.0f) < 0.01f
                    && Math.abs(volume - 0.2f) < 0.01f) {
                TrapEntry entry = new TrapEntry(pos, TrapType.DRAGON_PLAST, DRAGON_PLAST_DURATION);
                entry.typeDetermined = false;
                entries.add(entry);
                determineDragonType(entry);
                return;
            }
        }

        if (mode.is(Mode.SpookyTime)) {
            if (soundId.equals(TRAPKA_SOUND)
                    && Math.abs(pitch - 0.5f) < 0.01f
                    && Math.abs(volume - 0.5f) < 0.01f) {
                entries.add(new TrapEntry(pos, TrapType.TRAPKA, 15000L));
                return;
            }
            if (soundId.equals(PLAST_SOUND)
                    && Math.abs(pitch - 0.5f) < 0.01f
                    && Math.abs(volume - 0.5f) < 0.01f) {
                TrapEntry entry = new TrapEntry(pos, TrapType.PLAST, 20000L);
                entry.needsNetheriteCheck = true;
                entry.typeDetermined = false;
                entries.add(entry);
                return;
            }
            if (soundId.equals(DRAGON_SOUND)
                    && Math.abs(pitch - 0.7f) < 0.01f
                    && Math.abs(volume - 0.5f) < 0.01f) {
                TrapEntry entry = new TrapEntry(pos, TrapType.DRAGON_PLAST, DRAGON_PLAST_DURATION);
                entry.typeDetermined = false;
                entries.add(entry);
                determineDragonType(entry);
            }
        }
    }

    @EventHandler
    public void onRender2D(Render2DEvent event) {
        if (mc.world == null || mc.player == null || entries.isEmpty()) return;

        long now = System.currentTimeMillis();
        DrawContext context = event.context();
        MatrixStack matrices = event.matrixStack();

        for (TrapEntry entry : entries) {
            if (!entry.typeDetermined) {
                if (entry.needsNetheriteCheck && now - entry.creationTime >= 500L) {
                    if (hasNetheriteBlock(entry.position, 5, 9, 5)) {
                        entry.type = TrapType.DRAGON_PLAST;
                        entry.duration = 30000L;
                    } else {
                        entry.type = TrapType.PLAST;
                        entry.duration = 20000L;
                    }
                    entry.needsNetheriteCheck = false;
                    entry.typeDetermined = true;
                }
                if (entry.type == TrapType.DRAGON_PLAST && now - entry.creationTime > 50L) {
                    determineDragonType(entry);
                }
            }
        }

        for (TrapEntry entry : entries) {
            long elapsedMs = now - entry.lastUpdateMs;
            if (elapsedMs <= 0L) elapsedMs = 16L;
            elapsedMs = Math.min(elapsedMs, TAG_FADE_MAX_STEP_MS);
            entry.lastUpdateMs = now;

            float deltaSeconds = elapsedMs / 1000f;

            boolean expired = entry.typeDetermined && now - entry.creationTime > entry.duration;
            boolean undetermined = !entry.typeDetermined && now - entry.creationTime > 1000L;

            if (expired || undetermined) entry.alive = false;

            float targetAlpha = entry.alive ? 1f : 0f;
            float speedPerSec = targetAlpha > entry.alpha ? TAG_FADE_IN_PER_SEC : TAG_FADE_OUT_PER_SEC;
            entry.alpha = approach(entry.alpha, targetAlpha, speedPerSec * deltaSeconds);
        }

        entries.removeIf(entry -> !entry.alive && entry.alpha <= 0.01f);

        for (TrapEntry entry : entries) {
            if (entry.alpha <= 0.01f) continue;
            if (!entry.typeDetermined && now - entry.creationTime < 50L) continue;

            long remaining = entry.duration - (now - entry.creationTime);
            if (remaining <= 0 && entry.alive) continue;
            if (remaining < 0) remaining = 0;

            Vec3d renderPos = new Vec3d(entry.position.x, entry.position.y + 1.1, entry.position.z);
            Vector2f screen = ProjectionUtil.project(renderPos);
            if (screen.x == Float.MAX_VALUE && screen.y == Float.MAX_VALUE) continue;

            renderTrapTag(context, matrices, screen.x, screen.y, entry, remaining);
        }
    }

    private void renderTrapTag(DrawContext context, MatrixStack matrices, float screenX, float screenY, TrapEntry entry, long remainingMs) {
        float fade = entry.alpha;
        String itemName = entry.type.getDisplayName();
        String timerText = formatTimer(remainingMs);

        float panelHeight = 15f;
        float panelRadius = 4f;
        float panelPad = 3f;
        float contentGap = 3f;
        float iconSize = 10.5f;
        float timerPad = 2f;

        float nameWidth = Fonts.PS_MEDIUM.getWidth(itemName, NAME_FONT_SIZE);
        float etaWidth = Fonts.PS_REGULAR.getWidth(timerText, TIMER_FONT_SIZE);
        float timerBoxWidth = etaWidth + timerPad * 2f;
        float panelWidth = panelPad + iconSize + contentGap + nameWidth + contentGap + timerBoxWidth + panelPad;
        panelWidth = Math.max(panelWidth, 68f);

        float x = screenX - panelWidth * 0.5f;
        float y = screenY - 17f;

        Color accent = getLabelColor(entry.type);
        Color timerBg = alphaMul(ColorUtil.setAlpha(accent, 95), fade);
        Color timerTextColor = alphaMul(TIMER_TEXT_COLOR, fade);

        Color panelBg = new Color(13, 11, 22, (int) (180 * fade));
        RenderUtil.BLUR_RECT.draw(matrices, x, y, panelWidth, panelHeight, panelRadius, panelBg);

        float iconX = x + panelPad;
        float iconY = y + (panelHeight - iconSize) * 0.5f;
        ItemStack icon = (entry.item != null) ? new ItemStack(entry.item) : entry.type.getIcon();
        renderItemIcon(context, matrices, iconX, iconY, iconSize, icon);

        float nameX = iconX + iconSize + contentGap;
        float nameY = y + (panelHeight - Fonts.PS_MEDIUM.getHeight(NAME_FONT_SIZE)) * 0.5f - 0.3f;
        Fonts.PS_MEDIUM.drawText(matrices, itemName, nameX, nameY, NAME_FONT_SIZE, alphaMul(OWNER_TEXT_COLOR, fade));

        float timerX = x + panelWidth - panelPad - timerBoxWidth;
        float timerY = y + (panelHeight - 10f) * 0.5f;
        RenderUtil.RECT.draw(matrices, timerX, timerY, timerBoxWidth, 10f, 2f, timerBg);
        float etaY = y + (panelHeight - Fonts.PS_REGULAR.getHeight(TIMER_FONT_SIZE)) * 0.5f;
        Fonts.PS_REGULAR.drawText(matrices, timerText, timerX + (timerBoxWidth - etaWidth) * 0.5f, etaY, TIMER_FONT_SIZE, timerTextColor);
    }

    private void renderItemIcon(DrawContext context, MatrixStack matrices, float x, float y, float size, ItemStack icon) {
        matrices.push();
        float scale = size / 16f;
        matrices.translate(x, y, 0);
        matrices.scale(scale, scale, 1f);
        context.drawItem(icon, 0, 0);
        matrices.pop();
    }

    private Color getLabelColor(TrapType type) {
        return switch (type) {
            case TRAPKA -> new Color(255, 100, 100, 255);
            case PLAST -> new Color(100, 200, 255, 255);
            case DRAGON_TRAP -> new Color(255, 160, 50, 255);
            case DRAGON_PLAST -> new Color(180, 100, 255, 255);
        };
    }

    private String formatTimer(long remainingMs) {
        if (remainingMs <= 0) return "0.0s";
        float seconds = remainingMs / 1000.0f;
        if (seconds >= 60f) {
            int minutes = (int) (seconds / 60f);
            int secs = (int) (seconds % 60f);
            return String.format("%d:%02d", minutes, secs);
        }
        return String.format("%.1fs", seconds);
    }

    private Color alphaMul(Color color, float factor) {
        int alpha = (int) Math.max(0, Math.min(255, Math.round(color.getAlpha() * factor)));
        return ColorUtil.setAlpha(color, alpha);
    }

    private float approach(float current, float target, float speed) {
        if (current < target) return Math.min(target, current + speed);
        if (current > target) return Math.max(target, current - speed);
        return current;
    }

    private void determineDragonType(TrapEntry entry) {
        if (entry.typeDetermined || mc.world == null) return;
        if (hasRespawnAnchor(entry.position, 6)) {
            entry.type = TrapType.DRAGON_TRAP;
            entry.duration = DRAGON_TRAP_DURATION;
            entry.typeDetermined = true;
        }
    }

    private boolean hasRespawnAnchor(Vec3d pos, int radius) {
        if (mc.world == null) return false;
        int cx = (int) pos.x, cy = (int) pos.y, cz = (int) pos.z;
        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++)
                for (int y = -3; y <= 3; y++) {
                    try {
                        if (mc.world.getBlockState(new BlockPos(cx + x, cy + y, cz + z))
                                .isOf(Blocks.RESPAWN_ANCHOR)) return true;
                    } catch (Exception ignored) {}
                }
        return false;
    }

    private boolean hasNetheriteBlock(Vec3d pos, int rx, int ry, int rz) {
        if (mc.world == null) return false;
        int cx = (int) pos.x, cy = (int) pos.y, cz = (int) pos.z;
        for (int x = -rx; x <= rx; x++)
            for (int z = -rz; z <= rz; z++)
                for (int y = -ry; y <= ry; y++) {
                    try {
                        if (mc.world.getBlockState(new BlockPos(cx + x, cy + y, cz + z))
                                .isOf(Blocks.NETHERITE_BLOCK)) return true;
                    } catch (Exception ignored) {}
                }
        return false;
    }

    private int countNetheriteScrap() {
        if (mc.player == null) return 0;
        int count = 0;
        for (int i = 0; i < 45; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.NETHERITE_SCRAP)
                count += stack.getCount();
        }
        return count;
    }
}
