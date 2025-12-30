
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

@WebServlet("/CancelLeaveServlet")
public class CancelLeaveServlet extends HttpServlet {
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
        int empId = Integer.parseInt(String.valueOf(session.getAttribute("empid")));
        int leaveId = Integer.parseInt(idParam);

        // Update status from APPROVED to CANCEL_REQUESTED
      
        String sql = "UPDATE LEAVE_REQUESTS SET STATUS_ID = " +
                "(SELECT STATUS_ID FROM LEAVE_STATUSES WHERE UPPER(STATUS_CODE) = 'CANCELLED') " +
                "WHERE LEAVE_ID = ? AND EMPID = ? " +
                "AND STATUS_ID = (SELECT STATUS_ID FROM LEAVE_STATUSES WHERE UPPER(STATUS_CODE) = 'APPROVED')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, leaveId);
            ps.setInt(2, empId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                response.getWriter().print("OK");
            } else {
                response.sendError(400, "Cancellation request failed. Leave must be in APPROVED status.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(500, "Database Error: " + e.getMessage());
        }
    }
}