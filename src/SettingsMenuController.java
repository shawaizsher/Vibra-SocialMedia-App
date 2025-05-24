import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;

import java.io.IOException;

public class SettingsMenuController {
    private MainMenuController mainMenuController;

    public void setMainMenuController(MainMenuController mainMenuController) {
        this.mainMenuController = mainMenuController;
    }

    public void initialize() {
        Font.loadFont(getClass().getResourceAsStream("/Fonts/AniczarSerif-Regular.ttf"), 14);

    }

    private BorderPane mainLayout;

    public void setMainLayout(BorderPane mainLayout) {
        this.mainLayout = mainLayout;
    }

    @FXML
    void goToEditProfile(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/EditProfile.fxml"));
            Parent editProfileView = loader.load();

            EditProfileController controller = loader.getController();
            controller.setMainLayout(mainLayout);

            mainLayout.setCenter(editProfileView);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goToAboutPage(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/aboutPage.fxml"));
            Parent editProfileView = loader.load();

            aboutPageController controller = loader.getController();
            controller.setMainLayout(mainLayout);

            mainLayout.setCenter(editProfileView);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goToPrivacy(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML Files/privacy.fxml"));
            Parent privacyView = loader.load();

            privacyViewController controller = loader.getController();
            controller.setMainLayout(mainLayout);

            mainLayout.setCenter(privacyView);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void goToNotifications(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("FXML files/notifications_Menu.fxml"));
            BorderPane notificationsView = loader.load();

            NotificationsMenuController controller = loader.getController();
            controller.setMainLayout(mainLayout);

            mainLayout.setCenter(notificationsView);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
