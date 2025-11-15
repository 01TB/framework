package servlet;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import servlet.util.ControllerInfo;
import servlet.models.ModelView;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

public class DispatcherServlet extends HttpServlet {

    private RequestDispatcher defaultDispatcher;

    @Override
    public void init() {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getRequestURI().substring(req.getContextPath().length());
        if (path.isEmpty()) {
            path = "/"; // Gérer la racine
        }

        // Vérifier si c'est une ressource statique
        boolean resourceExists = getServletContext().getResource(path) != null;

        if (resourceExists) {
            defaultServe(req, resp);
        } else {
            // Pas de ressource statique : vérifier les mappings de controllers
            @SuppressWarnings("unchecked")
            Map<String, ControllerInfo> urlMap = (Map<String, ControllerInfo>) getServletContext().getAttribute("urlMap");

            ControllerInfo info = urlMap.get(path);
            if (info != null) {
                // Mapping trouvé : afficher infos controller/méthode
                PrintWriter out = resp.getWriter();
                out.println("Controller: " + info.getControllerClass().getName());
                out.println("Method name: " + info.getMethod().getName());

                // Valeur de retour de la méthode 
                Method methodURL = info.getMethod();
                try {
                    Object controllerNewInstance = info.getControllerClass().getDeclaredConstructor().newInstance();
                    Object returnObject = methodURL.invoke(controllerNewInstance);

                    
                    if(returnObject instanceof String) {    // Type de retour String 
                        resp.setContentType("text/plain");
                        out.println("Retour de la méthode du controller (String) : " + returnObject);
                    } else if (returnObject instanceof ModelView) { // Type de retour ModelView
                        resp.setContentType("text/html");
                        ModelView mv = (ModelView) returnObject;
                        RequestDispatcher dispatcher = req.getRequestDispatcher("/" + mv.getView());
                        dispatcher.forward(req, resp);
                    } else {
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

    private void customServe(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (PrintWriter out = resp.getWriter()) {
            String uri = req.getRequestURI();
            String responseBody = """
                <html>
                    <head><title>Resource Not Found</title></head>
                    <body>
                        <h1>Unknown resource</h1>
                        <p>The requested URL was not found: <strong>%s</strong></p>
                    </body>
                </html>
                """.formatted(uri);

            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.setContentType("text/html;charset=UTF-8");
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