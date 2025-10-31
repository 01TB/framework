package servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import servlet.util.ControllerScanner;
import servlet.util.mapping.MappingInfo;

public class FrontServlet extends HttpServlet {

    private RequestDispatcher defaultDispatcher;
    private ControllerScanner controllerScanner;

    @Override
    public void init() throws ServletException {
        defaultDispatcher = getServletContext().getNamedDispatcher("default");
        controllerScanner = new ControllerScanner(getServletContext());
        
        try {
            // Récupérer le package de scan depuis web.xml
            String scanPackage = getServletContext().getInitParameter("scanPackage");
            
            if (scanPackage == null || scanPackage.trim().isEmpty()) {
                // Valeur par défaut si non configuré
                scanPackage = "controllers";
                getServletContext().log("Aucun package de scan configuré, utilisation par défaut: 'controllers'");
            } else {
                getServletContext().log("Package de scan configuré: " + scanPackage);
            }
            
            controllerScanner.scanControllers(scanPackage);
        } catch (Exception e) {
            throw new ServletException("Erreur lors du scan des contrôleurs", e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res) 
            throws ServletException, IOException {
        
        String path = req.getRequestURI().substring(req.getContextPath().length());
        
        // Vérifier d'abord si c'est un mapping de contrôleur
        MappingInfo mapping = controllerScanner.getMapping(path);
        
        if (mapping != null) {
            handleControllerMapping(req, res, mapping);
        } else {
            // Vérifier si c'est une ressource statique
            boolean resourceExists = getServletContext().getResource(path) != null;
            
            if (resourceExists) {
                defaultServe(req, res);
            } else {
                customServe(req, res);
            }
        }
    }

    private void handleControllerMapping(HttpServletRequest req, HttpServletResponse res, 
                                       MappingInfo mapping) throws IOException {
        try (PrintWriter out = res.getWriter()) {
            String responseBody = """
                Controller: %s
                Method: %s
                URL: %s
                """.formatted(
                    mapping.controllerClass.getSimpleName(),
                    mapping.method.getName(),
                    req.getRequestURI()
                );

            res.setContentType("text/plain;charset=UTF-8");
            out.println(responseBody);
            
            // Log dans la console serveur
            getServletContext().log("Framework: Appel de " + 
                mapping.controllerClass.getSimpleName() + "." + mapping.method.getName() + 
                " pour l'URL: " + req.getRequestURI());
        } catch (Exception e) {
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Erreur lors de l'exécution du contrôleur: " + e.getMessage());
        }
    }

    private void customServe(HttpServletRequest req, HttpServletResponse res) throws IOException {
        try (PrintWriter out = res.getWriter()) {
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

            res.setContentType("text/html;charset=UTF-8");
            out.println(responseBody);
        }
    }

    private void defaultServe(HttpServletRequest req, HttpServletResponse res) 
            throws ServletException, IOException {
        defaultDispatcher.forward(req, res);
    }
}