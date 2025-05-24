import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.time.Duration;
import java.util.Optional;


public class ProfileMenuController {

    private boolean isBlocked = false;
    private boolean requestSent = false;

    @FXML
    private Label friendsCount;
    @FXML
    private Label postsCount;
    @FXML
    private Label userNameLabel;
    @FXML
    private Label userBio;
    @FXML
    private ImageView profilePic;
    @FXML
    public BorderPane mainLayout;
    @FXML
    private VBox postsContainer;
    @FXML
    private Button editProfileButton;
    @FXML
    private Button addFriendButton;
    @FXML
    private Button blockUserButton;


    @FXML
    private HBox friendRequestButtonsBox;

    private User profileUser;

    public void initialize() {
        String css = getClass().getResource("CssFiles/profile.css").toExternalForm();
        if (css != null) {
            mainLayout.getStylesheets().add(css);
        } else {
            System.out.println("CSS file not found.");
        }
    }
    public void updateRequestButtonsVisibility(boolean show) {
        friendRequestButtonsBox.setVisible(show);
        friendRequestButtonsBox.setManaged(show);
    }
    public void loadUserProfile(String username) {
        editProfileButton.setVisible(false);
        userNameLabel.setText(username);
        loadProfilePicture(username);

        try (Connection conn = DatabaseConnection.getConnection()) {
            int userId = getUserId(conn, username);
            if (userId == -1) {
                System.out.println("No user_id found for " + username);
                friendsCount.setText("0");
                postsCount.setText("0");
                userBio.setText("No bio available");
                return;
            }

            profileUser = new User();
            profileUser.setId(userId);
            profileUser.setUsername(username);

            int currentUserId = UserSession.getInstance().getUser().getId();
            boolean isPrivate = false;
            boolean isFriend = false;

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT is_private FROM user_settings WHERE user_id = ?")) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    isPrivate = rs.getBoolean("is_private");
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM friends f1\n" +
                            "WHERE f1.user_id = ? AND f1.friend_id = ?\n" +
                            "UNION\n" +
                            "SELECT 1 FROM friends f2\n" +
                            "WHERE f2.user_id = ? AND f2.friend_id = ?")) {
                ps.setInt(1, currentUserId);
                ps.setInt(2, userId);
                ps.setInt(3, userId);
                ps.setInt(4, currentUserId);

                ResultSet rs = ps.executeQuery();
                isFriend = rs.next();
            }


            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) AS friend_count FROM friends f1 JOIN friends f2 " +
                            "ON f1.user_id = f2.friend_id AND f1.friend_id = f2.user_id WHERE f1.user_id = ?")) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                friendsCount.setText(rs.next() ? String.valueOf(rs.getInt("friend_count")) : "0");
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) AS post_count FROM posts WHERE user_id = ?")) {
                ps.setInt(1, userId);
                ResultSet rs = ps.executeQuery();
                postsCount.setText(rs.next() ? String.valueOf(rs.getInt("post_count")) : "0");
            }

            if (isPrivate && currentUserId != userId && !isFriend) {
                userBio.setText("This profile is private.");
                postsContainer.getChildren().clear(); // Optional: show label
            } else {
                // Bio
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT bio FROM users WHERE username = ?")) {
                    ps.setString(1, username);
                    ResultSet rs = ps.executeQuery();
                    userBio.setText(rs.next() ? rs.getString("bio") : "No bio available");
                }

                loadUserPosts(userId);
            }

            if (currentUserId != userId) {
                blockUserButton.setVisible(true);

                if (isFriend) {
                    addFriendButton.setText("Remove Friend");
                    addFriendButton.setStyle("-fx-background-color: linear-gradient(to right, #1e3c72, #2a0845);"); // Optional: red/yellow
                    addFriendButton.setOnAction(e -> removeFriend(currentUserId, userId));
                } else {
                    addFriendButton.setText("Add Friend");
                    addFriendButton.setStyle("-fx-background-color: linear-gradient(to right, #1e3c72, #2a0845);"); // Original style

                    addFriendButton.setOnAction(e -> {
                        try {
                            boolean success = FriendNotificationDAO.sendFriendRequest(currentUserId, userId);
                            if (success) {
                                addFriendButton.setText("Request Sent");
                                addFriendButton.setDisable(true);
                                System.out.println("Friend request sent.");
                            } else {
                                System.out.println("Friend request failed (possibly blocked or already pending).");
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            }

           setProfileUser(profileUser);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void removeFriend(int userId, int friendId) {
        try (Connection conn = DatabaseConnection.getConnection())
        {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)")) {
                ps.setInt(1, userId);
                ps.setInt(2, friendId);
                ps.setInt(3, friendId);
                ps.setInt(4, userId);
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("Friend removed.");
                    loadUserProfile(profileUser.getUsername());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private int getUserId(Connection conn, String username) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT user_id FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("user_id") : -1;
        }
    }

    private void loadProfilePicture(String username) {
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement("SELECT profile_picture FROM users WHERE username = ?")) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                byte[] imgBytes = rs.getBytes("profile_picture");
                if (imgBytes != null && imgBytes.length > 0) {
                    profilePic.setImage(new Image(new ByteArrayInputStream(imgBytes)));
                } else {
                    loadDefaultProfilePicture();
                }
            } else {
                loadDefaultProfilePicture();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            loadDefaultProfilePicture();
        }

        profilePic.setFitWidth(120);
        profilePic.setFitHeight(100);
        profilePic.setPreserveRatio(false);
        profilePic.setStyle("-fx-border-color: #ccc; -fx-border-width: 1px;");
    }

    private void loadDefaultProfilePicture()
    {
        URL imageUrl = getClass().getResource("/Images/default_picture.jpg");
        if (imageUrl != null) {
            profilePic.setImage(new Image(imageUrl.toExternalForm()));
        }
    }
    private void loadUserPosts(int userId) {
        postsContainer.getChildren().clear();
        int currentUserId = UserSession.getInstance().getUser().getId();

        List<Post> posts = PostDAO.getPostsByUserId(userId);
        for (Post post : posts) {
            boolean isUserPost = (post.getUserId() == currentUserId);
            postsContainer.getChildren().add(createPostNode(post, isUserPost));
        }
    }

    private VBox createPostNode(Post post, boolean isUserPost) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 10;");

        User postUser = User.getUserById(post.getUserId());
        if (postUser == null) {
            postUser = new User();
            postUser.setUsername("Unknown");
        }

        ImageView profileImageView = new ImageView();
        profileImageView.setFitWidth(50);
        profileImageView.setFitHeight(50);
        profileImageView.setPreserveRatio(false);
        profileImageView.setSmooth(true);
        profileImageView.setClip(new Circle(25, 25, 25));

        byte[] profilePicBytes = postUser.getProfilePicture();
        if (profilePicBytes != null && profilePicBytes.length > 0) {
            profileImageView.setImage(new Image(new ByteArrayInputStream(profilePicBytes)));
        } else {
            URL imageUrl = getClass().getResource("/Images/default_picture.jpg");
            if (imageUrl != null) {
                profileImageView.setImage(new Image(imageUrl.toExternalForm()));
            }
        }

        VBox contentBox = new VBox(2);
        Label usernameLabel = new Label(postUser.getUsername());
        usernameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label contentLabel = new Label(post.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 16px;");

        String createdAtStr = post.getCreatedAt();
        String timeAgoText;
        if (createdAtStr != null && !createdAtStr.isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
                LocalDateTime createdAt = LocalDateTime.parse(createdAtStr, formatter);
                timeAgoText = getTimeAgo(createdAt);
            } catch (DateTimeParseException parseException) {
                parseException.printStackTrace();
                timeAgoText = "unknown time";
            }
        } else {
            timeAgoText = "unknown time";
        }
        Label timeLabel = new Label("Posted " + timeAgoText);
        timeLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");

        contentBox.getChildren().addAll(usernameLabel, contentLabel, timeLabel);

        if (post.getMedia() != null && post.getMediaType() != null) {
            if (post.getMediaType().equalsIgnoreCase("image")) {
                ImageView mediaView = new ImageView(new Image(new ByteArrayInputStream(post.getMedia())));
                mediaView.setFitWidth(300);
                mediaView.setPreserveRatio(true);
                mediaView.setSmooth(true);
                contentBox.getChildren().add(mediaView);
            } else if (post.getMediaType().equalsIgnoreCase("video")) {
                Label videoLabel = new Label("[Video content not previewable]");
                videoLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
                contentBox.getChildren().add(videoLabel);
            }
        }

        int userId = UserSession.getInstance().getUser().getId();

        MenuButton optionsMenu = new MenuButton("⋮");
        optionsMenu.setStyle("-fx-background-color: transparent; -fx-font-size: 18px; -fx-text-fill: #555;");
        optionsMenu.setFocusTraversable(false);
        optionsMenu.setPopupSide(Side.BOTTOM);

        if (post.getUserId() == userId) {
            MenuItem editItem = new MenuItem("Edit Post");
            MenuItem deleteItem = new MenuItem("Remove Post");

            editItem.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog(post.getContent());
                dialog.setTitle("Edit Post");
                dialog.setHeaderText("Edit your post:");
                dialog.setContentText("Content:");
                dialog.showAndWait().ifPresent(newContent -> {
                    if (!newContent.trim().isEmpty()) {
                        post.setContent(newContent.trim());
                        PostDAO.updatePostContent(post.getPostId(), newContent.trim());
                        contentLabel.setText(newContent.trim());
                    }
                });
            });

            deleteItem.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Delete Post");
                alert.setHeaderText("Are you sure you want to delete this post?");
                alert.setContentText("This action cannot be undone.");

                alert.showAndWait().ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        PostDAO.deletePost(post.getPostId());
                        ((VBox) box.getParent()).getChildren().remove(box);
                    }
                });
            });

            optionsMenu.getItems().addAll(editItem, deleteItem);
        } else {
            optionsMenu.setVisible(false);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(15, profileImageView, contentBox, spacer, optionsMenu);
        topRow.setAlignment(Pos.TOP_LEFT);


        Button likeButton = new Button();
        boolean isLiked = PostDAO.hasUserLikedPost(userId, post.getPostId());
        likeButton.setText(isLiked ? "♥ Liked" : "♡ Like");
        likeButton.setStyle("-fx-background-color: #000080; -fx-font-size: 13px; -fx-text-fill: white");

        Label likeCountLabel = new Label(PostDAO.getLikeCount(post.getPostId()) + " likes");
        likeCountLabel.setStyle("-fx-font-weight: bold;");

        likeButton.setOnAction(event -> {
            if (PostDAO.toggleLike(userId, post.getPostId())) {
                boolean nowLiked = PostDAO.hasUserLikedPost(userId, post.getPostId());
                likeButton.setText(nowLiked ? "♥ Liked" : "♡ Like");
                likeCountLabel.setText(PostDAO.getLikeCount(post.getPostId()) + " likes");
            }
        });

        VBox commentSection = new VBox(5);
        List<Comment> comments = PostDAO.getCommentsByPostId(post.getPostId());
        for (Comment c : comments) {
            String commentTimeAgo;
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
                LocalDateTime commentedAt = LocalDateTime.parse(c.getCommentedAt(), formatter);
                commentTimeAgo = getTimeAgo(commentedAt);
            } catch (DateTimeParseException e) {
                e.printStackTrace();
                commentTimeAgo = "unknown time";
            }

            Label commentLabel = new Label(commentTimeAgo + " · " + c.getComment_Username() + ": " + c.getCommentText());
            commentLabel.setStyle("-fx-font-size: 12px;");
            commentSection.getChildren().add(commentLabel);
        }

        TextField commentField = new TextField();
        commentField.setPromptText("Add a comment...");
        commentField.setPrefWidth(200);

        Button commentButton = new Button("Comment");
        commentField.setOnAction(event -> commentButton.fire());

        commentButton.setOnAction(event -> {
            String text = commentField.getText().trim();
            if (!text.isEmpty()) {
                Comment newComment = new Comment(
                        0,
                        post.getPostId(),
                        userId,
                        text,
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                        UserSession.getInstance().getUser().getUsername()
                );
                PostDAO.addComment(newComment);
                commentField.clear();

                String commentTimeAgo;
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
                    LocalDateTime commentedAt = LocalDateTime.parse(newComment.getCommentedAt(), formatter);
                    commentTimeAgo = getTimeAgo(commentedAt);
                } catch (DateTimeParseException commentParseException) {
                    commentParseException.printStackTrace();
                    commentTimeAgo = "unknown time";
                }

                Label newCommentLabel = new Label(commentTimeAgo + " · " + newComment.getComment_Username() + ": " + newComment.getCommentText());
                newCommentLabel.setStyle("-fx-font-size: 12px;");
                commentSection.getChildren().add(newCommentLabel);
            }
        });

        HBox commentAndLikesBox = new HBox(10);
        commentAndLikesBox.setAlignment(Pos.CENTER_LEFT);
        commentAndLikesBox.getChildren().addAll(likeButton, likeCountLabel, commentField, commentButton);

        box.getChildren().addAll(topRow, commentSection, commentAndLikesBox);
        return box;
    }


    private void configureAddFriendButton(User user, boolean requestAlreadySent) {
        int loggedInUserId = UserSession.getInstance().getUser().getId();

        if (requestAlreadySent) {
            addFriendButton.setText("Unsend Request");
            addFriendButton.setDisable(false);
            addFriendButton.setOnAction(e -> {
                try {
                    FriendNotificationDAO.cancelSentFriendRequest(loggedInUserId, user.getId());
                    configureAddFriendButton(user, false);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Request Cancelled");
                    alert.setHeaderText(null);
                    alert.setContentText("Friend request unsent.");
                    alert.showAndWait();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Unable to unsend friend request");
                    alert.setContentText("Something went wrong.");
                    alert.showAndWait();
                }
            });
        } else {
            addFriendButton.setText("Add Friend");
            addFriendButton.setDisable(false);
            addFriendButton.setOnAction(e -> {
                try {
                    FriendNotificationDAO.sendFriendRequest(loggedInUserId, user.getId());
                    configureAddFriendButton(user, true);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Friend Request Sent");
                    alert.setHeaderText(null);
                    alert.setContentText("Friend request sent to " + user.getUsername());
                    alert.showAndWait();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Unable to send friend request");
                    alert.setContentText("Something went wrong.");
                    alert.showAndWait();
                }
            });
        }
    }

    @FXML
    private void handleEditProfile() {
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

    private String getTimeAgo(LocalDateTime createdAt) {
        Duration duration = Duration.between(createdAt, LocalDateTime.now());

        long seconds = duration.getSeconds();
        if (seconds < 10) return "just now";
        if (seconds < 60) return seconds + "s ago";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ago";
        long days = hours / 24;
        if (days < 7) return days + "d ago";
        long weeks = days / 7;
        if (weeks < 4) return weeks + "w ago";
        long months = days / 30;
        if (months < 12) return months + "mo ago";
        long years = days / 365;
        return years + "y ago";
    }

    public void setMainLayout(BorderPane mainLayout) {
        this.mainLayout = mainLayout;
    }
    private void setProfileUser(User user) {
        int loggedInUserId = UserSession.getInstance().getUser().getId();
        boolean isOwnProfile = (loggedInUserId == user.getId());
        this.profileUser = user;

        editProfileButton.setVisible(isOwnProfile);
        addFriendButton.setVisible(!isOwnProfile);
        blockUserButton.setVisible(!isOwnProfile);
        updateRequestButtonsVisibility(false);

        if (isOwnProfile) return;

        try {
            this.isBlocked = FriendNotificationDAO.isBlocked(loggedInUserId, user.getId());
            blockUserButton.setText(isBlocked ? "Unblock" : "Block");

            blockUserButton.setOnAction(e -> {
                try {
                    if (isBlocked) {
                        FriendNotificationDAO.unblockUser(loggedInUserId, user.getId());
                        isBlocked = false;
                        blockUserButton.setText("Block");

                        if (!FriendNotificationDAO.areAlreadyFriends(loggedInUserId, user.getId())) {
                            configureAddFriendButton(user, false);
                        }

                    } else {
                        FriendNotificationDAO.blockUser(loggedInUserId, user.getId());
                        isBlocked = true;
                        blockUserButton.setText("Unblock");

                        addFriendButton.setDisable(true);
                        addFriendButton.setText("Blocked");
                        addFriendButton.setOnAction(null);
                        updateRequestButtonsVisibility(false);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });

            if (isBlocked) {
                addFriendButton.setDisable(true);
                addFriendButton.setText("Blocked");
                addFriendButton.setOnAction(null);
                return;
            }

            if (FriendNotificationDAO.areAlreadyFriends(loggedInUserId, user.getId())) {
                addFriendButton.setText("Remove Friend");
                addFriendButton.setDisable(false);
                addFriendButton.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Remove Friend");
                    confirm.setHeaderText(null);
                    confirm.setContentText("Are you sure you want to remove " + user.getUsername() + " as a friend?");

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        removeFriend(loggedInUserId, user.getId());
                        configureAddFriendButton(user, false);

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Removed");
                        alert.setHeaderText(null);
                        alert.setContentText("You have removed " + user.getUsername() + " from your friends.");
                        alert.showAndWait();
                    }
                });
            }

            else if (FriendNotificationDAO.hasIncomingRequest(user.getId(), loggedInUserId)) {
                Button acceptButton = new Button("Accept Request");
                Button rejectButton = new Button("Reject Request");

                acceptButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                rejectButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

                acceptButton.setOnAction(e -> {
                    try {
                        int requestId = FriendNotificationDAO.getPendingRequestId(user.getId(), loggedInUserId);
                        FriendNotificationDAO.acceptFriendRequest(requestId);
                        loadUserProfile(user.getUsername());
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                });

                rejectButton.setOnAction(e -> {
                    try {
                        int requestId = FriendNotificationDAO.getPendingRequestId(user.getId(), loggedInUserId);
                        FriendNotificationDAO.rejectFriendRequest(requestId);
                        updateRequestButtonsVisibility(false);
                        configureAddFriendButton(user, false);

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Request Rejected");
                        alert.setHeaderText(null);
                        alert.setContentText("Friend request rejected.");
                        alert.showAndWait();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Unable to reject request");
                        alert.setContentText("Something went wrong.");
                        alert.showAndWait();
                    }
                });

                friendRequestButtonsBox.getChildren().setAll(acceptButton, rejectButton);
                updateRequestButtonsVisibility(true);
                addFriendButton.setVisible(false);
            }

            else if (FriendNotificationDAO.isRequestPending(loggedInUserId, user.getId())) {
                configureAddFriendButton(user, true);
            }

            else {
                configureAddFriendButton(user, false);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
