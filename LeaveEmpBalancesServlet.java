import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

@WebServlet("/LeaveEmpBalancesServlet")
public class LeaveEmpBalancesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null
                || session.getAttribute("empid") == null
                || session.getAttribute("role") == null
                || !"ADMIN".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendRedirect("login.jsp?error=Please+login+as+admin");
            return;
        }

        List<Map<String, Object>> employees = new ArrayList<>();
        List<Map<String, Object>> leaveTypes = new ArrayList<>();
        Map<Integer, Map<Integer, Map<String, Object>>> balanceIndex = new HashMap<>();
        String error = null;

        try (Connection conn = DatabaseConnection.getConnection()) {

            // 1) Load all Leave Types
            String typeSql = "SELECT LEAVE_TYPE_ID, TYPE_CODE, DESCRIPTION FROM LEAVE_TYPES ORDER BY LEAVE_TYPE_ID";
            try (PreparedStatement ps = conn.prepareStatement(typeSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> t = new HashMap<>();
                    t.put("id", rs.getInt("LEAVE_TYPE_ID"));
                    t.put("code", rs.getString("TYPE_CODE"));
                    t.put("desc", rs.getString("DESCRIPTION"));
                    leaveTypes.add(t);
                }
            }

            // 2) Load all Employees (including Gender)
            String empSql =
                    "SELECT EMPID, FULLNAME, EMAIL, GENDER, HIREDATE, PROFILE_PICTURE " +
                    "FROM USERS " + 
                    "WHERE UPPER(ROLE) = 'EMPLOYEE' " +
                    "ORDER BY FULLNAME";
            try (PreparedStatement ps = conn.prepareStatement(empSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> e = new HashMap<>();
                    e.put("empid", rs.getInt("EMPID"));
                    e.put("fullname", rs.getString("FULLNAME"));
                    e.put("email", rs.getString("EMAIL"));
                    e.put("gender", rs.getString("GENDER"));
                    e.put("hiredate", rs.getDate("HIREDATE"));
                    e.put("profilePic", rs.getString("PROFILE_PICTURE"));
                    employees.add(e);
                }
            }

            // 3) Load Existing Leave Balances from DB
            String balSql =
                    "SELECT EMPID, LEAVE_TYPE_ID, ENTITLEMENT, CARRIED_FWD, USED, PENDING, TOTAL " +
                    "FROM LEAVE_BALANCES";
            try (PreparedStatement ps = conn.prepareStatement(balSql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int empId = rs.getInt("EMPID");
                    int typeId = rs.getInt("LEAVE_TYPE_ID");

                    double entitlement = rs.getDouble("ENTITLEMENT");
                    double carriedFwd  = rs.getDouble("CARRIED_FWD");
                    double used        = rs.getDouble("USED");
                    double pending     = rs.getDouble("PENDING");
                    double total       = rs.getDouble("TOTAL");

                    // Available = (Total) - (Used + Pending)
                    double available = total - used - pending; 
                    if (available < 0) available = 0;

                    Map<String, Object> b = new HashMap<>();
                    b.put("entitlement", entitlement);
                    b.put("carriedFwd", carriedFwd);
                    b.put("used", used);
                    b.put("pending", pending);
                    b.put("total", total);
                    b.put("available", available);

                    balanceIndex.computeIfAbsent(empId, k -> new HashMap<>()).put(typeId, b);
                }
            }

            // ✅ 4) COMPUTE DEFAULT BALANCES FOR MISSING RECORDS WITH GENDER FILTERING
            for (Map<String, Object> emp : employees) {
                int empId = (Integer) emp.get("empid");
                String gender = (emp.get("gender") != null) ? emp.get("gender").toString().toUpperCase() : "";
                java.sql.Date sqlHireDate = (java.sql.Date) emp.get("hiredate");
                LocalDate hireDate = (sqlHireDate != null) ? sqlHireDate.toLocalDate() : LocalDate.now();

                Map<Integer, Map<String, Object>> empBals = balanceIndex.computeIfAbsent(empId, k -> new HashMap<>());

                for (Map<String, Object> type : leaveTypes) {
                    int typeId = (Integer) type.get("id");
                    String typeCode = ((String) type.get("code")).toUpperCase();

                    // ✅ GENDER LOGIC: Skip computing if gender doesn't match.
                    // This ensures the JSP sees them as "Not Assigned" or "N/A"
                    if (typeCode.contains("MATERNITY") && gender.startsWith("M")) continue;
                    if ((typeCode.contains("PATERNITY") || typeCode.contains("PARENITY")) && gender.startsWith("F")) continue;

                    // If no record exists in balanceIndex, compute default using the Engine
                    if (!empBals.containsKey(typeId)) {
                        LeaveBalanceEngine.EntitlementResult er = 
                            LeaveBalanceEngine.computeEntitlement(typeCode, hireDate, gender);
                        
                        Map<String, Object> b = new HashMap<>();
                        b.put("entitlement", (double) er.proratedEntitlement);
                        b.put("carriedFwd", 0.0);
                        b.put("used", 0.0);
                        b.put("pending", 0.0);
                        b.put("total", (double) er.proratedEntitlement);
                        b.put("available", (double) er.proratedEntitlement);

                        empBals.put(typeId, b);
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            error = ex.getMessage();
        }

        request.setAttribute("employees", employees);
        request.setAttribute("leaveTypes", leaveTypes);
        request.setAttribute("balanceIndex", balanceIndex);
        request.setAttribute("error", error);

        request.getRequestDispatcher("leaveEmpBalances.jsp").forward(request, response);
    }
}