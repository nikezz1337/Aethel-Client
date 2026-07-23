package dev.aethel.mixin;

import dev.aethel.event.list.EventHandledScreen;
import dev.aethel.module.list.misc.AuctionHelperModule;
import dev.aethel.module.list.misc.ItemScroller;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Shadow @Nullable protected Slot focusedSlot;

    @Shadow protected abstract void onMouseClick(@Nullable Slot slot, int slotId, int button, SlotActionType actionType);

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ItemScroller itemScroller = ItemScroller.INSTANCE;

        if (!itemScroller.isEnabled() || mc.player == null || mc.interactionManager == null) return;

        long window = mc.getWindow().getHandle();
        boolean leftMousePressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
        boolean shiftPressed = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        if (!leftMousePressed || !shiftPressed) {
            itemScroller.resetTimer();
            return;
        }

        Slot slot = focusedSlot;
        if (slot == null || !slot.hasStack()) return;

        if (!itemScroller.canQuickMove()) return;

        onMouseClick(slot, slot.id, 0, SlotActionType.QUICK_MOVE);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
        new EventHandledScreen(focusedSlot).post();
    }

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void onDrawSlot(DrawContext context, Slot slot, CallbackInfo ci) {
        AuctionHelperModule module = AuctionHelperModule.getInstance();
        if (module.isEnabled()) module.onRenderChest(context, slot);
    }
}
