import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationsController {

    @FXML
    public BorderPane mainLayout;

    @FXML
    private VBox notificationsContainer;

    // Helper class to hold notification message + timestamp + optional action
    private static class NotificationItem {
        String message;
        LocalDateTime timestamp;
        Runnable onAction;

        NotificationItem(String message, LocalDateTime timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }

        NotificationItem(String message, LocalDateTime timestamp, Runnable onAction) {
            this.message = message;
            this.timestamp = timestamp;
            this.onAction = onAction;
        }
    }

    // Helper class for friend request info
    private static class FriendRequestInfo {
        int senderId;
        String senderUsername;
        LocalDateTime sentAt;

        FriendRequestInfo(int senderId, String senderUsername, LocalDateTime sentAt) {
            this.senderId = senderId;
            this.senderUsername = senderUsername != null ? senderUsername : "";
            this.sentAt = sentAt;
        }
    }

    private boolean isNotificationsTurnedOff(int userId) {
        String sql = "SELECT notifications_toggle FROM user_settings WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("notifications_toggle");  // true means notifications OFF
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;  // default: notifications ON
    }

    private static List<NotificationItem> getNotificationsWithTimestamp(int userId) throws SQLException {
        String sql = "SELECT message, created_at FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        List<NotificationItem> notifications = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String message = rs.getString("message");
                Timestamp ts = rs.getTimestamp("created_at");
                LocalDateTime time = ts != null ? ts.toLocalDateTime() : null;
                notifications.add(new NotificationItem(message, time));
            }
        }
        return notifications;
    }

    private static List<FriendRequestInfo> getPendingFriendRequestSenders(int userId) throws SQLException {
        String sql = """
            SELECT u.user_id, u.username, fr.sent_at
            FROM friend_requests fr
            JOIN users u ON fr.sender_id = u.user_id
            WHERE fr.receiver_id = ? AND fr.status = 'pending'
            ORDER BY fr.sent_at DESC
        """;

        List<FriendRequestInfo> requests = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int senderId = rs.getInt("user_id");
                String senderUsername = rs.getString("username");
                Timestamp ts = rs.getTimestamp("sent_at");
                LocalDateTime sentAt = ts != null ? ts.toLocalDateTime() : null;

                requests.add(new FriendRequestInfo(senderId, senderUsername, sentAt));
            }
        }
        return requests;
    }

    @FXML
    public void initialize() {
        int userId = UserSession.getInstance().getUser().getId();

        if (isNotificationsTurnedOff(userId)) {
            Label offLabel = new Label("You have notifications turned off.");
            offLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: red; -fx-font-weight: bold;");
            notificationsContainer.getChildren().clear();
            notificationsContainer.getChildren().add(offLabel);
            return;
        }

        Platform.runLater(() -> {
            Scene scene = notificationsContainer.getScene();
            if (scene != null) {
                scene.getStylesheets().add(getClass().getResource("/CssFiles/notifications.css").toExternalForm());
            }
        });

        try {
            List<NotificationItem> generalNotifications = getNotificationsWithTimestamp(userId);

            List<FriendRequestInfo> pendingRequests = getPendingFriendRequestSenders(userId);

            // Convert friend requests to NotificationItems
            List<NotificationItem> friendRequestNotifications = new ArrayList<>();
            for (FriendRequestInfo req : pendingRequests) {
                String message = req.senderUsername + " sent you a friend request.";
                // Pass senderUsername to viewProfile function
                Runnable action = () -> viewProfile(req.senderUsername);
                friendRequestNotifications.add(new NotificationItem(message, req.sentAt, action));
            }

            // Combine all notifications
            List<NotificationItem> allNotifications = new ArrayList<>();
            allNotifications.addAll(generalNotifications);
            allNotifications.addAll(friendRequestNotifications);

            if (allNotifications.isEmpty()) {
                Label noNotifications = new Label("You have no new notifications.");
                noNotifications.setStyle("-fx-font-size: 16px;");
                notificationsContainer.getChildren().add(noNotifications);
                return;
            }

            // Sort by timestamp descending (newest first)
            allNotifications.sort((a, b) -> b.timestamp.compareTo(a.timestamp));

            notificationsContainer.getChildren().clear();

            // Add notification UI elements
            for (NotificationItem item : allNotifications) {
                String timeAgo = getTimeAgo(item.timestamp);
                VBox box = createNotificationBox(item.message, timeAgo, item.onAction != null);

                if (item.onAction != null) {
                    Button btn = (Button) box.getChildren().stream()
                            .filter(node -> node instanceof Button)
                            .findFirst()
                            .orElse(null);
                    if (btn != null) {
                        btn.setOnAction(e -> item.onAction.run());
                    }
                }

                notificationsContainer.getChildren().add(box);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Label errorLabel = new Label("⚠️ Failed to load notifications.");
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
            notificationsContainer.getChildren().add(errorLabel);
        }
    }

    // Updated function to handle View Profile button click using username
    private void viewProfile(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML Files/ProfileMenu.fxml"));
            Parent profileView = loader.load();

            ProfileMenuController controller = loader.getController();
            controller.setMainLayout(mainLayout);
            controller.loadUserProfile(username);

            mainLayout.setCenter(profileView);

        } catch (Exception e) {
            e.printStackTrace();
            Label errorLabel = new Label("⚠️ Failed to load user profile.");
            errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
            notificationsContainer.getChildren().clear();
            notificationsContainer.getChildren().add(errorLabel);
        }
    }

    public String getTimeAgo(LocalDateTime time) {
        if (time == null) return "Unknown time";
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(time, now);

        if (duration.toMinutes() < 1) return "Just now";
        if (duration.toMinutes() < 60) return duration.toMinutes() + " minutes ago";
        if (duration.toHours() < 24) return duration.toHours() + " hours ago";
        return duration.toDays() + " days ago";
    }

    public void setMainLayout(BorderPane mainLayout) {
        this.mainLayout = mainLayout;
    }

    private VBox createNotificationBox(String message, String timeAgo, boolean showViewProfileButton) {
        VBox box = new VBox(4);
        box.getStyleClass().add("notification-item");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("notification-message");

        Label timeLabel = new Label(timeAgo != null ? timeAgo : "");
        timeLabel.getStyleClass().add("notification-time");

        box.getChildren().addAll(messageLabel, timeLabel);

        if (showViewProfileButton) {
            Button viewProfileButton = new Button("View Profile");
            viewProfileButton.getStyleClass().add("notification-view-btn");
            box.getChildren().add(viewProfileButton);
        }

        return box;
    }
}