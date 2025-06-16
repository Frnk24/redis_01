package controlador;

import com.mycompany.ventasredislimpio.UsuariosJpaController;
import modelo.Usuarios;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // Obtenemos la instancia compartida del controlador
        UsuariosJpaController usuarioController = (UsuariosJpaController) getServletContext().getAttribute("usuarioController");
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        
        String mensaje = "";
        boolean exito = false;
        String redirectURL = "";
        
        try {
            // Usamos el método específico que creamos en el JpaController
            Usuarios usuario = usuarioController.findUsuarioByEmail(email);

            // Comparamos el usuario y la contraseña (en texto plano, como acordamos)
            if (usuario != null && usuario.getPassword().equals(password)) {
                // Login Exitoso
                exito = true;
                mensaje = "Login correcto. Redirigiendo...";
                
                // Creamos la sesión
                HttpSession session = request.getSession(true);
                session.setAttribute("usuario", usuario);
                session.setMaxInactiveInterval(1800); // 30 minutos
                
                // Determinamos a dónde redirigir
                if ("admin".equals(usuario.getRol())) {
                    redirectURL = "admin.html";
                } else {
                    redirectURL = "tienda.html";
                }
            } else {
                // Login Fallido
                exito = false;
                mensaje = "Correo electrónico o contraseña incorrectos.";
            }
        } catch (Exception e) {
            exito = false;
            mensaje = "Error en el servidor al intentar iniciar sesión.";
            e.printStackTrace();
        }

        // Construimos y enviamos la respuesta JSON
        String jsonResponse = String.format(
            "{\"exito\": %b, \"mensaje\": \"%s\", \"redirectURL\": \"%s\"}", 
            exito, mensaje, redirectURL
        );
        
        try (PrintWriter out = response.getWriter()) {
            out.print(jsonResponse);
            out.flush();
        }
    }
}