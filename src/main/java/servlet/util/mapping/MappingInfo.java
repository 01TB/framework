package servlet.util.mapping;

import java.lang.reflect.Method;

public class MappingInfo {
        public final Object controllerInstance;
        public final Method method;
        public final Class<?> controllerClass;
        
        public MappingInfo(Object controllerInstance, Method method, Class<?> controllerClass) {
            this.controllerInstance = controllerInstance;
            this.method = method;
            this.controllerClass = controllerClass;
        }
    }