package servlet.util.cast;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class UtilCast {

    public static Object convert(String value, Class<?> targetType) {
        if (value == null) return null;

        if (targetType == String.class) {
            return value;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value) || "on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
        } else if (targetType == LocalDate.class) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(value, formatter);
        }

        return value; 
    }
    
    public static void setPropertyValue(Object root, String propertyPath, Object rawValue, Class<?> rootType) {
        String[] parts = propertyPath.split("\\.");
        //  dept.nom -> { "dept", "nom" } 
        Object current = root;
        Class<?> currentType = rootType;

        // Naviguer jusqu'à l'avant-dernier niveau
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            String cap = Character.toUpperCase(part.charAt(0)) + part.substring(1);

            try {
                Method getter = currentType.getMethod("get" + cap);
                Object nested = getter.invoke(current);
                if (nested == null) {
                    Class<?> nestedType = getter.getReturnType();
                    nested = nestedType.getDeclaredConstructor().newInstance();
                    currentType.getMethod("set" + cap, nestedType).invoke(current, nested);
                }
                current = nested;
                currentType = nested.getClass();
            } catch (Exception e) {
                return;
            }
        }

        // Dernier niveau : setter
        String leaf = parts[parts.length - 1];
        String cap = Character.toUpperCase(leaf.charAt(0)) + leaf.substring(1);
        String setterName = "set" + cap;

        try {
            Method setter = null;
            Class<?> targetType = null;

            // Chercher le bon setter
            for (Method m : currentType.getMethods()) {
                if (m.getName().equals(setterName) && m.getParameterCount() == 1) {
                    Class<?> paramType = m.getParameterTypes()[0];

                    if (rawValue instanceof String && paramType.isArray() && paramType.getComponentType() == String.class) {
                        // String → String[]
                        if (setter == null) {
                            setter = m;
                            targetType = paramType;
                        }
                    } else if (rawValue instanceof String[] && paramType.isArray() && paramType.getComponentType() == String.class) {
                        setter = m;
                        targetType = paramType;
                        break;
                    } else if (rawValue instanceof String && !paramType.isArray()) {
                        setter = m;
                        targetType = paramType;
                        break;
                    } else if (rawValue instanceof String[] && paramType == List.class) {
                        setter = m;
                        targetType = paramType;
                        break;
                    }
                }
            }

            if (setter == null) return;

            Object finalValue;
            if (rawValue instanceof String[] arr && targetType == List.class) {
                finalValue = Arrays.asList(arr);
            } else if (rawValue instanceof String str && targetType.isArray()) {
                finalValue = new String[] { str };
            } else if (rawValue instanceof String str) {
                finalValue = UtilCast.convert(str, targetType);
            } else {
                finalValue = rawValue;
            }

            setter.invoke(current, finalValue);

        } catch (Exception e) {
            System.err.println("Erreur setter : " + propertyPath + " = " + rawValue);
        }
    }
    
}
