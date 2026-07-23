package antileak.base.client.modules.impl.render.base.implement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.world.GameMode;
import antileak.base.elysium;
import antileak.base.api.events.implement.EventRender;
import antileak.base.api.utils.animation.AnimationUtils;
import antileak.base.api.utils.animation.Easings;
import antileak.base.api.utils.color.ColorUtils;
import antileak.base.api.utils.draggable.Draggable;
import antileak.base.api.utils.render.RenderUtils;
import antileak.base.api.utils.render.font.ReplaceSymbols;
import antileak.base.api.utils.render.fonts.msdf.Font;
import antileak.base.api.utils.render.fonts.msdf.Fonts;
import antileak.base.api.utils.scissor.ScissorUtils;
import antileak.base.client.modules.impl.render.EntityESP;
import antileak.base.client.modules.impl.render.base.InterfaceProcessing;

import java.util.*;
import java.util.regex.Pattern;

public class StaffList extends InterfaceProcessing {

    private static final int STATUS_VANISH_COLOR = 0xFFFF465A;
    private static final int STATUS_GM3_COLOR    = 0xFFFFDC46;
    private static final int STATUS_ONLINE_COLOR = 0xFF64FF78;
    private static final int STATUS_NEAR_COLOR   = 0xFF64CFFF;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<String, StaffData>  staffDataCache  = new LinkedHashMap<>();
    private final Map<String, Float>      staffAnimations = new HashMap<>();
    private final Set<String>             activeStaff     = new HashSet<>();

    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    private final Pattern botPattern  = Pattern.compile("^\\d+$");

    private final Set<String>    validStaffPrefixes = new HashSet<>();
    private final AnimationUtils widthAnimation     = new AnimationUtils(60, 10.5f, Easings.QUAD_OUT);
    private float staffAnimatedHeight = 18f;
    private long  lastStaffUpdate     = 0;
    private final List<String>   visiblePlayers  = new ArrayList<>();
    private final Set<String>    animationScratch = new HashSet<>();

    private Font font12;
    private Font font14;
    private Font iconFont;
    private antileak.base.api.utils.render.fonts.ttf.MCFontRenderer lox(int size) {
        return antileak.base.api.utils.render.fonts.ttf.Fonts.getFont("lox.ttf", size);
    }
    public StaffList(Draggable draggable) {
        super(draggable);
        validStaffPrefixes.addAll(Arrays.asList(
                "supp", "ꜱupp", "mod", "der", "adm", "wne", "мод", "помо", "адм",
                "владе", "отри", "таф", "taf", "curat", "курато", "dev", "раз",
                "сапп", "yt", "ютуб", "стажер", "сотрудник"
        ));
    }


    private static class PrefixSegment {
        final String text;
        final int    color;
        float width12;
        PrefixSegment(String text, int color) { this.text = text; this.color = color; }
    }

    private static class StaffData {
        String             status;
        List<PrefixSegment> segments;
        float prefixWidth12;
        float nameWidth12;
        StaffData(String status) { this.status = status; this.segments = new ArrayList<>(); }
    }


    private void initFonts() {
        if (font12 == null) {
            font12   = Fonts.getFont("suisse", 12);
            font14   = Fonts.getFont("suisse", 14);
            iconFont = Fonts.getFont("icon", 13);
        }
    }


    private boolean isBot(String name) {
        return botPattern.matcher(name).matches();
    }


    @Override
    public void onRender(EventRender.Default eventRender) {
        if (mc.player == null || mc.world == null) return;
        initFonts();
        long now = System.currentTimeMillis();
        if (now - lastStaffUpdate > 500) { updateStaffCache(); lastStaffUpdate = now; }
        updateAnimations();
        renderDefaultStyle(eventRender);
        super.onRender(eventRender);
    }


