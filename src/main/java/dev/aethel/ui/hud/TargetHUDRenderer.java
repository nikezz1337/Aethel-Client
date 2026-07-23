package dev.aethel.ui.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.Aethel;
import dev.aethel.module.list.combat.Aura;
import dev.aethel.module.list.render.Interface;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.base.Instance;
import dev.aethel.util.world.ServerUtil;
import dev.aethel.util.render.builders.Builder;
import dev.aethel.util.render.builders.states.QuadColorState;
import dev.aethel.util.render.builders.states.QuadRadiusState;
import dev.aethel.util.render.builders.states.SizeState;
import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;
import dev.aethel.util.render.math.Scissor;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class TargetHUDRenderer implements IMinecraft {
    private static final net.minecraft.util.Identifier TARGET_HUD_GLOW_TEXTURE = net.minecraft.util.Identifier.of("mre", "images/glow.png");

    private final Interface interfaceModule;

    private final Animation animation = new Animation(Easing.EXPO_OUT, 300);
    private final Animation armorAnim = new Animation(Easing.EXPO_OUT, 300);
    private final Animation hpAnimation = new Animation(Easing.EXPO_OUT, 300);
    private final Animation outdatedHpAnimation = new Animation(Easing.EXPO_OUT, 600);
    private final Animation absorptionAnimation = new Animation(Easing.EXPO_OUT, 300);

    private float lastHealthVal = 0;
    private long lastTime = System.currentTimeMillis();
    private Entity lastTarget;
    private float lastHpPercent = -1f;

    private float trailHealthPercent = 1f;
    private float lastHealthPercent = 1f;
    private float lastAbsorptionPercent = 0f;
    private float lastHpRaw = -1f;
    private final List<DamageParticle> damageParticles = new ArrayList<>();
    private final List<HeadParticle> headParticles = new ArrayList<>();

    public TargetHUDRenderer(Interface interfaceModule) {
        this.interfaceModule = interfaceModule;
    }

    public void render(DrawContext context) {
        if (mc.player == null) return;

        renderCelestial(context);
    }

    private void renderCelestial(DrawContext context) {
        if (mc.player == null) return;

        Aura killAura = Instance.get(Aura.class);
        boolean chatOpen = mc.currentScreen instanceof ChatScreen;
        LivingEntity target = null;

        if (killAura.isEnabled() && Aura.target != null && Aura.target.isAlive()) {
            target = Aura.target;
        } else if (mc.targetedEntity instanceof LivingEntity living && living.isAlive()) {
            target = living;
        } else if (chatOpen) {
            target = mc.player;
        }

        if (target != null) {
            lastTarget = target;
            animation.run(1);
            armorAnim.run(1);
        } else {
            animation.run(0);
            armorAnim.run(0);
        }

        float animAlpha = (float) animation.getValue();
        if (animAlpha <= 0.05f || lastTarget == null || !(lastTarget instanceof LivingEntity)) return;

        LivingEntity livingEntity = (LivingEntity) lastTarget;

        int aInt = (int) (255 * animAlpha);
        int t1 = ColorProvider.getThemeColor();
        int t2 = ColorProvider.getThemeColorTwo();

        float x = interfaceModule.getTargetHUDDrag().getX();
        float y = interfaceModule.getTargetHUDDrag().getY();
        float width = 105f;
        float height = 36.5f;
        float panelRadius = 4f;

        // Фон - приглушённый оттенок темы
        int bgColor = ColorProvider.rgba(
                ((t1 >> 16) & 0xFF) >> 2,
                ((t1 >> 8) & 0xFF) >> 2,
                (t1 & 0xFF) >> 2,
                Math.min(135, aInt)
        );

        // Глоу/обводка
        int[] glow = ColorProvider.getOrbitalRect(t1, t2, 800.0, aInt);
        int tmp = glow[1]; glow[1] = glow[3]; glow[3] = tmp;

        Matrix4f posMat = context.getMatrices().peek().getPositionMatrix();

        DrawUtil.drawShadow(posMat, x, y, width, height, panelRadius, 8f, ColorProvider.rgba(0, 0, 0, 80));
        interfaceModule.drawGlow(posMat, x, y, width, height, panelRadius, animAlpha);
        DrawUtil.drawRound(x - 0.5f, y - 0.5f, width + 1f, height + 1f, panelRadius, glow[0], glow[1], glow[2], glow[3]);
        DrawUtil.drawRound(x, y, width, height, panelRadius, bgColor);

        // Голова — из скина игрока (как в оригинальном Celestial)
        float headSize = 30f;
        float headX = x + 2.5f;
        float headY = y + 3.75f;
        int headColor = ColorProvider.rgba(255,
                (int) (255 * (1 - livingEntity.hurtTime / 10f)),
                (int) (255 * (1 - livingEntity.hurtTime / 10f)),
                (int) (255 * animAlpha));

        AbstractClientPlayerEntity playerEntity = lastTarget instanceof AbstractClientPlayerEntity ? (AbstractClientPlayerEntity) lastTarget : null;
        if (playerEntity != null) {
            try {
                int texId = mc.getTextureManager().getTexture(playerEntity.getSkinTextures().texture()).getGlId();
                Builder.texture()
                        .size(new SizeState(headSize, headSize))
                        .radius(new QuadRadiusState(3))
                        .color(new QuadColorState(headColor))
                        .texture(8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, texId)
                        .smoothness(1f)
                        .build()
                        .render(context.getMatrices().peek().getPositionMatrix(), headX, headY);
            } catch (Exception ignored) {}
        } else {
            net.minecraft.item.Item spawnEgg = net.minecraft.item.SpawnEggItem.forEntity(livingEntity.getType());
            if (spawnEgg != null) {
                context.getMatrices().push();
                context.getMatrices().translate(headX + headSize / 2f, headY + headSize / 2f, 50.0);
                float animatedScale = (headSize / 16.0f) * (float) armorAnim.getValue();
                context.getMatrices().scale(animatedScale, animatedScale, 1.0f);
                context.getMatrices().translate(-8.0, -8.0, 0.0);
                context.drawItem(new ItemStack(spawnEgg), 0, 0);
                context.getMatrices().pop();
            } else {
                DrawUtil.drawRound(headX, headY, headSize, headSize, 3, ColorProvider.rgba(40, 40, 40, (int) (255 * animAlpha)));
                DrawUtil.drawText(Fonts.ICONS_NURIK.get(), "N", headX + 1.5f, headY + 8f, ColorProvider.rgba(255, 255, 255, (int) (255 * animAlpha)), 24f);
            }
        }

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        net.minecraft.client.render.DiffuseLighting.disableGuiDepthLighting();

        // Текст справа от головы
        float textX = headX + headSize + 2f;

        String rawName = livingEntity.getName().getString();
        String name = transliterate(rawName);

        int textColor = ColorProvider.rgba(222, 222, 222, (int) (255 * animAlpha));

        DrawUtil.drawText(Fonts.SFBOLD.get(), name, textX, y + 7f, textColor, 9f, 0.3f, 0.7f, width);

        float currentHp = Math.max(0f, ServerUtil.getHealthFloat(livingEntity));
        float absorptionHP = Math.max(0f, livingEntity.getAbsorptionAmount());
        float maxHealth = Math.max(1f, livingEntity.getMaxHealth());

        String hpText = String.format(java.util.Locale.US, "HP: %.1f", currentHp);
        String absorpText = String.format(java.util.Locale.US, "%.1f", absorptionHP);

        float fontSize = 6.5f;
        float hpY = y + 15.5f;
        DrawUtil.drawText(Fonts.SFBOLD.get(), hpText, textX + 0.5f, hpY, textColor, fontSize);

        if (absorptionHP > 0f) {
            float hpW = Fonts.SFBOLD.get().getWidth(hpText, fontSize);
            float plusW = Fonts.SFBOLD.get().getWidth("  + ", fontSize);
            DrawUtil.drawText(Fonts.SFBOLD.get(), "  + ", textX + hpW, hpY, ColorProvider.rgba(160, 160, 160, (int) (255 * animAlpha)), fontSize);
            DrawUtil.drawText(Fonts.SFBOLD.get(), absorpText, textX + hpW + plusW, hpY, ColorProvider.rgba(255, 205, 70, (int) (255 * animAlpha)), fontSize);
        }

        float myTotalHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float targetTotalHp = currentHp + absorptionHP;
        float damage = 1.0f;
        ItemStack weapon = mc.player.getMainHandStack();

        if (weapon != null && !weapon.isEmpty()) {
            String itemName = net.minecraft.registry.Registries.ITEM.getId(weapon.getItem()).getPath();
            if (itemName.contains("netherite_sword")) damage += 7.0f;
            else if (itemName.contains("diamond_sword")) damage += 6.0f;
            else if (itemName.contains("iron_sword")) damage += 5.0f;
            else if (itemName.contains("stone_sword")) damage += 4.0f;
            else if (itemName.contains("golden_sword") || itemName.contains("wooden_sword")) damage += 3.0f;
            else if (itemName.contains("netherite_axe")) damage += 9.0f;
            else if (itemName.contains("diamond_axe") || itemName.contains("iron_axe") || itemName.contains("stone_axe")) damage += 8.0f;
            else if (itemName.contains("golden_axe") || itemName.contains("wooden_axe")) damage += 6.0f;
            if (weapon.hasGlint()) damage += 3.0f;
        }

        if (mc.player.hasStatusEffect(StatusEffects.STRENGTH)) {
            damage += 3.0f * (mc.player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() + 1);
        }
        if (mc.player.hasStatusEffect(StatusEffects.WEAKNESS)) {
            damage -= 4.0f * (mc.player.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() + 1);
        }

        float potentialDamage = damage * 1.5f;
        float targetArmor = livingEntity.getArmor();
        float targetToughness = (float) livingEntity.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.ARMOR_TOUGHNESS);
        float f = 2.0F + targetToughness / 4.0F;
        float g = MathHelper.clamp(targetArmor - potentialDamage / f, targetArmor * 0.2F, 20.0F);
        potentialDamage = potentialDamage * (1.0F - g / 25.0F);

        int epf = 0;
        for (ItemStack armorPiece : livingEntity.getArmorItems()) {
            if (!armorPiece.isEmpty() && armorPiece.hasGlint()) epf += 4;
        }
        epf = Math.min(20, epf);
        if (epf > 0) potentialDamage = potentialDamage * (1.0F - (epf * 0.04F));



        float barX = textX - 1f;
        float barY = y + 25f;
        float barWidth = width - headSize - 10f;
        float barHeight = 7.5f;

        if (lastHpRaw == -1f || lastTarget != livingEntity) {
            lastHpRaw = currentHp;
            damageParticles.clear();
        }

        if (currentHp < lastHpRaw) {
            int count = MathHelper.clamp((int)((lastHpRaw - currentHp) * 4), 10, 25);
            Color pColor = getHealthBarColor(currentHp, maxHealth);
            float lostHpWidth = barWidth * MathHelper.clamp((lastHpRaw - currentHp) / maxHealth, 0f, 1f);
            float currentHpWidth = barWidth * MathHelper.clamp(currentHp / maxHealth, 0f, 1f);

            for (int i = 0; i < count; i++) {
                float spawnX = barX + currentHpWidth + (float)(Math.random() * lostHpWidth);
                float spawnY = barY + barHeight / 2f;
                damageParticles.add(new DamageParticle(spawnX, spawnY, pColor.getRGB()));
            }
            lastHpRaw = currentHp;
        } else if (currentHp > lastHpRaw) {
            lastHpRaw = currentHp;
        }

        damageParticles.removeIf(p -> p.getAlpha() <= 0);
        if (!damageParticles.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            RenderSystem.setShaderTexture(0, TARGET_HUD_GLOW_TEXTURE);

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Matrix4f matrix = context.getMatrices().peek().getPositionMatrix();

            for (DamageParticle p : damageParticles) {
                p.update();
                float pAlpha = p.getAlpha() * animAlpha;
                int c = ColorProvider.setAlpha(p.color, (int) (pAlpha * 255));
                float half = p.getSize() / 2f;

                buffer.vertex(matrix, p.x - half, p.y - half, 0).texture(0, 0).color(c);
                buffer.vertex(matrix, p.x - half, p.y + half, 0).texture(0, 1).color(c);
                buffer.vertex(matrix, p.x + half, p.y + half, 0).texture(1, 1).color(c);
                buffer.vertex(matrix, p.x + half, p.y - half, 0).texture(1, 0).color(c);
            }

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableBlend();
        }

        DrawUtil.drawRound(barX, barY, barWidth, barHeight, 2f, ColorProvider.rgba(45, 45, 45, (int) (255 * animAlpha)));

        float hpPercent = MathHelper.clamp(currentHp / maxHealth, 0f, 1f);
        float absorptionPercent = MathHelper.clamp(absorptionHP / maxHealth, 0f, 1f);

        lastHealthPercent += (hpPercent - lastHealthPercent) * 0.25f;
        lastAbsorptionPercent += (absorptionPercent - lastAbsorptionPercent) * 0.15f;
        trailHealthPercent += (lastHealthPercent - trailHealthPercent) * 0.008f;

        float hpWidth = barWidth * lastHealthPercent;
        float trailWidth = barWidth * trailHealthPercent;
        float absWidth = barWidth * lastAbsorptionPercent;

        if (trailWidth > hpWidth) {
            DrawUtil.drawRound(barX, barY, trailWidth, barHeight, 2f, ColorProvider.setAlpha(ColorProvider.getThemeColor(), (int) (135 * animAlpha)));
        }
        if (hpWidth > 0.5f) {
            int[] colors = ColorProvider.getOrbitalRect(t1, t2, 800.0, (int) (255 * animAlpha));
            float rR = (hpWidth >= barWidth - 1f || hpWidth <= barHeight) ? 2f : 0f;
            Builder.rectangle()
                    .size(new SizeState(hpWidth, barHeight))
                    .radius(new QuadRadiusState(2f, 2f, rR, rR))
                    .color(new QuadColorState(colors[0], colors[1], colors[2], colors[3]))
                    .build()
                    .render(context.getMatrices().peek().getPositionMatrix(), barX, barY);
        }
        if (absWidth > 0) {
            int absBase = ColorProvider.rgba(255, 222, 0, (int) (255 * animAlpha));
            int absLeft = ColorProvider.rgba(180, 155, 0, (int) (255 * animAlpha));
            DrawUtil.drawRound(barX, barY, absWidth, barHeight, 2f, absLeft, absLeft, absBase, absBase);
        }

        // Предметы (анимация от основного animation, не отдельная armorAnim)
        if (animAlpha > 0.05f) {
            List<ItemStack> items = new ArrayList<>();
            items.add(livingEntity.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD));
            items.add(livingEntity.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST));
            items.add(livingEntity.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS));
            items.add(livingEntity.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET));
            items.add(livingEntity.getMainHandStack());
            items.add(livingEntity.getOffHandStack());
            items.removeIf(ItemStack::isEmpty);

            if (!items.isEmpty()) {
                float itemScale = 0.7f;
                float slotSize = 14f * itemScale;
                float padding = 1f;
                float totalArmorWidth = (items.size() * slotSize) + ((items.size() - 1) * padding);
                float itemX = x + (width - totalArmorWidth) / 2f + 25;
                float itemY = headY - slotSize - 5.5f;

                context.getMatrices().push();
                context.getMatrices().translate(0, 0, 100);
                for (ItemStack stack : items) {
                    context.getMatrices().push();
                    context.getMatrices().translate(itemX, itemY, 0);
                    context.getMatrices().scale(animAlpha * itemScale, animAlpha * itemScale, 1f);
                    context.drawItem(stack, 0, 0);
                    context.drawStackOverlay(mc.textRenderer, stack, 0, 0);
                    context.getMatrices().pop();
                    itemX += slotSize + padding;
                }
                context.getMatrices().pop();
            }
        }

        interfaceModule.getTargetHUDDrag().setWidth(width);
        interfaceModule.getTargetHUDDrag().setHeight(height);
    }





    public void drawEntity(float x, float y, float scale, float yawAngle, float pitchAngle, LivingEntity entity) {
        MatrixStack matrices = new MatrixStack();
        matrices.push();
        matrices.translate(x, y, 50.0);
        matrices.scale(-scale, scale, scale);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(yawAngle));
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(pitchAngle));

        float bodyYaw = entity.bodyYaw;
        float prevBodyYaw = entity.prevBodyYaw;
        float headYaw = entity.headYaw;
        float prevHeadYaw = entity.prevHeadYaw;
        float yaw = entity.getYaw();
        float prevYaw = entity.prevYaw;
        float pitch = entity.getPitch();
        float prevPitch = entity.prevPitch;

        entity.bodyYaw = 0;
        entity.prevBodyYaw = 0;
        entity.headYaw = 0;
        entity.prevHeadYaw = 0;
        entity.setYaw(0);
        entity.prevYaw = 0;
        entity.setPitch(0);
        entity.prevPitch = 0;

        net.minecraft.client.render.DiffuseLighting.disableGuiDepthLighting();
        net.minecraft.client.render.VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        mc.getEntityRenderDispatcher().render(entity, 0.0, 0.0, 0.0, tickDelta, matrices, immediate, 0x00F000F0);

        immediate.draw();
        net.minecraft.client.render.DiffuseLighting.enableGuiDepthLighting();

        entity.bodyYaw = bodyYaw;
        entity.prevBodyYaw = prevBodyYaw;
        entity.headYaw = headYaw;
        entity.prevHeadYaw = prevHeadYaw;
        entity.setYaw(yaw);
        entity.prevYaw = prevYaw;
        entity.setPitch(pitch);
        entity.prevPitch = prevPitch;

        matrices.pop();
    }

    private Color getHealthBarColor(float currentHp, float maxHp) {
        float ratio = MathHelper.clamp(currentHp / maxHp, 0.0f, 1.0f);
        Color colorAtMax = new Color(44, 246, 53);
        Color colorAt56  = new Color(160, 228, 69);
        Color colorAt38  = new Color(222, 191, 79);
        Color colorAt32  = new Color(233, 150, 87);
        Color colorAt11  = new Color(255, 125, 98);

        if (ratio >= 0.56f) {
            float t = MathHelper.clamp((1.0f - ratio) / (1.0f - 0.56f), 0.0f, 1.0f);
            return lerpColor(colorAtMax, colorAt56, t);
        } else if (ratio >= 0.38f) {
            float t = MathHelper.clamp((0.56f - ratio) / (0.56f - 0.38f), 0.0f, 1.0f);
            return lerpColor(colorAt56, colorAt38, t);
        } else if (ratio >= 0.32f) {
            float t = MathHelper.clamp((0.38f - ratio) / (0.38f - 0.32f), 0.0f, 1.0f);
            return lerpColor(colorAt38, colorAt32, t);
        } else if (ratio >= 0.11f) {
            float t = MathHelper.clamp((0.32f - ratio) / (0.32f - 0.11f), 0.0f, 1.0f);
            return lerpColor(colorAt32, colorAt11, t);
        } else {
            return colorAt11;
        }
    }

    private Color lerpColor(Color a, Color b, float t) {
        return new Color(
                (int) (a.getRed() + t * (b.getRed() - a.getRed())),
                (int) (a.getGreen() + t * (b.getGreen() - a.getGreen())),
                (int) (a.getBlue() + t * (b.getBlue() - a.getBlue()))
        );
    }

    private String transliterate(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            String replacement = switch (c) {
                case 'а', 'А' -> c == 'А' ? "A" : "a";
                case 'б', 'Б' -> c == 'Б' ? "B" : "b";
                case 'в', 'В' -> c == 'В' ? "V" : "v";
                case 'г', 'Г' -> c == 'Г' ? "G" : "g";
                case 'д', 'Д' -> c == 'Д' ? "D" : "d";
                case 'е', 'Е' -> c == 'Е' ? "E" : "e";
                case 'ё', 'Ё' -> c == 'Ё' ? "Yo" : "yo";
                case 'ж', 'Ж' -> c == 'Ж' ? "Zh" : "zh";
                case 'з', 'З' -> c == 'З' ? "Z" : "z";
                case 'и', 'И' -> c == 'И' ? "I" : "i";
                case 'й', 'Й' -> c == 'Й' ? "Y" : "y";
                case 'к', 'К' -> c == 'К' ? "K" : "k";
                case 'л', 'Л' -> c == 'Л' ? "L" : "l";
                case 'м', 'М' -> c == 'М' ? "M" : "m";
                case 'н', 'Н' -> c == 'Н' ? "N" : "n";
                case 'о', 'О' -> c == 'О' ? "O" : "o";
                case 'п', 'П' -> c == 'П' ? "P" : "p";
                case 'р', 'Р' -> c == 'Р' ? "R" : "r";
                case 'с', 'С' -> c == 'С' ? "S" : "s";
                case 'т', 'Т' -> c == 'Т' ? "T" : "t";
                case 'у', 'У' -> c == 'У' ? "U" : "u";
                case 'ф', 'Ф' -> c == 'Ф' ? "F" : "f";
                case 'х', 'Х' -> c == 'Х' ? "Kh" : "kh";
                case 'ц', 'Ц' -> c == 'Ц' ? "Ts" : "ts";
                case 'ч', 'Ч' -> c == 'Ч' ? "Ch" : "ch";
                case 'ш', 'Ш' -> c == 'Ш' ? "Sh" : "sh";
                case 'щ', 'Щ' -> c == 'Щ' ? "Shch" : "shch";
                case 'ъ', 'Ъ' -> "";
                case 'ы', 'Ы' -> c == 'Ы' ? "Y" : "y";
                case 'ь', 'Ь' -> "";
                case 'э', 'Э' -> c == 'Э' ? "E" : "e";
                case 'ю', 'Ю' -> c == 'Ю' ? "Yu" : "yu";
                case 'я', 'Я' -> c == 'Я' ? "Ya" : "ya";
                default -> String.valueOf(c);
            };
            result.append(replacement);
        }
        return result.toString();
    }

    private static class HeadParticle {
        float x, y, vx, vy, size;
        long spawnTime;
        int color;

        HeadParticle(float startX, float startY, int color) {
            this.x = startX;
            this.y = startY;
            double angle = Math.random() * Math.PI * 2;
            double speed = Math.random() * 0.4 + 0.1;
            this.vx = (float) (Math.cos(angle) * speed);
            this.vy = (float) (Math.sin(angle) * speed);
            this.size = (float) (Math.random() * 8 + 2);
            this.spawnTime = System.currentTimeMillis();
            this.color = color;
        }

        void update() {
            x += vx;
            y += vy;
        }

        float getAlpha() {
            long elapsed = System.currentTimeMillis() - spawnTime;
            if (elapsed >= 2000) return 0;
            return 1f - ((float) elapsed / 2000f);
        }
    }

    private static class DamageParticle {
        float x, y, vx, vy, baseSize;
        long spawnTime, maxLife;
        int color;

        DamageParticle(float x, float y, int color) {
            this.x = x;
            this.y = y;
            double angle = Math.random() * Math.PI * 2;
            double speed = Math.random() * 2.0 + 0.5;
            this.vx = (float) (Math.cos(angle) * speed);
            this.vy = (float) (Math.sin(angle) * speed);
            this.baseSize = (float) (Math.random() * 7 + 6);
            this.spawnTime = System.currentTimeMillis();
            this.maxLife = (long) (Math.random() * 700 + 800);
            this.color = color;
        }

        void update() {
            x += vx;
            y += vy;
            vx *= 0.85f;
            vy *= 0.85f;
        }

        float getAlpha() {
            long elapsed = System.currentTimeMillis() - spawnTime;
            if (elapsed >= maxLife) return 0;
            return 1f - ((float) elapsed / maxLife);
        }

        float getSize() {
            return baseSize * getAlpha();
        }
    }
}
