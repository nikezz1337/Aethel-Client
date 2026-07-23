package dev.ethereal.client.ui.clickgui.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import dev.ethereal.api.module.setting.Setting;
import dev.ethereal.api.utils.animation.AnimationUtil;
import dev.ethereal.client.ui.UIComponent;

@Getter
@RequiredArgsConstructor
public abstract class SettingComponent extends UIComponent {
    private final Setting<?> setting;
    private final AnimationUtil visibleAnimation = new AnimationUtil();

    public void updateHeight(float value) {
        setHeight(scaled(value));
    }
}
