package dev.ethereal.client.features.modules.render.nametags;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector2f;
import dev.ethereal.api.event.events.render.Render2DEvent;
import dev.ethereal.api.system.configs.FriendManager;
import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.display.BoxRender;
import dev.ethereal.api.utils.render.fonts.Font;
import dev.ethereal.api.utils.render.fonts.Fonts;
import dev.ethereal.client.features.modules.other.HealthResolverModule;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NameTagsRender implements QuickImports {
    private static final String TARGET_ITEMS = "\u041F\u0440\u0435\u0434\u043C\u0435\u0442\u044B";
    private static final String TARGET_SELF = "\u0421\u0435\u0431\u044F";
    private static final String INFO_ITEMS = "\u041F\u0440\u0435\u0434\u043C\u0435\u0442\u044B";
    private static final String INFO_POTIONS = "\u0417\u0435\u043B\u044C\u044F";
    private static final String OPTION_INDICATORS = "\u0418\u0434\u0438\u043A\u0430\u0446\u0438\u044F";

    private final NameTagsModule module;
    private final NameTagsItems nameTagsItems;
    private final NameTagsPotions nameTagsPotions;

    private static final float FIXED_SCALE = 0.72f;
    private static final double MAX_DISTANCE_SQ = 10000.0;
    private static final double SEARCH_RADIUS = 100.0;

    public NameTagsRender(NameTagsModule module) {
        this.module = module;
        this.nameTagsItems = new NameTagsItems(module);
        this.nameTagsPotions = new NameTagsPotions(module);
    }

    private final org.joml.Quaternionf cachedCameraRotation = new org.joml.Quaternionf();
    private final org.joml.Vector3f tempVec = new org.joml.Vector3f();
    private final Vector2f projectedTop = new Vector2f();
    private final Vector2f projectedRight = new Vector2f();
    private final Vector2f projectedBottom = new Vector2f();
    private final RenderDebugProfiler profiler = new RenderDebugProfiler();
    private float cachedFov = 70f;
    private final org.joml.Vector3f cachedCamPos = new org.joml.Vector3f();
    private float cachedHalfScreenWidth;
    private float cachedHalfScreenHeight;
    private float cachedMaxScreenWidth;
    private float cachedMaxScreenHeight;
    private double cachedTanHalfFov;
    private long cachedWorldTime = Long.MIN_VALUE;
    private final Map<Integer, CachedNameTag> nameTagCache = new HashMap<>();

    public void onRender(Render2DEvent event) {
        if (mc.world == null || mc.player == null) return;

        boolean debug = module.debugProfiler.getValue();
        long frameStart = debug ? System.nanoTime() : 0L;
        if (debug) profiler.beginFrame();

        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        cachedCamPos.set((float) camPos.x, (float) camPos.y, (float) camPos.z);

        var camera = mc.getEntityRenderDispatcher().camera;
        var yawQuat = net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw());
        var pitchQuat = net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch());
        yawQuat.mul(pitchQuat, cachedCameraRotation).conjugate();
        cachedFov = mc.gameRenderer.getFov(camera, event.partialTicks(), true);
        cachedHalfScreenWidth = mc.getWindow().getScaledWidth() / 2f;
        cachedHalfScreenHeight = mc.getWindow().getScaledHeight() / 2f;
        cachedMaxScreenWidth = mc.getWindow().getScaledWidth() + 200f;
        cachedMaxScreenHeight = mc.getWindow().getScaledHeight() + 200f;
        cachedTanHalfFov = Math.tan(Math.toRadians(cachedFov / 2.0));
        cachedWorldTime = mc.world.getTime();

        boolean renderDroppedItems = module.targets.isEnabled("Предметы")
                && !dev.ethereal.client.features.modules.render.ItemESP.getInstance().isEnabled();
        boolean renderSelf = module.targets.isEnabled("Себя") && !mc.options.getPerspective().isFirstPerson();
        boolean renderItemInfo = module.information.isEnabled("Предметы");
        boolean renderPotions = module.information.isEnabled("Зелья");
        boolean renderIndicators = module.options.isEnabled("Идикация");
        boolean render3DBox = module.box3d.getValue();

        if (debug) profiler.playersChecked += mc.world.getPlayers().size();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && !module.entityFilter.isValid(player)) {
                if (debug) profiler.filteredEntities++;
                continue;
            }
            if (player == mc.player && !renderSelf) {
                if (debug) profiler.filteredEntities++;
                continue;
            }

            double distSq = player.squaredDistanceTo(camPos.x, camPos.y, camPos.z);
            if (distSq > MAX_DISTANCE_SQ) {
                if (debug) profiler.distanceCulled++;
                continue;
            }

            renderTag(player, event.context(), event.partialTicks(), distSq, render3DBox, renderItemInfo, renderPotions, renderIndicators, debug);
        }

        if (renderDroppedItems) {
            Box searchBox = mc.player.getBoundingBox().expand(SEARCH_RADIUS);
            for (ItemEntity itemEntity : mc.world.getEntitiesByClass(ItemEntity.class, searchBox, entity -> !entity.isRemoved())) {
                if (debug) profiler.itemsChecked++;

                double distSq = itemEntity.squaredDistanceTo(camPos.x, camPos.y, camPos.z);
                if (distSq > MAX_DISTANCE_SQ) {
                    if (debug) profiler.distanceCulled++;
                    continue;
                }

                renderTag(itemEntity, event.context(), event.partialTicks(), distSq, render3DBox, false, false, false, debug);
            }
        }

        if (debug) {
            profiler.frameTimeNs += System.nanoTime() - frameStart;
            profiler.endFrame();
        }
    }

    private Vector2f projectFast(double wx, double wy, double wz, Vector2f dest) {
        long start = module.debugProfiler.getValue() ? System.nanoTime() : 0L;
        tempVec.set(
            (float) (cachedCamPos.x - wx),
            (float) (cachedCamPos.y - wy),
            (float) (cachedCamPos.z - wz)
        );
        tempVec.rotate(cachedCameraRotation);

        if (tempVec.z >= 0f) {
            if (start != 0L) {
                profiler.projectTimeNs += System.nanoTime() - start;
                profiler.projectCalls++;
            }
            return dest.set(Float.MAX_VALUE, Float.MAX_VALUE);
        }

        float w = cachedHalfScreenWidth;
        float h = cachedHalfScreenHeight;
        double scale = h / (tempVec.z * cachedTanHalfFov);
        if (start != 0L) {
            profiler.projectTimeNs += System.nanoTime() - start;
            profiler.projectCalls++;
        }
        return dest.set(
            (float) (-tempVec.x * scale + w),
            (float) (h - tempVec.y * scale)
        );
    }

    private void renderTag(Entity entity, DrawContext context, float partialTicks, double distSq,
                           boolean render3DBox, boolean renderItemInfo, boolean renderPotions, boolean renderIndicators, boolean debug) {
        if (debug) profiler.tagsRendered++;
        double xI = MathHelper.lerp(partialTicks, entity.prevX, entity.getX());
        double yI = MathHelper.lerp(partialTicks, entity.prevY, entity.getY());
        double zI = MathHelper.lerp(partialTicks, entity.prevZ, entity.getZ());

        if (render3DBox) {
            long boxStart = debug ? System.nanoTime() : 0L;
            render3DBox(entity, xI, yI, zI);
            if (debug) profiler.boxTimeNs += System.nanoTime() - boxStart;
        }

        Box box = entity.getBoundingBox().offset(xI - entity.getX(), yI - entity.getY(), zI - entity.getZ());

        double cx = (box.minX + box.maxX) / 2.0;
        double topY = box.maxY;
        double cz = (box.minZ + box.maxZ) / 2.0;

        Vector2f topCenter = projectFast(cx, topY, cz, projectedTop);
        if (topCenter.x == Float.MAX_VALUE) {
            if (debug) profiler.behindCameraCulled++;
            return;
        }

        float x = topCenter.x;
        float y = topCenter.y;

        if (x < -200 || x > cachedMaxScreenWidth || y < -200 || y > cachedMaxScreenHeight) {
            if (debug) profiler.screenCulled++;
            return;
        }

        long nameStart = debug ? System.nanoTime() : 0L;
        float nameTagTop = renderName(entity, x, y, context);
        if (debug) profiler.nameTimeNs += System.nanoTime() - nameStart;

        if (!(entity instanceof PlayerEntity player)) return;

        if (renderItemInfo) {
            long itemsStart = debug ? System.nanoTime() : 0L;
            nameTagsItems.renderItems(player, x, nameTagTop, context);
            if (debug) profiler.itemsTimeNs += System.nanoTime() - itemsStart;
        }

        if (renderPotions) {
            Vector2f rightEdge = projectFast(box.maxX, topY, cz, projectedRight);
            long potionsStart = debug ? System.nanoTime() : 0L;
            nameTagsPotions.renderPotions(player, rightEdge.x + 2f, y, context);
            if (debug) profiler.potionsTimeNs += System.nanoTime() - potionsStart;
        }

        if (renderIndicators) {
            Vector2f bottomCenter = projectFast(cx, box.minY, cz, projectedBottom);
            long indicatorsStart = debug ? System.nanoTime() : 0L;
            nameTagsItems.renderSpecialItems(player, x, bottomCenter.y - 2f * FIXED_SCALE, context);
            if (debug) profiler.indicatorsTimeNs += System.nanoTime() - indicatorsStart;
        }
    }

    private float renderName(Entity entity, float x, float y, DrawContext context) {
        boolean debug = module.debugProfiler.getValue();
        MatrixStack matrixStack = context.getMatrices();
        Font font = Fonts.MEDIUM;

        long buildTextStart = debug ? System.nanoTime() : 0L;
        CachedNameTag cached = getCachedNameTag(entity, debug);
        if (debug) profiler.buildTextTimeNs += System.nanoTime() - buildTextStart;

        float size = cached.fontSize();

        long widthStart = debug ? System.nanoTime() : 0L;
        float textWidth = cached.width();
        if (debug) profiler.fontWidthTimeNs += System.nanoTime() - widthStart;
        float pad = 2f;
        float bgH = size + pad * 2f;
        float bgW = textWidth + pad * 2f;
        float bgX = x - bgW / 2f;
        float bgY = y - bgH;

        Color backgroundColor = cached.friend()
                ? new Color(0, 120, 0, 200)
                : new Color(8, 8, 10, 200);

        long rectStart = debug ? System.nanoTime() : 0L;
        RenderUtil.RECT.draw(matrixStack, bgX, bgY + 1, bgW, bgH, 1.5f, backgroundColor);
        if (debug) profiler.bgRectTimeNs += System.nanoTime() - rectStart;

        long drawStart = debug ? System.nanoTime() : 0L;
        drawSegmentedText(font, matrixStack, cached.segments(), bgX + pad, bgY + pad, size);
        if (debug) profiler.fontDrawTimeNs += System.nanoTime() - drawStart;
        return bgY;
    }

    private CachedNameTag getCachedNameTag(Entity entity, boolean debug) {
        CachedNameTag cached = nameTagCache.get(entity.getId());
        if (cached != null && cached.worldTime() == cachedWorldTime) {
            return cached;
        }

        long friendCheckStart = debug ? System.nanoTime() : 0L;
        boolean isFriend = !(entity instanceof ItemEntity) && FriendManager.getInstance().contains(entity.getName().getString());
        if (debug) profiler.friendCheckTimeNs += System.nanoTime() - friendCheckStart;

        MutableText displayText;
        if (entity instanceof ItemEntity itemEntity) {
            displayText = buildItemDisplayText(itemEntity);
        } else {
            long convertStart = debug ? System.nanoTime() : 0L;
            displayText = buildEntityDisplayText(entity, isFriend, debug);
            if (debug) profiler.convertDisplayNameTimeNs += System.nanoTime() - convertStart;
        }

        float fontSize = 8f * FIXED_SCALE;
        List<TextSegment> segments = flattenText(displayText);
        CachedNameTag rebuilt = new CachedNameTag(
                cachedWorldTime,
                segments,
                getSegmentedWidth(Fonts.MEDIUM, segments, fontSize),
                fontSize,
                isFriend
        );
        nameTagCache.put(entity.getId(), rebuilt);
        return rebuilt;
    }

    private MutableText buildItemDisplayText(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getStack();
        MutableText displayText = stack.getName().copy();
        if (stack.getCount() > 1) {
            displayText.append(Text.literal(" [").styled(style -> style.withColor(Formatting.WHITE)));
            displayText.append(Text.literal(String.valueOf(stack.getCount())).styled(style -> style.withColor(Formatting.RED)));
            displayText.append(Text.literal("x]").styled(style -> style.withColor(Formatting.WHITE)));
        }
        return displayText;
    }

    private MutableText buildEntityDisplayText(Entity entity, boolean isFriend, boolean debug) {
        MutableText displayText = (MutableText) convertDisplayName(entity.getDisplayName());

        if (isFriend) {
            displayText = Text.empty();
            if (entity instanceof PlayerEntity player) {
                Text prefix = player.getScoreboardTeam() != null ? player.getScoreboardTeam().getPrefix() : null;
                Text suffix = player.getScoreboardTeam() != null ? player.getScoreboardTeam().getSuffix() : null;

                if (prefix != null) {
                    String cleanPrefix = stripLegacyFormatting(prefix.getString()).trim();
                    if (!cleanPrefix.isEmpty()) {
                        displayText.append(Text.literal(cleanPrefix).styled(style -> style.withColor(Formatting.WHITE)));
                        displayText.append(Text.literal(" ").styled(style -> style.withColor(Formatting.WHITE)));
                    }
                }

                displayText.append(Text.literal(entity.getName().getString()).styled(style -> style.withColor(Formatting.WHITE)));

                if (suffix != null) {
                    String cleanSuffix = stripLegacyFormatting(suffix.getString()).trim();
                    if (!cleanSuffix.isEmpty()) {
                        displayText.append(Text.literal(" ").styled(style -> style.withColor(Formatting.WHITE)));
                        displayText.append(Text.literal(cleanSuffix).styled(style -> style.withColor(Formatting.WHITE)));
                    }
                }
            } else {
                displayText.append(Text.literal(entity.getName().getString()).styled(style -> style.withColor(Formatting.WHITE)));
            }
        }

        if (entity instanceof LivingEntity living) {
            long healthStart = debug ? System.nanoTime() : 0L;
            float health = living.getHealth();
            if (health > 0.0F) {
                displayText.append(Text.literal(" [").styled(style -> style.withColor(Formatting.WHITE)));
                displayText.append(Text.literal(getHealthString(living)).styled(style -> style.withColor(Formatting.RED)));
                displayText.append(Text.literal("]").styled(style -> style.withColor(Formatting.WHITE)));
            }
            if (debug) profiler.healthAppendTimeNs += System.nanoTime() - healthStart;
        }

        return displayText;
    }

    private String getHealthString(LivingEntity entity) {
        float health = HealthResolverModule.getInstance().getHealthFromScoreboard(entity)[0] + entity.getAbsorptionAmount();
        return String.valueOf((int) Math.round(health));
    }

    private void render3DBox(Entity entity, double x, double y, double z) {
        Box box = entity.getBoundingBox().offset(x - entity.getX(), y - entity.getY(), z - entity.getZ());
        Color themeColor = UIColors.primary();
        float alpha = module.boxAlpha.getValue();
        Color fillColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int) (alpha * 255));
        Color outlineColor = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 255);

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        RenderUtil.BOX.drawBox(x1, y1, z1, x2, y2, z2, 1.5f, fillColor, BoxRender.Render.FILL, 0.0f);
        RenderUtil.BOX.drawBox(x1, y1, z1, x2, y2, z2, 1.5f, outlineColor, BoxRender.Render.OUTLINE, 0.0f);
    }

    private record PrefixInfo(String name, Formatting color) {}

    private static final Map<String, PrefixInfo> DONAT_PREFIXES = new HashMap<>();
    static {
        DONAT_PREFIXES.put("ꔀ", new PrefixInfo("PLAYER", Formatting.GRAY));
        DONAT_PREFIXES.put("ꔄ", new PrefixInfo("HERO", Formatting.BLUE));
        DONAT_PREFIXES.put("ꔈ", new PrefixInfo("TITAN", Formatting.YELLOW));
        DONAT_PREFIXES.put("ꔒ", new PrefixInfo("AVENGER", Formatting.GREEN));
        DONAT_PREFIXES.put("ꔖ", new PrefixInfo("OVERLORD", Formatting.AQUA));
        DONAT_PREFIXES.put("ꔠ", new PrefixInfo("MAGISTER", Formatting.GOLD));
        DONAT_PREFIXES.put("ꔤ", new PrefixInfo("IMPERATOR", Formatting.RED));
        DONAT_PREFIXES.put("ꔨ", new PrefixInfo("DRAGON", Formatting.LIGHT_PURPLE));
        DONAT_PREFIXES.put("ꔲ", new PrefixInfo("BULL", Formatting.DARK_PURPLE));
        DONAT_PREFIXES.put("ꕒ", new PrefixInfo("RABBIT", Formatting.WHITE));
        DONAT_PREFIXES.put("ꔶ", new PrefixInfo("TIGER", Formatting.GOLD));
        DONAT_PREFIXES.put("ꕄ", new PrefixInfo("DRACULA", Formatting.RED));
        DONAT_PREFIXES.put("ꕖ", new PrefixInfo("BUNNY", Formatting.WHITE));
        DONAT_PREFIXES.put("ꕀ", new PrefixInfo("HYDRA", Formatting.DARK_GREEN));
        DONAT_PREFIXES.put("ꕈ", new PrefixInfo("COBRA", Formatting.GREEN));
        DONAT_PREFIXES.put("ꔁ", new PrefixInfo("MEDIA", Formatting.BLUE));
        DONAT_PREFIXES.put("ꔅ", new PrefixInfo("YT", Formatting.RED));
        DONAT_PREFIXES.put("ꕠ", new PrefixInfo("D.HELPER", Formatting.GREEN));
        DONAT_PREFIXES.put("ꔉ", new PrefixInfo("HELPER", Formatting.YELLOW));
        DONAT_PREFIXES.put("ꔗ", new PrefixInfo("MODER", Formatting.YELLOW));
        DONAT_PREFIXES.put("ꔡ", new PrefixInfo("MODER+", Formatting.LIGHT_PURPLE));
        DONAT_PREFIXES.put("ꔥ", new PrefixInfo("ST.MODER", Formatting.BLUE));
        DONAT_PREFIXES.put("ꔳ", new PrefixInfo("ML.ADMIN", Formatting.AQUA));
        DONAT_PREFIXES.put("ꔷ", new PrefixInfo("ADMIN", Formatting.DARK_RED));
    }

    private static Text convertDisplayName(Text original) {
        MutableText result = Text.empty();
        processText(original, result);
        return result;
    }

    private static void processText(Text component, MutableText result) {
        String content = "";
        if (component.getContent() instanceof net.minecraft.text.PlainTextContent.Literal literal) {
            content = literal.string();
        }
        if (!content.isEmpty()) {

            boolean found = false;
            for (Map.Entry<String, PrefixInfo> entry : DONAT_PREFIXES.entrySet()) {
                if (content.contains(entry.getKey())) {
                    String replaced = content.replace(entry.getKey(), "");
                    replaced = stripLegacyFormatting(replaced).trim();
                    if (!replaced.isEmpty()) {
                        result.append(Text.literal(entry.getValue().name() + " ").styled(style -> style.withColor(entry.getValue().color())));
                        result.append(Text.literal(replaced).setStyle(component.getStyle()));
                    } else {
                        result.append(Text.literal(entry.getValue().name()).styled(style -> style.withColor(entry.getValue().color())));
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                result.append(parseFormattingCodes(content, component.getStyle()));
            }
        }
        for (Text sibling : component.getSiblings()) {
            processText(sibling, result);
        }
    }

    private static String stripLegacyFormatting(String text) {
        String stripped = Formatting.strip(text);
        return stripped != null ? stripped : text;
    }

    private static MutableText parseFormattingCodes(String text, net.minecraft.text.Style baseStyle) {
        MutableText result = Text.empty();
        String[] parts = text.split("(?=[§&][0-9a-zA-Z])");
        net.minecraft.text.Style currentStyle = baseStyle;

        for (String part : parts) {
            if (part.isEmpty()) continue;
            if ((part.charAt(0) == '§' || part.charAt(0) == '&') && part.length() > 1) {
                char code = Character.toLowerCase(part.charAt(1));
                String rest = part.substring(2);
                Formatting fmt = Formatting.byCode(code);
                if (fmt != null) {
                    currentStyle = fmt == Formatting.RESET
                        ? net.minecraft.text.Style.EMPTY
                        : net.minecraft.text.Style.EMPTY.withFormatting(fmt);
                }
                if (!rest.isEmpty()) {
                    result.append(Text.literal(rest).setStyle(currentStyle));
                }
            } else {
                result.append(Text.literal(part).setStyle(currentStyle));
            }
        }
        return result;
    }

    private static float getSegmentedWidth(Font font, List<TextSegment> segments, float size) {
        float width = 0f;
        for (TextSegment segment : segments) {
            width += font.getWidth(segment.text(), size);
        }
        return width;
    }

    private static void drawSegmentedText(Font font, MatrixStack matrixStack, List<TextSegment> segments, float x, float y, float size) {
        float drawX = x;
        for (TextSegment segment : segments) {
            if (segment.text().isEmpty()) continue;
            font.drawText(matrixStack, segment.text(), drawX, y, size, segment.color());
            drawX += font.getWidth(segment.text(), size);
        }
    }

    private static java.util.List<TextSegment> flattenText(Text text) {
        java.util.List<TextSegment> segments = new java.util.ArrayList<>();
        collectSegments(text, segments);
        return segments;
    }

    private static void collectSegments(Text text, java.util.List<TextSegment> segments) {
        String content = "";
        if (text.getContent() instanceof net.minecraft.text.PlainTextContent.Literal literal) {
            content = literal.string();
        }
        if (!content.isEmpty()) {
            Color color = getTextColor(text.getStyle());
            if (!segments.isEmpty() && segments.get(segments.size() - 1).color().equals(color)) {
                TextSegment last = segments.get(segments.size() - 1);
                segments.set(segments.size() - 1, new TextSegment(last.text() + content, color));
            } else {
                segments.add(new TextSegment(content, color));
            }
        }

        for (Text sibling : text.getSiblings()) {
            collectSegments(sibling, segments);
        }
    }

    private static Color getTextColor(net.minecraft.text.Style style) {
        if (style == null || style.getColor() == null) {
            return Color.WHITE;
        }
        return new Color(style.getColor().getRgb());
    }

    private record TextSegment(String text, Color color) {}
    private record CachedNameTag(long worldTime, List<TextSegment> segments, float width, float fontSize, boolean friend) {}

    private static final class RenderDebugProfiler {
        private long reportAtMs = System.currentTimeMillis() + 1000L;

        private int frames;
        private int playersChecked;
        private int itemsChecked;
        private int tagsRendered;
        private int filteredEntities;
        private int distanceCulled;
        private int behindCameraCulled;
        private int screenCulled;
        private int projectCalls;

        private long frameTimeNs;
        private long projectTimeNs;
        private long nameTimeNs;
        private long buildTextTimeNs;
        private long friendCheckTimeNs;
        private long convertDisplayNameTimeNs;
        private long healthAppendTimeNs;
        private long fontWidthTimeNs;
        private long bgRectTimeNs;
        private long fontDrawTimeNs;
        private long itemsTimeNs;
        private long potionsTimeNs;
        private long indicatorsTimeNs;
        private long boxTimeNs;

        private void beginFrame() {
            frames++;
        }

        private void endFrame() {
            long now = System.currentTimeMillis();
            if (now < reportAtMs) return;

            double framesSafe = Math.max(frames, 1);
            String message = String.format(Locale.US,
                    "[NameTagsProfiler] avg=%.3fms name=%.3fms build=%.3fms friend=%.3fms convert=%.3fms health=%.3fms width=%.3fms rect=%.3fms draw=%.3fms items=%.3fms potions=%.3fms indicators=%.3fms box=%.3fms project=%.3fms calls=%d tags=%d players=%d itemsEnt=%d filtered=%d dist=%d behind=%d screen=%d",
                    frameTimeNs / 1_000_000.0 / framesSafe,
                    nameTimeNs / 1_000_000.0 / framesSafe,
                    buildTextTimeNs / 1_000_000.0 / framesSafe,
                    friendCheckTimeNs / 1_000_000.0 / framesSafe,
                    convertDisplayNameTimeNs / 1_000_000.0 / framesSafe,
                    healthAppendTimeNs / 1_000_000.0 / framesSafe,
                    fontWidthTimeNs / 1_000_000.0 / framesSafe,
                    bgRectTimeNs / 1_000_000.0 / framesSafe,
                    fontDrawTimeNs / 1_000_000.0 / framesSafe,
                    itemsTimeNs / 1_000_000.0 / framesSafe,
                    potionsTimeNs / 1_000_000.0 / framesSafe,
                    indicatorsTimeNs / 1_000_000.0 / framesSafe,
                    boxTimeNs / 1_000_000.0 / framesSafe,
                    projectTimeNs / 1_000_000.0 / framesSafe,
                    projectCalls,
                    tagsRendered,
                    playersChecked,
                    itemsChecked,
                    filteredEntities,
                    distanceCulled,
                    behindCameraCulled,
                    screenCulled
            );

            System.out.println(message);

            frames = 0;
            playersChecked = 0;
            itemsChecked = 0;
            tagsRendered = 0;
            filteredEntities = 0;
            distanceCulled = 0;
            behindCameraCulled = 0;
            screenCulled = 0;
            projectCalls = 0;
            frameTimeNs = 0L;
            projectTimeNs = 0L;
            nameTimeNs = 0L;
            buildTextTimeNs = 0L;
            friendCheckTimeNs = 0L;
            convertDisplayNameTimeNs = 0L;
            healthAppendTimeNs = 0L;
            fontWidthTimeNs = 0L;
            bgRectTimeNs = 0L;
            fontDrawTimeNs = 0L;
            itemsTimeNs = 0L;
            potionsTimeNs = 0L;
            indicatorsTimeNs = 0L;
            boxTimeNs = 0L;
            reportAtMs = now + 1000L;
        }
    }
}
