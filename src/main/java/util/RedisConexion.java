package util;

import java.util.HashSet;
import java.util.Set;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;

public class RedisConexion {

   
    private static final String MASTER_NAME = "mymaster"; 
    
    private static final String REDIS_PASSWORD = "2509";

    private static final JedisSentinelPool pool;

    static {
        JedisPoolConfig poolConfig = new JedisPoolConfig();

        Set<String> sentinels = new HashSet<>();
        sentinels.add("127.0.0.1:26379");

        pool = new JedisSentinelPool(MASTER_NAME, sentinels, poolConfig, 2000, REDIS_PASSWORD);

        System.out.println("JedisSentinelPool inicializado correctamente. Conectado a master '" + MASTER_NAME + "'.");
    }

    public static Jedis getJedis() {
        return pool.getResource();
    }

    public static void shutdown() {
        if (pool != null) {
            pool.close();
            System.out.println("JedisSentinelPool cerrado.");
        }
    }

    // Constructor privado para evitar instancias.
    private RedisConexion() {}
}