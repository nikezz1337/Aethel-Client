package dev.aethel.module.list.render;

import dev.aethel.config.FriendManager;
import dev.aethel.event.list.EventHUD;
import dev.aethel.module.list.render.Interface;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.base.Instance;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.msdf.MsdfFont;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import dev.aethel.util.world.ServerUtil;
import net.minecraft.client.gui.DrawContext;
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
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.*;

public class NameTagsRender implements IMinecraft {

    private static final float FIXED_SCALE = 0.8f;
    private static final double SEARCH_RADIUS = 100.0;

    private final NameTags module;
    private final NameTagsItems nameTagsItems;
    private final NameTagsPotions nameTagsPotions;

    // --- cached projection state ---
    private final Quaternionf cachedCameraRotation = new Quaternionf();
    private final Vector3f tempVec = new Vector3f();
    private final Vector2f projectedTop = new Vector2f();
    private final Vector2f projectedRight = new Vector2f();
    private final Vector2f projectedBottom = new Vector2f();
    private final Vector3f cachedCamPos = new Vector3f();
    private float cachedFov = 70f;
    private float cachedHalfScreenWidth;
    private float cachedHalfScreenHeight;
    private float cachedMaxScreenWidth;
    private float cachedMaxScreenHeight;
    private double cachedTanHalfFov;
    private long cachedWorldTime = Long.MIN_VALUE;

    private final Map<Integer, CachedNameTag> nameTagCache = new HashMap<>();

    // --- donat prefixes ---
    private record PrefixInfo(String name, Formatting color) {}

    private static final Map<String, PrefixInfo> DONAT_PREFIXES = new LinkedHashMap<>();
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

    public NameTagsRender(NameTags module) {
        this.module = module;
        this.nameTagsItems = new NameTagsItems(module);
        this.nameTagsPotions = new NameTagsPotions(module);
    }

