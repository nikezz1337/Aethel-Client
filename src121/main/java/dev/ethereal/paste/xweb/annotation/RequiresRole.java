package dev.ethereal.paste.xweb.annotation;

import dev.ethereal.paste.xweb.Role;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Ограничивает доступ к коду по минимальной роли.
// Перед выполнением вызывай Guard.requireRole(annotation.value()).
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresRole {
    Role value();
}
