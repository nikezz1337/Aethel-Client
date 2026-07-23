package dev.ethereal.api.utils.rotation;

import java.util.function.Supplier;

public record RotationChanger(
        int priority,
        Supplier<Float[]> rotations,
        Supplier<Boolean> remove
) {}