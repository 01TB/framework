// src/servlet/util/ControllerInfo.java
package servlet.util;

import servlet.annotation.parameters.PathParam;

import java.lang.reflect.Method;

public class ControllerInfo {
    private final Class<?> controllerClass;
    private final Method method; // nom de la méthode d'action du controller
    private final PathPattern pathPattern;
    private final String[] parameterNames; // noms des @PathParam

    public ControllerInfo(Class<?> controllerClass, Method method, PathPattern pathPattern) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.pathPattern = pathPattern;

        // Extraire les noms des paramètres annotés @PathParam
        var params = method.getParameters();
        var names = new java.util.ArrayList<String>();
        for (var param : params) {
            if (param.isAnnotationPresent(PathParam.class)) {
                String name = param.getAnnotation(PathParam.class).value();
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