import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/ManageHolidayServlet")
public class ManageHolidayServlet extends HttpServlet {

  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

@Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    List<Map<String,Object>> holidays = new ArrayList<>();

    String sql =
      "SELECT HOLIDAY_ID, HOLIDAY_NAME, HOLIDAY_TYPE, " +
      "       TO_CHAR(HOLIDAY_DATE,'DD/MM/YYYY') AS DATE_DISPLAY, " +
      "       TO_CHAR(HOLIDAY_DATE,'YYYY-MM-DD') AS DATE_ISO " +
      "FROM HOLIDAYS " +
      "ORDER BY HOLIDAY_DATE";

    try (Connection con = DatabaseConnection.getConnection();
         PreparedStatement ps = con.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      System.out.println("DB USER = " + con.getMetaData().getUserName());

      while (rs.next()) {
        Map<String,Object> h = new HashMap<>();
        h.put("id", String.valueOf(rs.getLong("HOLIDAY_ID")));
        h.put("name", rs.getString("HOLIDAY_NAME"));
        h.put("type", rs.getString("HOLIDAY_TYPE"));
        h.put("dateDisplay", rs.getString("DATE_DISPLAY"));
        h.put("dateIso", rs.getString("DATE_ISO"));

        holidays.add(h);
      }

      System.out.println("HOLIDAYS LOADED = " + holidays.size());

    } catch (Exception e) {
      throw new ServletException("Error loading holidays", e);
    }

    request.setAttribute("holidays", holidays);
    request.getRequestDispatcher("/holidays.jsp").forward(request, response);
  }
}
