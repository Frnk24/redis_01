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
import modelo.ProductoService;

@WebServlet(name = "RegistroServlet", urlPatterns = {"/RegistroServlet"})
public class RegistroServlet extends HttpServlet {

    
    private UsuariosJpaController usuarioController;

    
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ProductoService productoService = (ProductoService) getServletContext().getAttribute("productoService");
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
        } else {
            // Verificamos si el email ya existe en la base de datos
            Usuarios usuarioExistente = null;
            try {
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
                
                // IMPORTANTE: Guardamos la contraseña en texto plano como acordamos.
                // En un sistema real, aquí iría el hashing con BCrypt.
                nuevoUsuario.setPassword(password); 
                
                nuevoUsuario.setRol("cliente"); // Todos los nuevos usuarios son clientes.
                
                try {
                    usuarioController.create(nuevoUsuario);
                    exito = true;
                    mensaje = "¡Registro exitoso! Serás redirigido al login en 2 segundos.";
                } catch (Exception e) {
                    mensaje = "Ocurrió un error al crear la cuenta.";
                    e.printStackTrace();
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