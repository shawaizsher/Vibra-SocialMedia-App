package MessagingService;

import org.glassfish.tyrus.server.Server;

public class ChatServer {
    public static void main(String[] args) {
        Server server = new Server("0.0.0.0", 8025, "/ws", null, ChatEndpoint.class);
        try {
            server.start();
            System.out.println("WebSocket server started at ws://<your-ip>:8025/ws/chat");
            System.out.println("Press Enter to stop the server...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            server.stop();
        }
    }
}
