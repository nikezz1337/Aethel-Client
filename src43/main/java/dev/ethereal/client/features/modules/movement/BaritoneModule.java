package dev.ethereal.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.util.math.BlockPos;
import dev.ethereal.api.module.Category;
import dev.ethereal.api.module.Module;
import dev.ethereal.api.module.ModuleRegister;

@ModuleRegister(name = "Baritone", category = Category.MOVEMENT)
public class BaritoneModule extends Module {
    @Getter private static final BaritoneModule instance = new BaritoneModule();

    private Object baritone;
    private boolean baritoneAvailable = false;

    public BaritoneModule() {
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            setEnabled(false);
            return;
        }

        try {
            Class<?> baritoneAPIClass = Class.forName("baritone.api.BaritoneAPI");
            Object provider = baritoneAPIClass.getMethod("getProvider").invoke(null);
            baritone = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            baritoneAvailable = true;
        } catch (Exception e) {
            baritoneAvailable = false;
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        if (baritone != null && baritoneAvailable) {
            try {
                Object pathingBehavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
                pathingBehavior.getClass().getMethod("cancelEverything").invoke(pathingBehavior);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onEvent() {
    }

    public void goToBlock(BlockPos pos) {
        if (!baritoneAvailable || baritone == null) return;
        
        try {
            Class<?> goalBlockClass = Class.forName("baritone.api.pathing.goals.GoalBlock");
            Object goal = goalBlockClass.getConstructor(BlockPos.class).newInstance(pos);
            
            Object customGoalProcess = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
            customGoalProcess.getClass().getMethod("setGoalAndPath", Class.forName("baritone.api.pathing.goals.Goal")).invoke(customGoalProcess, goal);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void goToXZ(int x, int z) {
        if (!baritoneAvailable || baritone == null) return;
        
        try {
            Class<?> goalXZClass = Class.forName("baritone.api.pathing.goals.GoalXZ");
            Object goal = goalXZClass.getConstructor(int.class, int.class).newInstance(x, z);
            
            Object customGoalProcess = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
            customGoalProcess.getClass().getMethod("setGoalAndPath", Class.forName("baritone.api.pathing.goals.Goal")).invoke(customGoalProcess, goal);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void goToY(int y) {
        if (!baritoneAvailable || baritone == null) return;
        
        try {
            Class<?> goalYClass = Class.forName("baritone.api.pathing.goals.GoalY");
            Object goal = goalYClass.getConstructor(int.class).newInstance(y);
            
            Object customGoalProcess = baritone.getClass().getMethod("getCustomGoalProcess").invoke(baritone);
            customGoalProcess.getClass().getMethod("setGoalAndPath", Class.forName("baritone.api.pathing.goals.Goal")).invoke(customGoalProcess, goal);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void mine(String blockName) {
        if (!baritoneAvailable || baritone == null) return;
        
        try {
            Object mineProcess = baritone.getClass().getMethod("getMineProcess").invoke(baritone);
            mineProcess.getClass().getMethod("mine", String.class).invoke(mineProcess, blockName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void follow(String playerName) {
        if (!baritoneAvailable || baritone == null) return;
        
        try {
            Object followProcess = baritone.getClass().getMethod("getFollowProcess").invoke(baritone);

            System.out.println("Follow command requires Baritone to be installed as a separate mod");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelPath() {
        if (!baritoneAvailable || baritone == null) return;
        
        try {
            Object pathingBehavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
            pathingBehavior.getClass().getMethod("cancelEverything").invoke(pathingBehavior);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isPathing() {
        if (!baritoneAvailable || baritone == null) return false;
        
        try {
            Object pathingBehavior = baritone.getClass().getMethod("getPathingBehavior").invoke(baritone);
            return (boolean) pathingBehavior.getClass().getMethod("isPathing").invoke(pathingBehavior);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isBaritoneAvailable() {
        return baritoneAvailable;
    }
}
