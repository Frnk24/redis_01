
package controlador;


import com.google.gson.Gson;
import com.mycompany.ventasredislimpio.ProductosJpaController;
import modelo.Productos;
import util.JPAUtil;
import util.RedisConexion;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.List;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import modelo.ProductoService;
import redis.clients.jedis.Jedis;

@WebServlet(name = "AdminProductoServlet", urlPatterns = {"/admin/productos"})
public class AdminProductoServlet extends HttpServlet {

    private final Gson gson = new Gson();

   

    // GET se usará para obtener la lista de todos los productos
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ProductosJpaController productoController = (ProductosJpaController) getServletContext().getAttribute("productoController");
        List<Productos> listaProductos = productoController.findProductosEntities();
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.print(gson.toJson(listaProductos));
            out.flush();
        }
    }

    // POST se usará para actualizar un producto existente
    @Override
protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    ProductosJpaController productoController = (ProductosJpaController) getServletContext().getAttribute("productoController");
    boolean exito = false;
    String mensaje = "Error desconocido.";
    String idParam = request.getParameter("id");
    
    try {
        // Obtenemos los datos del formulario
        String nombre = request.getParameter("nombre");
        BigDecimal precio = new BigDecimal(request.getParameter("precio"));
        int stock = Integer.parseInt(request.getParameter("stock"));

        if (idParam != null && !idParam.isEmpty() && !idParam.equals("0")) {
            // --- LÓGICA DE ACTUALIZACIÓN ---
            int id = Integer.parseInt(idParam);
            Productos productoAActualizar = productoController.findProductos(id);
            if (productoAActualizar != null) {
                // Actualizamos los valores del objeto
                productoAActualizar.setNombre(nombre);
                productoAActualizar.setPrecio(precio);
                productoAActualizar.setStock(stock);
                
                // Guardamos los cambios en la base de datos
                productoController.edit(productoAActualizar);
                
                // Invalidamos la caché de este producto en Redis
                try (Jedis jedis = RedisConexion.getJedis()) {
                    String redisKey = "producto:" + id;
                    jedis.del(redisKey);
                }

                // ¡Notificamos a los clientes a través del WebSocket!
                
                ProductoWebSocketServer.notificarActualizacion();
                
                exito = true;
                mensaje = "Producto actualizado correctamente.";
            } else {
                mensaje = "El producto no fue encontrado para actualizar.";
            }
        } else {
            // --- LÓGICA DE CREACIÓN ---
            Productos nuevoProducto = new Productos();
            nuevoProducto.setNombre(nombre);
            nuevoProducto.setPrecio(precio);
            nuevoProducto.setStock(stock);
            
            // Creamos el nuevo producto en la base de datos
            productoController.create(nuevoProducto);
            
            // ¡Notificamos a los clientes a través del WebSocket!
            ProductoWebSocketServer.notificarActualizacion();
            
            exito = true;
            mensaje = "Producto creado exitosamente.";
        }

    } catch (NumberFormatException e) {
        mensaje = "Error en el formato de los números (precio, stock). Por favor, use '.' para decimales.";
        e.printStackTrace();
    } catch (Exception e) {
        mensaje = "Ocurrió un error en la operación del servidor.";
        e.printStackTrace();
    }

    // Enviamos la respuesta JSON al frontend
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(String.format("{\"exito\": %b, \"mensaje\": \"%s\"}", exito, mensaje));
}
}