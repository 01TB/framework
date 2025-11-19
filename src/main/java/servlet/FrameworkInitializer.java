package servlet;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import servlet.annotation.Controller;
import servlet.annotation.URLMapping;
import servlet.util.ControllerInfo;
import servlet.util.PathPattern;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
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
                        URLMapping mappingAnno = method.getAnnotation(URLMapping.class);
                        String url = normalizePath(mappingAnno.url());
                        String fullUrl = normalizePath(basePath + url);

                        PathPattern pattern = new PathPattern(fullUrl);
                        ControllerInfo info = new ControllerInfo(clazz, method, fullUrl);
                        urlMap.put(pattern, info);
                    }
                }
            }
        }
        
        // Stocker la map dans le contexte pour l'utiliser plus tard
        context.setAttribute("urlMap", urlMap);
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