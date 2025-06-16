
package controlador;

import com.google.gson.Gson;
import modelo.Productos;
import modelo.ProductoService;
import util.JPAUtil;
import util.RedisConexion;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Tuple;

@WebServlet(name = "RankingServlet", urlPatterns = {"/ranking"})
public class RankingServlet extends HttpServlet {

    
    private final Gson gson = new Gson();

    
    /// En RankingServlet.java
@Override
protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
    
    ProductoService productoService = (ProductoService) getServletContext().getAttribute("productoService");
    
    if (productoService == null) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Servicio no disponible.");
        return;
    }
    
    List<RankingItem> rankingConDetalles = new ArrayList<>();
    
    try (Jedis jedis = RedisConexion.getJedis()) {
        List<Tuple> topProductos = jedis.zrevrangeWithScores("ranking:mas_vendidos", 0, 4);
        
        for (Tuple tupla : topProductos) {
            try {
                String productoKey = tupla.getElement();
                long ventas = (long) tupla.getScore();
                int productoId = Integer.parseInt(productoKey.split(":")[1]);
                
                Productos producto = productoService.obtenerProductoPorId(productoId);
                
                // Doble validación: nos aseguramos de que el producto y su nombre no son nulos
                if (producto != null && producto.getNombre() != null) {
                    rankingConDetalles.add(new RankingItem(producto.getNombre(), ventas));
                } else {
                System.err.println("ADVERTENCIA: Omitiendo producto del ranking (ID: " + productoId + ") porque no se encontró o es inválido.");
                }
            } catch (Exception e) {
                System.err.println("Error procesando un item del ranking: " + tupla.getElement() + ". Error: " + e.getMessage());
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error al obtener datos del ranking.");
        return;
    }
    
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    try (PrintWriter out = response.getWriter()) {
        out.print(gson.toJson(rankingConDetalles));
        out.flush();
    }
}

// Clase auxiliar FUERA de doGet, para una estructura JSON limpia y tipada.
// Esto es mejor que un objeto anónimo.
private static class RankingItem {
    String nombre;
    long totalVentas;

    public RankingItem(String nombre, long totalVentas) {
        this.nombre = nombre;
        this.totalVentas = totalVentas;
    }
}
}