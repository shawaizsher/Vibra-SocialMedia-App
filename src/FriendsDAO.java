import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FriendsDAO {

    private static Connection connection = DatabaseConnection.getConnection(); // or your own DB connection method

    public static List<Integer> getFriendIds(int userId) {
        List<Integer> friendIds = new ArrayList<>();
        String sql = "SELECT friend_id FROM friends WHERE user_id = ? " +
                "UNION " +
                "SELECT user_id FROM friends WHERE friend_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    friendIds.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return friendIds;
    }

    public FriendsDAO(Connection connection) {
        this.connection = connection;
    }

    public static List<User> getFriendsOfUser(int userId) {
        List<User> friends = new ArrayList<>();

        String sql = """
        SELECT DISTINCT u.*
        FROM users u
        JOIN friends f ON (
            (f.user_id = ? AND u.user_ID = f.friend_id)
            OR
            (f.friend_id = ? AND u.user_ID = f.user_id)
        )
        WHERE u.user_ID != ?
    """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                User friend = new User();
                friend.setId(rs.getInt("user_Id"));
                friend.setUsername(rs.getString("username"));
                friend.setBio(rs.getString("bio"));
                friends.add(friend);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return friends;
    }
}


