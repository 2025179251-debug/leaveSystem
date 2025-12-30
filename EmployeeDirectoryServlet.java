import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet("/EmployeeDirectoryServlet")
public class EmployeeDirectoryServlet extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null ||
                !"ADMIN".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendRedirect("login.jsp?error=Please login as admin.");
            return;
        }

        List<Map<String, Object>> users = new ArrayList<>();

        String sql = "SELECT EMPID, FULLNAME, EMAIL, ROLE, PHONENO, HIREDATE " +
                     "FROM USERS ORDER BY FULLNAME ASC";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> u = new HashMap<>();
                u.put("empid", rs.getInt("EMPID"));
                u.put("fullname", rs.getString("FULLNAME"));
                u.put("email", rs.getString("EMAIL"));
                u.put("role", rs.getString("ROLE"));
                u.put("phone", rs.getString("PHONENO"));
                u.put("hiredate", rs.getDate("HIREDATE"));
                users.add(u);
            }
        } catch (Exception e) {
            throw new ServletException("Error loading directory", e);
        }

        request.setAttribute("users", users);
        request.getRequestDispatcher("employeeDirectory.jsp").forward(request, response);
    }
}
