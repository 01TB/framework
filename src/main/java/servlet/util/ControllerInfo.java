// src/servlet/util/ControllerInfo.java
package servlet.util;

import servlet.util.PathPattern;
import java.lang.reflect.Method;

public class ControllerInfo {
    private final Class<?> controllerClass;
    private final Method method;
    private final PathPattern pathPattern;
    private final String[] parameterNames; // noms des @PathParam

    public ControllerInfo(Class<?> controllerClass, Method method, String urlPattern) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.pathPattern = new PathPattern(urlPattern);

        // Extraire les noms des paramètres annotés @PathParam
        var params = method.getParameters();
        var names = new java.util.ArrayList<String>();
        for (var param : params) {
            if (param.isAnnotationPresent(servlet.annotation.PathParam.class)) {
                String name = param.getAnnotation(servlet.annotation.PathParam.class).value();
                names.add(name);
            } else {
                names.add(null); // ou gérer autrement
            }
        }
        this.parameterNames = names.toArray(new String[0]);
    }

    // getters
    public Class<?> getControllerClass() { return controllerClass; }
    public Method getMethod() { return method; }
    public PathPattern getPathPattern() { return pathPattern; }
    public String[] getParameterNames() { return parameterNames; }
}