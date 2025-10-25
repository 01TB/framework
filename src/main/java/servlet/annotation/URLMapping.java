package servlet.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Target(ElementType.METHOD)
// L’annotation doit être conservée à l’exécution (pour être lisible par réflexion)
@Retention(RetentionPolicy.RUNTIME)
public @interface URLMapping {
    String url() default "/";
}
