	import java.io.IOException;
	import java.net.URLEncoder;
	import java.sql.Connection;
	import java.sql.Date;
	import java.sql.DriverManager;
	import java.sql.PreparedStatement;

	import jakarta.servlet.ServletException;
	import jakarta.servlet.annotation.WebServlet;
	import jakarta.servlet.http.HttpServlet;
	import jakarta.servlet.http.HttpServletRequest;
	import jakarta.servlet.http.HttpServletResponse;
	
	
	@WebServlet("/OracleServlet")  // must match form action
	public class OracleServlet extends HttpServlet {
			private static final long serialVersionUID = 1L;

	    // Update these to match your DB settings
	    private static final String ORACLE_DRIVER = "oracle.jdbc.driver.OracleDriver";
	    private static final String ORACLE_URL    = "jdbc:oracle:thin:@//localhost:1521/freepdb1"; 
	    private static final String ORACLE_USER   = "LEAVE";
	    private static final String ORACLE_PASS   = "leave";

	    @Override
	    protected void doPost(HttpServletRequest request, HttpServletResponse response)
	            throws ServletException, IOException {

	        // Retrieve form input (names must match index.html)
	        String empId       = request.getParameter("empId");
	        String fullName    = request.getParameter("fullName");
	        String email       = request.getParameter("email");
	        String password    = request.getParameter("password");
	        String gender      = request.getParameter("gender");
	        String hireDateStr = request.getParameter("hireDate");  // yyyy-MM-dd
	        String phoneNo     = request.getParameter("phoneNo");
	        String address     = request.getParameter("address");
	        String icNumber    = request.getParameter("icNumber");

	        Date hireDate = null;
	        if (hireDateStr != null && !hireDateStr.isEmpty()) {
	            // HTML date format yyyy-MM-dd is compatible with Date.valueOf
	            hireDate = Date.valueOf(hireDateStr);
	        }

	        Connection con = null;
	        PreparedStatement ps = null;

	        try {
	            // 1. Load the driver
	            Class.forName(ORACLE_DRIVER);

	            // 2. Create connection
	            con = DriverManager.getConnection(ORACLE_URL, ORACLE_USER, ORACLE_PASS);

	            // 3. Create SQL statement (must match your Employee table)
	            String sql = "INSERT INTO Employee "
	                       + "(EmpID, FullName, Email, Password, Gender, HireDate, PhoneNo, Address, IC_Number) "
	                       + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

	            ps = con.prepareStatement(sql);

	            // 4. Set parameter values
	            ps.setString(1, empId);
	            ps.setString(2, fullName);
	            ps.setString(3, email);
	            ps.setString(4, password);   // in real app, hash before storing
	            ps.setString(5, gender);
	            ps.setDate(6, hireDate);
	            ps.setString(7, phoneNo);
	            ps.setString(8, address);
	            ps.setString(9, icNumber);

	            // 5. Execute query
	            int rows = ps.executeUpdate();

	            String message;
	            if (rows > 0) {
	                message = "Employee record inserted successfully.";
	            } else {
	                message = "Employee insertion failed.";
	            }

	            // 6. Redirect to JSP with message as a request parameter ?msg=
	            String encodedMsg = URLEncoder.encode(message, "UTF-8");
	            response.sendRedirect("result.jsp?msg=" + encodedMsg);

	        } catch (Exception e) {
	            // You can customize message here too if you want to show DB error
	            throw new ServletException("DB Error", e);
	        } finally {
	            try { if (ps  != null) ps.close(); } catch (Exception e) {}
	            try { if (con != null) con.close(); } catch (Exception e) {}
	        }
	    }
	}
