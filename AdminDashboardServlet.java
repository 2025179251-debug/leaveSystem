import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet("/AdminDashboardServlet")
public class AdminDashboardServlet extends HttpServlet {

   
	private static final long serialVersionUID = 1L;

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Object empidObj = request.getSession().getAttribute("empid");
        Object roleObj  = request.getSession().getAttribute("role");

        if (empidObj == null || roleObj == null || !"ADMIN".equalsIgnoreCase(roleObj.toString())) {
            response.sendRedirect("login.jsp?error=Please login as admin.");
            return;
        }

        List<Map<String, Object>> leaves = new ArrayList<>();
        int pendingCount = 0;
        int cancelReqCount = 0;

        String sql =
            "SELECT lr.LEAVE_ID, lr.EMPID, u.FULLNAME, " +
            "       lt.TYPE_CODE AS LEAVE_TYPE, " +
            "       ls.STATUS_CODE AS STATUS, " +
            "       lr.START_DATE, lr.END_DATE, lr.DURATION, lr.APPLIED_ON, lr.ADMIN_COMMENT " +
            "FROM LEAVE_REQUESTS lr " +
            "JOIN USERS u ON u.EMPID = lr.EMPID " +
            "JOIN LEAVE_TYPES lt ON lt.LEAVE_TYPE_ID = lr.LEAVE_TYPE_ID " +
            "JOIN LEAVE_STATUSES ls ON ls.STATUS_ID = lr.STATUS_ID " +
            "WHERE ls.STATUS_CODE IN ('PENDING','CANCELLATION_REQUESTED') " +
            "ORDER BY lr.APPLIED_ON ASC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("leaveId", rs.getInt("LEAVE_ID"));
                row.put("empid", rs.getInt("EMPID"));
                row.put("fullname", rs.getString("FULLNAME"));
                row.put("leaveType", rs.getString("LEAVE_TYPE"));
                row.put("status", rs.getString("STATUS"));
                row.put("startDate", rs.getDate("START_DATE"));
                row.put("endDate", rs.getDate("END_DATE"));
                row.put("duration", rs.getString("DURATION"));
                row.put("appliedOn", rs.getDate("APPLIED_ON"));
                row.put("adminComment", rs.getString("ADMIN_COMMENT"));

                String status = rs.getString("STATUS");
                if ("PENDING".equalsIgnoreCase(status)) pendingCount++;
                if ("CANCELLATION_REQUESTED".equalsIgnoreCase(status)) cancelReqCount++;

                leaves.add(row);
            }

        } catch (Exception e) {
            throw new ServletException("Error loading dashboard", e);
        }

        request.setAttribute("leaves", leaves);
        request.setAttribute("pendingCount", pendingCount);
        request.setAttribute("cancelReqCount", cancelReqCount);

        request.getRequestDispatcher("adminDashboard.jsp").forward(request, response);
    }
}
