package util;

import java.util.HashSet;
import java.util.Set;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

public class RedisConexion {

    // --- CONFIGURACIÓN PARA SENTINEL ---
    // El nombre de tu master, definido en sentinel.conf
    private static final String MASTER_NAME = "mymaster"; 
    
    // La contraseña para autenticarse tanto con el maestro/réplicas como con el sentinel
    private static final String REDIS_PASSWORD = "2509";

    // La piscina de conexiones. Ahora será de tipo JedisSentinelPool.
    private static final JedisSentinelPool pool;

    static {
        // Configuramos el pool, igual que antes.
        JedisPoolConfig poolConfig = new JedisPoolConfig();

        // --- CONJUTO DE CENTINELAS ---
        // Creamos un Set con la dirección de TUS centinelas. 
        // Aunque solo tienes uno, es una buena práctica usar un Set.
        Set<String> sentinels = new HashSet<>();
        sentinels.add("127.0.0.1:26379");

        // --- INICIALIZACIÓN DEL POOL DE SENTINEL ---
        // Se le pasa el nombre del master, la lista de centinelas, la configuración,
        // el timeout y la contraseña.
        pool = new JedisSentinelPool(MASTER_NAME, sentinels, poolConfig, 2000, REDIS_PASSWORD);

        System.out.println("JedisSentinelPool inicializado correctamente. Conectado a master '" + MASTER_NAME + "'.");
    }

    /**
     * Obtiene una conexión del pool. El pool se encarga automáticamente de 
     * darnos una conexión al MAESTRO actual.
     * No necesitas cambiar nada en el resto de tu código que usa este método.
     * @return una instancia de Jedis lista para usar.
     */
    public static Jedis getJedis() {
        return pool.getResource();
    }

    /**
     * Cierra el pool de conexiones cuando la aplicación se detiene.
     */
    public static void shutdown() {
        if (pool != null) {
            pool.close();
            System.out.println("JedisSentinelPool cerrado.");
        }
    }

    // Constructor privado para evitar instancias.
    private RedisConexion() {}
}