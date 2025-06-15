package util;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class JPAUtil {
    private static final String PERSISTENCE_UNIT_NAME = "com.mycompany_Prueba_war_1.0-SNAPSHOTPU"; 
    
    // 2. Aquí guardamos la única instancia del Factory
    private static EntityManagerFactory factory;

    // 3. Este es el método que todos usarán para pedir el Factory
    public static EntityManagerFactory getEntityManagerFactory() {
        // 4. Si es la primera vez que se pide, lo crea.
        if (factory == null) {
            factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        }
        // 5. Si ya existe, simplemente lo devuelve.
        return factory;
    }

    public static void shutdown() {
        if (factory != null) {
            factory.close();
        }
    }
}