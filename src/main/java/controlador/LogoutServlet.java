package controlador; // O tu paquete

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "LogoutServlet", urlPatterns = {"/LogoutServlet"})
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // 1. Obtenemos la sesión actual
        HttpSession session = request.getSession(false);
        
        // 2. Si existe una sesión, la invalidamos
        if (session != null) {
            session.invalidate();
        }
        
        // 3. Redirigimos al usuario a la página de login (index.html)
        response.sendRedirect(request.getContextPath() + "/index.html");
    }
}