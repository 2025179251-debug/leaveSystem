import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class NotificationService {

    // ✅ Insert notif to one user (EMPID)
    public static void notifyUser(Connection con, int empId, String type, String message) throws Exception {
        String sql = """
            INSERT INTO NOTIFICATIONS
              (NOTIFICATION_ID, EMPID, TYPE, MESSAGE, IS_READ, CREATED_AT)
            VALUES
              ('N' || LPAD(NOTIF_SEQ.NEXTVAL, 5, '0'), ?, ?, ?, 'N', SYSTIMESTAMP)
        """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, empId);
            ps.setString(2, type);
            ps.setString(3, message);
            ps.executeUpdate();
        }
    }

    // ✅ Notify all admins
    public static void notifyAllAdmins(Connection con, String type, String message) throws Exception {
        String q = "SELECT EMPID FROM USERS WHERE UPPER(ROLE)='ADMIN'";
        try (PreparedStatement ps = con.prepareStatement(q);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int adminId = rs.getInt("EMPID");
                notifyUser(con, adminId, type, message);
            }
        }
    }
}
