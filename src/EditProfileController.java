import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;

import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;


public class EditProfileController {

    @FXML
    private byte[] newProfileImageBytes = null;
    @FXML
    private Button backButton;

    @FXML
    private TextArea bioField;

    @FXML
    private Button cancelButton;

    @FXML
    private Button changePictureBtn;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private BorderPane mainLayout;

    @FXML
    private PasswordField newPasswordField;

    @FXML
    private PasswordField oldPasswordField;

    @FXML
    private ImageView profileImageView;

    @FXML
    private Button saveButton;

    @FXML
    private HBox topBar;

    @FXML
    private TextField usernameField;

    @FXML
    public void initialize() {
        String username = UserSession.getInstance().getUser().getUsername();
        loadDataFromDatabase(username);
    }

    public void setMainLayout(BorderPane mainLayout) {
        this.mainLayout = mainLayout;
    }

    @FXML
    void handleBackButton(ActionEvent event) {
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

    void loadDataFromDatabase(String username) {
        String query = "SELECT bio, profile_picture FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // Load and set bio
                String bio = rs.getString("bio");
                bioField.setText(bio != null ? bio : "");

                // Load and set profile picture
                byte[] imageBytes = rs.getBytes("profile_picture");
                if (imageBytes != null) {
                    ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                    Image image = new Image(bis);
                    profileImageView.setImage(image);

                    profileImageView.setFitWidth(100);
                    profileImageView.setFitHeight(100);
                    profileImageView.setPreserveRatio(true);
                    profileImageView.setSmooth(true);


                    profileImageView.setClip(null);
                } else {
                    System.out.println("No profile picture found.");
                }

                usernameField.setText(username);
            } else {
                System.out.println("User not found.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void saveChanges(ActionEvent event) {
        String currentUsername = UserSession.getInstance().getUser().getUsername();

        String newUsername = usernameField.getText().trim();
        String bio = bioField.getText().trim();
        String oldPassword = oldPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (newUsername.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Username cannot be empty.");
            return;
        }

        boolean wantsToChangePassword = !newPassword.isEmpty() || !confirmPassword.isEmpty();

        if (wantsToChangePassword) {
            if (oldPassword.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Please enter your old password to change it.");
                return;
            }

            try (Connection conn = DatabaseConnection.getConnection()) {
                String verifyQuery = "SELECT password FROM users WHERE username = ?";
                try (PreparedStatement verifyStmt = conn.prepareStatement(verifyQuery)) {
                    verifyStmt.setString(1, currentUsername);
                    ResultSet rs = verifyStmt.executeQuery();

                    if (rs.next()) {
                        String dbPassword = rs.getString("password");
                        if (!dbPassword.equals(oldPassword)) {
                            showAlert(Alert.AlertType.ERROR, "Old password is incorrect.");
                            return;
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "User not found.");
                        return;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "An error occurred while verifying the password.");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                showAlert(Alert.AlertType.ERROR, "New passwords do not match.");
                return;
            }
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String updateQuery = "UPDATE users SET username = ?, bio = ?" +
                    (wantsToChangePassword ? ", password = ?" : "") +
                    (newProfileImageBytes != null ? ", profile_picture = ?" : "") +
                    " WHERE username = ?";

            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                updateStmt.setString(1, newUsername);
                updateStmt.setString(2, bio);

                int index = 3;
                if (wantsToChangePassword) {
                    updateStmt.setString(index++, newPassword);
                }
                if (newProfileImageBytes != null) {
                    updateStmt.setBytes(index++, newProfileImageBytes);
                }
                updateStmt.setString(index, currentUsername);

                int rowsUpdated = updateStmt.executeUpdate();
                if (rowsUpdated > 0) {
                    UserSession.getInstance().getUser().setUsername(newUsername);
                    showAlert(Alert.AlertType.INFORMATION, "Profile updated successfully.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Update failed. Try again.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "An error occurred: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Update Profile");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleChangePfp(ActionEvent event)
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Picture");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());

        if (selectedFile != null) {
            try (FileInputStream fis = new FileInputStream(selectedFile)) {
                Image image = new Image(fis);
                profileImageView.setImage(image);

                fis.getChannel().position(0);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    bos.write(buffer, 0, read);
                }
                newProfileImageBytes = bos.toByteArray();

            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Failed to load image.");
            }
        }
    }



}

