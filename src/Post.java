public class Post {
    private int postId;
    private int userId;
    private String content;
    private String createdAt;
    private int likeCount;
    private byte[] media;
    private String mediaType;



    // Constructor
    public Post(int postId, int userId, String content, String createdAt, int likeCount) {
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
    }

    public Post(int postId, int userId, String content, String createdAt) {
        this.postId = postId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
    }


    public Post() {
    }

    // Getters
    public int getPostId() {
        return postId;
    }

    public int getUserId() {
        return userId;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public int getLikeCount() {
        return likeCount;
    }

    // Setters
    public void setPostId(int postId) {
        this.postId = postId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public byte[] getMedia() { return media; }
    public void setMedia(byte[] media) { this.media = media; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
}
