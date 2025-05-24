import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserSettingsDAO {

    public static UserSettings getUserSettings(int userId) throws SQLException {
        String query = "SELECT * FROM user_settings WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new UserSettings(
                        rs.getInt("user_id"),
                        rs.getBoolean("is_private"),
                        rs.getBoolean("read_receipts"),
                        rs.getBoolean("online_status"),
                        rs.getString("message_permission")
                );
            }
        }
        return null;
    }

    public static boolean changePassword(int userId, String oldPassword, String newPassword) throws SQLException {
        String selectQuery = "SELECT password FROM users WHERE user_id = ?";
        String updateQuery = "UPDATE users SET password = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {

            selectStmt.setInt(1, userId);
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                String currentPassword = rs.getString("password");

                if (!currentPassword.equals(oldPassword)) {
                    return false;
                }

                try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                    updateStmt.setString(1, newPassword);
                    updateStmt.setInt(2, userId);
                    updateStmt.executeUpdate();
                    return true;
                }
            }
        }
        return false;
    }
    public static boolean isProfilePrivate(int userId)
    {
        String query = "SELECT u.user_id, u.username, u.bio, u.profile_picture, us.is_private " +
                "FROM users u " +
                "LEFT JOIN user_settings us ON u.user_id = us.user_id " +
                "WHERE u.user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("is_private");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public static List<User> getUsersWithMessageSetting(String setting)
    {
        List<User> users = new ArrayList<>();

        String sql = "SELECT u.user_Id, u.username, u.email, u.profile_picture, u.bio " +
                "FROM users u " +
                "JOIN user_settings us ON u.user_id = us.user_id " +
                "WHERE us.message_permission = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, setting);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                User user = new User(
                        rs.getInt("user_Id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getBytes("profile_picture"),
                        rs.getString("bio")
                );
                users.add(user);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return users;
    }




}




