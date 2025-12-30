import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@WebServlet("/ApplyLeaveServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1MB
        maxFileSize = 5L * 1024 * 1024,       // 5MB
        maxRequestSize = 6L * 1024 * 1024
)
public class ApplyLeaveServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Set<String> ALLOWED_MIME = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg"
    );

    private String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private int getStatusId(Connection con, String statusCode) throws SQLException {
        String sql = "SELECT STATUS_ID FROM LEAVE_STATUSES WHERE UPPER(STATUS_CODE)=UPPER(?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, statusCode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("STATUS_ID");
            }
        }
        throw new SQLException("STATUS_CODE not found: " + statusCode);
    }

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

    private void addPendingAndRecalcTotal(Connection con, int empId, int leaveTypeId, double pendingDays) throws SQLException {
        String upd =
                "UPDATE LEAVE_BALANCES " +
                "SET PENDING = NVL(PENDING,0) + ?, " +
                "    TOTAL   = (NVL(ENTITLEMENT,0) + NVL(CARRIED_FWD,0)) - (NVL(USED,0) + (NVL(PENDING,0) + ?)) " +
                "WHERE EMPID = ? AND LEAVE_TYPE_ID = ?";

        try (PreparedStatement ps = con.prepareStatement(upd)) {
            ps.setDouble(1, pendingDays);
            ps.setDouble(2, pendingDays);
            ps.setInt(3, empId);
            ps.setInt(4, leaveTypeId);

            int rows = ps.executeUpdate();
            if (rows == 0) throw new SQLException("LEAVE_BALANCES row missing.");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empid") == null ||
                session.getAttribute("role") == null ||
                !"EMPLOYEE".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendRedirect("login.jsp?error=" + url("Please login as employee."));
            return;
        }

        List<Map<String, Object>> leaveTypes = new ArrayList<>();
        String typeError = "";

        String sqlTypes = "SELECT LEAVE_TYPE_ID, TYPE_CODE, DESCRIPTION FROM LEAVE_TYPES ORDER BY TYPE_CODE";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sqlTypes);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", rs.getInt("LEAVE_TYPE_ID"));
                m.put("code", rs.getString("TYPE_CODE"));
                m.put("desc", rs.getString("DESCRIPTION"));
                leaveTypes.add(m);
            }

        } catch (Exception e) {
            typeError = e.getMessage();
        }

        request.setAttribute("leaveTypes", leaveTypes);
        request.setAttribute("typeError", typeError);
        request.setAttribute("msg", request.getParameter("msg"));
        request.setAttribute("error", request.getParameter("error"));

        request.getRequestDispatcher("/applyLeave.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empid") == null ||
                session.getAttribute("role") == null ||
                !"EMPLOYEE".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendRedirect("login.jsp?error=" + url("Please login as employee."));
            return;
        }

        int empId = Integer.parseInt(String.valueOf(session.getAttribute("empid")));

        String leaveTypeIdStr = request.getParameter("leaveTypeId");
        String durationUi = request.getParameter("duration"); // FULL_DAY / HALF_DAY_AM / HALF_DAY_PM
        String startStr = request.getParameter("startDate");
        String endStr = request.getParameter("endDate");
        String reason = request.getParameter("reason");

        if (leaveTypeIdStr == null || leaveTypeIdStr.isBlank()
                || durationUi == null || durationUi.isBlank()
                || startStr == null || startStr.isBlank()
                || reason == null || reason.isBlank()) {
            response.sendRedirect("ApplyLeaveServlet?error=" + url("Please fill in all required fields."));
            return;
        }

        int leaveTypeId;
        try { leaveTypeId = Integer.parseInt(leaveTypeIdStr); }
        catch (NumberFormatException e) {
            response.sendRedirect("ApplyLeaveServlet?error=" + url("Invalid leave type."));
            return;
        }

        boolean isHalf = "HALF_DAY_AM".equalsIgnoreCase(durationUi) || "HALF_DAY_PM".equalsIgnoreCase(durationUi);

        String halfSession = null;
        if ("HALF_DAY_AM".equalsIgnoreCase(durationUi)) halfSession = "AM";
        if ("HALF_DAY_PM".equalsIgnoreCase(durationUi)) halfSession = "PM";

        LocalDate startDate;
        LocalDate endDate;

        try { startDate = LocalDate.parse(startStr); }
        catch (Exception e) {
            response.sendRedirect("ApplyLeaveServlet?error=" + url("Invalid start date."));
            return;
        }

        if (isHalf) {
            endDate = startDate;
        } else {
            if (endStr == null || endStr.isBlank()) {
                response.sendRedirect("ApplyLeaveServlet?error=" + url("End date is required for Full Day."));
                return;
            }
            try { endDate = LocalDate.parse(endStr); }
            catch (Exception e) {
                response.sendRedirect("ApplyLeaveServlet?error=" + url("Invalid end date."));
                return;
            }
        }

        if (endDate.isBefore(startDate)) {
            response.sendRedirect("ApplyLeaveServlet?error=" + url("End date cannot be before start date."));
            return;
        }

        // DB constraint: DURATION in ('FULL_DAY','HALF_DAY')
        String durationDb = isHalf ? "HALF_DAY" : "FULL_DAY";
        double durationDays = isHalf ? 0.5 : (ChronoUnit.DAYS.between(startDate, endDate) + 1);

        Part filePart;
        try {
            filePart = request.getPart("attachment");
        } catch (IllegalStateException ex) {
            response.sendRedirect("ApplyLeaveServlet?error=" + url("Attachment too large. Max 5MB."));
            return;
        }

        boolean hasFile = (filePart != null && filePart.getSize() > 0);
        String fileName = null;
        String mimeType = null;
        long fileSize = 0;

        if (hasFile) {
            fileName = filePart.getSubmittedFileName();
            mimeType = filePart.getContentType();
            fileSize = filePart.getSize();

            if (mimeType == null || !ALLOWED_MIME.contains(mimeType)) {
                response.sendRedirect("ApplyLeaveServlet?error=" + url("Invalid file type. Only PDF, PNG, JPG allowed."));
                return;
            }
        }

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            int pendingStatusId = getStatusId(con, "PENDING");

            String insertLeave =
                    "INSERT INTO LEAVE_REQUESTS " +
                    "(EMPID, LEAVE_TYPE_ID, STATUS_ID, START_DATE, END_DATE, DURATION, DURATION_DAYS, APPLIED_ON, ADMIN_COMMENT, REASON, HALF_SESSION) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, SYSDATE, NULL, ?, ?)";

            int leaveId;
            try (PreparedStatement ps = con.prepareStatement(insertLeave, new String[]{"LEAVE_ID"})) {
                ps.setInt(1, empId);
                ps.setInt(2, leaveTypeId);
                ps.setInt(3, pendingStatusId);
                ps.setDate(4, java.sql.Date.valueOf(startDate));
                ps.setDate(5, java.sql.Date.valueOf(endDate));
                ps.setString(6, durationDb);
                ps.setDouble(7, durationDays);
                ps.setString(8, reason.trim());
                ps.setString(9, halfSession);

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) leaveId = keys.getInt(1);
                    else throw new SQLException("Failed to retrieve generated LEAVE_ID.");
                }
            }

            if (hasFile) {
                String insertFile =
                        "INSERT INTO LEAVE_REQUEST_ATTACHMENTS " +
                        "(LEAVE_ID, FILE_NAME, MIME_TYPE, FILE_SIZE, FILE_DATA, UPLOADED_ON) " +
                        "VALUES (?, ?, ?, ?, ?, SYSDATE)";

                try (PreparedStatement psFile = con.prepareStatement(insertFile);
                     InputStream in = filePart.getInputStream()) {
                    psFile.setInt(1, leaveId);
                    psFile.setString(2, fileName);
                    psFile.setString(3, mimeType);
                    psFile.setLong(4, fileSize);
                    psFile.setBinaryStream(5, in);
                    psFile.executeUpdate();
                }
            }

            ensureBalanceRow(con, empId, leaveTypeId);
            addPendingAndRecalcTotal(con, empId, leaveTypeId, durationDays);

            con.commit();

            response.sendRedirect("ApplyLeaveServlet?msg=" + url("Your application has been submitted. Kindly wait for admin review.")
                    + "&leaveId=" + leaveId);

        } catch (Exception e) {
            try { if (con != null) con.rollback(); } catch (Exception ignore) {}
            response.sendRedirect("ApplyLeaveServlet?error=" + url("DB Error: " + e.getMessage()));
        } finally {
            try { if (con != null) con.setAutoCommit(true); } catch (Exception ignore) {}
            try { if (con != null) con.close(); } catch (Exception ignore) {}
        }
    }
}
