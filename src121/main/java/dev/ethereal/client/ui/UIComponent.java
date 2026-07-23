package dev.ethereal.client.ui;

import lombok.Getter;
import lombok.Setter;
import dev.ethereal.api.system.interfaces.UIApi;
import dev.ethereal.client.services.RenderService;

@Getter
@Setter
public abstract class UIComponent implements UIApi {
    private float x, y, width, height, alpha;

    public float gap() {
        return scaled(2f);
    }

    public float offset() {
        return gap() * 1.5f;
    }

    public float scaled(float value) {
        return RenderService.getInstance().scaled(value);
    }

    public float getScale() {
        return RenderService.getInstance().getScale();
    }
}
