import MessagingService.ChatClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class MessagingController {

    @FXML private TextArea chatArea;
    @FXML private TextField messageField;

    private ChatClient chatClient;

    private String senderId;
    private String receiverId;

    public void initialize(String receiverUsername) {
        User currentUser = UserSession.getInstance().getUser();
        this.senderId = currentUser.getUsername();
        this.receiverId = receiverUsername;

        String serverUri = "ws://192.168.1.71:8025/ws/chat";

        chatClient = new ChatClient(serverUri, message -> {
            Platform.runLater(() -> chatArea.appendText(receiverId + ": " + message + "\n"));
        });
    }

    @FXML
    public void sendMessage() {
        String content = messageField.getText();
        if (content == null || content.isEmpty()) return;

        String jsonMsg = String.format(
                "{\"senderId\":\"%s\", \"receiverId\":\"%s\", \"content\":\"%s\"}",
                senderId, receiverId, content
        );

        chatClient.sendMessage(jsonMsg);
        chatArea.appendText("Me: " + content + "\n");
        messageField.clear();
    }
}


