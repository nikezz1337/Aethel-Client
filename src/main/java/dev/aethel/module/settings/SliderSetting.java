package dev.aethel.module.settings;

import dev.aethel.util.text.ValueUnit;

import java.util.function.Supplier;

public class SliderSetting extends Setting {

    private double value;
    private ValueUnit unit;
    private final double min;
    private final double max;
    private final double step;

    public SliderSetting(String name,
                         ValueUnit unit,
                         double defaultValue,
                         double min,
                         double max,
                         double step) {
        super(name);
        this.value = defaultValue;
        this.unit = unit;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public SliderSetting(String name,
                         double defaultValue,
                         double min,
                         double max,
                         double step) {
        super(name);
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public double getValue() {
        return value;
    }

    public ValueUnit getUnit() {
        return unit;
    }

    public void setUnit(ValueUnit unit) {
        this.unit = unit;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getStep() {
        return step;
    }

    public String getFormattedValue() {
        return unit.format(value);
    }

    public int getIntValue() {
        return (int) value;
    }

    public float getFloatValue() {
        return (float) value;
    }

    public void setValue(double value) {
        this.value = Math.max(min, Math.min(max, value));
    }

    public void increment() {
        setValue(value + step);
    }

    public void decrement() {
        setValue(value - step);
    }

    @Override
    public String getValueAsString() {
        return Double.toString(value);
    }

    @Override
    public void setValueFromString(String value) {
        try {
            setValue(Double.parseDouble(value));
        } catch (NumberFormatException ignored) {}
    }

    @Override
    public SliderSetting setVisible(Supplier<Boolean> visible) {
        this.visible = visible;
        return this;
    }
}
