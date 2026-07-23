package antileak.base.client;

import antileak.base.mods.maseffects.MaseffectsParticleTypes;
import antileak.base.mods.maseffects.particles.ReviveParticle;
import antileak.base.mods.maseffects.particles.ReviveSparkParticle;
import antileak.base.mods.particular.ParticularParticleTypes;
import antileak.base.mods.particular.particles.WaterSplashEmitterParticle;
import antileak.base.mods.particular.particles.WaterSplashFoamParticle;
import antileak.base.mods.particular.particles.WaterSplashParticle;
import antileak.base.mods.particular.particles.WaterSplashRingParticle;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public class ElysiumClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();

        registry.register(MaseffectsParticleTypes.REVIVE, ReviveParticle.Factory::new);
        registry.register(MaseffectsParticleTypes.REVIVE_SPARK, ReviveSparkParticle.Factory::new);

        registry.register(ParticularParticleTypes.WATER_SPLASH, WaterSplashParticle.Factory::new);
        registry.register(ParticularParticleTypes.WATER_SPLASH_FOAM, WaterSplashFoamParticle.Factory::new);
        registry.register(ParticularParticleTypes.WATER_SPLASH_RING, WaterSplashRingParticle.Factory::new);
        registry.register(ParticularParticleTypes.WATER_SPLASH_EMITTER, (type, world, x, y, z, velocityX, velocityY, velocityZ) ->
                new WaterSplashEmitterParticle(world, x, y, z, velocityX, velocityY, velocityZ));
    }
}
