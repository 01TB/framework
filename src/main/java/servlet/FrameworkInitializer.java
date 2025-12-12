package servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import servlet.annotation.Controller;
import servlet.annotation.mappings.DeleteMapping;
import servlet.annotation.mappings.GetMapping;
import servlet.annotation.mappings.PostMapping;
import servlet.annotation.mappings.PutMapping;
import servlet.annotation.mappings.URLMapping;
import servlet.util.ControllerInfo;
import servlet.util.PathPattern;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

public class FrameworkInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        Set<Class<?>> allClasses = scanClasses(context);
        Map<PathPattern, ControllerInfo> urlMap = new HashMap<>();

        for (Class<?> clazz : allClasses) {
            if (clazz.isAnnotationPresent(Controller.class)) {
                Controller ctrlAnno = clazz.getAnnotation(Controller.class);
                String basePath = normalizePath(ctrlAnno.path());

                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(URLMapping.class)) {
                        String url = method.getAnnotation(URLMapping.class).url();
                        registerMapping(method, "GET", basePath, normalizePath(url), clazz, urlMap); // ou toutes les méthodes
                    }
                    if (method.isAnnotationPresent(GetMapping.class)) {
                        String url = method.getAnnotation(GetMapping.class).url();
                        registerMapping(method, "GET", basePath, normalizePath(url), clazz, urlMap);
                    }
                    if (method.isAnnotationPresent(PostMapping.class)) {
                        String url = method.getAnnotation(PostMapping.class).url();
                        registerMapping(method, "POST", basePath, normalizePath(url), clazz, urlMap);
                    }
                    if (method.isAnnotationPresent(PutMapping.class)) {
                        String url = method.getAnnotation(PutMapping.class).url();
                        registerMapping(method, "PUT", basePath, normalizePath(url), clazz, urlMap);
                    }
                    if (method.isAnnotationPresent(DeleteMapping.class)) {
                        String url = method.getAnnotation(DeleteMapping.class).url();
                        registerMapping(method, "DELETE", basePath, normalizePath(url), clazz, urlMap);
                    }
                }
            }
        }
        
        // Stocker la map dans le contexte pour l'utiliser plus tard
        context.setAttribute("urlMap", urlMap);
    }

    private void registerMapping(
        Method method, 
        String httpMethod, 
        String basePath, 
        String methodPath,                      
        Class<?> clazz, 
        Map<PathPattern, ControllerInfo> urlMap) {
            String fullUrl = normalizePath(basePath + methodPath);
            PathPattern pattern = new PathPattern(fullUrl,httpMethod);
            ControllerInfo info = new ControllerInfo(clazz, method, pattern);
            
            // Ajout dans la map
            urlMap.put(pattern, info);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Rien à faire ici
    }

    private Set<Class<?>> scanClasses(ServletContext context) {
        Set<Class<?>> classes = new HashSet<>();
        try {
            scanDirectory("/WEB-INF/classes/", "", classes, context);
        } catch (Exception e) {
            e.printStackTrace(); // Loggez en production
        }
        return classes;
    }

    private void scanDirectory(String path, String packageName, Set<Class<?>> classes, ServletContext context) throws IOException, ClassNotFoundException {
        Set<String> resourcePaths = context.getResourcePaths(path);
        if (resourcePaths == null) return;

        for (String resourcePath : resourcePaths) {
            if (resourcePath.endsWith("/")) {
                // Répertoire : récursion
                String subPackage = resourcePath.substring(path.length()).replace("/", ".");
                scanDirectory(resourcePath, packageName + subPackage, classes, context);
            } else if (resourcePath.endsWith(".class")) {
                // Fichier classe
                String className = (packageName + resourcePath.substring(path.length(), resourcePath.length() - 6))
                        .replace("/", ".")
                        .replaceAll("^\\.", ""); // Nettoyer les points initiaux
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    // Ignorer les classes non chargeables (ex: anonymes ou dépendances manquantes)
                }
            }
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "/";
        if (!path.startsWith("/")) path = "/" + path;
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        return path;
    }
}