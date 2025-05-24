import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class PostDAO {

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static Connection getConnection() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    public static boolean savePost(Post post) {
        String sql = "INSERT INTO posts (user_id, content, created_at, media_type, media) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, post.getUserId());
            stmt.setString(2, post.getContent());

            // Parse post.getCreatedAt string to Timestamp for DB insertion
            try {
                LocalDateTime dateTime = LocalDateTime.parse(post.getCreatedAt(), ISO_FORMATTER);
                stmt.setTimestamp(3, Timestamp.valueOf(dateTime));
            } catch (DateTimeParseException e) {
                // fallback to current time if parsing fails
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            }

            if (post.getMedia() != null && post.getMediaType() != null && !post.getMediaType().isEmpty()) {
                stmt.setString(4, post.getMediaType());
                stmt.setBytes(5, post.getMedia());
            } else {
                stmt.setNull(4, Types.VARCHAR);
                stmt.setNull(5, Types.BLOB);
            }

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static List<Post> getAllPosts() {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM posts ORDER BY created_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Post post = extractPostFromResultSet(rs);
                posts.add(post);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return posts;
    }


    public static Post getLatestPostByUserId(int userId) {
        String sql = "SELECT * FROM posts WHERE user_id = ? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return extractPostFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static List<Post> getLatestPostsByUserId(int userId, int limit) {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM posts WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    posts.add(extractPostFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }


    public static boolean likePost(int userId, int postId) {
        String sql = "INSERT INTO post_likes (user_id, post_id) VALUES (?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, postId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            // You might want to check for duplicate key exceptions if user already liked
            // For now, just print stack trace
            e.printStackTrace();
            return false;
        }
    }


    public static boolean unlikePost(int userId, int postId) {
        String sql = "DELETE FROM post_likes WHERE user_id = ? AND post_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, postId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static int getLikeCount(int postId) {
        String sql = "SELECT COUNT(*) FROM post_likes WHERE post_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }


    public static boolean addComment(Comment comment) {
        String sql = "INSERT INTO post_comments (post_id, user_id, content, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, comment.getPostId());
            stmt.setInt(2, comment.getUserId());
            stmt.setString(3, comment.getCommentText());

            try {
                LocalDateTime dateTime = LocalDateTime.parse(comment.getCommentedAt(), ISO_FORMATTER);
                stmt.setTimestamp(4, Timestamp.valueOf(dateTime));
            } catch (DateTimeParseException e) {
                // Fallback to current time if parsing fails
                stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            }

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static List<Comment> getCommentsByPostId(int postId) {
        List<Comment> comments = new ArrayList<>();
        String query = "SELECT pc.comment_id, pc.post_id, pc.user_id, pc.content, pc.created_at, " +
                "u.username, u.profile_picture " +
                "FROM post_comments pc " +
                "JOIN users u ON pc.user_id = u.user_ID " +
                "WHERE pc.post_id = ? " +
                "ORDER BY pc.created_at ASC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, postId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int commentId = rs.getInt("comment_id");
                    int userId = rs.getInt("user_id");
                    String content = rs.getString("content");
                    Timestamp ts = rs.getTimestamp("created_at");
                    String createdAt = ts != null ? ts.toLocalDateTime().format(DISPLAY_FORMATTER) : "";
                    String username = rs.getString("username");

                    Comment comment = new Comment(commentId, postId, userId, content, createdAt, username);
                    comments.add(comment);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return comments;
    }


    public static boolean hasUserLikedPost(int userId, int postId) {
        String sql = "SELECT 1 FROM post_likes WHERE user_id = ? AND post_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, postId);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static boolean toggleLike(int userId, int postId) {
        if (hasUserLikedPost(userId, postId)) {
            return unlikePost(userId, postId);
        } else {
            return likePost(userId, postId);
        }
    }


    public static void updatePostContent(int postId, String newContent) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE posts SET content = ? WHERE post_id = ?")) {
            stmt.setString(1, newContent);
            stmt.setInt(2, postId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static boolean deletePost(int postId) {
        String sql = "DELETE FROM posts WHERE post_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    // Helper method to extract a Post object from ResultSet
    private static Post extractPostFromResultSet(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setPostId(rs.getInt("post_id"));
        post.setUserId(rs.getInt("user_id"));
        post.setContent(rs.getString("content"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            LocalDateTime ldt = ts.toLocalDateTime();
            post.setCreatedAt(ldt.format(DISPLAY_FORMATTER));
        } else {
            post.setCreatedAt("");
        }

        post.setMediaType(rs.getString("media_type"));
        post.setMedia(rs.getBytes("media"));
        return post;
    }
    public static List<Post> getPostsByUserId(int userId) {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT * FROM posts WHERE user_id = ? ORDER BY created_at DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    posts.add(extractPostFromResultSet(rs));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return posts;
    }


}
