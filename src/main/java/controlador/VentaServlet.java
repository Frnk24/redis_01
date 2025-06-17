package controlador;

import modelo.Productos;
import modelo.ProductoService;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(name = "VentaServlet", urlPatterns = {"/VentaServlet"})
public class VentaServlet extends HttpServlet {


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        ProductoService productoService = (ProductoService) getServletContext().getAttribute("productoService");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String mensaje;
        boolean exito = false;

        try {
            int idProducto = Integer.parseInt(request.getParameter("idProducto"));
            int cantidad = Integer.parseInt(request.getParameter("cantidad"));

            Productos productoAntesDeVenta = productoService.obtenerProductoPorId(idProducto);
            String nombreProducto = (productoAntesDeVenta != null) ? productoAntesDeVenta.getNombre() : "Producto Desconocido";

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
            e.printStackTrace();
        }

        RespuestaJson respuesta = new RespuestaJson(exito, mensaje);
        String jsonResponse = new com.google.gson.Gson().toJson(respuesta);
        
        try (PrintWriter out = response.getWriter()) {
            out.print(jsonResponse);
            out.flush();
        }
    }

    private static class RespuestaJson {
        boolean exito;
        String mensaje;

        public RespuestaJson(boolean exito, String mensaje) {
            this.exito = exito;
            this.mensaje = mensaje;
        }
    }
    
    // NO hay método destroy()
}