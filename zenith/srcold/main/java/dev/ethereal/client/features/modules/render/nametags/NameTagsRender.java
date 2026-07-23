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
import dev.ethereal.api.utils.math.ProjectionUtil;
import dev.ethereal.api.utils.render.RenderUtil;
import dev.ethereal.api.utils.render.display.BoxRender;
import dev.ethereal.api.utils.render.fonts.Font;
import dev.ethereal.api.utils.render.fonts.Fonts;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NameTagsRender implements QuickImports {
    private final NameTagsModule module;
    private final NameTagsItems nameTagsItems;
    private final NameTagsPotions nameTagsPotions;
    
    private static final float FIXED_SCALE = 0.72f;

    public NameTagsRender(NameTagsModule module) {
        this.module = module;
        this.nameTagsItems = new NameTagsItems(module);
        this.nameTagsPotions = new NameTagsPotions(module);
    }

    private final org.joml.Quaternionf cachedCameraRotation = new org.joml.Quaternionf();
    private final org.joml.Vector3f tempVec = new org.joml.Vector3f();
    private float cachedFov = 70f;
    private org.joml.Vector3f cachedCamPos = new org.joml.Vector3f();

    public void onRender(Render2DEvent.Render2DEventData event) {
        if (mc.world == null || mc.player == null) return;
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        cachedCamPos.set((float)camPos.x, (float)camPos.y, (float)camPos.z);

        var camera = mc.getEntityRenderDispatcher().camera;
        var yawQuat = net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw());
        var pitchQuat = net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch());
        yawQuat.mul(pitchQuat, cachedCameraRotation).conjugate();
        cachedFov = mc.gameRenderer.getFov(camera, mc.getRenderTickCounter().getTickDelta(false), true);

        for (Entity entity1 : mc.world.getEntities()) {
            double distSq = entity1.squaredDistanceTo(camPos.x, camPos.y, camPos.z);
            if (distSq > 10000) continue;

            if (entity1 instanceof PlayerEntity player) {
                if (module.entityFilter.isValid(player) ||
                        player == mc.player && module.targets.isEnabled("Self") && !mc.options.getPerspective().isFirstPerson()) {
                    renderTag(player, event.context(), event.partialTicks(), distSq);
                }
            } else if (entity1 instanceof ItemEntity itemEntity && module.targets.isEnabled("Предметы")) {
                renderTag(itemEntity, event.context(), event.partialTicks(), distSq);
            }
        }
    }

    private Vector2f projectFast(double wx, double wy, double wz) {
        tempVec.set(
            (float)(cachedCamPos.x - wx),
            (float)(cachedCamPos.y - wy),
            (float)(cachedCamPos.z - wz)
        );
        tempVec.rotate(cachedCameraRotation);

        if (tempVec.z >= 0f) return new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE);

        float w = mc.getWindow().getScaledWidth() / 2f;
        float h = mc.getWindow().getScaledHeight() / 2f;
        double scale = h / (tempVec.z * Math.tan(Math.toRadians(cachedFov / 2.0)));
        return new Vector2f(
            (float)(-tempVec.x * scale + w),
            (float)(h - tempVec.y * scale)
        );
    }

    private void renderTag(Entity entity, DrawContext context, float partialTicks, double distSq) {
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        double xI = MathHelper.lerp(partialTicks, entity.prevX, entity.getX());
        double yI = MathHelper.lerp(partialTicks, entity.prevY, entity.getY());
        double zI = MathHelper.lerp(partialTicks, entity.prevZ, entity.getZ());

        if (module.box3d.getValue()) {
            render3DBox(entity, xI, yI, zI);
        }

        Box box = entity.getBoundingBox().offset(xI - entity.getX(), yI - entity.getY(), zI - entity.getZ());

        double cx = (box.minX + box.maxX) / 2.0;
        double topY = box.maxY;
        double cz = (box.minZ + box.maxZ) / 2.0;

        Vector2f topCenter = projectFast(cx, topY, cz);
        if (topCenter.x == Float.MAX_VALUE) return;

        float x = topCenter.x;
        float y = topCenter.y;

        if (x < -200 || x > mc.getWindow().getScaledWidth() + 200 || y < -200 || y > mc.getWindow().getScaledHeight() + 200) return;

        float nameTagTop = renderName(entity, x, y, context);

        if (!(entity instanceof PlayerEntity player)) return;

        if (module.information.isEnabled("Предметы")) {
            nameTagsItems.renderItems(player, x, nameTagTop, context);
        }

        if (module.information.isEnabled("Зелья")) {
            Vector2f rightEdge = projectFast(box.maxX, topY, cz);
            nameTagsPotions.renderPotions(player, rightEdge.x + 2f, y, context);
        }

        if (module.options.isEnabled("Индикация")) {
            Vector2f bottomCenter = projectFast(cx, box.minY, cz);
            nameTagsItems.renderSpecialItems(player, x, bottomCenter.y - 2f * FIXED_SCALE, context);
        }
    }

    private float renderName(Entity entity, float x, float y, DrawContext context) {
        MatrixStack matrixStack = context.getMatrices();
        Font font = Fonts.MEDIUM;

        float scale = FIXED_SCALE;
        float size = 8f * scale;
        
        net.minecraft.text.MutableText displayText;
        if (entity instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getStack();
            displayText = stack.getName().copy();
            
            if (stack.getCount() > 1) {
                displayText.append(Text.literal(" [").styled(style -> style.withColor(Formatting.RESET)));
                displayText.append(Text.literal(String.valueOf(stack.getCount())).styled(style -> style.withColor(Formatting.RED)));
                displayText.append(Text.literal("x").styled(style -> style.withColor(Formatting.GRAY)));
                displayText.append(Text.literal("]").styled(style -> style.withColor(Formatting.RESET)));
            }
        } else {
            boolean isFriend = FriendManager.getInstance().contains(entity.getName().getString());

            displayText = (MutableText) convertDisplayName(entity.getDisplayName());

            if (isFriend) {
                displayText = Text.empty();
                if (entity instanceof PlayerEntity player) {
                    Text prefix = player.getScoreboardTeam() != null ? player.getScoreboardTeam().getPrefix() : null;
                    Text suffix = player.getScoreboardTeam() != null ? player.getScoreboardTeam().getSuffix() : null;

                    if (prefix != null) {
                        String cleanPrefix = prefix.getString().replaceAll("§[0-9a-fk-or]", "").trim();
                        if (!cleanPrefix.isEmpty()) {
                            displayText.append(Text.literal(cleanPrefix));
                            displayText.append(Text.literal(" "));
                        }
                    }

                    displayText.append(Text.literal(entity.getName().getString()).styled(style -> style.withColor(Formatting.WHITE)));

                    if (suffix != null) {
                        String cleanSuffix = suffix.getString().replaceAll("§[0-9a-fk-or]", "").trim();
                        if (!cleanSuffix.isEmpty()) {
                            displayText.append(Text.literal(" "));
                            displayText.append(Text.literal(cleanSuffix));
                        }
                    }
                } else {
                    displayText.append(Text.literal(entity.getName().getString()).styled(style -> style.withColor(Formatting.WHITE)));
                }
            }

            if (entity instanceof LivingEntity living) {
                float health = living.getHealth();
                if (health > 0.0F) {
                    displayText.append(Text.literal(" [").styled(style -> style.withColor(Formatting.RESET)));
                    displayText.append(Text.literal(getHealthString(living)).styled(style -> style.withColor(Formatting.RED)));
                    displayText.append(Text.literal("]").styled(style -> style.withColor(Formatting.RESET)));
                }
            }
        }

        float textWidth = font.getWidth(displayText, size);
        float pad = 2f;
        float bgH = size + pad * 2f;
        float bgW = textWidth + pad * 2f;
        float bgX = x - bgW / 2f;
        float bgY = y - bgH;

        boolean isFriendEntity = !(entity instanceof ItemEntity) &&
                FriendManager.getInstance().contains(entity.getName().getString());

        Color backgroundColor = isFriendEntity
                ? new Color(0, 120, 0, 200)
                : new Color(8, 8, 10, 200);
        RenderUtil.BLUR_RECT.draw(matrixStack, bgX, bgY+1, bgW, bgH, 1.5f, backgroundColor);

        font.drawText(matrixStack, displayText, bgX + pad, bgY + pad, size);
        return bgY;
    }

    private String getHealthString(LivingEntity entity) {
        float health = entity.getHealth() + entity.getAbsorptionAmount();
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
                    replaced = replaced.replaceAll("[§&][0-9a-fk-orA-FK-OR]", "").trim();
                    if (!replaced.isEmpty()) {
                        result.append(Text.literal(entry.getValue().color() + entry.getValue().name() + " "));
                        result.append(Text.literal(replaced).setStyle(component.getStyle()));
                    } else {
                        result.append(Text.literal(entry.getValue().color() + entry.getValue().name()));
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
