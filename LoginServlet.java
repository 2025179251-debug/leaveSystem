import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || password == null || email.isBlank() || password.isBlank()) {
            response.sendRedirect("login.jsp?error=" + url("Please enter email and password."));
            return;
        }

        String sql = "SELECT EMPID, FULLNAME, ROLE " +
                     "FROM USERS " +
                     "WHERE EMAIL = ? AND PASSWORD = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {

                    int empid = rs.getInt("EMPID");
                    String fullname = rs.getString("FULLNAME");
                    String role = rs.getString("ROLE"); // expected 'ADMIN' / 'EMPLOYEE'

                    // ✅ session
                    HttpSession session = request.getSession(true);
                    session.setAttribute("empid", empid);
                    session.setAttribute("fullname", fullname);
                    session.setAttribute("role", role);

                    // ✅ redirect ikut role
                    if ("ADMIN".equalsIgnoreCase(role)) {
                        response.sendRedirect("AdminDashboardServlet");
                    } else {
                        response.sendRedirect("EmployeeDashboardServlet");
                    }

                } else {
                    response.sendRedirect("login.jsp?error=" + url("Invalid email or password."));
                }
            }

        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            response.sendRedirect("login.jsp?error=" + url("Login error. Please try again."));
        }
    }

    private String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
