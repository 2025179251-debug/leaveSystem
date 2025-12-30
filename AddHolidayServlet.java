import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Servlet implementation class AddHolidayServlet
 */
@WebServlet("/AddHolidayServlet")
public class AddHolidayServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	  @Override
	  protected void doPost(HttpServletRequest request, HttpServletResponse response)
	      throws ServletException, IOException {

		   String date = request.getParameter("holidayDate");
	    String name = request.getParameter("holidayName");
	    String type = request.getParameter("holidayType");

	    if (name == null || date == null || type == null ||
	        name.isBlank() || date.isBlank() || type.isBlank()) {
	      response.sendRedirect("ManageHolidayServlet?error=Please fill all fields.");
	      return;
	    }

	    String sql = """
	      INSERT INTO HOLIDAYS (HOLIDAY_DATE, HOLIDAY_NAME, HOLIDAY_TYPE)
	       VALUES (TO_DATE(?, 'YYYY-MM-DD'), ?,  ?) """;

	    try (Connection con = DatabaseConnection.getConnection();
	         PreparedStatement ps = con.prepareStatement(sql)) {

	      ps.setString(1, date);
	      ps.setString(2, name);
	      ps.setString(3, type);
	      ps.executeUpdate();

	      response.sendRedirect("ManageHolidayServlet?msg=New holiday has been added successfully.");

	    } catch (Exception e) {
	      response.sendRedirect("ManageHolidayServlet?error=Failed to add holiday.");
	    }
	  }
	}
