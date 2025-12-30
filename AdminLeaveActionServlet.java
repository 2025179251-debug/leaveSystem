import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;

@WebServlet("/AdminLeaveActionServlet")
public class AdminLeaveActionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ✅ admin guard
        HttpSession session = request.getSession(false);
        Object roleObj = (session == null) ? null : session.getAttribute("role");
        if (roleObj == null || !"ADMIN".equalsIgnoreCase(roleObj.toString())) {
            response.sendRedirect("login.jsp?error=" + url("Please login as admin."));
            return;
        }

        String leaveIdStr = request.getParameter("leaveId");
        String action = request.getParameter("action"); // APPROVE / REJECT / APPROVE_CANCEL / REJECT_CANCEL
        String comment = request.getParameter("comment");

        if (leaveIdStr == null || leaveIdStr.isBlank()) {
            response.sendRedirect("AdminDashboardServlet?msg=" + url("Missing leaveId."));
            return;
        }

        int leaveId;
        try {
            leaveId = Integer.parseInt(leaveIdStr);
        } catch (NumberFormatException nfe) {
            response.sendRedirect("AdminDashboardServlet?msg=" + url("Invalid leaveId."));
            return;
        }

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // 1) Read leave request info (need for balances)
            String qInfo = """
                SELECT lr.EMPID,
                       lr.LEAVE_TYPE_ID,
                       NVL(lr.DURATION_DAYS, 0) AS DURATION_DAYS,
                       ls.STATUS_CODE
                FROM LEAVE_REQUESTS lr
                JOIN LEAVE_STATUSES ls ON ls.STATUS_ID = lr.STATUS_ID
                WHERE lr.LEAVE_ID = ?
            """;

            int empId;
            int leaveTypeId;
            double durationDays;
            String oldStatus;

            try (PreparedStatement ps = con.prepareStatement(qInfo)) {
                ps.setInt(1, leaveId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        response.sendRedirect("AdminDashboardServlet?msg=" + url("Leave record not found."));
                        return;
                    }
                    empId = rs.getInt("EMPID");
                    leaveTypeId = rs.getInt("LEAVE_TYPE_ID");
                    durationDays = rs.getDouble("DURATION_DAYS");
                    oldStatus = rs.getString("STATUS_CODE");
                }
            }

            // 2) Decide new status based on action + old status
            String newStatus;
            boolean isCancelReq = "CANCELLATION_REQUESTED".equalsIgnoreCase(oldStatus);

            if ("APPROVE".equalsIgnoreCase(action)) {
                newStatus = "APPROVED";
            } else if ("REJECT".equalsIgnoreCase(action)) {
                // kalau reject cancellation request -> balik APPROVED (bukan REJECTED)
                newStatus = isCancelReq ? "APPROVED" : "REJECTED";
            } else if ("APPROVE_CANCEL".equalsIgnoreCase(action)) {
                newStatus = "CANCELLED";
            } else if ("REJECT_CANCEL".equalsIgnoreCase(action)) {
                newStatus = "APPROVED";
            } else {
                newStatus = oldStatus; // no change
            }

            // 3) Update leave request status + admin comment
            String qUpdate = """
                UPDATE LEAVE_REQUESTS
                SET STATUS_ID = (SELECT STATUS_ID FROM LEAVE_STATUSES WHERE UPPER(STATUS_CODE)=UPPER(?)),
                    ADMIN_COMMENT = ?
                WHERE LEAVE_ID = ?
            """;

            try (PreparedStatement ps = con.prepareStatement(qUpdate)) {
                ps.setString(1, newStatus);
                ps.setString(2, (comment == null ? null : comment.trim()));
                ps.setInt(3, leaveId);

                int updated = ps.executeUpdate();
                if (updated == 0) {
                    con.rollback();
                    response.sendRedirect("AdminDashboardServlet?msg=" + url("No row updated."));
                    return;
                }
            }

            // 4) Update LEAVE_BALANCES based on transition
            // TOTAL = (ENTITLEMENT + CARRIED_FWD) - (USED + PENDING)

            // default delta
            double deltaPending = 0.0;
            double deltaUsed = 0.0;

            // PENDING -> APPROVED
            if ("PENDING".equalsIgnoreCase(oldStatus) && "APPROVED".equalsIgnoreCase(newStatus)) {
                deltaPending = -durationDays;
                deltaUsed = +durationDays;
            }
            // PENDING -> REJECTED
            else if ("PENDING".equalsIgnoreCase(oldStatus) && "REJECTED".equalsIgnoreCase(newStatus)) {
                deltaPending = -durationDays;
                deltaUsed = 0.0;
            }
            // CANCELLATION_REQUESTED -> CANCELLED (release used)
            else if ("CANCELLATION_REQUESTED".equalsIgnoreCase(oldStatus) && "CANCELLED".equalsIgnoreCase(newStatus)) {
                deltaUsed = -durationDays;
                deltaPending = 0.0;
            }
            // CANCELLATION_REQUESTED -> APPROVED (reject cancel)
            else if ("CANCELLATION_REQUESTED".equalsIgnoreCase(oldStatus) && "APPROVED".equalsIgnoreCase(newStatus)) {
                // no balance change
            }

            // Apply balance updates only if needed
            if (deltaPending != 0.0 || deltaUsed != 0.0) {
                ensureBalanceRow(con, empId, leaveTypeId);
                applyBalanceDelta(con, empId, leaveTypeId, deltaPending, deltaUsed);
            }

            // 5) Notification (optional) - if you have NotificationService
            // You can keep your existing NotificationService.notifyUser(...) logic here

            con.commit();
            response.sendRedirect("AdminDashboardServlet?msg=" + url("Updated: " + newStatus));

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (Exception ignore) {}
            throw new ServletException("Update failed", e);
        } finally {
            try { if (con != null) con.setAutoCommit(true); } catch (Exception ignore) {}
            try { if (con != null) con.close(); } catch (Exception ignore) {}
        }
    }

    // Create row if not exist (so update won't fail)
    private void ensureBalanceRow(Connection con, int empId, int leaveTypeId) throws SQLException {
        String merge =
                "MERGE INTO LEAVE_BALANCES b " +
                "USING (SELECT ? AS EMPID, ? AS LEAVE_TYPE_ID FROM dual) x " +
                "ON (b.EMPID = x.EMPID AND b.LEAVE_TYPE_ID = x.LEAVE_TYPE_ID) " +
                "WHEN NOT MATCHED THEN " +
                "  INSERT (EMPID, LEAVE_TYPE_ID, ENTITLEMENT, CARRIED_FWD, USED, PENDING, TOTAL) " +
                "  VALUES (?, ?, 0, 0, 0, 0, 0)";

        try (PreparedStatement ps = con.prepareStatement(merge)) {
            ps.setInt(1, empId);
            ps.setInt(2, leaveTypeId);
            ps.setInt(3, empId);
            ps.setInt(4, leaveTypeId);
            ps.executeUpdate();
        }
    }

    private void applyBalanceDelta(Connection con, int empId, int leaveTypeId,
                                   double deltaPending, double deltaUsed) throws SQLException {

        // clamp to avoid negative numbers
        String upd = """
            UPDATE LEAVE_BALANCES
            SET
                PENDING = GREATEST(0, NVL(PENDING,0) + ?),
                USED    = GREATEST(0, NVL(USED,0) + ?),
                TOTAL   = (NVL(ENTITLEMENT,0) + NVL(CARRIED_FWD,0))
                          - (GREATEST(0, NVL(USED,0) + ?) + GREATEST(0, NVL(PENDING,0) + ?))
            WHERE EMPID = ? AND LEAVE_TYPE_ID = ?
        """;

        try (PreparedStatement ps = con.prepareStatement(upd)) {
            ps.setDouble(1, deltaPending);
            ps.setDouble(2, deltaUsed);
            ps.setDouble(3, deltaUsed);
            ps.setDouble(4, deltaPending);
            ps.setInt(5, empId);
            ps.setInt(6, leaveTypeId);

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new SQLException("LEAVE_BALANCES row not found for EMPID=" + empId + " LEAVE_TYPE_ID=" + leaveTypeId);
            }
        }
    }

    private String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
