package dev.ethereal.api.utils.shader;

import dev.ethereal.api.system.interfaces.ShaderOutlineSource;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class ShaderOutline {

    private final ShaderOutlineSource source;

    public ShaderOutline(ShaderOutlineSource source) {
        this.source = source;
    }

    public void render(float partialTicks) {
    }

    public List<BlockPos> getRenderPositions() {
        return source.getOutlineRenderPoses();
    }
}
