import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class privacyViewController {

    @FXML private BorderPane mainLayout;

    // Checkboxes
    @FXML private CheckBox privateProfileCheckbox;
    @FXML private CheckBox readReceiptsCheckbox;
    @FXML private CheckBox onlineStatusCheckbox;

    // ComboBox
    @FXML private ComboBox<String> messagePermissionComboBox;

    // Password fields
    @FXML private PasswordField oldPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    // Blocked users
    @FXML private ListView<User> blockedUsersListView;
    @FXML private TextField blockUserField;
    @FXML private Button blockUserButton;

    @FXML private Button saveChangesButton;
    @FXML private Button cancelButton;
    @FXML private Button deleteAccountButton;
    @FXML private Button BackButton;

    public void initialize() {
        messagePermissionComboBox.setItems(FXCollections.observableArrayList("Everyone", "Friends Only", "No One"));
        loadUserSettings();
        loadBlockedUsers();
    }

    private void loadUserSettings() {
        int userId = UserSession.getInstance().getUser().getId();
        try {
            UserSettings settings = UserSettingsDAO.getUserSettings(userId);
            if (settings != null) {
                privateProfileCheckbox.setSelected(settings.isPrivate());
                readReceiptsCheckbox.setSelected(settings.isReadReceiptsEnabled());
                onlineStatusCheckbox.setSelected(settings.isOnlineStatusVisible());
                messagePermissionComboBox.setValue(settings.getMessagePermission());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load user settings.", Alert.AlertType.ERROR);
        }
    }

    private void loadBlockedUsers() {
        int userId = UserSession.getInstance().getUser().getId();

        List<User> blockedUsers = FriendNotificationDAO.getBlockedUsers(userId);
        ObservableList<User> observableList = FXCollections.observableArrayList(blockedUsers);
        blockedUsersListView.setItems(observableList);

        blockedUsersListView.setCellFactory(param -> new ListCell<>() {
            private final HBox container = new HBox(10);
            private final Label usernameLabel = new Label();
            private final Button unblockButton = new Button("Unblock");

            {
                container.setAlignment(Pos.CENTER_LEFT);
                container.getChildren().addAll(usernameLabel, unblockButton);
                unblockButton.setStyle("-fx-background-color: #e53935; -fx-text-fill: white;");
            }

            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);

                if (empty || user == null) {
                    setGraphic(null);
                } else {
                    usernameLabel.setText(user.getUsername());
                    unblockButton.setOnAction(e -> {
                        try {
                            FriendNotificationDAO.unblockUser(userId, user.getId());
                            loadBlockedUsers();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showAlert("Error", "Failed to unblock user.", Alert.AlertType.ERROR);
                        }
                    });
                    setGraphic(container);
                }
            }
        });
    }



    @FXML
    private void handleSaveChanges(ActionEvent event) {
        int userId = UserSession.getInstance().getUser().getId();

        String oldPass = oldPasswordField.getText();
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (!newPass.isEmpty()) {
            if (!newPass.equals(confirmPass)) {
                showAlert("Validation", "New passwords do not match.", Alert.AlertType.WARNING);
                return;
            }

            try {
                boolean changed = UserSettingsDAO.changePassword(userId, oldPass, newPass);
                if (!changed) {
                    showAlert("Authentication", "Incorrect old password.", Alert.AlertType.ERROR);
                    return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Password update failed.", Alert.AlertType.ERROR);
                return;
            }
        }

        boolean isPrivate = privateProfileCheckbox.isSelected();
        boolean readReceipts = readReceiptsCheckbox.isSelected();
        boolean onlineStatus = onlineStatusCheckbox.isSelected();
        String messagePermission = messagePermissionComboBox.getValue();

        if (messagePermission == null) {
            messagePermission = "Everyone";
        }

        String query = """
        INSERT INTO user_settings (user_id, is_private, read_receipts, online_status,message_permission)
        VALUES (?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            is_private = VALUES(is_private),
            read_receipts = VALUES(read_receipts),
            online_status = VALUES(online_status),
            message_permission = VALUES(message_permission)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            stmt.setBoolean(2, isPrivate);
            stmt.setBoolean(3, readReceipts);
            stmt.setBoolean(4, onlineStatus);
            stmt.setString(5, messagePermission);

            stmt.executeUpdate();
            showAlert("Success", "Settings saved successfully.", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to save settings.", Alert.AlertType.ERROR);
        }
    }


    @FXML
    private void handleBlockUser(ActionEvent event) {
        String username = blockUserField.getText().trim();
        int currentUserId = UserSession.getInstance().getUser().getId();

        if (!username.isEmpty()) {
            try {
                User blockedUser = User.getUserByUsername(username);
                if (blockedUser == null) {
                    showAlert("Error", "User not found.", Alert.AlertType.ERROR);
                    return;
                }

                int blockedUserId = blockedUser.getId();
                FriendNotificationDAO.blockUser(currentUserId, blockedUserId);
                blockUserField.clear();
                loadBlockedUsers();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "Failed to block user.", Alert.AlertType.ERROR);
            }
        }
    }


    @FXML
    private void handleDeleteAccount(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Account Deletion");
        confirm.setHeaderText("Are you sure you want to delete your account?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                int userId = UserSession.getInstance().getUser().getId();
                try {
                    deleteUserAccount(userId); // Delete from DB
                    showAlert("Account Deleted", "Your account has been successfully deleted.", Alert.AlertType.INFORMATION);
                    logoutUser();
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Error", "Failed to delete your account. Please try again later.", Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void deleteUserAccount(int userId) throws SQLException {
        String deleteSettingsSQL = "DELETE FROM user_settings WHERE user_id = ?";
        String deleteUserSQL = "DELETE FROM users WHERE user_Id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement stmt1 = conn.prepareStatement(deleteSettingsSQL);
                 PreparedStatement stmt2 = conn.prepareStatement(deleteUserSQL)) {

                stmt1.setInt(1, userId);
                stmt1.executeUpdate();

                stmt2.setInt(1, userId);
                int affectedRows = stmt2.executeUpdate();

                if (affectedRows == 0) {
                    conn.rollback();
                    throw new SQLException("Deleting user failed, no rows affected.");
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }


    private void logoutUser() {
        UserSession.getInstance().clearSession();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/LoginMenu.fxml"));
            Parent loginRoot = loader.load();
            Scene loginScene = new Scene(loginRoot);

            Stage currentStage = (Stage) mainLayout.getScene().getWindow();
            currentStage.setScene(loginScene);
            currentStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to load login screen.", Alert.AlertType.ERROR);
        }
    }


    @FXML
    private void handleBackButton(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/SettingsMenu.fxml"));
            Parent settingsView = loader.load();
            SettingsMenuController controller = loader.getController();
            controller.setMainLayout(mainLayout);
            mainLayout.setCenter(settingsView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setMainLayout(BorderPane mainLayout) {
        this.mainLayout = mainLayout;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }


}
