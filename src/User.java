import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class User {
    private int id;
    private String username;
    private String bio;
    private byte[] profilePicture;
    private LocalDateTime lastActive;
    private int posts;
    private int followers;
    private int following;
    private String email;


    public User() {} // Empty constructor for DAO loading

    public User(String username, int posts, int followers, int following) {
        this.username = username;
        this.posts = posts;
        this.followers = followers;
        this.following = following;
    }
    public User(int id, String username) {
        this.id = id;
        this.username = username;
    }

    public User(int id, String username, String email, byte[] profilePicture, String bio, String lastActive) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.profilePicture = profilePicture;
        this.bio = bio;
    }
    public User(int id, String username, String bio, byte[] profilePicture,String email) {
        this.id = id;
        this.username = username;
        this.bio = bio;
        this.profilePicture = profilePicture;
        this.email = email;
    }


    // --- Getters ---
    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getBio() { return bio; }
    public byte[] getProfilePicture() { return profilePicture; }

    // --- Setters ---
    public void setId(int id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setBio(String bio) { this.bio = bio; }
    public void setProfilePicture(byte[] profilePicture) { this.profilePicture = profilePicture; }
    public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; }

    // --- Fetch user by username ---
    public static User getUserByUsername(String username) {
        String query = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setBio(rs.getString("bio"));
                user.setProfilePicture(rs.getBytes("profile_picture")); // if using BLOB
                user.setLastActive(rs.getTimestamp("last_active").toLocalDateTime());
                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static User getUserObjectByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                return user;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
    public static User getUserById(int id) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setUsername(rs.getString("username"));
                user.setBio(rs.getString("bio")); // optional
                user.setProfilePicture(rs.getBytes("profile_picture"));
                if (rs.getTimestamp("last_active") != null) {
                    user.setLastActive(rs.getTimestamp("last_active").toLocalDateTime());
                }
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


}
