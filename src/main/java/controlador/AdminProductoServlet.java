package controlador;

import com.google.gson.Gson;
import com.mycompany.ventasredislimpio.ProductosJpaController;
import com.mycompany.ventasredislimpio.exceptions.NonexistentEntityException; // Importante para el manejo de errores
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

    // POST se usará para crear o actualizar un producto existente
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        ProductosJpaController productoController = (ProductosJpaController) getServletContext().getAttribute("productoController");
        boolean exito = false;
        String mensaje = "Error desconocido.";
        String idParam = request.getParameter("id");
        
        try {
            String nombre = request.getParameter("nombre");
            BigDecimal precio = new BigDecimal(request.getParameter("precio"));
            int stock = Integer.parseInt(request.getParameter("stock"));
            String imagenUrl = request.getParameter("imagenUrl");

            if (idParam != null && !idParam.isEmpty() && !idParam.equals("0")) {
                // --- LÓGICA DE ACTUALIZACIÓN ---
                int id = Integer.parseInt(idParam);
                Productos productoAActualizar = productoController.findProductos(id);
                if (productoAActualizar != null) {
                    productoAActualizar.setNombre(nombre);
                    productoAActualizar.setPrecio(precio);
                    productoAActualizar.setStock(stock);
                    productoAActualizar.setImagenUrl(imagenUrl);

                    productoController.edit(productoAActualizar);
                    
                    try (Jedis jedis = RedisConexion.getJedis()) {
                        String redisKey = "producto:" + id;
                        jedis.del(redisKey);
                    }

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
                nuevoProducto.setImagenUrl(imagenUrl);
                productoController.create(nuevoProducto);
                
                ProductoWebSocketServer.notificarActualizacion();
                
                exito = true;
                mensaje = "Producto creado exitosamente.";
            }

        } catch (NumberFormatException e) {
            mensaje = "Error en el formato de los números (precio, stock).";
            e.printStackTrace();
        } catch (Exception e) {
            mensaje = "Ocurrió un error en la operación del servidor.";
            e.printStackTrace();
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format("{\"exito\": %b, \"mensaje\": \"%s\"}", exito, mensaje));
    }

    // =======================================================================
    // ===               NUEVO MÉTODO PARA GESTIONAR DELETE                ===
    // =======================================================================
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        ProductosJpaController productoController = (ProductosJpaController) getServletContext().getAttribute("productoController");
        boolean exito = false;
        String mensaje = "Error desconocido al eliminar.";

        try {
            String idParam = request.getParameter("id");
            if (idParam != null && !idParam.isEmpty()) {
                int id = Integer.parseInt(idParam);

                // 1. Eliminar de la base de datos
                productoController.destroy(id);

                // 2. Invalidar la caché de este producto en Redis
                try (Jedis jedis = RedisConexion.getJedis()) {
                    String redisKey = "producto:" + id;
                    jedis.del(redisKey);
                }

                // 3. Notificar a los clientes a través del WebSocket
                ProductoWebSocketServer.notificarActualizacion();

                exito = true;
                mensaje = "Producto eliminado exitosamente.";

            } else {
                mensaje = "No se proporcionó un ID de producto para eliminar.";
            }

        } catch (NonexistentEntityException e) {
            // Este error ocurre si el producto ya no existe en la BD
            mensaje = "El producto no fue encontrado. Es posible que ya haya sido eliminado.";
            exito = false;
        } catch (NumberFormatException e) {
            mensaje = "El formato del ID del producto es inválido.";
        } catch (Exception e) {
            mensaje = "Ocurrió un error en el servidor al intentar eliminar el producto.";
            e.printStackTrace(); // Importante para depurar en la consola del servidor
        }
        
        // Enviamos la respuesta JSON al frontend
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format("{\"exito\": %b, \"mensaje\": \"%s\"}", exito, mensaje));
    }
}