
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.event.ActionEvent;

import java.io.IOException;

public class aboutPageController {

    @FXML
    private Button BackButton;
    private static BorderPane mainLayout;

    public static void setMainLayout(BorderPane layout) {
        mainLayout = layout;
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
}
