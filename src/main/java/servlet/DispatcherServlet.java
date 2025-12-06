package servlet;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.util.ControllerInfo;
import servlet.util.PathPattern;
import servlet.util.cast.UtilCast;
import servlet.annotation.parameters.PathParam;
import servlet.annotation.parameters.RequestParam;
import servlet.models.ModelView;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class DispatcherServlet extends HttpServlet {

    private RequestDispatcher defaultDispatcher;

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        String httpMethod = req.getMethod(); // GET, POST, PUT, DELETE
        if (path.isEmpty()) {
            path = "/"; // Gérer la racine
        }

        // Vérifier si c'est une ressource statique
        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            defaultServe(req, resp);
        } else {
            // Pas de ressource statique : vérifier les mappings de controllers
            Map<PathPattern, ControllerInfo> urlMap = 
                (Map<PathPattern, ControllerInfo>) getServletContext().getAttribute("urlMap");

            ControllerInfo info = null;
            Map<String, String> pathParams = null;

            for (Entry<PathPattern, ControllerInfo> entry : urlMap.entrySet()) {
                PathPattern pattern = entry.getKey();
                if (pattern.matches(path,httpMethod)) {
                    info = entry.getValue();
                    pathParams = pattern.extractParameters(path);
                    break;
                }
            }
            
            if (info != null) {
                // Mapping trouvé : afficher infos controller/méthode                

                // Valeur de retour de la méthode 
                Method methodURL = info.getMethod();
                try {
                    Object controllerInstance = info.getControllerClass().getDeclaredConstructor().newInstance();

                    // Préparer les arguments pour la méthode
                    Parameter[] methodParams = methodURL.getParameters();
                    Object[] args = new Object[methodParams.length];

                    for (int i = 0; i < methodParams.length; i++) {
                        Parameter param = methodParams[i];
                        Object argValue = null;
                        

                        if (param.isAnnotationPresent(PathParam.class)) {
                            // @PathParam fonctionnant
                            String name = param.getAnnotation(PathParam.class).value();
                            String value = pathParams.get(name);

                            if (value != null) {
                                argValue = UtilCast.convert(value, param.getType());
                            }

                        } else if (param.isAnnotationPresent(RequestParam.class)) {
                            // @RequestParam
                            String paramName = param.getAnnotation(RequestParam.class).value();
                            String value = req.getParameter(paramName);  // <-- vient du formulaire ou query string

                            if (value != null && !value.isEmpty()) {
                                argValue = UtilCast.convert(value, param.getType());
                            } else {
                                // Gérer required + defaultValue plus tard
                                if (param.getType() == String.class) {
                                    argValue = "";
                                }
                            }
                        } else if (param.getType() == Map.class) {
                            // Si l'argument de la méthode d'action est de class java.util.Map
                            Type genericType = param.getParameterizedType();
                            if (genericType instanceof ParameterizedType) {
                                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                                Type[] typeArgs = parameterizedType.getActualTypeArguments();
                                // Si il y'a les 2 types génériques <String,Object> dans le Map<String,Object>
                                if(typeArgs.length == 2 && typeArgs[0] == String.class && typeArgs[1] == Object.class) {
                                    // Réception de tous les paramètres de la requête dans une Map<String,String[]>
                                    Map<String, String[]> parameterMap = req.getParameterMap();
                                    Map<String, Object> paramMap = new HashMap<>();
                                    for (Entry<String, String[]> entry : parameterMap.entrySet()) {
                                        String key = entry.getKey();
                                        String[] values = entry.getValue();
                                        if(values != null && values.length == 1) {
                                            paramMap.put(key, values[0]); // Valeur unique
                                        } else {
                                            paramMap.put(key, values); // Valeurs multiples
                                        }
                                        System.out.println("Clé : " + key + " = [" + String.join(",", values) + "]");
                                    }
                                    argValue = paramMap;
                                    Map<String, Object> debugMap = (Map<String, Object>) argValue;
                                    for (String key : debugMap.keySet()) {
                                        Object val = debugMap.get(key);
                                        System.out.print("Clé dans Map<String,Object> : " + key + " = " + val);
                                    }
                                }
                            }
                        } else {
                            // === GESTION OBJET COMPLEXE (avec support tableaux via []) ===
                            Class<?> paramType = param.getType();
                            if (paramType.getName().startsWith("java.") || paramType.isPrimitive() || 
                                paramType == LocalDate.class || paramType.isEnum()) {
                                continue;
                            }

                            try {
                                Object instance = paramType.getDeclaredConstructor().newInstance();
                                Map<String, String[]> parameterMap = req.getParameterMap();

                                for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                                    String key = entry.getKey();
                                    String[] values = entry.getValue();
                                    if (values == null || values.length == 0) continue;

                                    if (key.contains("[]")) {
                                        // === CAS TABLEAU : couleurs[] → String[] couleurs ===
                                        String arrayKey = key.replace("[]", "");
                                        String[] nonEmptyValues = Arrays.stream(values)
                                            .filter(v -> v != null && !v.isEmpty())
                                            .toArray(String[]::new);

                                        if (nonEmptyValues.length > 0) {
                                            UtilCast.setPropertyValue(instance, arrayKey, nonEmptyValues, paramType);
                                        }
                                    } else if (values.length == 1) {
                                        // === CAS SIMPLE : une seule valeur ===
                                        String value = values[0];
                                        if (value != null && !value.isEmpty()) {
                                            UtilCast.setPropertyValue(instance, key, value, paramType);
                                        }
                                    } else {
                                        // === CAS MULTI (rare, mais possible) sans [] : on prend la première valeur ===
                                        String value = values[0];
                                        if (value != null && !value.isEmpty()) {
                                            UtilCast.setPropertyValue(instance, key, value, paramType);
                                        }
                                    }
                                }

                                argValue = instance;

                            } catch (Exception e) {
                                System.err.println("Erreur binding objet complexe : " + paramType.getName() + " → " + e.getMessage());
                                e.printStackTrace();
                                argValue = null;
                            }
                        }

                        args[i] = argValue;
                    }

                    Object returnObject = methodURL.invoke(controllerInstance, args);
                    
                    if(returnObject instanceof String) {    // Type de retour String 
                        resp.setContentType("text/plain;charset=UTF-8");
                        PrintWriter out = resp.getWriter();
                        out.println("Controller: " + info.getControllerClass().getName());
                        out.println("Method name: " + info.getMethod().getName());
                        out.println("Retour de la méthode du controller (String) : " + returnObject);
                    } else if (returnObject instanceof ModelView) { // Type de retour ModelView
                        ModelView mv = (ModelView) returnObject;
                        processModelView(req, resp, mv);
                    } else {
                        PrintWriter out = resp.getWriter();
                        resp.setContentType("text/plain;charset=UTF-8");
                        out.println("Le type de retour de la méthode du controller n'est ni de type ModelView ni String!");
                    }
                } catch (InstantiationException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
                    System.out.println("Erreur lors de la création de l'instance du controller : " + e.getMessage());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    System.out.println("Erreur lors de l'invocation de la méthode : " + e.getMessage());
                }
            } else {
                // Ni statique ni mapping : 404 custom
                customServe(req, resp);
            }
        }
    }

    // Affectation des paramètres du ModelView aux attributs responses pour le dispatch
    private void processModelView(HttpServletRequest req, HttpServletResponse resp, ModelView mv) throws ServletException, IOException {
        if(!mv.getData().isEmpty()){
            for (String key : mv.getData().keySet()) {
                req.setAttribute(key, mv.getData().get(key));
            }
        }
        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        RequestDispatcher dispatcher = req.getRequestDispatcher("/" + mv.getView());
        dispatcher.forward(req, resp);
    }


    private void customServe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (PrintWriter out = resp.getWriter()) {
            String uri = req.getRequestURI();
            String httpMethod = req.getMethod();
            String responseBody = """
                <html>
                    <head><title>Resource Not Found</title></head>
                    <body>
                        <h1>Unknown resource</h1>
                        <p>The requested URL was not found: <strong>%s</strong> [%s]</p>
                    </body>
                </html>
                """.formatted(uri,httpMethod);

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.setContentType("text/html;charset=UTF-8");
            resp.setCharacterEncoding("UTF-8");
            out.println(responseBody);
        }
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        defaultDispatcher.forward(req, resp);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        service(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        service(req, resp);
    }
}