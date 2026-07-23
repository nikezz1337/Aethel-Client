package dev.ethereal.api.utils.player;

import dev.ethereal.api.system.interfaces.QuickImports;
import lombok.Generated;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class InvUtil implements QuickImports {
   public static int find(Item item) {
      int slot = -1;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            slot = i;
            break;
         }
      }

      if (slot < 9 && slot != -1) {
         slot += 36;
      }

      return slot;
   }

   public static int find(Item item, boolean ignoreEnchanted) {
      return find(item, ignoreEnchanted, false);
   }

   public static int find(Item item, boolean ignoreEnchanted, boolean onlyEnchanted) {
      int slot = -1;
      if (onlyEnchanted) {
         for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item && stack.hasEnchantments()) {
               slot = i;
               break;
            }
         }
      } else {
         for (int ix = 0; ix < 36; ix++) {
            ItemStack stack = mc.player.getInventory().getStack(ix);
            if (!stack.isEmpty() && stack.getItem() == item && !stack.hasEnchantments()) {
               slot = ix;
               break;
            }
         }

         if (slot == -1 && !ignoreEnchanted) {
            for (int ixx = 0; ixx < 36; ixx++) {
               ItemStack stack = mc.player.getInventory().getStack(ixx);
               if (!stack.isEmpty() && stack.getItem() == item) {
                  slot = ixx;
                  break;
               }
            }
         }
      }

      if (slot < 9 && slot != -1) {
         slot += 36;
      }

      return slot;
   }

   /**
    * Находит слот предмета по части его отображаемого имени (без учёта регистра).
    * Ищет по всему инвентарю (0-35, где 0-8 = хотбар, 9-35 = основной инвентарь).
    * @return номер слота для clickSlot (0-8 → +36, 9-35 как есть), или -1 если не найдено
    */
   public static int findByName(String name) {
      String lower = name.toLowerCase(Locale.ROOT);
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getName().getString().toLowerCase(Locale.ROOT).contains(lower)) {
            if (i < 9) return i + 36;
            return i;
         }
      }
      return -1;
   }

   @Generated
   private InvUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
