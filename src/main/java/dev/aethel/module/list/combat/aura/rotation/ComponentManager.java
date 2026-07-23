package dev.aethel.module.list.combat.aura.rotation;

import java.util.HashMap;

public final class ComponentManager extends HashMap<Class<? extends Component>, Component> {

    public void add(Component... components) {
        for (Component component : components) {
            this.put(component.getClass(), component);
        }
    }

    public void unregister(Component... components) {
        for (Component component : components) {
            this.remove(component.getClass());
        }
    }

    public <T extends Component> T get(final Class<T> clazz) {
        return this.values()
                .stream()
                .filter(component -> component.getClass() == clazz)
                .map(clazz::cast)
                .findFirst()
                .orElse(null);
    }
}
