package servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

public class FrontServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("text/html;charset=UTF-8");

        String path = request.getRequestURI(); // ex: /home

        // Simple routing logic
        response.getWriter().println("<html><body><h1>Framework a capturé l’URL : : "+path+"</h1></body></html>");
    }
}