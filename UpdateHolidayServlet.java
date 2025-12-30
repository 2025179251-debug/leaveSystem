import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet("/UpdateHolidayServlet")
public class UpdateHolidayServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

      
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null ||
                !"ADMIN".equalsIgnoreCase(String.valueOf(session.getAttribute("role")))) {
            response.sendRedirect("login.jsp?error=Please login as admin.");
            return;
        }

        // ✅ get parameter dari form
        String holidayId = request.getParameter("holidayId");
        String holidayName = request.getParameter("holidayName");
        String holidayDate = request.getParameter("holidayDate"); // expected YYYY-MM-DD
        String holidayType = request.getParameter("holidayType");

        // basic validation
        if (holidayId == null || holidayId.isBlank()
                || holidayName == null || holidayName.isBlank()
                || holidayDate == null || holidayDate.isBlank()
                || holidayType == null || holidayType.isBlank()) {

            response.sendRedirect("ManageHolidayServlet?error=Missing required fields.");
            return;
        }

        String sql =
            "UPDATE HOLIDAYS " +
            "SET HOLIDAY_NAME = ?, HOLIDAY_DATE = TO_DATE(?, 'YYYY-MM-DD'), HOLIDAY_TYPE = ? " +
            "WHERE HOLIDAY_ID = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, holidayName.trim());
            ps.setString(2, holidayDate.trim());
            ps.setString(3, holidayType.trim());
            ps.setString(4, holidayId.trim());

            int updated = ps.executeUpdate();

            if (updated > 0) {
                response.sendRedirect("ManageHolidayServlet?msg=Holiday updated successfully.");
            } else {
                response.sendRedirect("ManageHolidayServlet?error=Holiday not found (ID: " + holidayId + ").");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("ManageHolidayServlet?error=Failed to update holiday.");
        }
    }
}
