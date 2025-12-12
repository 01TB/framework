package servlet.util.cast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
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
        if (root == null || propertyPath == null || propertyPath.isEmpty()) return;

        String[] parts = propertyPath.split("\\.");
        Object current = root;
        Class<?> currentType = rootType;

        try {
            // === Naviguer jusqu'à l'avant-dernier niveau ===
            for (int i = 0; i < parts.length - 1; i++) {
                String part = parts[i];
                Field field = findField(currentType, part);
                if (field == null) return;

                field.setAccessible(true);
                Object nested = field.get(current);

                if (nested == null) {
                    Constructor<?> constructor = field.getType().getDeclaredConstructor();
                    constructor.setAccessible(true);
                    nested = constructor.newInstance();
                    field.set(current, nested);
                }

                current = nested;
                currentType = field.getType();
            }

            // === Dernier niveau : assigner la valeur ===
            String leaf = parts[parts.length - 1];
            Field targetField = findField(currentType, leaf);
            if (targetField == null) return;

            targetField.setAccessible(true);
            Object finalValue = convertValue(rawValue, targetField.getType());
            targetField.set(current, finalValue);

        } catch (Exception e) {
            System.err.println("Erreur lors de l'instanciation/assignation via champ : " + propertyPath + " = " + rawValue);
            e.printStackTrace();
        }
    }

    // --- Méthodes utilitaires ---

    private static Field findField(Class<?> clazz, String fieldName) {
        String capped = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        // Essayer les noms possibles : nom, nomAvecMajuscule, _nom, etc.
        String[] candidates = { fieldName, capped, "_" + fieldName, "m" + capped };

        for (String name : candidates) {
            try {
                return clazz.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                // Continuer
            }
        }

        // Parcourir la hiérarchie
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            return findField(superClass, fieldName);
        }
        return null;
    }

    private static Object convertValue(Object rawValue, Class<?> targetType) {
        if (rawValue == null) return null;

        if (targetType.isArray() && targetType.getComponentType() == String.class) {
            if (rawValue instanceof String s) {
                return new String[]{s};
            } else if (rawValue instanceof String[] arr) {
                return arr;
            }
        }

        if (targetType == List.class && rawValue instanceof String[]) {
            return Arrays.asList((String[]) rawValue);
        }

        if (rawValue instanceof String str) {
            return convert(str, targetType);
        }

        return rawValue;
    }
    
}
