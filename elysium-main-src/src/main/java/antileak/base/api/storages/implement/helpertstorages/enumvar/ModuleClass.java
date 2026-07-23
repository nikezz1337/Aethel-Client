package antileak.base.api.storages.implement.helpertstorages.enumvar;


import antileak.base.client.modules.Module;

import java.util.List;

public class ModuleClass extends GlobalObject<Module> implements ModuleRewords {

    public static ModuleClass INSTANCE = new ModuleClass();

    public void initialize() {
        this.add(
                antibot,
                antithorns,
                aimBot,
                airStuck,
                arrows,
                aura,
                autoAccept,
                autoArmor,
                autoDuel,
                autoEat,
                autoLeave,
                autoExplosion,
                autoForest,
                nameProtect,
                autoSwap,
                autoTool,
                autoTotem,
                autoTrap,
                blockesp,
                blockOverlay,
                chams,
                clientSounds,
                clickPearl,
                cosmetics,
                cubes,
                elytraBoost,
                elytraExploit,
                grimExploit,
                elytraMotion,
                elytraSwap,
                elytraTarget,
                entityESP,
                fireworkESP,
                fastExp,
                freeCam,
                fullBright,
                grimGlide,
                highJump,
                hitBubbles,
                hitMarker,
                interfaceModule,
                interpolateF5,
                inventoryWalk,
                itemAim,
                itemRelease,
                itemScroller,
                jumpCircle,
                killEffect,
                kTLeave,
                leavetracker,
                lockSlot,
                lootTracker,
                noJumpDelay,
                noPush,
                noSlow,
                noVelocity,
                noVignette,
                noControllerWeb,
                noWeb,
                particularWater,
                pets,
                packetCriticals,
                particle,
                projectile,
                potionTracker,
                scoreboardHP,
                removals,
                rPSpoofer,
                seeInvisibles,
                shaderEsp,
                shaderHands,
                serverHelper,
                shulkerPreview,
                sonar,
                sprint,
                swingAnimations,
                targetESP,
                targetPearl,
                totemAngel,
                totemParticles,
                tpsSync,
                tapeMouse,
                trails,
                trajectories,
                viewModel,
                worldTweaks
        );
    }

    private void add(final Module... mod) {
        this.getObject().addAll(List.of(mod));
    }
}
