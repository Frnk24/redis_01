
package controlador;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

// La anotación @ServerEndpoint define la URL del WebSocket
@ServerEndpoint("/productos-updates")
public class ProductoWebSocketServer {
    
    // Usamos un Set sincronizado para guardar las sesiones de todos los clientes conectados.
    // Es 'static' porque solo debe haber una lista para todo el servidor.
    private static Set<Session> clientes = Collections.synchronizedSet(new HashSet<>());
    
    @OnOpen
    public void onOpen(Session session) {
        // Cuando un nuevo cliente se conecta, lo añadimos a nuestra lista.
        System.out.println("WebSocket: Nuevo cliente conectado - " + session.getId());
        clientes.add(session);
    }
    
    @OnClose
    public void onClose(Session session) {
        // Cuando un cliente se desconecta, lo removemos.
        System.out.println("WebSocket: Cliente desconectado - " + session.getId());
        clientes.remove(session);
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        // Manejo de errores
        System.err.println("WebSocket: Error en la sesión " + session.getId());
        throwable.printStackTrace();
        clientes.remove(session);
    }
    
    // Este método es el más importante. Es un método ESTÁTICO que otros
    // servlets pueden llamar para enviar un mensaje a TODOS los clientes conectados.
    public static void notificarActualizacion() {
        System.out.println("WebSocket: Notificando actualización de productos a " + clientes.size() + " clientes.");
        // Iteramos sobre todos los clientes conectados y les enviamos un mensaje.
        for (Session cliente : clientes) {
            if (cliente.isOpen()) {
                try {
                    // El mensaje puede ser cualquier string. "update" es simple y suficiente.
                    cliente.getBasicRemote().sendText("update");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}