    private boolean matchesStaffPrefix(String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String p : validStaffPrefixes) if (lower.contains(p)) return true;
        return false;
    }

    private List<PrefixSegment> parsePrefix(Text prefix) {
        List<PrefixSegment> segments = new ArrayList<>();
        prefix.visit((style, string) -> {
            if (string != null && !string.isEmpty())
                appendPrefixSegments(segments, string,
                        style.getColor() != null ? style.getColor().getRgb() : 0xFFFFFF);
            return Optional.empty();
        }, Style.EMPTY);
        return segments;
    }

    private void appendPrefixSegments(List<PrefixSegment> segments, String text, int baseColor) {
        int currentColor = baseColor;
        StringBuilder chunk = new StringBuilder();
        int chunkColor = currentColor;
        for (int offset = 0; offset < text.length(); ) {
            int codePoint = text.codePointAt(offset);
            int charCount = Character.charCount(codePoint);
            if (codePoint == '\u00A7' && offset + charCount < text.length()) {
                flushPrefixSegment(segments, chunk, chunkColor);
                char code = Character.toLowerCase(text.charAt(offset + charCount));
                Integer mc = sectionColorToRgb(code);
                currentColor = mc != null ? mc : code == 'r' ? baseColor : currentColor;
                chunkColor   = currentColor;
                offset += charCount + 1;
                continue;
            }
            String replacement = ReplaceSymbols.replaceCodePoint(codePoint);
            if (replacement != null) {
                flushPrefixSegment(segments, chunk, chunkColor);
                int total = Math.max(1, replacement.length());
                for (int i = 0; i < replacement.length(); i++) {
                    int rc = ReplaceSymbols.getGradientColorForReplacement(codePoint, i, total, 1.0f, currentColor);
                    if (chunk.length() > 0 && chunkColor != rc) flushPrefixSegment(segments, chunk, chunkColor);
                    chunkColor = rc;
                    chunk.append(replacement.charAt(i));
                }
                offset += charCount;
                continue;
            }
            if (chunk.length() > 0 && chunkColor != currentColor) flushPrefixSegment(segments, chunk, chunkColor);
            chunkColor = currentColor;
            chunk.appendCodePoint(codePoint);
            offset += charCount;
        }
        flushPrefixSegment(segments, chunk, chunkColor);
    }

    private void flushPrefixSegment(List<PrefixSegment> segments, StringBuilder chunk, int color) {
        if (chunk.isEmpty()) return;
        String t = chunk.toString();
        PrefixSegment seg = new PrefixSegment(t, color);
        seg.width12 = font12.getWidth(t);
        segments.add(seg);
        chunk.setLength(0);
    }

    private Integer sectionColorToRgb(char code) {
        return switch (code) {
            case '0' -> 0x000000; case '1' -> 0x0000AA; case '2' -> 0x00AA00;
            case '3' -> 0x00AAAA; case '4' -> 0xAA0000; case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00; case '7' -> 0xAAAAAA; case '8' -> 0x555555;
            case '9' -> 0x5555FF; case 'a' -> 0x55FF55; case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555; case 'd' -> 0xFF55FF; case 'e' -> 0xFFFF55;
            case 'f' -> 0xFFFFFF; default -> null;
        };
    }


    private List<PrefixSegment> getDonateSegmentsViaESP(String name) {
        if (EntityESP.INSTANCE == null || mc.world == null) return Collections.emptyList();
        PlayerEntity player = mc.world.getPlayers().stream()
                .filter(p -> p.getName().getString().equals(name))
                .findFirst().orElse(null);
        if (player == null) return Collections.emptyList();

        List<EntityESP.PublicDonateSegment> espSegments = EntityESP.INSTANCE.getDonateSegmentsForStaffList(player);
        if (espSegments == null || espSegments.isEmpty()) return Collections.emptyList();

        List<PrefixSegment> result = new ArrayList<>(espSegments.size());
        for (EntityESP.PublicDonateSegment ds : espSegments) {
            PrefixSegment seg = new PrefixSegment(ds.text(), ds.color());
            seg.width12 = font12.getWidth(ds.text());
            result.add(seg);
        }
        return result;
    }


    private void updateStaffCache() {
        activeStaff.clear();
        String selfName = mc.player.getName().getString();

        for (Team team : mc.world.getScoreboard().getTeams()) {
            Collection<String> players = team.getPlayerList();
            if (players.size() != 1) continue;
            String name = players.iterator().next();
            if (!namePattern.matcher(name).matches() || isBot(name) || name.equals(selfName)) continue;

            PlayerListEntry info    = mc.getNetworkHandler().getPlayerListEntry(name);
            boolean vanish  = info == null;
            boolean isGM3   = info != null && info.getGameMode() == GameMode.SPECTATOR;
            Text    prefixText = team.getPrefix();

            if (matchesStaffPrefix(prefixText.getString()) || vanish || isGM3
                    || elysium.INSTANCE.staffStorage.isStaff(name)) {
                activeStaff.add(name);
                boolean isNear = !vanish && !isGM3 && isPlayerNearby(name);
                String status  = vanish ? "VANISH" : isGM3 ? "GM3" : isNear ? "NEAR" : "ONLINE";

                StaffData existing = staffDataCache.computeIfAbsent(name, n -> new StaffData(status));
                existing.status = status;

                List<PrefixSegment> espSegs = getDonateSegmentsViaESP(name);
                existing.segments = espSegs.isEmpty() ? parsePrefix(prefixText) : espSegs;
                calculateWidths(existing, name);
            }
        }

        for (String staffName : elysium.INSTANCE.staffStorage.getStaffs()) {
            if (staffName.equals(selfName) || !namePattern.matcher(staffName).matches()
                    || isBot(staffName) || activeStaff.contains(staffName)) continue;
            activeStaff.add(staffName);

            PlayerListEntry info   = mc.getNetworkHandler().getPlayerListEntry(staffName);
            boolean vanish = info == null;
            boolean isGM3  = info != null && info.getGameMode() == GameMode.SPECTATOR;
            boolean isNear = !vanish && !isGM3 && isPlayerNearby(staffName);
            String  status = vanish ? "VANISH" : isGM3 ? "GM3" : isNear ? "NEAR" : "ONLINE";

            StaffData existing = staffDataCache.get(staffName);
            if (existing == null) {
                existing = new StaffData(status);
                List<PrefixSegment> espSegs = getDonateSegmentsViaESP(staffName);
                existing.segments = espSegs.isEmpty() ? new ArrayList<>() : espSegs;
                calculateWidths(existing, staffName);
                staffDataCache.put(staffName, existing);
            } else {
                existing.status = status;
                List<PrefixSegment> espSegs = getDonateSegmentsViaESP(staffName);
                if (!espSegs.isEmpty()) {
                    existing.segments = espSegs;
                    calculateWidths(existing, staffName);
                }
            }
        }
    }

    private boolean isPlayerNearby(String name) {
        if (mc.world == null) return false;
        for (var player : mc.world.getPlayers())
            if (player.getName().getString().equals(name)) return true;
        return false;
    }

    private void calculateWidths(StaffData data, String name) {
        data.prefixWidth12 = 0;
        for (PrefixSegment seg : data.segments) data.prefixWidth12 += seg.width12;
        data.nameWidth12 = font12.getWidth(name);
    }


    private void updateAnimations() {
        animationScratch.clear();
        animationScratch.addAll(staffAnimations.keySet());
        animationScratch.addAll(activeStaff);
        for (String name : animationScratch) {
            float cur = staffAnimations.getOrDefault(name, 0f);
            cur += ((activeStaff.contains(name) ? 1f : 0f) - cur) * 0.1f;
            staffAnimations.put(name, cur);
        }
        Iterator<Map.Entry<String, Float>> it = staffAnimations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Float> e = it.next();
            if (e.getValue() < 0.01f && !activeStaff.contains(e.getKey())) {
                it.remove();
                staffDataCache.remove(e.getKey());
            }
        }
    }

    private List<String> getVisiblePlayers() {
        visiblePlayers.clear();
        for (Map.Entry<String, Float> e : staffAnimations.entrySet())
            if (e.getValue() > 0.01f) visiblePlayers.add(e.getKey());
        Collections.sort(visiblePlayers);
        return visiblePlayers;
    }


    private int getStatusColor(String status) {
        return switch (status) {
            case "VANISH" -> STATUS_VANISH_COLOR;
            case "GM3"    -> STATUS_GM3_COLOR;
            case "NEAR"   -> STATUS_NEAR_COLOR;
            default       -> STATUS_ONLINE_COLOR;
        };
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "VANISH" -> "[Vanish]";
            case "GM3"    -> "[GM3]";
            case "NEAR"   -> "[Near]";
            default       -> "[Online]";
        };
    }


    private void renderDefaultStyle(EventRender.Default eventRender) {
        float x = draggable.getX(), y = draggable.getY();
        MatrixStack matrices = eventRender.getContext().getMatrices();
        int colorTheme;
        if (!elysium.INSTANCE.themeStorage.getThemes().getTheme().getName().equals("Rainbow")) {
            colorTheme = elysium.INSTANCE.themeStorage.getThemes().getTheme().color[0];
        } else {
            colorTheme = ColorUtils.getThemeColor();
        }

        List<String> visible = getVisiblePlayers();
        float maxWidth    = 60f;
        float headerHeight = 16f;
        float itemHeight   = 12f;
        float padding      = 5f;

        for (String name : visible) {
            StaffData data = staffDataCache.get(name);
            if (data != null) {
                String statusLabel = getStatusLabel(data.status);
                float  statusW     = font12.getWidth(statusLabel);
                float  totalW      = padding + data.prefixWidth12 + data.nameWidth12 + 4f + statusW + padding;
                if (totalW > maxWidth) maxWidth = totalW;
            }
        }

        widthAnimation.update(maxWidth);
        float width = widthAnimation.getValue();
        float contentHeight = 0f;
        for (String name : visible) contentHeight += itemHeight * staffAnimations.getOrDefault(name, 0f);
        float targetHeight = visible.isEmpty() ? headerHeight : headerHeight + contentHeight + 2;
        staffAnimatedHeight += (targetHeight - staffAnimatedHeight) * 0.12f;
        float height = staffAnimatedHeight;

        RenderUtils.drawDefaultHudElementRects(matrices, x, y, width, height, colorTheme, isUnusualRectType());
        font14.draw(matrices, "Staffs", x + 5, y + 6.5f, -1);
        lox(15).drawString("f", x + width - 13f, y + 7.5f, colorTheme);

        float offsetY = 18;
        ScissorUtils.push();
        ScissorUtils.setFromComponentCoordinates(x, y, width, height);

        for (String name : visible) {
            float anim = staffAnimations.getOrDefault(name, 0f);
            if (anim <= 0.01f) continue;
            StaffData data = staffDataCache.get(name);
            if (data == null) continue;

            int   alpha   = (int) (255 * anim);
            float yOffset = -5 * (1 - anim);

            float currentX = x + padding;
            for (PrefixSegment seg : data.segments) {
                font12.draw(matrices, seg.text, currentX, y + offsetY + 2 + yOffset,
                        ColorUtils.setAlphaColor(seg.color, alpha));
                currentX += seg.width12;
            }

            font12.draw(matrices, name, currentX, y + offsetY + 2 + yOffset,
                    ColorUtils.rgba(255, 255, 255, alpha));

            String statusLabel = getStatusLabel(data.status);
            float  statusW     = font12.getWidth(statusLabel);
            float  statusX     = x + width - statusW - padding;
            font12.draw(matrices, statusLabel, statusX, y + offsetY + 2 + yOffset,
                    ColorUtils.setAlphaColor(getStatusColor(data.status), alpha));

            offsetY += itemHeight * anim;
        }

        ScissorUtils.pop();
        ScissorUtils.unset();

        draggable.setWidth(width);
        draggable.setHeight(height);
    }
}