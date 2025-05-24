import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.sql.Connection;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.File;
import javafx.geometry.Pos;



public class HomePageController {

    @FXML private VBox postsContainer;
    @FXML private TextArea postTextArea;

    @FXML private Label mediaLabel;
    private File selectedMediaFile = null;
    private String selectedMediaType = null;


    private User currentUser;

    @FXML
    private void handleSelectMedia(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Photo or Video");

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Media files", "*.jpg", "*.png", "*.mp4", "*.mov");
        fileChooser.getExtensionFilters().add(extFilter);

        selectedMediaFile = fileChooser.showOpenDialog(new Stage());

        if (selectedMediaFile != null) {
            mediaLabel.setText("Selected: " + selectedMediaFile.getName());

            // 🔽 Add this block to detect media type
            String fileName = selectedMediaFile.getName().toLowerCase();
            if (fileName.endsWith(".jpg") || fileName.endsWith(".png")) {
                selectedMediaType = "image";
            } else if (fileName.endsWith(".mp4") || fileName.endsWith(".mov")) {
                selectedMediaType = "video";
            } else {
                selectedMediaType = null; // fallback
            }

        } else {
            mediaLabel.setText("No media selected");
        }
    }



    public void initialize() {
        Font.loadFont(getClass().getResourceAsStream("/Fonts/Oregano-Regular.ttf"), 14);
        String username = UserSession.getInstance().getUser().getUsername();
        currentUser = User.getUserObjectByUsername(username);
        loadPosts();
    }

