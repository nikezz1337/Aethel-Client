package dev.aethel.util.render;

import dev.aethel.util.providers.ResourceProvider;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormats;

public final class ShaderUtil {

    public static final ShaderProgramKey blockOverlay = register("blockoverlay/block_overlay");
    public static final ShaderProgramKey blockOverlayBalatro = register("blockoverlay/block_overlay_balatro");
    public static final ShaderProgramKey blockOverlayPlasma = register("blockoverlay/block_overlay_plasma");
    public static final ShaderProgramKey blockOverlayLightning = register("blockoverlay/block_overlay_lightning");
    public static final ShaderProgramKey blockOverlayEdge = register("blockoverlay/block_overlay_edge");

    private ShaderUtil() {}

    private static ShaderProgramKey register(String name) {
        return new ShaderProgramKey(
                ResourceProvider.getShaderIdentifier(name),
                VertexFormats.POSITION_TEXTURE_COLOR,
                Defines.EMPTY
        );
    }
}
