import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.scene.Scene;


public class RegisterMenuController {

    @FXML private TextField enterUsername;
    @FXML private TextField enterEmail;
    @FXML private PasswordField enterPassword;
    @FXML private PasswordField confirmPasswordField;

    @FXML private Label usernameStatusLabel;
    @FXML private Label emailErrorLabel;
    @FXML private Label passwordErrorLabel;
    @FXML private Label confirmPasswordErrorLabel;

    @FXML private Button enterBtn;
    @FXML private VBox verificationBox;

    private boolean usernameAvailable = false;
    private String generatedVerificationCode = null;

    @FXML
    public void checkUsername() {
        String username = enterUsername.getText().trim();
        if (username.isEmpty()) {
            usernameStatusLabel.setText("Username cannot be empty.");
            usernameStatusLabel.setStyle("-fx-text-fill: #ff6666;");
            usernameAvailable = false;
        } else if (username.length() < 3) {
            usernameStatusLabel.setText("Username must be at least 3 characters.");
            usernameStatusLabel.setStyle("-fx-text-fill: #ff6666;");
            usernameAvailable = false;
        }  else if (!username.matches("[a-z0-9_]+")) {
            usernameStatusLabel.setText("Only lowercase letters, numbers, and underscores allowed.");
            usernameStatusLabel.setStyle("-fx-text-fill: #ff6666;");
            usernameAvailable = false;
        } else if (username.equalsIgnoreCase("admin") || username.equalsIgnoreCase("takenname")) {
            usernameStatusLabel.setText("Username already taken.");
            usernameStatusLabel.setStyle("-fx-text-fill: #ff6666;");
            usernameAvailable = false;
        } else {
            usernameStatusLabel.setText("Username is available.");
            usernameStatusLabel.setStyle("-fx-text-fill: #66ff66;");
            usernameAvailable = true;
        }
    }

    @FXML
    private void enterBtnClick() {
        clearErrorLabels();

        String username = enterUsername.getText().trim();
        String email = enterEmail.getText().trim();
        String password = enterPassword.getText();
        String confirmPassword = confirmPasswordField.getText();

        boolean valid = true;

        if (username.isEmpty()) {
            usernameStatusLabel.setText("Username cannot be empty.");
            usernameStatusLabel.setStyle("-fx-text-fill: #ff6666;");
            usernameAvailable = false;
            valid = false;
        } else if (!usernameAvailable) {
            valid = false;
        }

        // Email check
        if (email.isEmpty() || !email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            emailErrorLabel.setText("Enter a valid email address.");
            valid = false;
        } else if (isEmailTaken(email)) {
            emailErrorLabel.setText("Email already in use.");
            valid = false;
        }

        // Password check
        if (password.length() < 6) {
            passwordErrorLabel.setText("Password must be at least 6 characters.");
            valid = false;
        }

        // Confirm password check
        if (!password.equals(confirmPassword)) {
            confirmPasswordErrorLabel.setText("Passwords do not match.");
            valid = false;
        }

        if (valid) {
            showVerificationDialog(email);

        }

    }

    private boolean isEmailTaken(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void showVerificationDialog(String email) {
        generatedVerificationCode = String.format("%06d", (int) (Math.random() * 1_000_000));

        EmailService.sendVerificationEmail(email, generatedVerificationCode);

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Email Verification");
        dialog.setHeaderText("A verification code was sent to " + email);
        dialog.setContentText("Enter the verification code:");

        dialog.getEditor().setStyle("-fx-text-fill: white; -fx-background-color: #333333;");
        ((Stage) dialog.getDialogPane().getScene().getWindow()).getScene().getStylesheets()
                .add(getClass().getResource("CssFiles/verificationdialogbox.css").toExternalForm());

        dialog.showAndWait().ifPresent(code -> {
            if (code.trim().equals(generatedVerificationCode)) {
                insertUserIntoDatabase(
                        enterUsername.getText().trim(),
                        enterEmail.getText().trim(),
                        enterPassword.getText() 
                );
                showSuccessAlert();
            } else {
                showErrorAlert("Invalid verification code. Please try again.");
            }
        });
    }


    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Verification Failed");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }


    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Registration Successful");
        alert.setHeaderText(null);
        alert.setContentText("Your account has been successfully registered!");
        alert.showAndWait();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/LoginMenu.fxml"));
            Parent loginRoot = loader.load();
            Stage stage = (Stage) enterBtn.getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearErrorLabels() {
        usernameStatusLabel.setText("");
        usernameStatusLabel.setStyle("-fx-text-fill: #ff6666;");
        emailErrorLabel.setText("");
        passwordErrorLabel.setText("");
        confirmPasswordErrorLabel.setText("");
    }
    @FXML
    private void handleLoginLinkClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/LoginMenu.fxml"));
            Parent loginRoot = loader.load();
            Stage stage = (Stage) enterBtn.getScene().getWindow();
            stage.setScene(new Scene(loginRoot));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void insertUserIntoDatabase(String username, String email, String password) {
        String insertUserSql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
        String getUserIdSql = "SELECT user_Id FROM users WHERE email = ?";
        String insertSettingsSql = "INSERT INTO user_settings (user_id, is_private, read_receipts, online_status, tag_review, message_permission) " +
                "VALUES (?, false, true, true, false, 'everyone')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement insertUserStmt = conn.prepareStatement(insertUserSql);
             PreparedStatement getUserIdStmt = conn.prepareStatement(getUserIdSql);
             PreparedStatement insertSettingsStmt = conn.prepareStatement(insertSettingsSql)) {

            // Insert user
            insertUserStmt.setString(1, username);
            insertUserStmt.setString(2, email);
            insertUserStmt.setString(3, password);
            insertUserStmt.executeUpdate();

            getUserIdStmt.setString(1, email);
            ResultSet rs = getUserIdStmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_Id");

                // Insert default settings
                insertSettingsStmt.setInt(1, userId);
                insertSettingsStmt.executeUpdate();

                System.out.println("User registered successfully with default settings.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



}
