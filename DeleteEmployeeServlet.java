import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet("/DeleteEmployeeServlet")
public class DeleteEmployeeServlet extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null ||
                !"ADMIN".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendRedirect("login.jsp?error=" + url("Please login as admin."));
            return;
        }

        String empidStr = request.getParameter("empid");
        if (empidStr == null || empidStr.isBlank()) {
            response.sendRedirect("EmployeeDirectoryServlet?error=" + url("Missing EMPID."));
            return;
        }

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM USERS WHERE EMPID = ? AND UPPER(ROLE) <> 'ADMIN'")) {
            ps.setInt(1, Integer.parseInt(empidStr));
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("EmployeeDirectoryServlet?error=" + url("Delete failed."));
            return;
        }

        response.sendRedirect("EmployeeDirectoryServlet?msg=" + url("Employee deleted."));
    }

    private String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
