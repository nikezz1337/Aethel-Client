package ru.zenith.api.compiler.annotations;

import ru.zenith.api.compiler.enums.VMProtectType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface VMProtect {
    VMProtectType value() default VMProtectType.NONE;
}
