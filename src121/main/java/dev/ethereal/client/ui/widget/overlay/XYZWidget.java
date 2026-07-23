package dev.ethereal.client.ui.widget.overlay;

import dev.ethereal.api.utils.render.fonts.Icons;
import dev.ethereal.client.ui.widget.InformationWidget;

public class XYZWidget extends InformationWidget {
    @Override
    public String getName() {
        return "XYZ";
    }

    public XYZWidget() {
        super(30f, 120f);
    }

    @Override
    public String getValue() {
        int x = (int) mc.player.getX();
        int y = (int) mc.player.getY();
        int z = (int) mc.player.getZ();
        return x + ", " + y + ", " + z;
    }

    @Override
    public Icons getIcon() {
        return Icons.COORDS;
    }
}
