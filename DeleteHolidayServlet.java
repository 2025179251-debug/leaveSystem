

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Servlet implementation class DeleteHolidayServlet
 */
@WebServlet("/DeleteHolidayServlet")
public class DeleteHolidayServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
      
	  @Override
	  protected void doPost(HttpServletRequest request, HttpServletResponse response)
	      throws ServletException, IOException {

	    String id = request.getParameter("holidayId");

	    if (id == null || id.isBlank()) {
	      response.sendRedirect("ManageHolidayServlet?error=Invalid holiday.");
	      return;
	    }

	    String sql = "DELETE FROM HOLIDAYS WHERE HOLIDAY_ID = ?";

	    try (Connection con = DatabaseConnection.getConnection();
	         PreparedStatement ps = con.prepareStatement(sql)) {

	      ps.setString(1, id);
	      ps.executeUpdate();

	      response.sendRedirect("ManageHolidayServlet?msg=Holiday has been deleted.");

	    } catch (Exception e) {
	      response.sendRedirect("ManageHolidayServlet?error=Failed to delete holiday.");
	    }
	  }
	}
