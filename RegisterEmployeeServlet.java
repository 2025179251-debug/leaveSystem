import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;

@WebServlet("/RegisterEmployeeServlet")
public class RegisterEmployeeServlet extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null ||
                !"ADMIN".equalsIgnoreCase(session.getAttribute("role").toString())) {
            response.sendRedirect("login.jsp?error=" + url("Please login as admin."));
            return;
        }

        request.getRequestDispatcher("adminRegisterEmployee.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null ||
                !"ADMIN".equalsIgnoreCase(session.getAttribute("role").toString())) {
            response.sendRedirect("login.jsp?error=" + url("Please login as admin."));
            return;
        }

        String fullname = request.getParameter("fullname");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String icNumber = request.getParameter("icNumber");
        String gender = request.getParameter("gender");
        String phoneNo = request.getParameter("phoneNo");
        String address = request.getParameter("address");
        String hireDate = request.getParameter("hireDate");
        String role = request.getParameter("role");

        if (fullname == null || email == null || password == null || icNumber == null ||
            fullname.isBlank() || email.isBlank() || password.isBlank() || icNumber.isBlank()) {
            response.sendRedirect("RegisterEmployeeServlet?error=" + url("Please fill all required fields."));
            return;
        }

        		String sql =
        		    "INSERT INTO USERS " +
        		    "(FULLNAME, EMAIL, PASSWORD, GENDER, HIREDATE, PHONENO, ADDRESS, IC_NUMBER, ROLE, PROFILE_PICTURE) " +
        		    "VALUES (?, ?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), ?, ?, ?, ?, NULL)";
        try (Connection con = DatabaseConnection.getConnection()) {

            // check duplicate email
            try (PreparedStatement chk = con.prepareStatement(
                    "SELECT COUNT(*) FROM USERS WHERE EMAIL = ?")) {
                chk.setString(1, email.trim());
                try (ResultSet rs = chk.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        response.sendRedirect("RegisterEmployeeServlet?error=" + url("Email already exists."));
                        return;
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, fullname.trim());
                ps.setString(2, email.trim());
                ps.setString(3, password); // (plain for assignment)
                ps.setString(4, gender);
                ps.setString(5, hireDate);
                ps.setString(6, phoneNo == null ? "" : phoneNo.trim());
                ps.setString(7, address == null ? "" : address.trim());
                ps.setString(8, icNumber.trim());
                ps.setString(9, role);

                ps.executeUpdate();
            }

            response.sendRedirect("RegisterEmployeeServlet?msg=" + url("Employee registered successfully."));

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("RegisterEmployeeServlet?error=" + url("Registration failed."));
        }
    }

    private String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
