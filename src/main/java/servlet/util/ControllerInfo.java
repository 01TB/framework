package servlet.util;

public class ControllerInfo {
    private final Class<?> controllerClass;
    private final java.lang.reflect.Method method;

    public ControllerInfo(Class<?> controllerClass, java.lang.reflect.Method method) {
        this.controllerClass = controllerClass;
        this.method = method;
    }

    public Class<?> getControllerClass() {
        return controllerClass;
    }

    public java.lang.reflect.Method getMethod() {
        return method;
    }
}
