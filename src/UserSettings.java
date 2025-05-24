public class UserSettings {
    private int userId;
    private boolean isPrivate;
    private boolean readReceiptsEnabled;
    private boolean onlineStatusVisible;
    private String messagePermission;

    public UserSettings(int userId, boolean isPrivate, boolean readReceiptsEnabled,
                        boolean onlineStatusVisible, String messagePermission) {
        this.userId = userId;
        this.isPrivate = isPrivate;
        this.readReceiptsEnabled = readReceiptsEnabled;
        this.onlineStatusVisible = onlineStatusVisible;
        this.messagePermission = messagePermission;
    }

    public int getUserId() {
        return userId;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isReadReceiptsEnabled() {
        return readReceiptsEnabled;
    }

    public boolean isOnlineStatusVisible() {
        return onlineStatusVisible;
    }

    public String getMessagePermission() {
        return messagePermission;
    }

    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public void setReadReceiptsEnabled(boolean readReceiptsEnabled) {
        this.readReceiptsEnabled = readReceiptsEnabled;
    }

    public void setOnlineStatusVisible(boolean onlineStatusVisible) {
        this.onlineStatusVisible = onlineStatusVisible;
    }

    public void setMessagePermission(String messagePermission) {
        this.messagePermission = messagePermission;
    }
}
