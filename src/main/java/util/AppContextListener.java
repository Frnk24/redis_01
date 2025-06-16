package util;

import controlador.ProductoWebSocketServer;
import com.mycompany.ventasredislimpio.ProductosJpaController;
import com.mycompany.ventasredislimpio.UsuariosJpaController;
import modelo.ProductoService;
import javax.persistence.EntityManagerFactory;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println(">>> INICIALIZANDO CONTEXTO DE LA APLICACIÓN <<<");
        
        // 1. Obtenemos el EntityManagerFactory (lo que ya hacía JPAUtil)
        EntityManagerFactory emf = JPAUtil.getEntityManagerFactory();
        
        // 2. Creamos UNA SOLA VEZ los controladores JPA
        ProductosJpaController productoController = new ProductosJpaController(emf);
        UsuariosJpaController usuarioController = new UsuariosJpaController(emf);
        
        // 3. Creamos UNA SOLA VEZ los servicios que dependen de los controladores
        ProductoService productoService = new ProductoService(productoController);
        
        // 4. Guardamos estas instancias en el ServletContext
        // El ServletContext es un "mapa" global disponible para toda la aplicación.
        ServletContext context = sce.getServletContext();
        context.setAttribute("productoController", productoController);
        context.setAttribute("usuarioController", usuarioController);
        context.setAttribute("productoService", productoService);
        
        System.out.println(">>> Servicios y Controladores inicializados y guardados en el contexto.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println(">>> DESTRUYENDO CONTEXTO DE LA APLICACIÓN <<<");
        
        // Liberamos los recursos cuando la aplicación se detiene
        JPAUtil.shutdown();
        RedisConexion.shutdown();
        
        // Intentamos detener el hilo del WebSocket (buena práctica)
        try {
            ProductoWebSocketServer.notificarActualizacion(); // Para "despertar" y cerrar conexiones
        } catch (Exception e) {}
    }
}