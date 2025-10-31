package servlet.util;

import jakarta.servlet.ServletContext;
import servlet.annotation.Controller;
import servlet.annotation.URLMapping;
import servlet.util.mapping.MappingInfo;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ControllerScanner {
    
    private final ConcurrentMap<String, MappingInfo> urlMappings = new ConcurrentHashMap<>();
    private final ServletContext servletContext;
    
    public ControllerScanner(ServletContext servletContext) {
        this.servletContext = servletContext;
    }
    
    public void scanControllers(String basePackage) throws Exception {
        servletContext.log("Début du scan des contrôleurs dans le package: " + basePackage);
        
        // Si basePackage est vide ou "*", scanner tout le classpath
        if (basePackage == null || basePackage.trim().isEmpty() || "*".equals(basePackage.trim())) {
            scanAllPackages();
        } else {
            // Scanner le package spécifique
            scanSpecificPackage(basePackage);
        }
        
        // Afficher les mappings trouvés
        servletContext.log("=== Framework Controller Mappings ===");
        if (urlMappings.isEmpty()) {
            servletContext.log("Aucun mapping trouvé!");
        } else {
            urlMappings.forEach((url, mapping) -> {
                servletContext.log(url + " -> " + mapping.controllerClass.getSimpleName() 
                    + "." + mapping.method.getName());
            });
        }
        servletContext.log("Nombre total de mappings: " + urlMappings.size());
    }
    
    private void scanAllPackages() throws Exception {
        servletContext.log("Scan de tous les packages...");
        
        // Obtenir le classloader et scanner tous les packages
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources("");
        
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (resource.getProtocol().equals("file")) {
                File rootDir = new File(resource.getFile());
                scanDirectory(rootDir, "");
            }
        }
    }
    
    private void scanSpecificPackage(String basePackage) throws Exception {
        String path = basePackage.replace('.', '/');
        Enumeration<URL> resources = Thread.currentThread()
                .getContextClassLoader()
                .getResources(path);
        
        boolean packageFound = false;
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            packageFound = true;
            File directory = new File(resource.getFile());
            if (directory.exists()) {
                scanDirectory(directory, basePackage);
            }
        }
        
        if (!packageFound) {
            servletContext.log("ATTENTION: Package non trouvé: " + basePackage);
        }
    }
    
    private void scanDirectory(File directory, String packageName) throws Exception {
        File[] files = directory.listFiles();
        if (files == null) {
            servletContext.log("Impossible de lister le répertoire: " + directory.getAbsolutePath());
            return;
        }
        
        for (File file : files) {
            if (file.isDirectory()) {
                // Scanner récursivement les sous-packages
                String newPackageName = packageName.isEmpty() ? 
                    file.getName() : packageName + '.' + file.getName();
                scanDirectory(file, newPackageName);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + 
                    file.getName().substring(0, file.getName().length() - 6);
                processClass(className);
            }
        }
    }
    
    private void processClass(String className) {
        try {
            // Ignorer les classes anonymes et internes
            if (className.contains("$")) {
                return;
            }
            
            Class<?> clazz = Class.forName(className);
            
            if (clazz.isAnnotationPresent(Controller.class)) {
                servletContext.log("Contrôleur trouvé: " + className);
                Controller controllerAnnotation = clazz.getAnnotation(Controller.class);
                String basePath = controllerAnnotation.path();
                
                // Instance du contrôleur
                Object controllerInstance = clazz.getDeclaredConstructor().newInstance();
                
                // Scanner les méthodes avec @URLMapping
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(URLMapping.class)) {
                        URLMapping mappingAnnotation = method.getAnnotation(URLMapping.class);
                        String fullUrl = normalizePath(basePath, mappingAnnotation.url());
                        
                        urlMappings.put(fullUrl, new MappingInfo(controllerInstance, method, clazz));
                        servletContext.log("Mapping URL: " + fullUrl + " -> " + 
                            clazz.getSimpleName() + "." + method.getName());
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            servletContext.log("Classe non trouvée: " + className);
        } catch (Exception e) {
            servletContext.log("Erreur lors du traitement de la classe " + className + ": " + e.getMessage());
        }
    }
    
    private String normalizePath(String basePath, String methodPath) {
        String path = basePath + methodPath;
        path = path.replace("//", "/");
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }
    
    public MappingInfo getMapping(String url) {
        return urlMappings.get(url);
    }
    
    public boolean hasMappings() {
        return !urlMappings.isEmpty();
    }
}