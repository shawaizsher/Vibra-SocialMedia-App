public class Comment {
    private int commentId;
    private int postId;
    private int userId;
    private String commentText;
    private String commentedAt;
    private String comment_username;

    // Constructor
    public Comment(int commentId, int postId, int userId, String commentText, String commentedAt, String username) {
        this.commentId = commentId;
        this.postId = postId;
        this.userId = userId;
        this.commentText = commentText;
        this.commentedAt = commentedAt;
        this.comment_username = username;
    }
    // Getters
    public int getCommentId() {
        return commentId;
    }
    public int getPostId() {
        return postId;
    }
    public int getUserId() {
        return userId;
    }
    public String getCommentText() {
        return commentText;
    }
    public String getCommentedAt() {
        return commentedAt;
    }
    public String getComment_Username() {
        return comment_username;
    }
    // Setters
    public void setCommentId(int commentId) {
        this.commentId = commentId;
    }
    public void setPostId(int postId) {
        this.postId = postId;
    }
    public void setUserId(int userId) {
        this.userId = userId;
    }
    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }
    public void setCommentedAt(String commentedAt) {
        this.commentedAt = commentedAt;
    }
    public void setComment_Username(String comment_username) {
        this.comment_username = comment_username;
    }


}
