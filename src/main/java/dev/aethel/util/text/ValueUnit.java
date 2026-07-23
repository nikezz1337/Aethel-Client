package dev.aethel.util.text;

public class ValueUnit {
    private final String suffix;
    private final double multiplier;

    public ValueUnit(String suffix, double multiplier) {
        this.suffix = suffix;
        this.multiplier = multiplier;
    }

    public String format(double value) {
        return String.format("%.1f%s", value * multiplier, suffix);
    }

    public static final ValueUnit NONE = new ValueUnit("", 1.0);
    public static final ValueUnit PERCENT = new ValueUnit("%", 100.0);
    public static final ValueUnit MS = new ValueUnit("ms", 1.0);
    public static final ValueUnit BLOCKS = new ValueUnit("b", 1.0);
    public static final ValueUnit CPS = new ValueUnit("cps", 1.0);
}