    @FXML
    private void handlePostSubmit() {
        String content = postTextArea.getText().trim();
        if (content.isEmpty() && selectedMediaFile == null) return;

        Post post = new Post();
        post.setUserId(currentUser.getId());
        post.setContent(content);
        post.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // Set media if selected
        if (selectedMediaFile != null && selectedMediaType != null) {
            try {
                byte[] mediaBytes = java.nio.file.Files.readAllBytes(selectedMediaFile.toPath());
                post.setMedia(mediaBytes);
                post.setMediaType(selectedMediaType);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (PostDAO.savePost(post)) {
            postTextArea.clear();
            selectedMediaFile = null;
            selectedMediaType = null;
            loadPosts();
        }
    }



    private void loadPosts() {
        postsContainer.getChildren().clear();

        Label yourPostLabel = new Label("Your Latest Post");
        yourPostLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        postsContainer.getChildren().add(yourPostLabel);

        Post latestUserPost = PostDAO.getLatestPostByUserId(currentUser.getId());
        if (latestUserPost != null) {
            postsContainer.getChildren().add(createPostNode(latestUserPost, true));
        }

        Label friendsPostLabel = new Label("Friend Posts");
        friendsPostLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10 0 0 0;");
        postsContainer.getChildren().add(friendsPostLabel);

        List<Integer> friendIds = FriendsDAO.getFriendIds(currentUser.getId());
        for (int friendId : friendIds) {
            List<Post> friendPosts = PostDAO.getLatestPostsByUserId(friendId, 3);
            for (Post post : friendPosts) {
                postsContainer.getChildren().add(createPostNode(post, false)); // friends
            }
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

        // Profile Picture
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
            profileImageView.setImage(new Image(imageUrl.toExternalForm()));
        }

        VBox contentBox = new VBox(2);
        Label usernameLabel = new Label(postUser.getUsername());
        usernameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Label contentLabel = new Label(post.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 16px;");

        Label timeLabel = new Label("Posted " + getTimeAgo(post.getCreatedAt()));
        timeLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 10;");

        contentBox.getChildren().addAll(usernameLabel, contentLabel, timeLabel);

        if (post.getMedia() != null && post.getMediaType() != null) {
            if (post.getMediaType().equalsIgnoreCase("image")) {
                ImageView mediaView = new ImageView(new Image(new ByteArrayInputStream(post.getMedia())));
                mediaView.setFitWidth(300);
                mediaView.setPreserveRatio(true);
                contentBox.getChildren().add(mediaView);
            } else if (post.getMediaType().equalsIgnoreCase("video")) {
                Label videoLabel = new Label("[Video content not previewable]");
                videoLabel.setStyle("-fx-font-style: italic; -fx-text-fill: gray;");
                contentBox.getChildren().add(videoLabel);
            }
        }

        HBox postLayout = new HBox(10, profileImageView, contentBox);
        postLayout.setAlignment(Pos.TOP_LEFT);

        int userId = UserSession.getInstance().getUser().getId();

        Button likeButton = new Button();
        boolean isLiked = PostDAO.hasUserLikedPost(userId, post.getPostId());
        likeButton.setText(isLiked ? "♥ Liked" : "♡ Like");
        likeButton.setStyle("-fx-background-color: #000080; -fx-font-size: 13px; -fx-text-fill: white");

        Label likeCountLabel = new Label(PostDAO.getLikeCount(post.getPostId()) + " likes");
        likeCountLabel.setStyle("-fx-font-weight: bold;");

        likeButton.setOnAction(e -> {
            if (PostDAO.toggleLike(userId, post.getPostId())) {
                boolean nowLiked = PostDAO.hasUserLikedPost(userId, post.getPostId());
                likeButton.setText(nowLiked ? "♥ Liked" : "♡ Like");
                likeCountLabel.setText(PostDAO.getLikeCount(post.getPostId()) + " likes");
            }
        });

        VBox commentSection = new VBox(5);
        List<Comment> comments = PostDAO.getCommentsByPostId(post.getPostId());
        for (Comment c : comments) {
            String timeAgo = getTimeAgo(c.getCommentedAt().replace("T", " "));
            Label commentLabel = new Label(timeAgo + " · " + c.getComment_Username() + ": " + c.getCommentText());
            commentLabel.setStyle("-fx-font-size: 12px;");
            commentSection.getChildren().add(commentLabel);
        }


        TextField commentField = new TextField();
        commentField.setPromptText("Add a comment...");
        commentField.setPrefWidth(200);

        Button commentButton = new Button("Comment");
        commentField.setOnAction(e -> commentButton.fire());

        commentButton.setOnAction(e -> {
            String text = commentField.getText().trim();
            if (!text.isEmpty()) {
                Comment newComment = new Comment(
                        0,
                        post.getPostId(),
                        userId,
                        text,
                        java.time.LocalDateTime.now().toString(),
                        UserSession.getInstance().getUser().getUsername()
                );
                PostDAO.addComment(newComment);
                commentField.clear();

                String timeAgo = getTimeAgo(newComment.getCommentedAt());
                Label newCommentLabel = new Label(timeAgo + " · " + newComment.getComment_Username() + ": " + newComment.getCommentText());
                newCommentLabel.setStyle("-fx-font-size: 12px;");
                commentSection.getChildren().add(newCommentLabel);
            }
        });

        HBox commentAndLikesBox = new HBox(10);
        commentAndLikesBox.setAlignment(Pos.CENTER_LEFT);
        commentAndLikesBox.getChildren().addAll(likeButton, likeCountLabel, commentField, commentButton);

        box.getChildren().addAll(postLayout, commentSection, commentAndLikesBox);
        return box;
    }


    private String getTimeAgo(String createdAt) {
        try {
            LocalDateTime postTime = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            Duration duration = Duration.between(postTime, LocalDateTime.now());

            if (duration.toMinutes() < 1) return "just now";
            if (duration.toMinutes() < 60) return duration.toMinutes() + " minutes ago";
            if (duration.toHours() < 24) return duration.toHours() + " hours ago";
            if (duration.toDays() < 7) return duration.toDays() + " days ago";
            return postTime.toLocalDate().toString();
        } catch (Exception e) {
            return createdAt;
        }
    }


    @FXML
    private void handleSelectPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Photo");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png"));
        selectedMediaFile = fileChooser.showOpenDialog(new Stage());
        if (selectedMediaFile != null) {
            selectedMediaType = "image";
            mediaLabel.setText("Selected: " + selectedMediaFile.getName()); // 🔽 Add this line
        } else {
            mediaLabel.setText("No media selected"); // Optional for clarity
        }
    }


    @FXML
    private void handleSelectVideo(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Video");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mov"));
        selectedMediaFile = fileChooser.showOpenDialog(new Stage());
        if (selectedMediaFile != null) {
            selectedMediaType = "video";
            mediaLabel.setText("Selected: " + selectedMediaFile.getName()); // 🔽 Add this line
        } else {
            mediaLabel.setText("No media selected"); // Optional for consistency
        }
    }




}