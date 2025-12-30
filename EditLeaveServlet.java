import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;

@WebServlet("/EditLeaveServlet")
public class EditLeaveServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empid") == null ||
            session.getAttribute("role") == null ||
            !"EMPLOYEE".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendError(401, "Unauthorized");
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendError(400, "Missing id");
            return;
        }

        int leaveId;
        try { leaveId = Integer.parseInt(idParam); }
        catch (NumberFormatException e) {
            response.sendError(400, "Invalid id");
            return;
        }

        int empId = Integer.parseInt(String.valueOf(session.getAttribute("empid")));

        String sql =
            "SELECT lr.LEAVE_ID, lr.LEAVE_TYPE_ID, lr.START_DATE, lr.END_DATE, lr.DURATION, lr.HALF_SESSION, lr.REASON, ls.STATUS_CODE " +
            "FROM LEAVE_REQUESTS lr " +
            "JOIN LEAVE_STATUSES ls ON lr.STATUS_ID = ls.STATUS_ID " +
            "WHERE lr.LEAVE_ID = ? AND lr.EMPID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, leaveId);
            ps.setInt(2, empId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    response.sendError(404, "Request not found");
                    return;
                }

                String status = rs.getString("STATUS_CODE");
                if (status == null || !"PENDING".equalsIgnoreCase(status)) {
                    response.sendError(403, "Only PENDING can edit");
                    return;
                }

                // Convert DB duration to UI duration
                String durationDb = rs.getString("DURATION");      // FULL_DAY / HALF_DAY
                String half = rs.getString("HALF_SESSION");        // AM/PM/null

                String durationUi = "FULL_DAY";
                if ("HALF_DAY".equalsIgnoreCase(durationDb)) {
                    if ("PM".equalsIgnoreCase(half)) durationUi = "HALF_DAY_PM";
                    else durationUi = "HALF_DAY_AM"; // default AM
                }

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                StringBuilder json = new StringBuilder();
                json.append("{");
                json.append("\"leaveId\":").append(rs.getInt("LEAVE_ID")).append(",");
                json.append("\"leaveTypeId\":").append(rs.getInt("LEAVE_TYPE_ID")).append(",");
                json.append("\"startDate\":\"").append(rs.getDate("START_DATE")).append("\",");
                json.append("\"endDate\":\"").append(rs.getDate("END_DATE")).append("\",");
                json.append("\"duration\":\"").append(esc(durationUi)).append("\",");
                json.append("\"reason\":\"").append(esc(rs.getString("REASON"))).append("\",");

                // dropdown list
                json.append("\"leaveTypes\":[");
                try (Statement st = conn.createStatement();
                     ResultSet rsTypes = st.executeQuery("SELECT LEAVE_TYPE_ID, TYPE_CODE FROM LEAVE_TYPES ORDER BY TYPE_CODE")) {
                    boolean first = true;
                    while (rsTypes.next()) {
                        if (!first) json.append(",");
                        json.append("{\"value\":").append(rsTypes.getInt("LEAVE_TYPE_ID"))
                            .append(",\"label\":\"").append(esc(rsTypes.getString("TYPE_CODE"))).append("\"}");
                        first = false;
                    }
                }
                json.append("]}");

                response.getWriter().print(json.toString());
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "DB error");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empid") == null ||
            session.getAttribute("role") == null ||
            !"EMPLOYEE".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendError(401, "Unauthorized");
            return;
        }

        int empId = Integer.parseInt(String.valueOf(session.getAttribute("empid")));

        int leaveId = Integer.parseInt(request.getParameter("leaveId"));
        int leaveTypeId = Integer.parseInt(request.getParameter("leaveType"));

        String durationUi = request.getParameter("duration"); // FULL_DAY / HALF_DAY_AM / HALF_DAY_PM
        String startStr = request.getParameter("startDate");
        String endStr   = request.getParameter("endDate");
        String reason   = request.getParameter("reason");

        boolean isHalf = "HALF_DAY_AM".equalsIgnoreCase(durationUi) || "HALF_DAY_PM".equalsIgnoreCase(durationUi);

        String durationDb = isHalf ? "HALF_DAY" : "FULL_DAY";
        String halfSession = null;
        if ("HALF_DAY_AM".equalsIgnoreCase(durationUi)) halfSession = "AM";
        if ("HALF_DAY_PM".equalsIgnoreCase(durationUi)) halfSession = "PM";

        java.time.LocalDate start = java.time.LocalDate.parse(startStr);
        java.time.LocalDate end = isHalf ? start : java.time.LocalDate.parse(endStr);

        if (end.isBefore(start)) {
            response.sendError(400, "End date cannot be before start date");
            return;
        }

        double durationDays = isHalf ? 0.5 : (java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1);

        // ✅ IMPORTANT: update DURATION_DAYS here (this is your main bug)
        String sql =
            "UPDATE LEAVE_REQUESTS " +
            "SET LEAVE_TYPE_ID=?, START_DATE=?, END_DATE=?, DURATION=?, HALF_SESSION=?, DURATION_DAYS=?, REASON=? " +
            "WHERE LEAVE_ID=? AND EMPID=? " +
            "  AND STATUS_ID=(SELECT STATUS_ID FROM LEAVE_STATUSES WHERE UPPER(STATUS_CODE)='PENDING')";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, leaveTypeId);
            ps.setDate(2, java.sql.Date.valueOf(start));
            ps.setDate(3, java.sql.Date.valueOf(end));
            ps.setString(4, durationDb);
            ps.setString(5, halfSession);
            ps.setDouble(6, durationDays);
            ps.setString(7, reason == null ? "" : reason.trim());
            ps.setInt(8, leaveId);
            ps.setInt(9, empId);

            int rows = ps.executeUpdate();
            if (rows == 1) {
                // if you submit via fetch(), return OK
                response.setContentType("text/plain");
                response.getWriter().print("OK");
            } else {
                response.sendError(400, "Update failed (not pending / not yours)");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "DB error");
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", " ");
    }
}
