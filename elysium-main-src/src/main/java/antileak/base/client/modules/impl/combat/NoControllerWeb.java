package antileak.base.client.modules.impl.combat;

import com.adl.nativeprotect.Native;
import net.minecraft.block.Blocks;
import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventBlockCollide;
import antileak.base.client.modules.Module;

public class NoControllerWeb extends Module {

    public static NoControllerWeb INSTANCE = new NoControllerWeb();

    public NoControllerWeb() {
        super("NoControllerWeb", "Позволяет ломать и бить сквозь паутину", ModuleCategory.COMBAT);
    }
    @Native
    @EventLink
    public void onBlockCollide(final EventBlockCollide e) {
        if (mc.world == null || e.getPos() == null) return;
        if (mc.world.getBlockState(e.getPos()).getBlock() == Blocks.COBWEB) {
            e.setCancelled(true);
        }
    }
}
