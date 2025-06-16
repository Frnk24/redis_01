package controlador;

import com.mycompany.ventasredislimpio.UsuariosJpaController;
import modelo.Usuarios;
import util.JPAUtil;
import java.io.IOException;
import java.io.PrintWriter;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// Quité 'ProductoService' porque no se usaba en este servlet.

@WebServlet(name = "RegistroServlet", urlPatterns = {"/RegistroServlet"})
public class RegistroServlet extends HttpServlet {

    // Quitamos la variable de instancia, no es necesaria aquí.
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        // === LA LÍNEA MÁGICA DE LA SOLUCIÓN ===
        // Obtenemos el controlador del ServletContext justo cuando lo necesitamos.
        UsuariosJpaController usuarioController = (UsuariosJpaController) getServletContext().getAttribute("usuarioController");
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        String nombre = request.getParameter("nombre");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        
        boolean exito = false;
        String mensaje;
        
        // --- Validación ---
        if (nombre == null || email == null || password == null || 
            nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
            mensaje = "Todos los campos son obligatorios.";
        } else if (usuarioController == null) {
            // Una validación extra por si el controlador no se inicializó.
            mensaje = "Error crítico: el controlador de usuarios no está disponible.";
        } else {
            // Verificamos si el email ya existe en la base de datos
            Usuarios usuarioExistente = null;
            try {
                // Ahora 'usuarioController' SÍ tiene un valor y no dará error.
                TypedQuery<Usuarios> query = usuarioController.getEntityManager().createNamedQuery("Usuarios.findByEmail", Usuarios.class);
                query.setParameter("email", email);
                usuarioExistente = query.getSingleResult();
            } catch (NoResultException e) {
                // Es bueno que no exista, significa que el email está disponible.
            }
            
            if (usuarioExistente != null) {
                mensaje = "El correo electrónico ya está en uso. Por favor, elija otro.";
            } else {
                // --- Creación del Usuario ---
                Usuarios nuevoUsuario = new Usuarios();
                nuevoUsuario.setNombre(nombre);
                nuevoUsuario.setEmail(email);
                
                // Aquí deberías usar jBCrypt, pero lo dejamos con texto plano como está.
                nuevoUsuario.setPassword(password); 
                
                nuevoUsuario.setRol("cliente");
                
                try {
                    usuarioController.create(nuevoUsuario);
                    exito = true;
                    mensaje = "¡Registro exitoso! Serás redirigido al login en 2 segundos.";
                } catch (Exception e) {
                    mensaje = "Ocurrió un error al crear la cuenta en la base de datos.";
                    e.printStackTrace(); // Muy importante para ver errores de BD en la consola.
                }
            }
        }
        
        // Enviamos la respuesta JSON
        try (PrintWriter out = response.getWriter()) {
            out.print(String.format("{\"exito\": %b, \"mensaje\": \"%s\"}", exito, mensaje));
            out.flush();
        }
    }
}