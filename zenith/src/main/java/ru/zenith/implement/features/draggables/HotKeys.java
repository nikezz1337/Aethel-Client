package ru.zenith.implement.features.draggables;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import ru.zenith.api.feature.draggable.AbstractDraggable;
import ru.zenith.api.feature.module.Module;
import ru.zenith.api.system.animation.Animation;
import ru.zenith.api.system.animation.Direction;
import ru.zenith.api.system.animation.implement.DecelerateAnimation;
import ru.zenith.api.system.font.FontRenderer;
import ru.zenith.api.system.font.Fonts;
import ru.zenith.api.system.shape.ShapeProperties;
import ru.zenith.common.util.color.ColorUtil;
import ru.zenith.common.util.math.MathUtil;
import ru.zenith.common.util.other.StringUtil;
import ru.zenith.common.util.entity.PlayerIntersectionUtil;
import ru.zenith.core.Main;

import java.util.*;

public class HotKeys extends AbstractDraggable {
    private List<Module> keysList = new ArrayList<>();

    public HotKeys() {
        super("Hot Keys", 300, 10, 80, 23,true);
    }

    @Override
    public boolean visible() {
        return !keysList.isEmpty() || PlayerIntersectionUtil.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        keysList = Main.getInstance().getModuleProvider().getModules().stream().filter(module -> module.getAnimation().getOutput().floatValue() != 0 && module.getKey() != -1).toList();
    }

    @Override
    public void drawDraggable(DrawContext e) {
        MatrixStack matrix = e.getMatrices();
        float centerX = getX() + getWidth() / 2F;

        FontRenderer font = Fonts.getSize(14, Fonts.Type.DEFAULT);
        FontRenderer fontIcons = Fonts.getSize(16, Fonts.Type.ICONS);
        FontRenderer fontModule = Fonts.getSize(13, Fonts.Type.DEFAULT);

        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), 13.5F)
                .round(4).color(ColorUtil.getClientColor()).build());

        fontIcons.drawString(matrix, "C", getX() + 3.5F, getY() + 6, ColorUtil.getText());
        font.drawString(matrix, getName(), (int) (centerX - font.getStringWidth(getName()) / 2), getY() + 5.5f, ColorUtil.getText());

        int offset = 23 - 6;
        int maxWidth = 80;

        for (Module module : keysList) {
            String bind = Formatting.GRAY + "|  " + Formatting.RESET + StringUtil.getBindName(module.getKey()) + "";
            float centerY = getY() + offset;
            float animation = module.getAnimation().getOutput().floatValue();
            float width = fontModule.getStringWidth(module.getName() + bind) + 15;

            MathUtil.scale(matrix,centerX,centerY,1,animation,() -> {
                blur.render(ShapeProperties.create(matrix, getX(), centerY - 2, getWidth(), 10).round(4).color(ColorUtil.getClientColor()).build());
                fontModule.drawString(matrix, module.getName(), getX() + 3.5f, centerY + 1.5f, ColorUtil.getText());
                fontModule.drawString(matrix, bind, getX() + getWidth() - 3.5f - fontModule.getStringWidth(bind), centerY + 1.5f, ColorUtil.getText());
            });

            offset += (int) (animation * 11);
            maxWidth = (int) Math.max(width, maxWidth);
        }

        setWidth(maxWidth);
        setHeight(offset);
    }
}