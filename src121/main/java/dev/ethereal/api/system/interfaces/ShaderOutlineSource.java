package dev.ethereal.api.system.interfaces;

import net.minecraft.util.math.BlockPos;

import java.util.List;

public interface ShaderOutlineSource {
    List<BlockPos> getOutlineRenderPoses();
    float getOutlineRenderBoxSize();
    float getOutlineRenderFactor();
    float getOutlineStrength();
    float getOutlineThreshold();
}
