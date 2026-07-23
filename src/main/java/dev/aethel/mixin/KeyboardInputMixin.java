package dev.aethel.mixin;

import dev.aethel.Aethel;
import dev.aethel.event.list.MoveInputEvent;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        KeyboardInput self = (KeyboardInput) (Object) this;

        MoveInputEvent event = new MoveInputEvent(
                self.movementForward,
                self.movementSideways,
                self.playerInput.jump(),
                self.playerInput.sneak(),
                0.3D
        );

        Aethel.getInstance().getEventBus().post(event);

        self.movementForward = event.getForward();
        self.movementSideways = event.getStrafe();

        self.playerInput = new PlayerInput(
                event.getForward() > 0,
                event.getForward() < 0,
                event.getStrafe() > 0,
                event.getStrafe() < 0,
                event.isJump(),
                event.isSneaking(),
                self.playerInput.sprint()
        );
    }
}
