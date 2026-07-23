package dev.ethereal.inject.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ethereal.Ethereal;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.client.GameLoopEvent;
import dev.ethereal.api.event.events.client.TickEvent;
import dev.ethereal.api.system.backend.SharedClass;
import dev.ethereal.api.utils.framelimiter.FrameLimiter;
import dev.ethereal.api.utils.player.InventoryFlowManager;
import dev.ethereal.client.ui.mainmenu.MenuScreenTransition;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Shadow
    public Screen currentScreen;

    @Unique
    private final FrameLimiter frameLimiter = new FrameLimiter(false);

    @Inject(method = "render", at = @At("HEAD"))
    public void gameLoopHook(boolean tick, CallbackInfo ci) {
        frameLimiter.execute(60, () -> Events.post(new GameLoopEvent()));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void preTickHook(CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        InventoryFlowManager.tick();
        Events.post(new TickEvent());
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void closeHook(CallbackInfo ci) {
        Ethereal.getInstance().onClose();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initHook(RunArgs args, CallbackInfo ci) {
        Ethereal.getInstance().postLoad();
    }

    @Inject(method = "setScreen", at = @At("HEAD"))
    public void screenTransitionHook(Screen screen, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            MenuScreenTransition.start(currentScreen, screen);
        }
    }
}
