package sweetie.evaware.client.ui.widget.overlay;

import sweetie.evaware.api.utils.math.MathUtil;
import sweetie.evaware.api.utils.render.fonts.Icons;
import sweetie.evaware.client.ui.widget.InformationWidget;

public class FPSWidget extends InformationWidget {
    private float animFps;

    @Override
    public String getName() {
        return "FPS";
    }

    public FPSWidget() {
        super(50f, 100f);
    }

    @Override
    public String getValue() {
        animFps = MathUtil.interpolate((int) animFps, mc.getCurrentFps(), 0.2f);
        return String.valueOf((int) animFps);
    }


    @Override
    public Icons getIcon() {
        return null;
    }
}
