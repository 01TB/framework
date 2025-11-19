package servlet.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {
    String value();           // nom du paramètre dans la requête (ex: "nom")
    boolean required() default true;  // optionnel plus tard
    String defaultValue() default ""; // optionnel plus tard
}