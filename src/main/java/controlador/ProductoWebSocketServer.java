
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

@ServerEndpoint("/productos-updates")
public class ProductoWebSocketServer {
    
    private static Set<Session> clientes = Collections.synchronizedSet(new HashSet<>());
    
    @OnOpen
    public void onOpen(Session session) {
        
        System.out.println("WebSocket: Nuevo cliente conectado - " + session.getId());
        clientes.add(session);
    }
    
    @OnClose
    public void onClose(Session session) {
        
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
    
    
    public static void notificarActualizacion() {
        System.out.println("WebSocket: Notificando actualización de productos a " + clientes.size() + " clientes.");
        
        for (Session cliente : clientes) {
            if (cliente.isOpen()) {
                try {
                   
                    cliente.getBasicRemote().sendText("update");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}