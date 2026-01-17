package servlet;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import servlet.util.ControllerInfo;
import servlet.util.PathPattern;
import servlet.util.cast.UtilCast;
import servlet.util.controllers.ControllerMapping;
import servlet.util.uploads.FileManager;
import servlet.annotation.parameters.PathParam;
import servlet.annotation.parameters.RequestParam;
import servlet.annotation.parameters.SessionParam;
import servlet.models.ApiResponse;
import servlet.models.ModelView;
import servlet.annotation.json.ResponseJSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
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
        // Extraction du chemin de la requête et normalisation
        String path = extractRequestPath(req);
        String httpMethod = req.getMethod();

        // Vérification si la ressource demandée est un fichier statique
        if (isStaticResource(path)) {
            defaultServe(req, resp);
            return;
        }

        // Recherche du mapping controller correspondant au chemin
        ControllerMapping mapping = findControllerMapping(path, httpMethod);

        if (mapping != null) {
            // Traitement de la requête via le controller mappé
            handleControllerRequest(req, resp, mapping);
        } else {
            // Aucun mapping trouvé : retour d'une erreur 404 personnalisée
            customServe(req, resp);
        }
    }

    /**
     * Extrait et normalise le chemin de la requête
     * 
     * @param req La requête HTTP
     * @return Le chemin normalisé (au minimum "/")
     */
    private String extractRequestPath(HttpServletRequest req) {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        return path.isEmpty() ? "/" : path;
    }

    /**
     * Vérifie si le chemin correspond à une ressource statique
     * 
     * @param path Le chemin à vérifier
     * @return true si une ressource statique existe, false sinon
     */
    private boolean isStaticResource(String path) {
        try {
            return getServletContext().getResource(path) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Recherche le mapping controller correspondant au chemin et méthode HTTP
     * 
     * @param path       Le chemin de la requête
     * @param httpMethod La méthode HTTP (GET, POST, PUT, DELETE)
     * @return Un objet ControllerMapping contenant les infos du controller, ou null
     *         si non trouvé
     */
    private ControllerMapping findControllerMapping(String path, String httpMethod) {
        Map<PathPattern, ControllerInfo> urlMap = (Map<PathPattern, ControllerInfo>) getServletContext()
                .getAttribute("urlMap");

        if (urlMap == null) {
            return null;
        }

        // Parcours des patterns pour trouver une correspondance
        for (Entry<PathPattern, ControllerInfo> entry : urlMap.entrySet()) {
            PathPattern pattern = entry.getKey();
            if (pattern.matches(path, httpMethod)) {
                ControllerInfo info = entry.getValue();
                Map<String, String> pathParams = pattern.extractParameters(path);
                return new ControllerMapping(info, pathParams);
            }
        }

        return null;
    }

    /**
     * Traite la requête en invoquant la méthode du controller correspondant
     * 
     * @param req     La requête HTTP
     * @param resp    La réponse HTTP
     * @param mapping Le mapping contenant les informations du controller
     */
    private void handleControllerRequest(HttpServletRequest req, HttpServletResponse resp, ControllerMapping mapping)
            throws ServletException, IOException {

        ControllerInfo info = mapping.getControllerInfo();
        Method method = info.getMethod();

        try {
            // Création d'une instance du controller
            Object controllerInstance = info.getControllerClass().getDeclaredConstructor().newInstance();

            // Préparation des arguments de la méthode à partir de la requête
            Object[] args = prepareMethodArguments(req, method, mapping.getPathParams());

            // Invocation de la méthode du controller
            Object returnObject = method.invoke(controllerInstance, args);

            // Traitement du résultat retourné par la méthode
            handleMethodReturn(req, resp, method, returnObject, info);

        } catch (InstantiationException | IllegalArgumentException | NoSuchMethodException | SecurityException e) {
            System.err.println("Erreur lors de la création de l'instance du controller : " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.err.println("Erreur lors de l'invocation de la méthode : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Prépare les arguments nécessaires à l'invocation de la méthode du controller
     * 
     * @param req        La requête HTTP
     * @param method     La méthode à invoquer
     * @param pathParams Les paramètres extraits du chemin
     * @return Un tableau d'objets représentant les arguments de la méthode
     */
    private Object[] prepareMethodArguments(HttpServletRequest req, Method method, Map<String, String> pathParams)
            throws ServletException, IOException {

        Parameter[] methodParams = method.getParameters();
        Object[] args = new Object[methodParams.length];

        // Traitement de chaque paramètre de la méthode
        for (int i = 0; i < methodParams.length; i++) {
            Parameter param = methodParams[i];
            args[i] = resolveMethodParameter(req, param, pathParams);
        }

        return args;
    }

    /**
     * Résout la valeur d'un paramètre de méthode en fonction de ses annotations et
     * son type
     * 
     * @param req        La requête HTTP
     * @param param      Le paramètre à résoudre
     * @param pathParams Les paramètres du chemin
     * @return La valeur résolue du paramètre
     */
    private Object resolveMethodParameter(HttpServletRequest req, Parameter param, Map<String, String> pathParams)
            throws ServletException, IOException {

        // Gestion des paramètres annotés @PathParam
        if (param.isAnnotationPresent(PathParam.class)) {
            return resolvePathParam(param, pathParams);
        }

        // Gestion des paramètres annotés @RequestParam
        if (param.isAnnotationPresent(RequestParam.class)) {
            return resolveRequestParam(req, param);
        }

        // Gestion des paramètres de type Map<String, Object> ou Map<String, byte[]>
        if (param.getType() == Map.class) {
            return resolveMapParameter(req, param);
        }

        // Gestion des objets complexes (binding automatique)
        return resolveComplexObject(req, param);
    }

    /**
     * Résout un paramètre annoté @PathParam
     * 
     * @param param      Le paramètre de la méthode
     * @param pathParams La map des paramètres du chemin
     * @return La valeur convertie du paramètre
     */
    private Object resolvePathParam(Parameter param, Map<String, String> pathParams) {
        String name = param.getAnnotation(PathParam.class).value();
        String value = pathParams.get(name);

        if (value != null) {
            return UtilCast.convert(value, param.getType());
        }

        return null;
    }

    /**
     * Résout un paramètre annoté @RequestParam
     * 
     * @param req   La requête HTTP
     * @param param Le paramètre de la méthode
     * @return La valeur convertie du paramètre
     */
    private Object resolveRequestParam(HttpServletRequest req, Parameter param) {
        String paramName = param.getAnnotation(RequestParam.class).value();
        String value = req.getParameter(paramName);

        if (value != null && !value.isEmpty()) {
            return UtilCast.convert(value, param.getType());
        }

        // Valeur par défaut pour les String vides
        return param.getType() == String.class ? "" : null;
    }

    /**
     * Résout un paramètre de type Map (session, paramètres, ou fichiers)
     * 
     * @param req   La requête HTTP
     * @param param Le paramètre de la méthode
     * @return Une Map contenant les données appropriées
     */
    private Object resolveMapParameter(HttpServletRequest req, Parameter param) throws ServletException, IOException {
        Type genericType = param.getParameterizedType();

        if (!(genericType instanceof ParameterizedType)) {
            return null;
        }

        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Type[] typeArgs = parameterizedType.getActualTypeArguments();

        if (typeArgs.length != 2 || typeArgs[0] != String.class) {
            return null;
        }

        // Gestion Map<String, Object> pour paramètres ou session
        if (typeArgs[1] == Object.class) {
            if (param.isAnnotationPresent(SessionParam.class)) {
                return extractSessionAttributes(req);
            } else {
                return extractRequestParameters(req);
            }
        }

        // Gestion Map<String, byte[]> pour les fichiers uploadés
        if (typeArgs[1] == byte[].class) {
            return extractFileUploads(req);
        }

        return null;
    }

    /**
     * Extrait tous les attributs de session dans une Map
     * 
     * @param req La requête HTTP
     * @return Une Map contenant tous les attributs de session
     */
    private Map<String, Object> extractSessionAttributes(HttpServletRequest req) {
        HttpSession session = req.getSession();
        Map<String, Object> sessionMap = new HashMap<>();

        Enumeration<String> attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            String key = attributeNames.nextElement();
            Object value = session.getAttribute(key);
            sessionMap.put(key, value);
            System.out.println("Clé de session : " + key + " = " + value);
        }

        return sessionMap;
    }

    /**
     * Extrait tous les paramètres de la requête dans une Map
     * 
     * @param req La requête HTTP
     * @return Une Map contenant tous les paramètres
     */
    private Map<String, Object> extractRequestParameters(HttpServletRequest req) {
        Map<String, String[]> parameterMap = req.getParameterMap();
        Map<String, Object> paramMap = new HashMap<>();

        for (Entry<String, String[]> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();

            if (values != null && values.length == 1) {
                paramMap.put(key, values[0]); // Valeur unique
            } else {
                paramMap.put(key, values); // Valeurs multiples
            }

            System.out.println("Clé : " + key + " = [" + String.join(",", values) + "]");
        }

        return paramMap;
    }

    /**
     * Extrait les fichiers uploadés dans une Map<String, byte[]>
     * 
     * @param req La requête HTTP
     * @return Une Map contenant les fichiers et leurs contenus
     */
    private Map<String, byte[]> extractFileUploads(HttpServletRequest req) throws ServletException, IOException {
        Collection<Part> parts = req.getParts();
        Map<String, byte[]> fileMap = new HashMap<>();

        for (Part part : parts) {
            byte[] fileBytes = part.getInputStream().readAllBytes();

            // Vérification et sauvegarde uniquement des vrais fichiers
            String fileName = FileManager.getFileName(part);
            if (fileName != null && !fileName.isEmpty()) {
                FileManager.saveToDisk(req, part, fileBytes);
            }

            fileMap.put(part.getName(), fileBytes);
        }

        return fileMap;
    }

    /**
     * Résout un objet complexe via binding automatique des propriétés (Entité issue
     * de front)
     * 
     * @param req   La requête HTTP
     * @param param Le paramètre de la méthode
     * @return L'instance de l'objet avec ses propriétés remplies
     */
    private Object resolveComplexObject(HttpServletRequest req, Parameter param) {
        Class<?> paramType = param.getType();

        // Ignorer les types primitifs et classes Java standard
        if (paramType.getName().startsWith("java.") || paramType.isPrimitive() ||
                paramType == LocalDate.class || paramType.isEnum()) {
            return null;
        }

        try {
            // Création de l'instance de l'objet complexe
            Object instance = paramType.getDeclaredConstructor().newInstance();
            Map<String, String[]> parameterMap = req.getParameterMap();

            // Remplissage des propriétés de l'objet
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String key = entry.getKey();
                String[] values = entry.getValue();

                if (values == null || values.length == 0) {
                    continue;
                }

                // Gestion des tableaux (paramètres avec [])
                if (key.contains("[]")) {
                    bindArrayProperty(instance, key, values, paramType);
                } else if (values.length == 1) {
                    // Gestion des valeurs simples
                    bindSingleProperty(instance, key, values[0], paramType);
                } else {
                    // Valeurs multiples sans [] : prendre la première
                    bindSingleProperty(instance, key, values[0], paramType);
                }
            }

            return instance;

        } catch (Exception e) {
            System.err.println("Erreur binding objet complexe : " + paramType.getName() + " → " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Lie une propriété tableau à l'objet (ex: couleurs[])
     * 
     * @param instance  L'instance de l'objet
     * @param key       La clé du paramètre (avec [])
     * @param values    Les valeurs du tableau
     * @param paramType Le type de l'objet
     */
    private void bindArrayProperty(Object instance, String key, String[] values, Class<?> paramType) {
        String arrayKey = key.replace("[]", "");

        // Filtrage des valeurs non vides
        String[] nonEmptyValues = Arrays.stream(values)
                .filter(v -> v != null && !v.isEmpty())
                .toArray(String[]::new);

        if (nonEmptyValues.length > 0) {
            UtilCast.setPropertyValue(instance, arrayKey, nonEmptyValues, paramType);
        }
    }

    /**
     * Lie une propriété simple à l'objet
     * 
     * @param instance  L'instance de l'objet
     * @param key       La clé du paramètre
     * @param value     La valeur du paramètre
     * @param paramType Le type de l'objet
     */
    private void bindSingleProperty(Object instance, String key, String value, Class<?> paramType) {
        if (value != null && !value.isEmpty()) {
            UtilCast.setPropertyValue(instance, key, value, paramType);
        }
    }

    /**
     * Traite le retour de la méthode du controller (JSON, String, ou ModelView)
     * 
     * @param req          La requête HTTP
     * @param resp         La réponse HTTP
     * @param method       La méthode invoquée
     * @param returnObject L'objet retourné par la méthode
     * @param info         Les informations du controller
     */
    private void handleMethodReturn(HttpServletRequest req, HttpServletResponse resp, Method method,
            Object returnObject, ControllerInfo info) throws ServletException, IOException {

        // Réponse JSON si l'annotation @ResponseJSON est présente
        if (method.isAnnotationPresent(ResponseJSON.class)) {
            handleJSONResponse(resp, returnObject);
            return;
        }

        // Réponse texte simple pour les String
        if (returnObject instanceof String) {
            handleStringResponse(resp, returnObject, info);
            return;
        }

        // Réponse avec vue pour les ModelView
        if (returnObject instanceof ModelView) {
            ModelView mv = (ModelView) returnObject;
            processModelView(req, resp, mv, method);
            return;
        }

        // Type de retour non supporté
        handleUnsupportedReturnType(resp);
    }

    /**
     * Traite une réponse JSON avec l'annotation @ResponseJSON
     * 
     * @param resp         La réponse HTTP
     * @param returnObject L'objet à sérialiser en JSON
     */
    private void handleJSONResponse(HttpServletResponse resp, Object returnObject) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        try {
            // Construction de la réponse API
            ApiResponse<Object> apiResponse = buildApiResponse(returnObject);
            String json = mapper.writeValueAsString(apiResponse);
            out.print(json);

        } catch (Exception e) {
            // Gestion des erreurs de sérialisation
            handleJSONError(resp, out, mapper, e);
        }

        out.flush();
    }

    /**
     * Construit une réponse API standardisée
     * 
     * @param returnObject L'objet retourné par le controller
     * @return Une ApiResponse encapsulant le résultat
     */
    private ApiResponse<Object> buildApiResponse(Object returnObject) {
        if (returnObject == null) {
            return new ApiResponse<>("success", 200, null);
        }

        if (returnObject instanceof ModelView) {
            ModelView mv = (ModelView) returnObject;
            return new ApiResponse<>("success", 200, mv.getData());
        }

        return new ApiResponse<>("success", 200, returnObject);
    }

    /**
     * Gère les erreurs de sérialisation JSON
     * 
     * @param resp   La réponse HTTP
     * @param out    Le writer de sortie
     * @param mapper L'ObjectMapper JSON
     * @param e      L'exception rencontrée
     */
    private void handleJSONError(HttpServletResponse resp, PrintWriter out, ObjectMapper mapper, Exception e) {
        ApiResponse<String> error = new ApiResponse<>("error", 500,
                "Erreur serveur : " + e.getMessage());

        try {
            out.print(mapper.writeValueAsString(error));
            resp.setStatus(500);
        } catch (Exception ex) {
            out.print("{\"status\":\"error\",\"code\":500,\"message\":\"Erreur génération JSON.\"}");
        }
    }

    /**
     * Traite une réponse de type String
     * 
     * @param resp         La réponse HTTP
     * @param returnObject L'objet String retourné
     * @param info         Les informations du controller
     */
    private void handleStringResponse(HttpServletResponse resp, Object returnObject, ControllerInfo info)
            throws IOException {

        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();

        out.println("Controller: " + info.getControllerClass().getName());
        out.println("Method name: " + info.getMethod().getName());
        out.println("Retour de la méthode du controller (String) : " + returnObject);
    }

    /**
     * Traite les types de retour non supportés
     * 
     * @param resp La réponse HTTP
     */
    private void handleUnsupportedReturnType(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.println("Le type de retour de la méthode du controller n'est ni de type ModelView ni String!");
    }

    // Affectation des paramètres du ModelView aux attributs responses pour le
    // dispatch
    private void processModelView(HttpServletRequest req, HttpServletResponse resp, ModelView mv, Method method)
            throws ServletException, IOException {
        if (!mv.getData().isEmpty()) {
            // Récupération des noms de paramètres Map<String,Object> annotés @SessionParam
            HttpSession session = req.getSession();

            for (String key : mv.getData().keySet()) {
                Object value = mv.getData().get(key);
                System.out.println("\n Clé du ModelView : " + key + " = " + value);

                // Vérifier si la clé correspond à un paramètre session Map<String,Object>
                if (key.compareTo("sessionData") == 0 && value instanceof Map) {
                    System.out.println("\n Variable de session trouvée dans le ModelView \n");
                    try {
                        Map<String, Object> dataMap = (Map<String, Object>) value;

                        // Nettoyer la session
                        Enumeration<String> attributeNames = session.getAttributeNames();
                        while (attributeNames.hasMoreElements()) {
                            session.removeAttribute(attributeNames.nextElement());
                        }

                        // Ajouter les nouvelles données en session
                        for (Map.Entry<String, Object> entry : dataMap.entrySet()) {
                            session.setAttribute(entry.getKey(), entry.getValue());
                        }
                    } catch (ClassCastException e) {
                        // Si ce n'est pas une Map<String,Object>, ignorer
                        req.setAttribute(key, value);
                    }
                    req.setAttribute(key, value);
                } else {
                    // Comportement normal : ajouter comme attribut de requête
                    req.setAttribute(key, value);
                }
            }
        }
        resp.setContentType("text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");
        RequestDispatcher dispatcher = req.getRequestDispatcher("/" + mv.getView());
        dispatcher.forward(req, resp);
    }

    /**
     * Récupère les noms des paramètres de type Map<String,Object>
     * annotés @SessionParam
     * 
     * @param method La méthode du controller
     * @return Une liste avec les noms des paramètres session
     */
    // private List<String> getSessionMapParameterNames(Method method) {
    //     List<String> sessionParams = new ArrayList<>();
    //     Parameter[] parameters = method.getParameters();

    //     for (Parameter param : parameters) {
    //         if (param.getType() == Map.class && param.isAnnotationPresent(SessionParam.class)) {
    //             Type genericType = param.getParameterizedType();

    //             if (genericType instanceof ParameterizedType) {
    //                 ParameterizedType parameterizedType = (ParameterizedType) genericType;
    //                 Type[] typeArgs = parameterizedType.getActualTypeArguments();

    //                 // Vérifier si c'est bien Map<String, Object>
    //                 if (typeArgs.length == 2 && typeArgs[0] == String.class && typeArgs[1] == Object.class) {
    //                     sessionParams.add(param.getName());
    //                 }
    //             }
    //         }
    //     }

    //     return sessionParams;
    // }

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
                    """.formatted(uri, httpMethod);

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