import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class FriendNotificationDAO {


    public static boolean hasIncomingRequest(int senderId, int receiverId) throws SQLException {
        String sql = "SELECT request_id FROM friend_requests WHERE sender_id = ? AND receiver_id = ? AND status = 'pending'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, receiverId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public static boolean isRequestPending(int senderId, int receiverId) throws SQLException {
        String sql = "SELECT 1 FROM friend_requests WHERE sender_id = ? AND receiver_id = ? AND status = 'pending'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, receiverId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public static int getPendingRequestId(int senderId, int receiverId) throws SQLException {
        String sql = "SELECT request_id FROM friend_requests WHERE sender_id = ? AND receiver_id = ? AND status = 'pending'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, receiverId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("request_id");
            } else {
                throw new SQLException("No pending request found.");
            }
        }
    }

    public static boolean sendFriendRequest(int senderId, int receiverId) throws SQLException {
        if (isBlocked(senderId, receiverId)) {
            System.out.println("Friend request blocked due to block status.");
            return false;
        }

        String sql = "INSERT INTO friend_requests (sender_id, receiver_id, status, sent_at) VALUES (?, ?, 'pending', NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, receiverId);
            return stmt.executeUpdate() > 0;
        }
    }

    public static void acceptFriendRequest(int requestId) throws SQLException {
        String sql = "UPDATE friend_requests SET status = 'accepted' WHERE request_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId);
            stmt.executeUpdate();
            addFriendRelationFromRequest(requestId);
        }
    }

    public static void rejectFriendRequest(int requestId) throws SQLException {
        String sql = "DELETE FROM friend_requests WHERE request_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId);
            stmt.executeUpdate();
        }
    }



    public static void cancelSentFriendRequest(int senderId, int receiverId) throws SQLException {
        String sql = "DELETE FROM friend_requests WHERE sender_id = ? AND receiver_id = ? AND status = 'pending'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, senderId);
            stmt.setInt(2, receiverId);
            stmt.executeUpdate();
        }
    }


    // ===== FRIENDS TABLE =====

    public static boolean areAlreadyFriends(int userId1, int userId2) throws SQLException {
        String sql = "SELECT 1 FROM friends f1 " +
                "JOIN friends f2 ON f1.user_id = f2.friend_id AND f1.friend_id = f2.user_id " +
                "WHERE f1.user_id = ? AND f1.friend_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId1);
            stmt.setInt(2, userId2);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    private static void addFriendRelationFromRequest(int requestId) throws SQLException {
        String sql = "SELECT sender_id, receiver_id FROM friend_requests WHERE request_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, requestId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int senderId = rs.getInt("sender_id");
                int receiverId = rs.getInt("receiver_id");

                String insert = "INSERT INTO friends (user_id, friend_id) VALUES (?, ?), (?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insert)) {
                    insertStmt.setInt(1, senderId);
                    insertStmt.setInt(2, receiverId);
                    insertStmt.setInt(3, receiverId);
                    insertStmt.setInt(4, senderId);
                    insertStmt.executeUpdate();
                }

                createNotification(senderId, getUsername(receiverId) + " accepted your friend request.");
            }
        }
    }

    // ===== BLOCKED USERS =====

    public static void blockUser(int blockerId, int blockedId) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Add to blocked table
            PreparedStatement ps = conn.prepareStatement("INSERT INTO blocked_users (blocker_id, blocked_id) VALUES (?, ?)");
            ps.setInt(1, blockerId);
            ps.setInt(2, blockedId);
            ps.executeUpdate();

            PreparedStatement unfriend = conn.prepareStatement(
                    "DELETE FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)"
            );
            unfriend.setInt(1, blockerId);
            unfriend.setInt(2, blockedId);
            unfriend.setInt(3, blockedId);
            unfriend.setInt(4, blockerId);
            unfriend.executeUpdate();
        }
    }


    public static boolean unblockUser(int blockerId, int blockedId) {
        String sql = "DELETE FROM blocked_users WHERE blocker_id = ? AND blocked_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, blockerId);
            stmt.setInt(2, blockedId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isBlocked(int userId1, int userId2) {
        String sql = "SELECT 1 FROM blocked_users WHERE (blocker_id = ? AND blocked_id = ?) OR (blocker_id = ? AND blocked_id = ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId1);
            stmt.setInt(2, userId2);
            stmt.setInt(3, userId2);
            stmt.setInt(4, userId1);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return true;
        }
    }

    // ===== NOTIFICATIONS =====

    public static void createNotification(int userId, String message) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, message) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, message);
            stmt.executeUpdate();
        }
    }

    public static List<String> getNotifications(int userId) throws SQLException {
        List<String> notifications = new ArrayList<>();
        String sql = "SELECT message FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                notifications.add(rs.getString("message"));
            }
        }
        return notifications;
    }

    // ===== HELPER =====

    private static String getUsername(int userId) throws SQLException {
        String sql = "SELECT username FROM users WHERE user_ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
        }
        return "Unknown";
    }
    public static List<User> getBlockedUsers(int blockerId) {
        List<User> blockedUsers = new ArrayList<>();
        String query = "SELECT b.blocked_id, u.username FROM blocked_users b JOIN users u ON b.blocked_id = u.user_Id WHERE b.blocker_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, blockerId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("blocked_id"));
                user.setUsername(rs.getString("username"));
                blockedUsers.add(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return blockedUsers;
    }
    public static List<String> loadPendingFriendRequests(int userId) {
        List<String> requests = new ArrayList<>();

        String sql = "SELECT u.username, fr.sender_id, fr.sent_at " +
                "FROM friend_requests fr " +
                "JOIN users u ON fr.sender_id = u.user_id " +
                "WHERE fr.receiver_id = ? AND fr.status = 'pending'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                Timestamp sentAt = rs.getTimestamp("sent_at");
                String message = username + " sent you a friend request.";
                requests.add(message);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return requests;
    }

    public static String getRequestTimeAgo(String senderUsername, int receiverId) {
        String sql = "SELECT f.sent_at FROM friend_requests f " +
                "JOIN users u ON f.sender_id = u.user_id " +
                "WHERE u.username = ? AND f.receiver_id = ? AND f.status = 'pending'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, senderUsername);
            stmt.setInt(2, receiverId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp sentTime = rs.getTimestamp("sent_at");
                LocalDateTime sentDateTime = sentTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                return new NotificationsController().getTimeAgo(sentDateTime);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "some time ago";
    }

}
