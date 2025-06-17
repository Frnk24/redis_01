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
           
            Map<String, String> itemsEnCarrito = jedis.hgetAll(carritoKey);

            if (itemsEnCarrito.isEmpty()) {
                mensaje = "Tu carrito está vacío.";
                exito = false;
            } else {
                
                // 2. Verificación de stock antes BD.
                boolean stockSuficiente = true;
                for (Map.Entry<String, String> item : itemsEnCarrito.entrySet()) {
                    int productoId = Integer.parseInt(item.getKey().split(":")[1]);
                    int cantidadDeseada = Integer.parseInt(item.getValue());
                    int stockActual = productoService.obtenerProductoPorId(productoId).getStock();

                    if (stockActual < cantidadDeseada) {
                        stockSuficiente = false;
                        mensaje = "No hay stock suficiente para el producto: " + productoService.obtenerProductoPorId(productoId).getNombre();
                        break; 
                    }
                }

               
                if (stockSuficiente) {
                    for (Map.Entry<String, String> item : itemsEnCarrito.entrySet()) {
                        int productoId = Integer.parseInt(item.getKey().split(":")[1]);
                        int cantidadComprada = Integer.parseInt(item.getValue());

                        
                        productoService.realizarVenta(productoId, cantidadComprada);
                        // Le decimos a Redis que incremente el score del producto vendido
        jedis.zincrby("ranking:mas_vendidos", cantidadComprada, "producto:" + productoId);
                    }

                    jedis.del(carritoKey);

                    ProductoWebSocketServer.notificarActualizacion();

                    mensaje = "¡Compra realizada con éxito!";
                    exito = true;
                } else {
                    exito = false;
                }
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
