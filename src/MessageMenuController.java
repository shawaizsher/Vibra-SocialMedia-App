import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import MessagingService.*;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MessageMenuController implements Initializable {

    @FXML
    private TextField friendSearchField;
    @FXML
    private VBox friendsListBox;
    @FXML
    private TextField messageInputArea;
    @FXML
    private Button sendButton, markReadButton, markUnreadButton;
    @FXML
    private ScrollPane messageScrollPane;
    @FXML
    private VBox messageContainer;

    private static final int PROFILE_PIC_SIZE = 40;

    private User selectedFriend;
    private final Gson gson = new Gson();
    private Connection dbConnection;
    private MessageDAO messageDAO;
    private ChatClient chatClient;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Font.loadFont(getClass().getResourceAsStream("Fonts/Raleway-SemiBold.ttf"), 14);
        showDefaultMessagePrompt();

        friendSearchField.textProperty().addListener((obs, oldText, newText) ->
                filterAndDisplayUsers(newText.trim().toLowerCase()));

        Platform.runLater(() -> {
            Scene scene = messageContainer.getScene();
            if (scene != null) {
                scene.getStylesheets().add(getClass().getResource("CssFiles/messagemenu.css").toExternalForm());
            }
        });

        sendButton.setText("Send");
        sendButton.setOnAction(this::handleSendMessage);

        try {
            dbConnection = DatabaseConnection.getConnection();
            messageDAO = new MessageDAO(dbConnection);
            connectToWebSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }

        filterAndDisplayUsers("");
    }

    private void connectToWebSocket() {
        String serverUri = "ws://192.168.1.71:8025/ws/chat";
        chatClient = new ChatClient(serverUri, this::handleIncomingMessage);
    }

    @FXML
    public void handleSendMessage(ActionEvent event) {
        String content = messageInputArea.getText().trim().isEmpty()
                ? messageInputArea.getText().trim()
                : messageInputArea.getText().trim();

        if (selectedFriend == null || content.isEmpty()) return;

        int senderId = UserSession.getInstance().getUser().getId();
        int receiverId = selectedFriend.getId();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());

        Message message = new Message(senderId, receiverId, content, timestamp);

        try {
            messageDAO.saveMessage(message);
            addMessageToContainer(content, true, false, null);
            messageInputArea.clear();
            messageInputArea.clear();

            if (chatClient != null) {
                chatClient.sendMessage(gson.toJson(message));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to send message. Please try again.");
        }
    }

    public void handleIncomingMessage(String json) {
        JsonObject jsonObj = JsonParser.parseString(json).getAsJsonObject();

        String content = jsonObj.get("content").getAsString();
        int senderId = jsonObj.get("senderId").getAsInt();
        int receiverId = jsonObj.get("receiverId").getAsInt();
        boolean isRead = jsonObj.has("isRead") && jsonObj.get("isRead").getAsBoolean();

        Timestamp readAt = null;
        if (jsonObj.has("readAt") && !jsonObj.get("readAt").isJsonNull()) {
            String readAtStr = jsonObj.get("readAt").getAsString();
            try {
                readAt = Timestamp.valueOf(readAtStr);
            } catch (IllegalArgumentException e) {
                readAt = null;
            }
        }

        int currentUserId = UserSession.getInstance().getUser().getId();
        boolean isSentByCurrentUser = (senderId == currentUserId);

        final String finalContent = content;
        final boolean finalIsSentByCurrentUser = isSentByCurrentUser;
        final boolean finalIsRead = isRead;
        final Timestamp finalReadAt = readAt;
        final int finalSenderId = senderId;

        Platform.runLater(() -> {
            if (selectedFriend != null &&
                    (finalIsSentByCurrentUser || selectedFriend.getId() == finalSenderId || selectedFriend.getId() == receiverId)) {

                addMessageToContainer(finalContent, finalIsSentByCurrentUser, finalIsRead, finalReadAt);

                if (!finalIsSentByCurrentUser) {
                    markMessageAsRead(finalSenderId, currentUserId);
                }
            }
        });
    }

    private void scrollToBottom() {
        Platform.runLater(() -> {
            messageScrollPane.layout();
            messageScrollPane.setVvalue(1.0);
        });
    }

    private void markMessageAsRead(int senderId, int receiverId) {
        System.out.println("markMessageAsRead called with senderId=" + senderId + ", receiverId=" + receiverId);
        String sql = "UPDATE messages SET is_read = TRUE, read_at = CURRENT_TIMESTAMP " +
                "WHERE sender_id = ? AND receiver_id = ? AND is_read = FALSE";

        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, receiverId);
            int updated = stmt.executeUpdate();
            Platform.runLater(() -> {
                if (selectedFriend != null) {
                    loadMessagesForFriend(selectedFriend.getId());
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addMessageToContainer(String content, boolean isSentByCurrentUser, boolean isRead, Timestamp readAt) {
        Label messageLabel = new Label(content);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);
        messageLabel.setStyle("-fx-background-color: " + (isSentByCurrentUser ? "#a3d2ca" : "#f7d6bf") + ";" +
                "-fx-padding: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-font-size: 14;");

        VBox messageVBox = new VBox(messageLabel);
        messageVBox.setSpacing(2);

        if (isSentByCurrentUser) {
            if (isRead) {
                String readTimeStr = "";
                if (readAt != null) {
                    readTimeStr = new SimpleDateFormat("h:mm a").format(readAt);
                }
                Label readLabel = new Label("Read at " + readTimeStr);
                readLabel.setStyle("-fx-font-size: 10; -fx-text-fill: black;");
                messageVBox.getChildren().add(readLabel);
            } else {
                Label sentLabel = new Label("✓✓ Sent");
                sentLabel.setStyle("-fx-font-size: 10; -fx-text-fill: gray;");
                messageVBox.getChildren().add(sentLabel);
            }
        }

        HBox messageBox = new HBox(messageVBox);
        messageBox.setAlignment(isSentByCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5));
        messageContainer.getChildren().add(messageBox);

        scrollToBottom();
    }

    private void loadMessagesForFriend(int friendId) {
        messageContainer.getChildren().clear();

        int currentUserId = UserSession.getInstance().getUser().getId();

        String query = "SELECT * FROM messages " +
                "WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) " +
                "ORDER BY timestamp ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, currentUserId);
            stmt.setInt(2, friendId);
            stmt.setInt(3, friendId);
            stmt.setInt(4, currentUserId);

            try (ResultSet rs = stmt.executeQuery()) {
                boolean hasMessages = false;
                while (rs.next()) {
                    hasMessages = true;
                    String content = rs.getString("message_text");
                    int senderId = rs.getInt("sender_id");
                    boolean isRead = rs.getBoolean("is_read");
                    Timestamp readAt = rs.getTimestamp("read_at");

                    boolean isSentByCurrentUser = (senderId == currentUserId);

                    addMessageToContainer(content, isSentByCurrentUser, isRead, readAt);
                }

                if (!hasMessages) {
                    // No messages found, display the prompt
                    messageContainer.getChildren().clear();
                    Label prompt = new Label("You haven't talked to this person yet!");
                    prompt.setStyle("-fx-font-size: 16px; -fx-text-fill: gray; -fx-padding: 20;");
                    prompt.setAlignment(Pos.CENTER);
                    prompt.setMaxWidth(Double.MAX_VALUE);

                    VBox wrapper = new VBox(prompt);
                    wrapper.setAlignment(Pos.CENTER);
                    wrapper.setPrefHeight(300);

                    messageContainer.getChildren().add(wrapper);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showDefaultMessagePrompt() {
        messageContainer.getChildren().clear();

        Label prompt = new Label("Select a friend to view messages.");
        prompt.setStyle("-fx-font-size: 16px; -fx-text-fill: gray; -fx-padding: 20;");
        prompt.setAlignment(Pos.CENTER);
        prompt.setMaxWidth(Double.MAX_VALUE);

        VBox wrapper = new VBox(prompt);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPrefHeight(300);

        messageContainer.getChildren().add(wrapper);
    }

    @FXML
    public void handleMarkReadButton(ActionEvent event) {
        updateReadStatus(true);
    }

    @FXML
    public void handleMarkUnreadButton(ActionEvent event) {
        updateReadStatus(false);
    }

    private void updateReadStatus(boolean markAsRead) {
        if (selectedFriend == null) return;

        int currentUserId = UserSession.getInstance().getUser().getId();
        int friendId = selectedFriend.getId();

        String sql = "UPDATE messages SET is_read = " + (markAsRead ? "TRUE, read_at = CURRENT_TIMESTAMP"
                : "FALSE, read_at = NULL") +
                " WHERE sender_id = ? AND receiver_id = ?";

        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setInt(1, friendId);
            stmt.setInt(2, currentUserId);
            int updated = stmt.executeUpdate();
            System.out.println("[DEBUG] Marked " + updated + " messages as " + (markAsRead ? "read" : "unread"));

            Platform.runLater(() -> {
                if (selectedFriend != null) {
                    loadMessagesForFriend(selectedFriend.getId());
                }
            });

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Failed to update message status.");
        }
    }

    private void filterAndDisplayUsers(String searchQuery) {
        try {
            friendsListBox.getChildren().clear();

            List<User> users = UserSettingsDAO.getUsersWithMessageSetting("Everyone");

            int currentUserId = UserSession.getInstance().getUser().getId();
            String queryLower = searchQuery.toLowerCase();

            for (User user : users) {
                if (user.getId() != currentUserId && user.getUsername().toLowerCase().contains(queryLower)) {
                    HBox userEntry = createUserEntry(user);
                    friendsListBox.getChildren().add(userEntry);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createUserEntry(User user) {
        HBox entry = new HBox();
        entry.getStyleClass().add("friend-entry");
        entry.setSpacing(10);
        entry.setAlignment(Pos.CENTER_LEFT);
        entry.setPadding(new Insets(5));

        // --- Profile Picture ---
        ImageView profilePic = new ImageView();
        profilePic.setFitWidth(40);
        profilePic.setFitHeight(40);

        Image image;
        if (user.getProfilePicture() != null) {
            image = new Image(new ByteArrayInputStream(user.getProfilePicture()));
        } else {
            image = new Image(getClass().getResourceAsStream("/Images/default_picture.jpg"));
        }

        profilePic.setImage(image);

        Circle clip = new Circle(20, 20, 20);
        profilePic.setClip(clip);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage clippedImage = profilePic.snapshot(params, null);
        profilePic.setClip(null);
        profilePic.setImage(clippedImage);

        // --- Username Label ---
        Label nameLabel = new Label(user.getUsername());
        nameLabel.getStyleClass().add("friend-name-label");
        nameLabel.setStyle("-fx-background-color: transparent; -fx-padding: 6 12; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: transparent;");

        // Hover effect
        nameLabel.setOnMouseEntered(e -> nameLabel.setStyle(
                "-fx-background-color: #e9f2ff; -fx-padding: 6 12; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #2196F3;"));
        nameLabel.setOnMouseExited(e -> nameLabel.setStyle(
                "-fx-background-color: transparent; -fx-padding: 6 12; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: transparent;"));

        entry.getChildren().addAll(profilePic, nameLabel);

        entry.setOnMouseClicked(e -> {
            selectedFriend = user;
            loadMessagesForFriend(user.getId());
        });

        return entry;
    }

    private void showError(String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    private String formatTime(Timestamp timestamp) {
        return new SimpleDateFormat("hh:mm a").format(timestamp);
    }
}