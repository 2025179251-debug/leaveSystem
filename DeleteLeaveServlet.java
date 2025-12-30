
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet("/DeleteLeaveServlet")
public class DeleteLeaveServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empid") == null) {
            response.sendError(401, "Unauthorized");
            return;
        }

        String idParam = request.getParameter("id");
        if (idParam == null || idParam.isBlank()) {
            response.sendError(400, "Missing ID");
            return;
        }

        int empId = Integer.parseInt(String.valueOf(session.getAttribute("empid")));
        int leaveId = Integer.parseInt(idParam);

        // Only delete if status is PENDING
        String sql = "DELETE FROM LEAVE_REQUESTS " +
                     "WHERE LEAVE_ID = ? AND EMPID = ? " +
                     "AND STATUS_ID = (SELECT STATUS_ID FROM LEAVE_STATUSES WHERE UPPER(STATUS_CODE) = 'PENDING')";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, leaveId);
            ps.setInt(2, empId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                response.getWriter().print("OK");
            } else {
                response.sendError(400, "Deletion failed. Request may no longer be pending.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Database Error: " + e.getMessage());
        }
    }
}