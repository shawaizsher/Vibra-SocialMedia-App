import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenuController {

    @FXML
    private BorderPane mainLayout;

    @FXML
    private TextField searchBox;

    @FXML
    private ListView<String> suggestionsListView;

    @FXML
    private Button logOutBtn;

    @FXML
    private Button notificationsBtn;

    @FXML
    public void initialize() {
        Font.loadFont(getClass().getResourceAsStream("/Fonts/AniczarSerif-Regular.ttf"), 14);

    }

    // Load user profile by username
    private void loadUserProfile(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/ProfileMenu.fxml"));
            Parent profileView = loader.load();

            ProfileMenuController controller = loader.getController();
            controller.setMainLayout(mainLayout);
            controller.loadUserProfile(username);

            mainLayout.setCenter(profileView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handle logout
    @FXML
    void handleLogout(ActionEvent event) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Logout");
        confirmAlert.setHeaderText("Are you sure you want to log out?");
        confirmAlert.setContentText("Click OK to logout or Cancel to stay.");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            Alert goodbye = new Alert(Alert.AlertType.INFORMATION);
            goodbye.setTitle("Logout");
            goodbye.setHeaderText("Goodbye!");
            goodbye.setContentText("You have been logged out.");
            goodbye.showAndWait();

            try {
                UserSession.clearSession();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/LoginMenu.fxml"));
                Parent loginView = loader.load();

                Stage currentStage = (Stage) mainLayout.getScene().getWindow();
                currentStage.setScene(new Scene(loginView));
                currentStage.centerOnScreen();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Load Home page
    @FXML
    public void showHome(ActionEvent event) {
        try {
            Parent homeView = FXMLLoader.load(getClass().getResource("FXML Files/homePage.fxml"));
            mainLayout.setCenter(homeView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load Friends page
    @FXML
    void showFriends(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/friendsPage.fxml"));
            Parent friendsView = loader.load();

            FriendsPageController controller = loader.getController();
            controller.setMainLayout(mainLayout);

            mainLayout.setCenter(friendsView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load Messages page
    @FXML
    void showMessages(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/MessageMenu.fxml"));
            Parent messagesView = loader.load();
            mainLayout.setCenter(messagesView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load Logged-In User's Profile
    @FXML
    public void showProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/ProfileMenu.fxml"));
            Parent profileView = loader.load();

            String username = UserSession.getInstance().getUser().getUsername();
            ProfileMenuController controller = loader.getController();
            controller.setMainLayout(mainLayout);
            controller.loadUserProfile(username);

            mainLayout.setCenter(profileView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load Notifications page
    @FXML
    public void showNotifications() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/notificationsPage.fxml"));
            Parent notificationsView = loader.load();

            NotificationsController controller = loader.getController();
            controller.setMainLayout(mainLayout);

            mainLayout.setCenter(notificationsView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load Settings page
    @FXML
    void showSettings(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/SettingsMenu.fxml"));
            Parent settingsView = loader.load();

            SettingsMenuController controller = loader.getController();
            controller.setMainLayout(mainLayout);
            controller.setMainMenuController(this);

            String cssPath = getClass().getResource("CssFiles/settings.css").toExternalForm();
            if (cssPath != null) {
                settingsView.getStylesheets().add(cssPath);
                System.out.println("CSS loaded successfully: " + cssPath);
            }

            mainLayout.setCenter(settingsView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Search functionality
    @FXML
    private void handleSearch() {
        String query = searchBox.getText().trim();
        if (query.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Search Error", "Please enter a username to search.");
            return;
        }

        User foundUser = User.getUserByUsername(query);
        if (foundUser != null) {
            showAlert(Alert.AlertType.INFORMATION, "User Found", "User \"" + foundUser.getUsername() + "\" found.");
            loadUserProfile(foundUser.getUsername());
        } else {
            showAlert(Alert.AlertType.ERROR, "User Not Found", "No user found with the username \"" + query + "\".");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // Set center node in BorderPane
    public void setMainLayoutCenter(Parent node) {
        mainLayout.setCenter(node);
    }

    public void applyTheme(String cssPath) {
        try {
            Parent root = mainLayout.getScene().getRoot();
            root.getStylesheets().clear();
            root.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            System.out.println("Theme applied: " + cssPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Optional getter for mainLayout if needed
    public BorderPane getMainLayout() {
        return mainLayout;
    }
    public void setCenterContent(Parent content) {
        mainLayout.setCenter(content);
    }
}
