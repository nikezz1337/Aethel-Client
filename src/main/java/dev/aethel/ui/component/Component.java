package dev.aethel.ui.component;

import dev.aethel.util.render.math.Animation;
import dev.aethel.util.render.math.Easing;

public abstract class Component implements IComponent {
    public float x, y, width, height;

    private final Animation alphaAnim = new Animation(Easing.BACK_OUT, 550);
    private final Animation alphaAnimSetting = new Animation(Easing.CUBIC_OUT, 280);
    private final Animation alphaAnimBack = new Animation(Easing.CUBIC_OUT, 280);

    public boolean isVisible() {
        return true;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public Animation getAlphaAnim() {
        return alphaAnim;
    }

    public Animation getAlphaAnimSetting() {
        return alphaAnimSetting;
    }

    public Animation getAlphaAnimBack() {
        return alphaAnimBack;
    }
}
