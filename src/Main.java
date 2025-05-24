/*
 * Semester Project: VIBRA - A Social Media Application
 * Course: [Database Management Systems Lab]
 * Institution: [Bahria University]
 * Semester: [Spring 2025]
 *
 * Team Members:
 * 1. Shawaiz Sher - Enrollment No: 01-131232-082 - Role: Developer
 * 2. Hassan Ali   - Enrollment No: 01-131232-032 - Role: Tester
 *
 * Project Description:
 * VIBRA is a social media application that allows users to login, register, and communicate
 * with friends via messages. It uses JavaFX for the UI, MySQL for the database, and WebSocket
 * for real-time messaging. This class serves as the main entry point for the application,
 * handling user authentication and navigation to different screens.
 *
 * Finalized on: May 24, 2025
 */

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.fxml.FXML;
import org.glassfish.tyrus.server.Server;
import MessagingService.ChatEndpoint;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class Main extends Application {

    // FXML injected fields for UI components

    private Server webSocketServer; // WebSocket server instance for real-time messaging


    @FXML
    private PasswordField passwordField; // Field for entering password

    @FXML
    private TextField usernameField; // Field for entering username

    /**
     * Starts the JavaFX application by launching the login screen and initializing the WebSocket server.
     *  primaryStage The primary stage for this application
     */
    @Override
    public void start(Stage primaryStage)
    {
        // Start the WebSocket server in a separate thread to handle real-time messaging
        startWebSocketServer();

        try {
            // Load the login menu FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/LoginMenu.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("CssFiles/style.css").toExternalForm());

            primaryStage.setTitle("Login Page");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();

        } catch (Exception e)
        {

            e.printStackTrace();
        }
    }
    /**
     * Starts the WebSocket server in a separate thread to handle real-time messaging.
     */
    private void startWebSocketServer() {
        new Thread(() -> {
            // Initialize and start the WebSocket server on port 8025
            webSocketServer = new Server("0.0.0.0", 8025, "/ws", null, ChatEndpoint.class);
            try {
                webSocketServer.start();
                System.out.println("WebSocket server started at ws://localhost:8025/ws/chat");
            } catch (Exception e) {
                // Log any exceptions that occur while starting the server
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Stops the WebSocket server when the application is closed.
     */
    @Override
    public void stop() throws Exception
    {
        if (webSocketServer != null) {
            webSocketServer.stop(); // Stop the WebSocket server
        }
        super.stop(); // Call the parent stop method
    }

    /**
     * Main entry point for the application.
     */
    public static void main(String[] args)
    {
        launch(args); // Launch the JavaFX application
    }

    /**
     * Handles the login button click event by authenticating the user and navigating to the main menu.
     */
    @FXML
    void LoginBtnClick(ActionEvent event)
    {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // SQL query to check if the username and password match a user in the database
        String query = "SELECT * FROM users WHERE username = ? AND password = ?";

        try
        {
            PreparedStatement preparedStatement = DatabaseConnection.getConnection().prepareStatement(query);
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next())
            {
                int userId = resultSet.getInt("user_id");
                User user = User.getUserById(userId);
                UserSession.init(user);

                FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/MainMenu.fxml"));
                BorderPane root = loader.load();

                FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("FXML Files/homePage.fxml"));
                Parent homeNode = homeLoader.load();

                MainMenuController mainMenuController = loader.getController();
                mainMenuController.setCenterContent(homeNode);

                Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("CssFiles/mainmenustyle.css").toExternalForm());

                stage.setScene(scene);
                stage.show();
            } else
            {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login Failed");
                alert.setHeaderText(null);
                alert.setContentText("Invalid username or password. Please try again.");
                alert.showAndWait();
            }

        }
        catch (SQLException | IOException e)
        {
            e.printStackTrace();
        }
    }
    /**
     * Handles the sign-up link click event by navigating to the registration screen.
     *  The action event triggered by the hyperlink click
     */
    @FXML
    void signUpBtnClick(ActionEvent event)
    {
        try
        {
            // Load the registration menu FXML file
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/RegisterMenu.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("CssFiles/style.css").toExternalForm());

            stage.setScene(scene);
            stage.show();

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Handles the forgot password link click event by prompting for an email and sending account details.
     */
    @FXML
    void forgotPasswordBtnClick(ActionEvent event)
    {
        // Create a dialog to prompt for the user's email
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Forgot Password");
        dialog.setHeaderText("Reset Your Password");
        dialog.setContentText("Please enter the email associated with your account:");

        // Show the dialog and process the result
        Optional<String> result = dialog.showAndWait();

        result.ifPresent(email -> {
            // SQL query to retrieve username and password by email
            String query = "SELECT username, password FROM users WHERE email = ?";
            try {
                // Prepare and execute the query
                PreparedStatement preparedStatement = DatabaseConnection.getConnection().prepareStatement(query);
                preparedStatement.setString(1, email);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    // User found: retrieve username and password
                    String username = resultSet.getString("username");
                    String password = resultSet.getString("password");

                    // Prepare email content with account details
                    String emailContent = "Dear user,\n\n" +
                            "Here are your account details:\n" +
                            "Username: " + username + "\n" +
                            "Password: " + password + "\n\n" +
                            "Please keep this information secure and consider resetting your password after logging in.\n\n" +
                            "Best regards,\nVIBRA Team";

                    // Send the email using EmailService
                    EmailService.sendAccountDetailsEmail(email, emailContent);

                    // Display a confirmation alert
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Account Details Sent");
                    alert.setHeaderText(null);
                    alert.setContentText("An email with your account details has been sent to " + email + ".");
                    alert.showAndWait();
                } else
                {
                    // Email not found: display an error alert
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText(null);
                    alert.setContentText("No account found with the email: " + email);
                    alert.showAndWait();
                }
            }
            catch (SQLException e)
            {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Database Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to retrieve account details: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
            } catch (IOException e)
            {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Email Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to send email: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
            }
        });
    }
}