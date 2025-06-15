package modelo;

import com.google.gson.Gson;
import com.mycompany.ventasredislimpio.ProductosJpaController;
import com.mycompany.ventasredislimpio.exceptions.NonexistentEntityException;
import modelo.Productos;
import util.RedisConexion; 
import java.util.logging.Level;
import java.util.logging.Logger;
import redis.clients.jedis.Jedis;

public class ProductoService {

    private final ProductosJpaController jpaController;
    private final Gson gson = new Gson();

    // El servicio necesita el JpaController para poder hablar con la base de datos
    public ProductoService(ProductosJpaController jpaController) {
        this.jpaController = jpaController;
    }

    /**
     * Obtiene un producto. Primero intenta buscarlo en la caché de Redis.
     * Si no lo encuentra, lo busca en la base de datos (MySQL) y lo guarda en Redis
     * para futuras consultas.
     *
     * @param id El ID del producto a buscar.
     * @return El objeto Producto, o null si no se encuentra.
     */
    public Productos obtenerProductoPorId(int id) {
        String redisKey = "producto:" + id; // Clave única para este producto en Redis

        try (Jedis jedis = RedisConexion.getJedis()) {
            // 1. INTENTAR OBTENER DE REDIS (CACHÉ)
            String productoJson = jedis.get(redisKey);

            if (productoJson != null) {
                // ¡CACHE HIT! Lo encontramos en Redis, es la vía rápida.
                System.out.println("CACHE HIT: Producto " + id + " encontrado en Redis.");
                return gson.fromJson(productoJson, Productos.class);
            } else {
                // ¡CACHE MISS! No está en Redis, vamos a la base de datos (la vía lenta).
                System.out.println("CACHE MISS: Producto " + id + " no encontrado en Redis. Buscando en MySQL con JPA...");
                
                Productos productoDeDB = jpaController.findProductos(id);
                
                if (productoDeDB != null) {
                    // 2. GUARDAR EN REDIS PARA LA PRÓXIMA VEZ
                    // Lo guardamos en formato JSON. setex lo guarda con un tiempo de expiración (ej. 1 hora)
                    jedis.setex(redisKey, 3600, gson.toJson(productoDeDB)); 
                    System.out.println("Producto " + id + " guardado en caché de Redis.");
                }
                
                return productoDeDB;
            }
        
    }
        
    }
    /**
     * Procesa la venta de un producto, actualizando el stock en la base de datos
     * y invalidando la caché en Redis para mantener la consistencia.
     *
     * @param id El ID del producto a vender.
     * @param cantidadComprada La cantidad de unidades a vender.
     * @return true si la venta fue exitosa, false en caso contrario.
     */
    public boolean realizarVenta(int id, int cantidadComprada) {
        // Obtenemos el producto (usará la caché si está disponible para una verificación rápida)
        Productos producto = this.obtenerProductoPorId(id);

        if (producto == null || producto.getStock() < cantidadComprada) {
            System.out.println("VENTA FALLIDA: Producto no existe o no hay stock suficiente.");
            return false; // No se puede vender si no existe o no hay stock.
        }

        // Si hay stock, procedemos a actualizar la "fuente de la verdad" (la base de datos)
        producto.setStock(producto.getStock() - cantidadComprada);
        
        try {
            // 1. ACTUALIZAR LA FUENTE DE VERDAD (MySQL) usando JPA.
            // Este es el paso más crítico.
            jpaController.edit(producto);
            System.out.println("BD ACTUALIZADA: Nuevo stock para producto " + id + " es " + producto.getStock());
            
            // 2. SI LA ACTUALIZACIÓN EN BD FUE EXITOSA, INVALIDAR LA CACHÉ EN REDIS.
            // Esto es vital para la CONSISTENCIA. Forzamos a que la próxima lectura
            // del producto vaya a la BD a buscar el nuevo stock y lo vuelva a cachear.
            
            String redisKey = "producto:" + id;
            try (Jedis jedis = RedisConexion.getJedis()) {
                jedis.del(redisKey); // El comando 'del' elimina la clave.
                System.out.println("CACHÉ INVALIDADA: La clave " + redisKey + " fue eliminada de Redis.");
            }
            return true;

        } catch (NonexistentEntityException ex) {
            Logger.getLogger(ProductoService.class.getName()).log(Level.SEVERE, "Error de consistencia: El producto existía pero desapareció durante la transacción.", ex);
        } catch (Exception ex) {
            Logger.getLogger(ProductoService.class.getName()).log(Level.SEVERE, "Error al editar el producto con JPA.", ex);
        }
        
        // Si llegamos aquí, algo falló en la actualización de la BD.
        return false;
    }
}