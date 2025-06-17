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

    public ProductoService(ProductosJpaController jpaController) {
        this.jpaController = jpaController;
    }


public Productos obtenerProductoPorId(int id) {
    String redisKey = "producto:" + id;

    try (Jedis jedis = RedisConexion.getJedis()) {
        // 1. AHORA, EN LUGAR DE 'GET', VERIFICAMOS SI EL HASH EXISTE.
        // hgetAll devuelve todos los campos y valores de un hash.
        // Si la clave no existe, devuelve un mapa vacío.
        Map<String, String> productoMap = jedis.hgetAll(redisKey);

        if (!productoMap.isEmpty()) {
            System.out.println("CACHE HIT (Hash): Producto " + id + " encontrado en Redis.");
            
            
            Productos producto = new Productos();
            producto.setId(Integer.parseInt(productoMap.get("id")));
            producto.setNombre(productoMap.get("nombre"));
            producto.setPrecio(new java.math.BigDecimal(productoMap.get("precio"))); 
            producto.setStock(Integer.parseInt(productoMap.get("stock")));
            producto.setImagenUrl(productoMap.get("imagenUrl"));
            return producto;

        } else {
            System.out.println("CACHE MISS (Hash): Producto " + id + " no encontrado. Buscando en MySQL...");
            
            Productos productoDeDB = jpaController.findProductos(id);
            
            if (productoDeDB != null) {
                // Creamos un mapa de String a String para guardar en el Hash.
                Map<String, String> nuevoProductoMap = new HashMap<>();
                nuevoProductoMap.put("id", String.valueOf(productoDeDB.getId()));
                nuevoProductoMap.put("nombre", productoDeDB.getNombre());
                nuevoProductoMap.put("precio", productoDeDB.getPrecio().toPlainString()); // Convertimos BigDecimal a String
                nuevoProductoMap.put("stock", String.valueOf(productoDeDB.getStock()));
                if (productoDeDB.getImagenUrl() != null) { nuevoProductoMap.put("imagenUrl", productoDeDB.getImagenUrl()); }
               
                jedis.hset(redisKey, nuevoProductoMap);
                
                jedis.expire(redisKey, 3600); 

                System.out.println("Producto " + id + " guardado en caché de Redis como Hash.");
            }
            
            return productoDeDB;
        }
    }
}

public boolean realizarVenta(int id, int cantidadComprada) {
    
    Productos producto = this.obtenerProductoPorId(id);

    if (producto == null || producto.getStock() < cantidadComprada) {
        System.out.println("VENTA FALLIDA (Hash): Producto no existe o no hay stock suficiente.");
        return false;
    }

    try {
       
        producto.setStock(producto.getStock() - cantidadComprada);
        jpaController.edit(producto);
        System.out.println("BD ACTUALIZADA (Hash): Nuevo stock para producto " + id + " es " + producto.getStock());
        
        String redisKey = "producto:" + id;
        try (Jedis jedis = RedisConexion.getJedis()) {
            if (jedis.exists(redisKey)) {
                // HINCRBY  Decrementa el valor del campo 'stock' por 'cantidadComprada'.
                jedis.hincrBy(redisKey, "stock", -cantidadComprada);
                System.out.println("CACHÉ ACTUALIZADA (Hash): Stock del producto " + id + " decrementado en Redis.");
            }
        }
        return true;

    } catch (Exception ex) {
        producto.setStock(producto.getStock() + cantidadComprada);
        Logger.getLogger(ProductoService.class.getName()).log(Level.SEVERE, "Error al editar el producto con JPA.", ex);
    }
    
    return false;
}
}