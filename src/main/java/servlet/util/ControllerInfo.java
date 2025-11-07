package servlet.util;

import java.lang.reflect.Method;

public class ControllerInfo {
    private final Class<?> controllerClass;
    private final Method method;

    public ControllerInfo(Class<?> controllerClass, Method method) {
        this.controllerClass = controllerClass;
        this.method = method;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public Method getMethod() {
        return method;
    }
}
