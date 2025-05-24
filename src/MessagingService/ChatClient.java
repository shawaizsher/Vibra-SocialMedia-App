package MessagingService;

import javax.websocket.*;
import java.net.URI;

@ClientEndpoint
public class ChatClient {
    private Session session;
    private final MessageListener listener;

    public ChatClient(String uri, MessageListener listener) {
        this.listener = listener;
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, new URI(uri));
        } catch (Exception e) {
            System.err.println("[WebSocket] Connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("[WebSocket] Connected to server");
    }

    @OnMessage
    public void onMessage(String message) {
        System.out.println("[WebSocket] Message received: " + message);
        if (listener != null) {
            listener.onMessageReceived(message);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("[WebSocket] Connection closed: " + reason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("[WebSocket] Error: " + throwable.getMessage());
        throwable.printStackTrace();
    }

    public void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message);
            System.out.println("[WebSocket] Sent message: " + message);
        } else {
            System.err.println("[WebSocket] Cannot send message, session is closed or null.");
        }
    }

    public interface MessageListener {
        void onMessageReceived(String message);
    }
}
