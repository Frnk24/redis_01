package modelo;

import com.google.gson.Gson;
import com.mycompany.ventasredislimpio.ProductosJpaController;
import com.mycompany.ventasredislimpio.exceptions.NonexistentEntityException;
import java.util.Map;
import java.util.HashMap;
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
    // Reemplaza el método obtenerProductoPorId en ProductoService.java

public Productos obtenerProductoPorId(int id) {
    String redisKey = "producto:" + id;

    try (Jedis jedis = RedisConexion.getJedis()) {
        // 1. AHORA, EN LUGAR DE 'GET', VERIFICAMOS SI EL HASH EXISTE.
        // hgetAll devuelve todos los campos y valores de un hash.
        // Si la clave no existe, devuelve un mapa vacío.
        Map<String, String> productoMap = jedis.hgetAll(redisKey);

        if (!productoMap.isEmpty()) {
            // ¡CACHE HIT! Lo encontramos en Redis como un Hash.
            System.out.println("CACHE HIT (Hash): Producto " + id + " encontrado en Redis.");
            
            // 2. CONSTRUIMOS EL OBJETO 'Productos' A PARTIR DEL MAPA.
            // Necesitamos convertir los strings del mapa a los tipos correctos (Integer, BigDecimal, etc.).
            Productos producto = new Productos();
            producto.setId(Integer.parseInt(productoMap.get("id")));
            producto.setNombre(productoMap.get("nombre"));
            // OJO: Tu DTO usa BigDecimal, así que creamos un BigDecimal desde el String.
            producto.setPrecio(new java.math.BigDecimal(productoMap.get("precio"))); 
            producto.setStock(Integer.parseInt(productoMap.get("stock")));
            producto.setImagenUrl(productoMap.get("imagenUrl"));
            return producto;

        } else {
            // ¡CACHE MISS! No está en Redis, vamos a la base de datos.
            System.out.println("CACHE MISS (Hash): Producto " + id + " no encontrado. Buscando en MySQL...");
            
            Productos productoDeDB = jpaController.findProductos(id);
            
            if (productoDeDB != null) {
                // 3. GUARDAMOS EN REDIS COMO UN HASH, CAMPO POR CAMPO.
                // Creamos un mapa de String a String para guardar en el Hash.
                Map<String, String> nuevoProductoMap = new HashMap<>();
                nuevoProductoMap.put("id", String.valueOf(productoDeDB.getId()));
                nuevoProductoMap.put("nombre", productoDeDB.getNombre());
                nuevoProductoMap.put("precio", productoDeDB.getPrecio().toPlainString()); // Convertimos BigDecimal a String
                nuevoProductoMap.put("stock", String.valueOf(productoDeDB.getStock()));
                if (productoDeDB.getImagenUrl() != null) { nuevoProductoMap.put("imagenUrl", productoDeDB.getImagenUrl()); }
                // El comando 'hset' guarda el mapa completo en la clave del Hash.
                jedis.hset(redisKey, nuevoProductoMap);
                
                // Es buena práctica ponerle una expiración a la caché.
                jedis.expire(redisKey, 3600); // Expira en 1 hora

                System.out.println("Producto " + id + " guardado en caché de Redis como Hash.");
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
    // Reemplaza el método realizarVenta en ProductoService.java

public boolean realizarVenta(int id, int cantidadComprada) {
    // La primera parte para obtener el producto no cambia.
    // Usará el nuevo método obtenerProductoPorId que ya lee de Hashes.
    Productos producto = this.obtenerProductoPorId(id);

    if (producto == null || producto.getStock() < cantidadComprada) {
        System.out.println("VENTA FALLIDA (Hash): Producto no existe o no hay stock suficiente.");
        return false;
    }

    try {
        // 1. ACTUALIZAMOS LA BASE DE DATOS PRIMERO (LA FUENTE DE LA VERDAD).
        // Esta parte es igual, porque JPA no sabe nada de Redis.
        producto.setStock(producto.getStock() - cantidadComprada);
        jpaController.edit(producto);
        System.out.println("BD ACTUALIZADA (Hash): Nuevo stock para producto " + id + " es " + producto.getStock());
        
        // 2. ACTUALIZAMOS LA CACHÉ, PERO AHORA DE FORMA MÁS INTELIGENTE.
        String redisKey = "producto:" + id;
        try (Jedis jedis = RedisConexion.getJedis()) {
            // Verificamos si la caché existe antes de intentar actualizarla.
            if (jedis.exists(redisKey)) {
                // HINCRBY es atómico. Decrementa el valor del campo 'stock' por 'cantidadComprada'.
                // Pasamos un valor negativo para restar.
                jedis.hincrBy(redisKey, "stock", -cantidadComprada);
                System.out.println("CACHÉ ACTUALIZADA (Hash): Stock del producto " + id + " decrementado en Redis.");
            }
            // NOTA: Con este enfoque, ya no necesitamos invalidar (borrar) la clave.
            // ¡Simplemente actualizamos el campo que cambió! Es más eficiente.
        }
        return true;

    } catch (Exception ex) {
        // Si la actualización de la BD falla, revertimos el cambio en el objeto en memoria
        // para que la información que se muestre sea consistente.
        producto.setStock(producto.getStock() + cantidadComprada);
        Logger.getLogger(ProductoService.class.getName()).log(Level.SEVERE, "Error al editar el producto con JPA.", ex);
    }
    
    return false;
}
}