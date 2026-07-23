package antileak.base.mods.maseffects;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class MaseffectsParticleTypes {
    public static final SimpleParticleType REVIVE = FabricParticleTypes.simple(true);
    public static final SimpleParticleType REVIVE_SPARK = FabricParticleTypes.simple(true);

    private MaseffectsParticleTypes() {
    }

    public static void register() {
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of("elysium", "revive"), REVIVE);
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of("elysium", "revive_spark"), REVIVE_SPARK);
    }
}