    public void onRender(EventHUD e) {
        if (mc.world == null || mc.player == null) return;

        float partialTicks = e.getRenderTickCounter().getTickDelta(true);
        DrawContext context = e.getDrawContext();

        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        cachedCamPos.set((float) camPos.x, (float) camPos.y, (float) camPos.z);

        var camera = mc.getEntityRenderDispatcher().camera;
        var yawQuat = RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw());
        var pitchQuat = RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch());
        yawQuat.mul(pitchQuat, cachedCameraRotation).conjugate();
        cachedFov = mc.gameRenderer.getFov(camera, partialTicks, true);
        cachedHalfScreenWidth = mc.getWindow().getScaledWidth() / 2f;
        cachedHalfScreenHeight = mc.getWindow().getScaledHeight() / 2f;
        cachedMaxScreenWidth = mc.getWindow().getScaledWidth() + 200f;
        cachedMaxScreenHeight = mc.getWindow().getScaledHeight() + 200f;
        cachedTanHalfFov = Math.tan(Math.toRadians(cachedFov / 2.0));
        cachedWorldTime = mc.world.getTime();

        boolean renderDroppedItems = module.targets.isEnabled("Предметы");
        boolean renderSelf = module.targets.isEnabled("Себя") && !mc.options.getPerspective().isFirstPerson();
        boolean renderItemInfo = module.information.isEnabled("Предметы");
        boolean renderPotions = module.information.isEnabled("Зелья");
        boolean renderIndicators = module.options.isEnabled("Индикация");

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player && !renderSelf) continue;
            if (player != mc.player && !isValidTarget(player)) continue;

            renderTag(player, context, partialTicks, renderItemInfo, renderPotions, renderIndicators);
        }

        if (renderDroppedItems) {
            Box searchBox = mc.player.getBoundingBox().expand(SEARCH_RADIUS);
            for (ItemEntity itemEntity : mc.world.getEntitiesByClass(ItemEntity.class, searchBox, entity -> !entity.isRemoved())) {
                renderTag(itemEntity, context, partialTicks, false, false, false);
            }
        }
    }

    private boolean isValidTarget(PlayerEntity player) {
        if (player.isRemoved()) return false;
        if (player.isSpectator()) return false;

        boolean targetsPlayers = module.targets.isEnabled("Игроки");
        boolean targetsAnimals = module.targets.isEnabled("Животные");
        boolean targetsMobs = module.targets.isEnabled("Мобы");

        if (targetsPlayers) return true;

        // For non-player entities we just check flags
        return false;
    }

    // --- fast projection (cached) ---
    private Vector2f projectFast(double wx, double wy, double wz, Vector2f dest) {
        tempVec.set(
                (float) (cachedCamPos.x - wx),
                (float) (cachedCamPos.y - wy),
                (float) (cachedCamPos.z - wz)
        );
        tempVec.rotate(cachedCameraRotation);

        if (tempVec.z >= 0f) {
            return dest.set(Float.MAX_VALUE, Float.MAX_VALUE);
        }

        float w = cachedHalfScreenWidth;
        float h = cachedHalfScreenHeight;
        double scale = h / (tempVec.z * cachedTanHalfFov);
        return dest.set(
                (float) (-tempVec.x * scale + w),
                (float) (h - tempVec.y * scale)
        );
    }

    // --- main tag render ---
    private void renderTag(Entity entity, DrawContext context, float partialTicks,
                           boolean renderItemInfo, boolean renderPotions, boolean renderIndicators) {
        double xI = MathHelper.lerp(partialTicks, entity.prevX, entity.getX());
        double yI = MathHelper.lerp(partialTicks, entity.prevY, entity.getY());
        double zI = MathHelper.lerp(partialTicks, entity.prevZ, entity.getZ());


        Box box = entity.getBoundingBox().offset(xI - entity.getX(), yI - entity.getY(), zI - entity.getZ());

        double cx = (box.minX + box.maxX) / 2.0;
        double topY = box.maxY;
        double cz = (box.minZ + box.maxZ) / 2.0;

        Vector2f topCenter = projectFast(cx, topY, cz, projectedTop);
        if (topCenter.x == Float.MAX_VALUE) return;

        float x = topCenter.x;
        float y = topCenter.y;

        if (x < -200 || x > cachedMaxScreenWidth || y < -200 || y > cachedMaxScreenHeight) return;

        float nameTagTop = renderName(entity, x, y, context);

        if (!(entity instanceof PlayerEntity player)) return;

        if (renderItemInfo) {
            nameTagsItems.renderItems(player, x, nameTagTop, context);
        }

        if (renderPotions) {
            Vector2f rightEdge = projectFast(box.maxX, topY, cz, projectedRight);
            nameTagsPotions.renderPotions(player, rightEdge.x + 2f, y, context);
        }

        if (renderIndicators) {
            Vector2f bottomCenter = projectFast(cx, box.minY, cz, projectedBottom);
            nameTagsItems.renderSpecialItems(player, x, bottomCenter.y - 2f * FIXED_SCALE, context);
        }
    }

    // --- name rendering ---
    private float renderName(Entity entity, float x, float y, DrawContext context) {
        MsdfFont font = Fonts.SFBOLD.get();

        CachedNameTag cached = getCachedNameTag(entity);

        float size = cached.fontSize();
        float textWidth = cached.width();
        float pad = 2.34f;
        float bgH = size + pad * 2f;
        float bgW = textWidth + pad * 2f;
        float bgX = x - bgW / 2f;
        float bgY = y - bgH;

        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();

        int bgColor;
        if (cached.friend()) {
            bgColor = ColorProvider.rgba(0, 90, 0, 200);
        } else {
            bgColor = ColorProvider.rgba(
                    ((t1 >> 16) & 0xFF) >> 2,
                    ((t1 >> 8) & 0xFF) >> 2,
                    (t1 & 0xFF) >> 2,
                    200
            );
        }

        float radius = 2f;

        // Тень
        Matrix4f mat = context.getMatrices().peek().getPositionMatrix();
        DrawUtil.drawShadow(mat, bgX, bgY + 1, bgW, bgH, radius, 8f, ColorProvider.rgba(0, 0, 0, 100));

        // Фон
        DrawUtil.drawRound(bgX, bgY + 1, bgW, bgH, radius, bgColor);

        // Имя
        DrawUtil.drawText(font, cached.displayText(), bgX + pad, bgY + pad , size, 255);
        return bgY;
    }

    // --- caching ---
    private record CachedNameTag(long worldTime, Text displayText, float width, float fontSize, boolean friend) {}

    private CachedNameTag getCachedNameTag(Entity entity) {
        CachedNameTag cached = nameTagCache.get(entity.getId());
        if (cached != null && cached.worldTime() == cachedWorldTime) {
            return cached;
        }

        boolean isFriend = !(entity instanceof ItemEntity) && FriendManager.isFriend(entity.getName().getString());

        MutableText displayText;
        if (entity instanceof ItemEntity itemEntity) {
            displayText = buildItemDisplayText(itemEntity);
        } else {
            displayText = buildEntityDisplayText(entity, isFriend);
        }

        float fontSize = 8f * FIXED_SCALE;
        float textWidth = Fonts.SFBOLD.get().getWidth(displayText, fontSize);

        CachedNameTag rebuilt = new CachedNameTag(cachedWorldTime, displayText, textWidth, fontSize, isFriend);
        nameTagCache.put(entity.getId(), rebuilt);
        return rebuilt;
    }

    // --- text building ---
    private MutableText buildItemDisplayText(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getStack();
        String itemName = stack.getName().getString();
        MutableText displayText = parseFormattingCodes(itemName, net.minecraft.text.Style.EMPTY);
        if (stack.getCount() > 1) {
            displayText.append(Text.literal(" [").styled(style -> style.withColor(Formatting.WHITE)));
            displayText.append(Text.literal(String.valueOf(stack.getCount())).styled(style -> style.withColor(Formatting.RED)));
            displayText.append(Text.literal("x]").styled(style -> style.withColor(Formatting.WHITE)));
        }
        return displayText;
    }

    private MutableText buildEntityDisplayText(Entity entity, boolean isFriend) {
        MutableText displayText = convertDisplayName(entity.getDisplayName());

        if (entity instanceof LivingEntity living) {
            float health = living.getHealth();
            if (health > 0.0F) {
                displayText.append(Text.literal(" [").styled(style -> style.withColor(Formatting.WHITE)));
                displayText.append(Text.literal(getHealthString(living)).styled(style -> style.withColor(Formatting.RED)));
                displayText.append(Text.literal("]").styled(style -> style.withColor(Formatting.WHITE)));
            }
        }

        return displayText;
    }

    private String getHealthString(LivingEntity entity) {
        float health = ServerUtil.getHealthFloat(entity);
        return String.valueOf((int) Math.round(health));
    }

    // --- display name processing (donat prefixes) ---
    private MutableText convertDisplayName(Text original) {
        MutableText result = Text.empty();
        processText(original, result);
        return result;
    }

    private void processText(Text component, MutableText result) {
        String content = "";
        if (component.getContent() instanceof net.minecraft.text.PlainTextContent.Literal literal) {
            content = literal.string();
        }
        if (!content.isEmpty()) {
            boolean found = false;
            for (Map.Entry<String, PrefixInfo> entry : DONAT_PREFIXES.entrySet()) {
                if (content.contains(entry.getKey())) {
                    // Заменяем донат-символ на цветной префикс, остальные §-коды НЕ трогаем
                    String colorCode = "§" + entry.getValue().color().getCode();
                    String replaced = content.replace(entry.getKey(), colorCode + entry.getValue().name() + " ");
                    result.append(parseFormattingCodes(replaced, component.getStyle()));
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

}
