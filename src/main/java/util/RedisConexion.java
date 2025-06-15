package util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisConexion {

    // Dirección y puerto de tu servidor Redis MASTER.
    // 'localhost' funciona si Redis corre en la misma máquina que tu aplicación.
    // 6379 es el puerto por defecto de Redis.
    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;
    
    // El 'pool' es la piscina de conexiones. Se crea una sola vez para toda la aplicación.
    private static final JedisPool pool;
    
    // Este bloque estático se ejecuta una sola vez, cuando la clase se carga por primera vez.
    // Es el lugar perfecto para configurar e inicializar el pool.
    static {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // Puedes configurar la piscina aquí si lo necesitas. Por ejemplo:
        // poolConfig.setMaxTotal(20); // Máximo 20 conexiones en total
        // poolConfig.setMaxIdle(10);  // Máximo 10 conexiones inactivas
        
        // Inicializamos la piscina de conexiones.
        pool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT);
        
        System.out.println("JedisPool inicializado correctamente.");
    }
    
    
     //Obtiene una conexión (un objeto Jedis) de la piscina.
    // Es MUY IMPORTANTE que esta conexión se cierre después de usarla
     //para que pueda volver a la piscina. La mejor forma de asegurar esto
     // es usando un bloque try-with-resources.
     // 
     // Ejemplo de uso correcto:
     // try (Jedis jedis = RedisConexion.getJedis()) {
    //     jedis.set("mi_llave", "mi_valor");
    // }
    // 
     // @return una instancia de Jedis lista para usar.
    public static Jedis getJedis() {
        return pool.getResource();
    }
    
    // Método para cerrar la piscina de conexiones cuando la aplicación se detiene.
    // Lo llamaremos desde el Servlet en su método destroy().
    public static void shutdown() {
        if (pool != null) {
            pool.close();
            System.out.println("JedisPool cerrado.");
        }
    }
    
    // Constructor privado para evitar que alguien cree instancias de esta clase de utilidad.
    private RedisConexion() {}
}