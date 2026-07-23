package dev.ethereal.api.event.interfaces;

public interface Notifiable<E> {
    void notify(E event);
}
