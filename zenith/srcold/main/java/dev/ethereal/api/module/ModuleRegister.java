package dev.ethereal.api.module;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleRegister {
    String name();
    Category category();
    String description() default "";
    int bind() default -999;
}
