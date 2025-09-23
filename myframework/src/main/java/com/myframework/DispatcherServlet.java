package com.myframework;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class DispatcherServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        String path = uri.substring(contextPath.length());

        // Exemple simple : renvoyer l’URL capturée
        resp.setContentType("text/plain");
        resp.getWriter().println("Framework a capturé l’URL : " + path);
    }
}
