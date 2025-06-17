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
        
        
        EntityManagerFactory emf = JPAUtil.getEntityManagerFactory();
        
        
        ProductosJpaController productoController = new ProductosJpaController(emf);
        UsuariosJpaController usuarioController = new UsuariosJpaController(emf);
        
       
        ProductoService productoService = new ProductoService(productoController);
     
        ServletContext context = sce.getServletContext();
        context.setAttribute("productoController", productoController);
        context.setAttribute("usuarioController", usuarioController);
        context.setAttribute("productoService", productoService);
        
        System.out.println(">>> Servicios y Controladores inicializados y guardados en el contexto.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println(">>> DESTRUYENDO CONTEXTO DE LA APLICACIÓN <<<");
        
       
        JPAUtil.shutdown();
        RedisConexion.shutdown();
        
       
        try {
            ProductoWebSocketServer.notificarActualizacion(); 
        } catch (Exception e) {}
    }
}