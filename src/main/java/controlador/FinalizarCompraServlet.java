package controlador;

import modelo.Usuarios;
import modelo.ProductoService;
import util.JPAUtil;
import util.RedisConexion;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import redis.clients.jedis.Jedis;

@WebServlet(name = "FinalizarCompraServlet", urlPatterns = {"/FinalizarCompraServlet"})
public class FinalizarCompraServlet extends HttpServlet {

    // Usaremos POST para esta acción, ya que modifica el estado del servidor.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ProductoService productoService = (ProductoService) getServletContext().getAttribute("productoService");
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No ha iniciado sesión.");
            return;
        }

        Usuarios usuario = (Usuarios) session.getAttribute("usuario");
        String carritoKey = "carrito:" + usuario.getId();

        String mensaje = null;
        boolean exito = false;

        try (Jedis jedis = RedisConexion.getJedis()) {
            // 1. Obtenemos el carrito del usuario desde Redis
            Map<String, String> itemsEnCarrito = jedis.hgetAll(carritoKey);

            if (itemsEnCarrito.isEmpty()) {
                mensaje = "Tu carrito está vacío.";
                exito = false;
            } else {
                // --- INICIO DE LA TRANSACCIÓN SIMULADA ---
                // En un sistema real, usaríamos una transacción de base de datos (em.getTransaction().begin()).
                // Por simplicidad, lo haremos como una secuencia de pasos.

                // 2. Verificación de stock para TODOS los productos ANTES de tocar la BD.
                boolean stockSuficiente = true;
                for (Map.Entry<String, String> item : itemsEnCarrito.entrySet()) {
                    int productoId = Integer.parseInt(item.getKey().split(":")[1]);
                    int cantidadDeseada = Integer.parseInt(item.getValue());
                    int stockActual = productoService.obtenerProductoPorId(productoId).getStock();

                    if (stockActual < cantidadDeseada) {
                        stockSuficiente = false;
                        mensaje = "No hay stock suficiente para el producto: " + productoService.obtenerProductoPorId(productoId).getNombre();
                        break; // Salimos del bucle en cuanto encontramos un producto sin stock.
                    }
                }

                // 3. Si hay stock para todo, procedemos a la venta.
                if (stockSuficiente) {
                    for (Map.Entry<String, String> item : itemsEnCarrito.entrySet()) {
                        int productoId = Integer.parseInt(item.getKey().split(":")[1]);
                        int cantidadComprada = Integer.parseInt(item.getValue());

                        // Usamos el método que ya teníamos para actualizar la BD y la caché del producto.
                        productoService.realizarVenta(productoId, cantidadComprada);
                    }

                    // 4. Limpiamos el carrito del usuario en Redis
                    jedis.del(carritoKey);

                    // Notificamos a todos los clientes conectados que hubo un cambio en los productos.
                    ProductoWebSocketServer.notificarActualizacion();

                    mensaje = "¡Compra realizada con éxito!";
                    exito = true;
                } else {
                    // Si no hubo stock, 'mensaje' ya fue establecido en el bucle de verificación.
                    exito = false;
                }
                // --- FIN DE LA TRANSACCIÓN SIMULADA ---
            }
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.print(String.format("{\"exito\": %b, \"mensaje\": \"%s\"}", exito, mensaje));
            out.flush();
        }
    }
}
