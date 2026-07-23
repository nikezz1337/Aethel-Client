package dev.ethereal.paste.xweb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Помечает код, требующий валидной аутентифицированной сессии.
// Перед выполнением такого кода вызывай Guard.requireAuthenticated().
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Protected {
}
