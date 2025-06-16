
package controlador;

import com.google.gson.Gson;
import modelo.Productos;
import modelo.Usuarios;
import modelo.ProductoService;
import util.JPAUtil;
import util.RedisConexion;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import redis.clients.jedis.Jedis;

@WebServlet(name = "CarritoServlet", urlPatterns = {"/CarritoServlet"})
public class CarritoServlet extends HttpServlet {

    private final Gson gson = new Gson();

    
    
    // El método GET se usará para OBTENER el contenido del carrito.
    @Override
protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    
    ProductoService productoService = (ProductoService) getServletContext().getAttribute("productoService");
    HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute("usuario") == null) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No ha iniciado sesión.");
        return;
    }

    Usuarios usuario = (Usuarios) session.getAttribute("usuario");
    String carritoKey = "carrito:" + usuario.getId();
    
    // Usaremos esta clase interna para una respuesta más limpia
    List<ItemCarrito> carritoConDetalles = new ArrayList<>();

    try (Jedis jedis = RedisConexion.getJedis()) {
        Map<String, String> itemsEnCarrito = jedis.hgetAll(carritoKey);
        for (Map.Entry<String, String> item : itemsEnCarrito.entrySet()) {
            try {
                int productoId = Integer.parseInt(item.getKey().split(":")[1]);
                int cantidad = Integer.parseInt(item.getValue());

                Productos producto = productoService.obtenerProductoPorId(productoId);
                
                // ¡AQUÍ ESTÁ LA VALIDACIÓN!
                // Si el producto no es nulo (es decir, existe en la BD), lo procesamos.
                // Si es nulo (quizás fue eliminado de la tienda), simplemente lo ignoramos.
                if (producto != null) {
                    carritoConDetalles.add(new ItemCarrito(producto, cantidad));
                } else {
                    System.out.println("ADVERTENCIA: El producto con ID " + productoId + 
                                       " está en el carrito pero no se encontró en la base de datos. Omitiendo.");
                }
            } catch (Exception e) {
                // Capturamos cualquier error al procesar un solo item para no romper todo el bucle
                System.err.println("Error procesando un item del carrito: " + e.getMessage());
            }
        }
    }
    
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    try (PrintWriter out = response.getWriter()) {
        out.print(gson.toJson(carritoConDetalles));
        out.flush();
    }
}


    // El método POST se usará para AÑADIR un producto al carrito.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        // Verificamos que el usuario haya iniciado sesión
        if (session == null || session.getAttribute("usuario") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Debe iniciar sesión para comprar.");
            return;
        }
        
        try {
            int idProducto = Integer.parseInt(request.getParameter("idProducto"));
            int cantidad = Integer.parseInt(request.getParameter("cantidad"));
            
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            String carritoKey = "carrito:" + usuario.getId(); // Clave única por usuario
            String productoField = "producto:" + idProducto; // Campo dentro del Hash
            
            try (Jedis jedis = RedisConexion.getJedis()) {
                // HINCRBY es perfecto: si el producto ya está en el carrito, suma la cantidad.
                // Si no está, lo crea con la cantidad especificada.
                jedis.hincrBy(carritoKey, productoField, cantidad);
            }
            
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"exito\": true, \"mensaje\": \"Producto añadido al carrito\"}");
            
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Datos de producto o cantidad inválidos.");
        }
    }
    // Añade este método a CarritoServlet.java

@Override
protected void doDelete(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    
    HttpSession session = request.getSession(false);
    if (session == null || session.getAttribute("usuario") == null) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No ha iniciado sesión.");
        return;
    }
    
    Usuarios usuario = (Usuarios) session.getAttribute("usuario");
    String carritoKey = "carrito:" + usuario.getId();
    String productoIdParam = request.getParameter("idProducto");
    
    try (Jedis jedis = RedisConexion.getJedis()) {
        if (productoIdParam != null && !productoIdParam.isEmpty()) {
            // Caso 1: Eliminar UN SOLO producto del carrito
            String productoField = "producto:" + productoIdParam;
            jedis.hdel(carritoKey, productoField); // hdel elimina un campo específico de un hash
        } else {
            // Caso 2: No se especifica un ID, se asume que se quiere vaciar TODO el carrito
            jedis.del(carritoKey); // del elimina la clave completa
        }
    }
    
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write("{\"exito\": true, \"mensaje\": \"Carrito actualizado\"}");
}
private static class ItemCarrito {
    int id; 
    String nombre;
    double precio;
    int cantidad;
    double subtotal;

    public ItemCarrito(Productos producto, int cantidad) {
        this.id = producto.getId();
        this.nombre = producto.getNombre();
        this.precio = producto.getPrecio().doubleValue();
        this.cantidad = cantidad;
        this.subtotal = this.precio * this.cantidad;
    }
}
}