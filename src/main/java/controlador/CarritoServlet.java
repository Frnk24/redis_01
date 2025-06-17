
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
    
    List<ItemCarrito> carritoConDetalles = new ArrayList<>();

    try (Jedis jedis = RedisConexion.getJedis()) {
        Map<String, String> itemsEnCarrito = jedis.hgetAll(carritoKey);
        for (Map.Entry<String, String> item : itemsEnCarrito.entrySet()) {
            try {
                int productoId = Integer.parseInt(item.getKey().split(":")[1]);
                int cantidad = Integer.parseInt(item.getValue());

                Productos producto = productoService.obtenerProductoPorId(productoId);
                
                
                if (producto != null) {
                    carritoConDetalles.add(new ItemCarrito(producto, cantidad));
                } else {
                    System.out.println("ADVERTENCIA: El producto con ID " + productoId + 
                                       " está en el carrito pero no se encontró en la base de datos. Omitiendo.");
                }
            } catch (Exception e) {
               
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


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("usuario") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Debe iniciar sesión para comprar.");
            return;
        }
        
        try {
            int idProducto = Integer.parseInt(request.getParameter("idProducto"));
            int cantidad = Integer.parseInt(request.getParameter("cantidad"));
            
            Usuarios usuario = (Usuarios) session.getAttribute("usuario");
            String carritoKey = "carrito:" + usuario.getId(); 
            String productoField = "producto:" + idProducto;
            
            try (Jedis jedis = RedisConexion.getJedis()) {
                // HINCRBY suma la cantidad.
                jedis.hincrBy(carritoKey, productoField, cantidad);
            }
            
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"exito\": true, \"mensaje\": \"Producto añadido al carrito\"}");
            
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Datos de producto o cantidad inválidos.");
        }
    }
    
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
            String productoField = "producto:" + productoIdParam;
            jedis.hdel(carritoKey, productoField); 
        } else {
            
            jedis.del(carritoKey); 
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