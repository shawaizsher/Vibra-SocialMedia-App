import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class NotificationsMenuController {

    @FXML
    private BorderPane mainLayout;

    @FXML
    private Button backButton;

    @FXML
    private CheckBox notificationsToggle;

    public void setMainLayout(BorderPane mainLayout) {
        this.mainLayout = mainLayout;
    }

    @FXML
    public void initialize() {
        int userId = UserSession.getInstance().getUser().getId();
        initializeNotificationsToggle(userId);
        setupNotificationsToggleListener(userId);
    }

    private void initializeNotificationsToggle(int userId) {
        String sql = "SELECT notifications_toggle FROM user_settings WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                boolean notificationsOff = rs.getBoolean("notifications_toggle");
                notificationsToggle.setSelected(!notificationsOff);
            } else {
                createDefaultUserSettings(userId);
                notificationsToggle.setSelected(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            displayError("Failed to load notification settings.");
        }
    }

    private void setupNotificationsToggleListener(int userId) {
        notificationsToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            try {
                updateNotificationsToggle(userId, !newValue); // newValue true means notifications ON, so store false in DB
            } catch (SQLException e) {
                e.printStackTrace();
                displayError("Failed to update notification settings.");
                // Revert CheckBox state on failure
                notificationsToggle.setSelected(oldValue);
            }
        });
    }

    private void updateNotificationsToggle(int userId, boolean notificationsOff) throws SQLException {
        String sql = """
            INSERT INTO user_settings (user_id, notifications_toggle)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE notifications_toggle = ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setBoolean(2, notificationsOff);
            stmt.setBoolean(3, notificationsOff);
            stmt.executeUpdate();
        }
    }

    private void createDefaultUserSettings(int userId) throws SQLException {
        String sql = "INSERT INTO user_settings (user_id, notifications_toggle) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setBoolean(2, false); // Default: notifications ON
            stmt.executeUpdate();
        }
    }

    private void displayError(String message) {
        Label errorLabel = new Label(message);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        mainLayout.setBottom(errorLabel);
    }

    @FXML
    private void handleBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FXML Files/SettingsMenu.fxml"));
            Parent centerView = loader.load();

            SettingsMenuController controller = loader.getController();
            controller.setMainLayout(mainLayout);

            mainLayout.setCenter(centerView);
        } catch (IOException e) {
            e.printStackTrace();
            displayError("Failed to load settings menu.");
        }
    }
}