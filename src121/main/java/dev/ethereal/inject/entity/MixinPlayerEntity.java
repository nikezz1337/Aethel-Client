package dev.ethereal.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ethereal.api.utils.color.UIColors;
import dev.ethereal.api.utils.other.TextUtil;
import dev.ethereal.client.features.modules.player.AssistantModule;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.ethereal.api.event.Events;
import dev.ethereal.api.event.events.player.world.AttackEvent;
import dev.ethereal.api.event.events.player.move.TravelEvent;
import dev.ethereal.api.system.backend.SharedClass;

import java.util.HashMap;
import java.util.Map;

import static dev.ethereal.api.system.interfaces.QuickImports.mc;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends MixinLivingEntity {

    @Unique
    private final Map<Identifier, Boolean> lastCooldownState = new HashMap<>();
    @Unique
    private TravelEvent ethereal$lastTravelEvent;

    @Inject(method = "attack", at = @At("HEAD"))
    public void attackEventHook(Entity target, CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        if ((Object) this == SharedClass.player()) {
            Events.post(new AttackEvent(target));
        }
    }

    @Inject(method = "travel", at = @At("HEAD"))
    public void travelHook(Vec3d movementInput, CallbackInfo ci) {
        if ((Object) this != SharedClass.player()) {
            ethereal$lastTravelEvent = null;
            return;
        }

        PlayerEntity self = (PlayerEntity) (Object) this;
        ethereal$lastTravelEvent = new TravelEvent(self.getYaw(), self.getPitch());
        Events.post(ethereal$lastTravelEvent);
    }

    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d travelRotationHook(Vec3d original) {
        if ((Object) this != SharedClass.player() || ethereal$lastTravelEvent == null) {
            return original;
        }

        PlayerEntity self = (PlayerEntity) (Object) this;
        return self.getRotationVector(ethereal$lastTravelEvent.getPitch(), ethereal$lastTravelEvent.getYaw());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        if ((Object) this != SharedClass.player()) return;
        if (SharedClass.player() == null) return;

        ItemCooldownManager cooldownManager = SharedClass.player().getItemCooldownManager();
        PlayerEntity player = SharedClass.player();

        // Проверяем все предметы в инвентаре
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;

            Identifier groupId = cooldownManager.getGroup(stack);
            boolean isCoolingDown = cooldownManager.isCoolingDown(stack);
            Boolean wasCoolingDown = lastCooldownState.get(groupId);

            if (wasCoolingDown != null && wasCoolingDown && !isCoolingDown) {
                if (AssistantModule.getInstance().getFunctions().isEnabled("Писать о перезарядке")) {
                    String itemName = stack.getName().getString();
                    int rgb = UIColors.primary().getRGB() & 0xFFFFFF;

                    MutableText msg = Text.empty()
                            .append(Text.literal("Ethereal").styled(s -> s.withBold(true).withColor(rgb)))
                            .append(Text.literal(" » ").formatted(Formatting.GRAY))
                            .append(itemName).formatted(Formatting.DARK_GRAY)
                            .append(" перезарядился").formatted(Formatting.GRAY);

                    mc.player.sendMessage(msg, false);
                }
            }

            lastCooldownState.put(groupId, isCoolingDown);
        }
    }
}
