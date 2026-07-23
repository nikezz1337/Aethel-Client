package dev.aethel.module;

import com.google.common.eventbus.Subscribe;
import dev.aethel.Aethel;
import dev.aethel.event.list.EventHUD;
import dev.aethel.module.list.misc.*;
import dev.aethel.module.list.player.*;
import dev.aethel.module.list.render.*;
import dev.aethel.module.list.render.hand.HandModule;
import dev.aethel.module.list.render.particles.ParticlesModule;
import dev.aethel.module.settings.BooleanSetting;
import dev.aethel.module.list.render.Removals;
import dev.aethel.module.list.render.WorldTweaks;
import dev.aethel.module.settings.ThemeSetting;
import dev.aethel.module.list.combat.Aura;
import dev.aethel.module.list.combat.ProjectileHelper;
import dev.aethel.module.list.combat.TriggerBot;
import dev.aethel.module.list.combat.AutoPotion;
import dev.aethel.module.list.combat.AutoSwap;
import dev.aethel.module.list.combat.AutoTotem;
import dev.aethel.module.list.combat.Hitbox;

import dev.aethel.module.list.movement.AirStuck;
import dev.aethel.module.list.movement.Sprint;
import dev.aethel.module.list.movement.GuiMove;
import dev.aethel.module.list.movement.NoSlow;
import dev.aethel.module.list.movement.NoWeb;
import dev.aethel.util.IMinecraft;
import dev.aethel.util.player.other.SlownessManager;

import java.util.ArrayList;
import java.util.List;

public class ModuleStorage implements IMinecraft {
    private final List<Module> modules = new ArrayList<>();

    public void injectRegisterModules() {
        modules.add(new ClickGui());
        modules.add(new Aura());

        modules.add(new Joiner());
        modules.add(new AutoAccept());
        modules.add(new NoPush());
        modules.add(new BlockESP());
        modules.add(new AutoLeave());
        modules.add(new ClientSounds());
        modules.add(new Tracker());
        modules.add(new Interface());
        modules.add(new TargetESP());
        modules.add(new Arrows());
        modules.add(new NameTags());
        modules.add(new FixHP());
        modules.add(new FullBright());
        modules.add(new ViewModel());
        modules.add(new SwingAnimations());
        modules.add(new ParticlesModule());
        modules.add(new NoDelayModule());
        modules.add(new ClanUpgrader());
        modules.add(ItemScroller.INSTANCE);
        modules.add(new AutoTool());
        modules.add(new AutoSwap());
        modules.add(new AutoTotem());
        modules.add(new AutoPotion());
        modules.add(new Hitbox());
        modules.add(new ProjectileHelper());
        modules.add(new TriggerBot());
        modules.add(new TargetPearl());
        modules.add(new ElytraHelper());
        modules.add(new Removals());
        modules.add(new WorldTweaks());
        modules.add(new WorldModule());
        modules.add(new Wings());
        modules.add(new Sprint());
        modules.add(new GuiMove());
        modules.add(new NoSlow());
        modules.add(new NoWeb());
        modules.add(new AirStuck());
        modules.add(new AutoEat());
        modules.add(new ChestStealer());
        modules.add(new Blink());
        modules.add(new AutoInvisible());
        modules.add(new NoInteract());
        modules.add(new Assistant());
        modules.add(new AntiAFK());
        modules.add(new NameProtect());
        modules.add(new ClickPearl());
        modules.add(new BlockOverlay());
        modules.add(new SeeInvisibles());
        modules.add(new Prediction());
        modules.add(new DamageTint());
        modules.add(new HandModule());
        modules.add(new PvPSafe());
        modules.add(new FreeCamera());

        Aethel.getInstance().getEventBus().register(this);
    }

    public <T extends Module> T get(final String name) {
        return this.modules.stream()
                .filter(module -> module.getName().equalsIgnoreCase(name))
                .map(module -> (T) module)
                .findFirst()
                .orElse(null);
    }

    public <T extends Module> T get(final Class<T> clazz) {
        return this.modules.stream()
                .filter(module -> clazz.isAssignableFrom(module.getClass()))
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }

    public List<Module> get(final ModuleCategory category) {
        return this.modules.stream()
                .filter(module -> module.getCategory() == category)
                .collect(java.util.stream.Collectors.toList());
    }

    @Subscribe
    private void onRender(EventHUD ignored) {
        if (!SlownessManager.slowTasksIsEmpty()) SlownessManager.updateSlowTasks();
        if (!SlownessManager.timeTasksIsEmpty()) SlownessManager.updateTimeTasks(false);
        for (var module : getModules()) {
            module.getAnimation().run(module.isEnabled());
            for (var setting : module.getSettings()) {
                if (setting instanceof BooleanSetting b) b.getAnimation().run(b.getValue());
                if (setting instanceof ThemeSetting t) t.getValue().animation.run(true);
            }
        }
    }

    public List<Module> getModules() {
        return modules;
    }
}
