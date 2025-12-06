package servlet.annotation.json;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)  // Applicable uniquement sur les méthodes
@Retention(RetentionPolicy.RUNTIME)  // Disponible au runtime pour AOP
public @interface ResponseJSON {
}
