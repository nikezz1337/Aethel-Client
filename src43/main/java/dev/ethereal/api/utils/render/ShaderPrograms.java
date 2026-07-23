package dev.ethereal.api.utils.render;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import dev.ethereal.api.system.backend.ClientInfo;

@UtilityClass
public class ShaderPrograms {

    public final ShaderProgramKey blockOverlay = register("blockoverlay", "block_overlay", VertexFormats.POSITION_TEXTURE_COLOR);
    public final ShaderProgramKey shaderHandsMaskDiff = register("hands", "hands_mask_diff", VertexFormats.POSITION_TEXTURE_COLOR);
    public final ShaderProgramKey shaderHandsOverlay = register("hands", "hands_overlay", VertexFormats.POSITION_TEXTURE_COLOR);
    public final ShaderProgramKey shaderHandsGlow = register("hands", "hands_glow", VertexFormats.POSITION_TEXTURE_COLOR);
    public final ShaderProgramKey shaderHandsKawaseDown = register("hands", "hands_kawase_down", VertexFormats.POSITION_TEXTURE_COLOR);
    public final ShaderProgramKey shaderHandsKawaseUp = register("hands", "hands_kawase_up", VertexFormats.POSITION_TEXTURE_COLOR);
    private ShaderProgramKey register(String shaderNamePackage, String shaderName, VertexFormat vertexFormat) {
        return new ShaderProgramKey(Identifier.of(ClientInfo.NAME, "core/" + shaderNamePackage + "/" + shaderName), vertexFormat, Defines.EMPTY);
    }
}
