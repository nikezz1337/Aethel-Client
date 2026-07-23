package dev.ethereal.inject.render;

import dev.ethereal.api.accessor.IPlayerListHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerListHud.class)
public class MixinPlayerListHud implements IPlayerListHud {
    
    @Shadow
    private Text header;

    @Override
    public Text getHeaderText() {
        return header;
    }
}
