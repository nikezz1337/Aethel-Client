package dev.ethereal.api.event.interfaces;

import dev.ethereal.api.event.Listener;

public interface Cacheable<T> {
    Listener<T>[] getCache();
}