package dev.ethereal.client.features.modules.movement.nitrofirework;

import dev.ethereal.api.system.backend.Choice;
import dev.ethereal.api.system.backend.Pair;
import dev.ethereal.api.system.interfaces.QuickImports;

public abstract class NitroFireworkMode extends Choice {
    public abstract Pair<Float, Float> velocityValues();
}
