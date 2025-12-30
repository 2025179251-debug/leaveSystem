import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

@WebServlet("/LeaveHistoryServlet")
public class LeaveHistoryServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String normalizeDbDuration(String d) {
        if (d == null) return "FULL_DAY";
        d = d.trim().toUpperCase();
        if ("FULL_DAY".equals(d) || "HALF_DAY".equals(d)) return d;
        return "FULL_DAY";
    }

    private String normalizeHalfSession(String s) {
        if (s == null) return "AM";
        s = s.trim().toUpperCase();
        if ("AM".equals(s) || "PM".equals(s)) return s;
        return "AM";
    }

    private String durationLabel(String dbDuration, String halfSession) {
        if ("HALF_DAY".equalsIgnoreCase(dbDuration)) {
            return "HALF DAY (" + normalizeHalfSession(halfSession) + ")";
        }
        return "FULL DAY";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empid") == null ||
            session.getAttribute("role") == null ||
            !"EMPLOYEE".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendRedirect("login.jsp?error=" + enc("Please login as employee."));
            return;
        }

        int empId = Integer.parseInt(String.valueOf(session.getAttribute("empid")));
        String statusFilter = request.getParameter("status");
        String yearFilter = request.getParameter("year");

        List<Map<String, Object>> leaves = new ArrayList<>();
        List<String> years = new ArrayList<>();
        String error = null;

        try (Connection conn = DatabaseConnection.getConnection()) {

            // 1. Ambil tahun untuk dropdown
            String yearSql = "SELECT DISTINCT EXTRACT(YEAR FROM START_DATE) AS YR FROM LEAVE_REQUESTS WHERE EMPID = ? ORDER BY YR DESC";
            try (PreparedStatement psYear = conn.prepareStatement(yearSql)) {
                psYear.setInt(1, empId);
                try (ResultSet rsYear = psYear.executeQuery()) {
                    while (rsYear.next()) {
                        years.add(rsYear.getString("YR"));
                    }
                }
            }

            // 2. Main Query
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT lr.LEAVE_ID, lr.START_DATE, lr.END_DATE, lr.DURATION, lr.DURATION_DAYS, lr.HALF_SESSION, ")
               .append("lr.REASON, lr.APPLIED_ON, lr.ADMIN_COMMENT, lt.TYPE_CODE, ls.STATUS_CODE, ")
               .append("(SELECT a.FILE_NAME FROM LEAVE_REQUEST_ATTACHMENTS a WHERE a.LEAVE_ID = lr.LEAVE_ID ORDER BY a.UPLOADED_ON DESC FETCH FIRST 1 ROW ONLY) AS FILE_NAME ")
               .append("FROM LEAVE_REQUESTS lr ")
               .append("JOIN LEAVE_TYPES lt ON lr.LEAVE_TYPE_ID = lt.LEAVE_TYPE_ID ")
               .append("JOIN LEAVE_STATUSES ls ON lr.STATUS_ID = ls.STATUS_ID ")
               .append("WHERE lr.EMPID = ? ");

            if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
                sql.append(" AND UPPER(ls.STATUS_CODE) = ? ");
            }
            if (yearFilter != null && !yearFilter.isBlank()) {
                sql.append(" AND EXTRACT(YEAR FROM lr.START_DATE) = ? ");
            }
            sql.append(" ORDER BY lr.APPLIED_ON DESC");

            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                ps.setInt(idx++, empId);

                if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter)) {
                    ps.setString(idx++, statusFilter.trim().toUpperCase());
                }
                if (yearFilter != null && !yearFilter.isBlank()) {
                    ps.setInt(idx++, Integer.parseInt(yearFilter));
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String dbDuration = normalizeDbDuration(rs.getString("DURATION"));
                        String halfSession = rs.getString("HALF_SESSION");
                        double durationDays = rs.getDouble("DURATION_DAYS");
                        
                        double totalDays;
                        if (!rs.wasNull() && durationDays > 0) {
                            totalDays = durationDays;
                        } else {
                            if ("HALF_DAY".equals(dbDuration)) {
                                totalDays = 0.5;
                            } else {
                                java.sql.Date sd = rs.getDate("START_DATE");
                                java.sql.Date ed = rs.getDate("END_DATE");
                                if (sd != null && ed != null) {
                                    long diff = (ed.toLocalDate().toEpochDay() - sd.toLocalDate().toEpochDay()) + 1;
                                    totalDays = (double) Math.max(diff, 1);
                                } else {
                                    totalDays = 0.0;
                                }
                            }
                        }

                        Map<String, Object> l = new HashMap<>();
                        l.put("id", rs.getInt("LEAVE_ID"));
                        l.put("type", rs.getString("TYPE_CODE"));
                        l.put("duration", durationLabel(dbDuration, halfSession));
                        l.put("start", rs.getDate("START_DATE"));
                        l.put("end", rs.getDate("END_DATE"));
                        l.put("totalDays", totalDays);
                        l.put("status", rs.getString("STATUS_CODE"));
                        l.put("appliedOn", rs.getTimestamp("APPLIED_ON"));
                        l.put("fileName", rs.getString("FILE_NAME"));
                        l.put("hasFile", (rs.getString("FILE_NAME") != null));
                        l.put("reason", rs.getString("REASON"));
                        l.put("adminComment", rs.getString("ADMIN_COMMENT"));

                        leaves.add(l);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            error = e.getMessage();
        }

        request.setAttribute("leaves", leaves);
        request.setAttribute("years", years);
        request.setAttribute("error", error);

        request.getRequestDispatcher("leaveHistory.jsp").forward(request, response);
    }
}