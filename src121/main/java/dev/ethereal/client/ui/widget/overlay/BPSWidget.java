package dev.ethereal.client.ui.widget.overlay;

import dev.ethereal.api.utils.math.MathUtil;
import dev.ethereal.api.utils.render.fonts.Icons;
import dev.ethereal.client.ui.widget.InformationWidget;

public class BPSWidget extends InformationWidget {
    @Override
    public String getName() {
        return "BPS";
    }

    public BPSWidget() {
        super(80f, 120f);
    }

    @Override
    public String getValue() {
        return String.format("%.1f", MathUtil.getEntityBPS(mc.player)) + " BPS";
    }

    @Override
    public Icons getIcon() {
        return Icons.SPEED;
    }
}
