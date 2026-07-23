package dev.aethel.module.settings;

import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;

import java.util.function.Supplier;

public class BooleanSetting extends Setting {
    private final Animation animation = new Animation(Easing.QUINTIC_OUT, 400);
    private final Animation clickAnimation = new Animation(Easing.QUINTIC_OUT, 400);
    private boolean clicked;
    private boolean value;
    private int key = -1;

    public BooleanSetting(String name, boolean defaultValue) {
        super(name);
        this.value = defaultValue;
    }

    public Animation getAnimation() {
        return animation;
    }

    public Animation getClickAnimation() {
        return clickAnimation;
    }

    public boolean isClicked() {
        return clicked;
    }

    public void setClicked(boolean clicked) {
        this.clicked = clicked;
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    @Override
    public String getValueAsString() {
        return Boolean.toString(value);
    }

    @Override
    public void setValueFromString(String value) {
        this.value = Boolean.parseBoolean(value);
    }

    public void toggle() {
        this.value = !this.value;
    }

    @Override
    public BooleanSetting setVisible(Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}
