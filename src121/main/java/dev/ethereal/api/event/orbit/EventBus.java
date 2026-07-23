package dev.ethereal.api.event.orbit;

import dev.ethereal.api.event.orbit.listeners.IListener;
import dev.ethereal.api.event.orbit.listeners.LambdaListener;
import dev.ethereal.api.event.orbit.listeners.ListOperations;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public class EventBus implements IEventBus {
    private record LambdaFactoryInfo(String packagePrefix,
                                     LambdaListener.Factory factory) {
    }

    private final Map<Object, List<IListener>> listenerCache;
    private final Map<Class<?>, List<IListener>> staticListenerCache;

    private final Map<Class<?>, List<IListener>> listenerMap;
    private final Supplier<List<IListener>> listenerListFactory;

    private final List<LambdaFactoryInfo> lambdaFactoryInfos = new CopyOnWriteArrayList<>();

    public EventBus(Map<Object, List<IListener>> listenerCache, Map<Class<?>, List<IListener>> staticListenerCache, Map<Class<?>, List<IListener>> listenerMap, Supplier<List<IListener>> listenerListFactory) {
        this.listenerCache = listenerCache;
        this.staticListenerCache = staticListenerCache;
        this.listenerMap = listenerMap;
        this.listenerListFactory = listenerListFactory;
    }

    public static EventBus threadSafe() {
        return new EventBus(Collections.synchronizedMap(new IdentityHashMap<>()), new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), CopyOnWriteArrayList::new);
    }

    public static EventBus threadUnsafe() {
        return new EventBus(new IdentityHashMap<>(), new IdentityHashMap<>(), new IdentityHashMap<>(), CopyOnWriteArrayList::new);
    }

    @Override
    public void registerLambdaFactory(String packagePrefix, LambdaListener.Factory factory) {
        lambdaFactoryInfos.add(new LambdaFactoryInfo(packagePrefix, factory));
    }

    @Override
    public boolean isListening(Class<?> eventClass) {
        List<IListener> listeners = listenerMap.get(eventClass);
        return listeners != null && !listeners.isEmpty();
    }

    public boolean isSubscribed(IListener listener) {
        List<IListener> listeners = listenerMap.get(listener.getTarget());
        return listeners != null && ListOperations.contains(listeners, listener);
    }

    public boolean isSubscribed(Object object) {
        return listenerCache.containsKey(object);
    }

    public boolean areAnySubscribed(Class<?> staticListener) {
        List<IListener> listeners = staticListenerCache.get(staticListener);
        if (listeners == null) return false;
        for (IListener listener : listeners) {
            if (this.isSubscribed(listener)) return true;
        }
        return false;
    }

    public boolean areAllSubscribed(Class<?> staticListener) {
        List<IListener> listeners = staticListenerCache.get(staticListener);
        if (listeners == null) return false;
        for (IListener listener : listeners) {
            if (!this.isSubscribed(listener)) return false;
        }
        return true;
    }

    @Override
    public <T> T post(T event) {
        List<IListener> listeners = listenerMap.get(event.getClass());

        if (listeners != null) {
            for (IListener listener : listeners) {
                listener.call(event);
            }
        }

        return event;
    }

    @Override
    public <T extends ICancellable> T post(T event) {
        List<IListener> listeners = listenerMap.get(event.getClass());

        if (listeners != null) {
            event.setCancelled(false);

            for (IListener listener : listeners) {
                listener.call(event);
                if (event.isCancelled()) break;
            }
        }

        return event;
    }

    @Override
    public void subscribe(Object object) {
        if (listenerCache.containsKey(object)) return;
        subscribe(getInstanceListeners(object), false);
    }

    @Override
    public void subscribe(Class<?> klass) {
        List<IListener> listeners = getStaticListeners(klass);
        subscribe(listeners, true);
    }

    @Override
    public void subscribe(IListener listener) {
        subscribe(listener, true);
    }

    private void subscribe(List<IListener> listeners, boolean check) {
        for (IListener listener : listeners) subscribe(listener, check);
    }

    private void subscribe(IListener listener, boolean check) {
        List<IListener> listeners = listenerMap.computeIfAbsent(listener.getTarget(), klass -> listenerListFactory.get());
        if (!check || !ListOperations.contains(listeners, listener)) ListOperations.insert(listeners, listener);
    }

    @Override
    public void unsubscribe(Object object) {
        List<IListener> listeners = listenerCache.get(object);
        if (listeners != null) {
            unsubscribe(listeners);
            listenerCache.remove(object);
        }
    }

    @Override
    public void unsubscribe(Class<?> klass) {
        List<IListener> listeners = staticListenerCache.get(klass);
        if (listeners != null) unsubscribe(listeners);
    }

    @Override
    public void unsubscribe(IListener listener) {
        List<IListener> listeners = listenerMap.get(listener.getTarget());
        if (listeners != null) ListOperations.remove(listeners, listener);
    }

    private void unsubscribe(List<IListener> listeners) {
        for (IListener listener : listeners) unsubscribe(listener);
    }

    private List<IListener> getStaticListeners(Class<?> klass) {
        return staticListenerCache.computeIfAbsent(klass, o -> getListeners(o, null, true));
    }

    private List<IListener> getInstanceListeners(Object object) {
        return listenerCache.computeIfAbsent(object, o -> getListeners(o.getClass(), o, false));
    }

    private List<IListener> getListeners(Class<?> klass, Object object, boolean staticOnly) {
        List<IListener> listeners = new ArrayList<>();
        getListeners(listeners, klass, object, staticOnly);
        return List.of(listeners.toArray(new IListener[0]));
    }

    private void getListeners(List<IListener> listeners, Class<?> klass, Object object, boolean staticOnly) {
        while (klass != null) {
            LambdaListener.Factory factory = null;

            for (var method : klass.getDeclaredMethods()) {
                if (isValid(method, staticOnly)) {
                    if (LambdaListener.requireLambdaFactoryRegistration() && factory == null)
                        factory = getLambdaFactory(klass); // Lazy-loaded
                    listeners.add(new LambdaListener(factory, klass, object, method));
                }
            }

            klass = klass.getSuperclass();
        }
    }

    private boolean isValid(Method method, boolean staticOnly) {
        if (staticOnly && !Modifier.isStatic(method.getModifiers())) return false;
        if (!method.isAnnotationPresent(EventHandler.class)) return false;
        if (method.getReturnType() != void.class) return false;
        if (method.getParameterCount() != 1) return false;

        return !method.getParameters()[0].getType().isPrimitive();
    }

    private LambdaListener.Factory getLambdaFactory(Class<?> klass) {
        for (LambdaFactoryInfo info : lambdaFactoryInfos) {
            if (klass.getName().startsWith(info.packagePrefix)) return info.factory;
        }

        throw new NoLambdaFactoryException(klass);
    }
}
