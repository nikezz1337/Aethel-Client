package dev.ethereal.api.utils.other;

import java.util.HashSet;
import java.util.Set;

import dev.ethereal.api.system.interfaces.QuickImports;
import dev.ethereal.api.utils.player.MoveUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

@Environment(EnvType.CLIENT)
public class MovementManager implements QuickImports {
   private static final MovementManager INSTANCE = new MovementManager();
   public final Set<String> lockRequests = new HashSet<>();

   private MovementManager() {
   }

   public static MovementManager getInstance() {
      return INSTANCE;
   }

   public void lockMovement(String moduleName) {
      if (mc.player != null && mc.player.isAlive() && mc.world != null) {
         this.lockRequests.add(moduleName);
      }
   }

   public void unlockMovement(String moduleName) {
      if (mc.player != null && mc.player.isAlive() && mc.world != null) {
         this.lockRequests.remove(moduleName);
         if (this.lockRequests.isEmpty()) {
            MoveUtil.updateMovementKeys();
         }
      }
   }

   public boolean isMovementLocked() {
      return !this.lockRequests.isEmpty();
   }

   public void tick() {
      if (isMovementLocked() && mc.player != null && mc.world != null) {
         for (KeyBinding key : MoveUtil.getMovementKeys()) {
            key.setPressed(false);
         }
      }
   }
}
