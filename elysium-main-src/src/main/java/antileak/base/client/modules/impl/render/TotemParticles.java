package antileak.base.client.modules.impl.render;

import antileak.base.api.events.EventLink;
import antileak.base.api.events.implement.EventPopTotem;
import antileak.base.client.modules.Module;
import antileak.base.mods.maseffects.TotemParticleSpawner;

public class TotemParticles extends Module {
    public static final TotemParticles INSTANCE = new TotemParticles();

    public TotemParticles() {
        super("TotemParticles", "Custom totem particles", ModuleCategory.RENDER);
    }

    @EventLink
    public void onPopTotem(EventPopTotem event) {
        if (event == null || event.getPlayer() == null) {
            return;
        }

        TotemParticleSpawner.spawn(event.getPlayer());
    }
}
