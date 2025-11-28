package servlet.annotation.parameters;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathParam {
    String value();  // le nom de la variable, ex: "id"
}