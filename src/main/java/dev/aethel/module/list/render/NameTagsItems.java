package dev.aethel.module.list.render;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.render.msdf.Fonts;
import dev.aethel.util.render.providers.ColorProvider;
import dev.aethel.util.render.renderers.DrawUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;

import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.util.*;

public class NameTagsItems implements IMinecraft {

    private static final String OPTION_ENCHANTS = "Зачарования";
    private static final String ITEM_SPHERE = "Сфера";
    private static final String ITEM_RUNE = "Руна";
    private static final String ITEM_BALL = "Шар";
    private static final String ITEM_TALISMAN = "Талисман";
    private static final String ITEM_ANGEL_WINGS = "Крылья ангела";
    private static final String ITEM_KRUSH = "Круш";
    private static final String FULL_KRUSH_TEXT = "§lФУЛ КРУШ";

    private static final Set<String> WHITELIST_ENCHANTS = new HashSet<>(Arrays.asList(
            "minecraft:protection",
            "minecraft:fire_protection",
            "minecraft:blast_protection",
            "minecraft:projectile_protection",
            "minecraft:thorns",
            "minecraft:sharpness",
            "minecraft:fire_aspect",
            "minecraft:knockback",
            "minecraft:looting",
            "minecraft:unbreaking",
            "minecraft:efficiency",
            "minecraft:power",
            "minecraft:punch",
            "minecraft:infinity",
            "minecraft:mending"
    ));

    private final NameTags module;

    public NameTagsItems(NameTags module) {
        this.module = module;
    }

    public void renderItems(PlayerEntity entity, float x, float y, DrawContext context) {
        MatrixStack matrixStack = context.getMatrices();

        ItemStack main = entity.getMainHandStack();
        ItemStack off = entity.getOffHandStack();

        List<ItemStack> items = new ArrayList<>(6);
        if (!main.isEmpty()) items.add(main);
        if (!off.isEmpty()) items.add(off);
        if (!module.options.isEnabled("Only hands")) {
            for (ItemStack armor : entity.getInventory().armor) {
                if (!armor.isEmpty()) items.add(armor);
            }
        }

        int itemsSize = items.size();
        if (itemsSize == 0) return;

        float scale = 0.5f;
        float gap = 2f;
        float itemSize = 16f * scale;
        float itemSpacing = itemSize + gap;
        float totalWidth = itemsSize * itemSpacing - gap;
        float startX = x - totalWidth / 2f;
        y -= itemSize + gap + 4f;
        float bgPad = 2f;
        float bgW = totalWidth + bgPad * 2f;
        float bgH = itemSize + bgPad * 2f;
        float bgX = x - bgW / 2f;
        DrawUtil.drawRound(bgX, y - bgPad, bgW, bgH, 2f, ColorProvider.rgba(8, 8, 10, 180));

        boolean enchants = module.options.isEnabled(OPTION_ENCHANTS);
        for (int i = 0; i < itemsSize; i++) {
            matrixStack.push();
            matrixStack.translate(startX + i * itemSpacing, y, 0);
            matrixStack.scale(scale, scale, 1);

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

            context.drawItem(items.get(i), 0, 0);
            context.drawStackOverlay(mc.textRenderer, items.get(i), 0, 0);

            if (enchants) {
                renderEnchantments(items.get(i), matrixStack, -gap, -itemSize / 2f);
            }

            matrixStack.pop();
        }
    }

    public void renderSpecialItems(PlayerEntity player, float x, float y, DrawContext context) {
        MatrixStack matrixStack = context.getMatrices();
        List<SpecialLabel> specialLabels = new ArrayList<>();
        boolean hasFullKrushSet = hasFullKrushSet(player);

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            String itemName = stack.getName().getString();

            boolean isTalik = (
                    stack.getItem() == Items.TOTEM_OF_UNDYING ||
                            stack.getItem() == Items.PLAYER_HEAD ||
                            stack.getItem() == Items.POPPED_CHORUS_FRUIT
            ) && (
                    itemName.contains(ITEM_SPHERE) ||
                            itemName.contains(ITEM_RUNE) ||
                            itemName.contains(ITEM_BALL) ||
                            itemName.contains(ITEM_TALISMAN)
            );

            boolean isAngelElytra = itemName.contains(ITEM_ANGEL_WINGS) && stack.getItem() == Items.ELYTRA;
            if (isAngelElytra || isTalik) {
                specialLabels.add(new SpecialLabel(itemName, ColorProvider.rgba(255, 255, 255, 255)));
                continue;
            }
        }

        if (hasFullKrushSet) {
            specialLabels.add(0, new SpecialLabel(FULL_KRUSH_TEXT, ColorProvider.rgba(255, 70, 70, 255)));
        }

        if (specialLabels.isEmpty()) return;

        float labelScale = 1.2f;
        float gap = 3f * labelScale;
        float textSize = 6.3f * labelScale;
        float pad = 3f * labelScale;

        y += 5f * labelScale;

        for (int i = 0; i < specialLabels.size(); i++) {
            SpecialLabel label = specialLabels.get(i);
            float textWidth = Fonts.SFBOLD.get().getWidth(label.text(), textSize);
            float itemY = y + i * (gap * 2f + textSize);

            float bgX = x - textWidth / 2f - pad;
            float bgY = itemY + gap / 1.5f - pad / 2f;
            float bgW = textWidth + pad * 2f;
            float bgH = textSize + pad;

            DrawUtil.drawRound(bgX, bgY, bgW, bgH, 2f * labelScale, ColorProvider.rgba(20, 20, 30, 200));
            DrawUtil.drawText(Fonts.SFBOLD.get(), label.text(), x - textWidth / 2f, itemY + gap / 1.5f, label.color(), textSize);
        }
    }

    private boolean hasFullKrushSet(PlayerEntity player) {
        List<ItemStack> armor = player.getInventory().armor;
        if (armor.size() < 4) return false;

        return isKrushArmorPiece(armor.get(0))
                && isKrushArmorPiece(armor.get(1))
                && isKrushArmorPiece(armor.get(2))
                && isKrushArmorPiece(armor.get(3));
    }

    private boolean isKrushArmorPiece(ItemStack stack) {
        return !stack.isEmpty() && stack.getName().getString().contains(ITEM_KRUSH);
    }

    private void renderEnchantments(ItemStack stack, MatrixStack matrices, float x, float y) {
        float offsetY = 0f;
        float fontSize = 7f;

        if (stack.hasEnchantments()) {
            ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
            for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
                RegistryEntry<Enchantment> enchantment = entry.getKey();
                if (!WHITELIST_ENCHANTS.contains(enchantment.getIdAsString())) continue;

                int level = entry.getIntValue();
                int max = enchantment.value().getMaxLevel();

                String shortName = enchantment.getIdAsString();
                shortName = shortName.substring(shortName.indexOf(':') + 1, Math.min(shortName.indexOf(':') + 4, shortName.length()));
                shortName = shortName.substring(0, 1).toUpperCase() + shortName.substring(1);

                int color = level < max + 1 ? ColorProvider.getThemeColor() : ColorProvider.rgba(255, 80, 80, 255);
                DrawUtil.drawText(Fonts.SFBOLD.get(), shortName + " " + level, x, y + offsetY, color, fontSize);
                offsetY -= fontSize;
            }
        }
    }

    private record SpecialLabel(String text, int color) {}
}
