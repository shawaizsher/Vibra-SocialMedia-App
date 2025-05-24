import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;

public class Friend {
    private String username;
    private String bio;
    private byte[] profileImageData;
    private String lastActive;

    public Friend(String username, String bio, byte[] profileImageData, String lastActive) {
        this.username = username;
        this.bio = bio;
        this.profileImageData = profileImageData;
        this.lastActive = lastActive;
    }

    public String getUsername() {
        return username;
    }

    public String getBio() {
        return bio;
    }

    public byte[] getProfileImageData() {
        return profileImageData;
    }

    public String getLastActive() {
        return lastActive;
    }

    public Image getProfileImage() {
        if (profileImageData != null) {
            return new Image(new ByteArrayInputStream(profileImageData));
        }
        return null;
    }
}
