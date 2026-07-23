package dev.ethereal.api.event;

import dev.ethereal.api.event.orbit.EventBus;
import dev.ethereal.api.event.orbit.ICancellable;

import java.lang.invoke.MethodHandles;

public final class Events {
    public static final EventBus BUS = EventBus.threadSafe();

    static {
        BUS.registerLambdaFactory(
                "dev.ethereal",
                (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup())
        );
    }

    private Events() {
    }

    public static <T> T post(T event) {
        return BUS.post(event);
    }

    public static <T extends ICancellable> T post(T event) {
        return BUS.post(event);
    }

    public static void subscribe(Object object) {
        BUS.subscribe(object);
    }

    public static void unsubscribe(Object object) {
        BUS.unsubscribe(object);
    }
}
