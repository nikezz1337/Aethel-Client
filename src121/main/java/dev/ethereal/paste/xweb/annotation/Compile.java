package dev.ethereal.paste.xweb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Маркер для прохода обфускатора/лоадера: помеченные методы возвращают секреты
// (ключи, сертификаты), которые шифруются на этапе сборки и расшифровываются лоадером.
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Compile {
}
