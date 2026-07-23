package dev.ethereal.api.event.interfaces;

import dev.ethereal.api.event.EventListener;

public interface Subscribable<L, T> {
    EventListener subscribe(L listener);
    void unsubscribe(L listener);
}
