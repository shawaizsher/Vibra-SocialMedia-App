import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javafx.fxml.Initializable;

import java.net.URL;

import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;


public class FriendsPageController implements Initializable {

    @FXML
    public BorderPane mainLayout;
    @FXML
    private VBox friendsList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        List<Friend> friends = loadFriendsFromDatabase();
        displayFriends(friends);
    }

    private List<Friend> loadFriendsFromDatabase() {
        List<Friend> friends = new ArrayList<>();
        String query = "SELECT username, bio, profile_picture, last_active FROM users " +
                "WHERE user_id IN (SELECT friend_id FROM friends WHERE user_id = ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, getCurrentUserId());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                String bio = rs.getString("bio");
                byte[] imageBytes = rs.getBytes("profile_picture");
                Timestamp lastActiveTime = rs.getTimestamp("last_active");

                String lastActive = calculateLastActive(lastActiveTime);
                friends.add(new Friend(username, bio, imageBytes, lastActive));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return friends;
    }

    private void displayFriends(List<Friend> friends) {
        friendsList.getChildren().clear();

        if (friends.isEmpty()) {
            Label emptyLabel = new Label("You have no friends added.");
            emptyLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #888888;");
            emptyLabel.setAlignment(Pos.CENTER);
            emptyLabel.setPrefWidth(Double.MAX_VALUE);
            VBox.setVgrow(emptyLabel, Priority.ALWAYS);
            friendsList.setAlignment(Pos.CENTER);
            friendsList.getChildren().add(emptyLabel);
            return;
        }

        for (Friend friend : friends) {
            VBox card = new VBox();
            card.setSpacing(8);
            card.setMaxWidth(600);
            card.setStyle(
                    "-fx-background-color: #ffffff;" +
                            "-fx-padding: 15;" +
                            "-fx-border-color: #e0e0e0;" +
                            "-fx-border-radius: 10;" +
                            "-fx-background-radius: 10;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0.3, 0, 2);"
            );

            HBox content = new HBox();
            content.setSpacing(10);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPrefWidth(Double.MAX_VALUE);

            ImageView profileView = new ImageView();
            Image img = friend.getProfileImage();
            if (img != null)
            {
                profileView.setImage(img);
            } else {
                URL imageUrl = getClass().getResource("/Images/default_picture.jpg");
                if (imageUrl != null) {
                    profileView.setImage(new Image(imageUrl.toExternalForm()));
                } else {
                    profileView.setImage(new Image("https://via.placeholder.com/100"));
                }
            }

            profileView.setFitWidth(50);
            profileView.setFitHeight(50);
            profileView.setPreserveRatio(true);
            profileView.setStyle("-fx-background-radius: 25;");

            VBox info = new VBox();
            info.setSpacing(2);

            Label usernameLabel = new Label(friend.getUsername());
            usernameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

            Label bioLabel = new Label(friend.getBio());
            bioLabel.setStyle("-fx-text-fill: #555555;");

            Label lastActiveLabel = new Label(friend.getLastActive());
            lastActiveLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");

            info.getChildren().addAll(usernameLabel, bioLabel, lastActiveLabel);

            MenuItem viewProfileItem = new MenuItem("View Profile");
            MenuItem blockItem = new MenuItem("Block");

            MenuButton menuButton = new MenuButton("⋮", null, viewProfileItem, blockItem);
            menuButton.setStyle("-fx-font-size: 14px; -fx-text-fill: #333333;");
            menuButton.setFocusTraversable(false);

            HBox.setHgrow(info, Priority.ALWAYS);
            content.getChildren().addAll(profileView, info, menuButton);
            card.getChildren().add(content);

            friendsList.getChildren().add(card);
            viewProfileItem.setOnAction(event -> {
                viewFriendProfile(friend.getUsername());
            });

            blockItem.setOnAction(event -> {
                blockFriend(friend.getUsername());
            });

        }
    }
    private void viewFriendProfile(String username) {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/ProfileMenu.fxml"));
            Parent profileRoot = loader.load();

            ProfileMenuController controller = loader.getController();
            controller.setMainLayout(mainLayout);
            controller.loadUserProfile(username);

            mainLayout.setCenter(profileRoot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void blockFriend(String username) {
        String getFriendIdQuery = "SELECT user_id FROM users WHERE username = ?";
        String removeFriendQuery = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        String addToBlockedQuery = "INSERT INTO blocked_users (blocker_id, blocked_id, blocked_at) VALUES (?, ?, CURRENT_TIMESTAMP)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement getIdStmt = conn.prepareStatement(getFriendIdQuery)) {

            // Step 1: Get friend's user ID
            getIdStmt.setString(1, username);
            ResultSet rs = getIdStmt.executeQuery();

            if (rs.next()) {
                int friendId = rs.getInt("user_id");
                int currentUserId = getCurrentUserId();

                // Step 2: Remove from friends
                try (PreparedStatement removeStmt = conn.prepareStatement(removeFriendQuery)) {
                    removeStmt.setInt(1, currentUserId);
                    removeStmt.setInt(2, friendId);
                    removeStmt.executeUpdate();
                }

                // Step 3: Add to blocked_users table
                try (PreparedStatement blockStmt = conn.prepareStatement(addToBlockedQuery)) {
                    blockStmt.setInt(1, currentUserId);
                    blockStmt.setInt(2, friendId);
                    blockStmt.executeUpdate();
                }

                // Step 4: Refresh friend list
                List<Friend> updatedFriends = loadFriendsFromDatabase();
                displayFriends(updatedFriends);
                showAlert("User Blocked", username + " has been blocked and removed from your friends.");

            } else {
                showAlert("Error", "Could not find user ID for " + username);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "An error occurred while blocking the user.");
        }
    }
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }



    private String calculateLastActive(Timestamp lastActive) {
        if (lastActive == null) return "Offline";

        Duration duration = Duration.between(lastActive.toInstant(), Instant.now());
        long minutes = duration.toMinutes();

        if (minutes < 5) return "Active now";
        if (minutes < 60) return "Active " + minutes + " min ago";

        long hours = minutes / 60;
        if (hours < 24) return "Active " + hours + " hour" + (hours > 1 ? "s" : "") + " ago";

        long days = hours / 24;
        return "Active " + days + " day" + (days > 1 ? "s" : "") + " ago";
    }


    private int getCurrentUserId() {
        String username = UserSession.getInstance().getUser().getUsername();

        String query = "SELECT user_id FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Default value if user not found
    }

    public void setMainLayout(BorderPane mainLayout) {
        this.mainLayout = mainLayout;
    }

}
