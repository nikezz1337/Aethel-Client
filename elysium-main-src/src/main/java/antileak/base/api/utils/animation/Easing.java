package antileak.base.api.utils.animation;

@FunctionalInterface
public interface Easing {
    double ease(double value);
}