package controlador;

// Imports para el problema del conector de MySQL
import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.sql.Driver;

// Imports para la lógica del Servlet y la aplicación
import com.mycompany.ventasredislimpio.ProductosJpaController;
import modelo.Productos;
import modelo.ProductoService;
import util.JPAUtil;
import util.RedisConexion;
import java.io.IOException;
import java.io.PrintWriter;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "VentaServlet", urlPatterns = {"/VentaServlet"})
public class VentaServlet extends HttpServlet {

    private ProductoService productoService;

    // init() se ejecuta una sola vez cuando el servlet se carga por primera vez.
    // Ideal para inicializar recursos.
    @Override
    public void init() throws ServletException {
        EntityManagerFactory emf = JPAUtil.getEntityManagerFactory();
        ProductosJpaController jpaController = new ProductosJpaController(emf);
        this.productoService = new ProductoService(jpaController);
        
        System.out.println("VentaServlet inicializado y ProductoService creado.");
    }
    
    /**
     * Maneja las peticiones POST enviadas desde el formulario en index.html vía AJAX.
     * Procesa la venta y devuelve una respuesta en formato JSON.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Establecemos que la respuesta será de tipo JSON y codificación UTF-8
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String mensaje;
        boolean exito = false;

        try {
            // 2. Obtenemos los datos del formulario
            int idProducto = Integer.parseInt(request.getParameter("idProducto"));
            int cantidad = Integer.parseInt(request.getParameter("cantidad"));

            // Obtenemos los detalles del producto ANTES de la venta para el mensaje de respuesta
            Productos productoAntesDeVenta = productoService.obtenerProductoPorId(idProducto);
            String nombreProducto = (productoAntesDeVenta != null) ? productoAntesDeVenta.getNombre() : "Producto Desconocido";

            // 3. Llamamos al servicio para ejecutar la lógica de negocio
            if (productoService.realizarVenta(idProducto, cantidad)) {
                mensaje = "¡Venta exitosa! Se compraron " + cantidad + " unidades de '" + nombreProducto + "'.";
                exito = true;
            } else {
                mensaje = "Error: No hay stock suficiente para '" + nombreProducto + "' o el producto no existe.";
                exito = false;
            }

        } catch (Exception e) {
        mensaje = "Error interno del servidor. Revisa la consola de NetBeans.";
        exito = false;
        System.err.println("Ha ocurrido una excepción en VentaServlet:");
        e.printStackTrace(); // <--- ¡ESTO ES LO MÁS IMPORTANTE!
    }

        // 4. Creamos un objeto para estructurar nuestra respuesta
        RespuestaJson respuesta = new RespuestaJson(exito, mensaje);
        
        // 5. Usamos la librería GSON para convertir nuestro objeto Java a un String JSON
        // (Asegúrate de tener GSON en tu pom.xml)
        String jsonResponse = new com.google.gson.Gson().toJson(respuesta);
        
        // 6. Escribimos la respuesta JSON al cliente (el JavaScript en index.html)
        try (PrintWriter out = response.getWriter()) {
            out.print(jsonResponse);
            out.flush();
        }
    }

    // destroy() se ejecuta una sola vez cuando la aplicación se detiene.
    // Es crucial para liberar recursos y evitar memory leaks.
    @Override
    public void destroy() {
        System.out.println("Iniciando proceso de apagado del Servlet...");
        
        // Liberamos nuestros propios pools de conexiones
        JPAUtil.shutdown();
        //RedisConexion.shutdown();
        System.out.println("Recursos de JPA y Redis liberados.");

        // Manejamos el problema del conector de MySQL
        try {
            System.out.println("Intentando detener el hilo de limpieza de MySQL...");
            AbandonedConnectionCleanupThread.checkedShutdown();
            System.out.println("Hilo de limpieza de MySQL detenido correctamente.");
        } catch (Throwable t) {
            System.err.println("Error menor al intentar detener el hilo de limpieza de MySQL (puede ser ignorado): " + t.getMessage());
        }

        // Deregistramos el driver JDBC
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                if (driver.getClass().getClassLoader() == getClass().getClassLoader()) {
                    DriverManager.deregisterDriver(driver);
                    System.out.println("Deregistrando driver JDBC: " + driver);
                }
            } catch (SQLException e) {
                System.err.println("Error al deregistrar driver JDBC: " + driver);
            }
        }
        
        System.out.println("Proceso de apagado del Servlet completado.");
    }

    /**
     * Clase interna estática para modelar la respuesta JSON.
     * Es una buena práctica para mantener el código organizado.
     */
    private static class RespuestaJson {
        boolean exito;
        String mensaje;

        public RespuestaJson(boolean exito, String mensaje) {
            this.exito = exito;
            this.mensaje = mensaje;
        }
    }
}