import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.*;

@WebServlet("/ProfileServlet")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1MB
        maxFileSize = 5 * 1024 * 1024,        // 5MB
        maxRequestSize = 10 * 1024 * 1024     // 10MB
)
public class ProfileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // ✅ simpan dalam folder webapp/uploads (pastikan wujud)
    private String getUploadDir(HttpServletRequest request) {
        String appPath = request.getServletContext().getRealPath("");
        return appPath + File.separator + "uploads";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empid") == null) {
            response.sendRedirect("login.jsp?error=Please login.");
            return;
        }

        int empid = Integer.parseInt(String.valueOf(session.getAttribute("empid")));

        String sql =
                "SELECT EMPID, FULLNAME, EMAIL, ROLE, PHONENO, ADDRESS, HIREDATE, IC_NUMBER, GENDER, PROFILE_PICTURE " +
                "FROM USERS WHERE EMPID = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, empid);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    response.sendRedirect("login.jsp?error=User not found.");
                    return;
                }

                request.setAttribute("empid", rs.getInt("EMPID"));
                request.setAttribute("fullname", rs.getString("FULLNAME"));
                request.setAttribute("email", rs.getString("EMAIL"));
                request.setAttribute("role", rs.getString("ROLE"));
                request.setAttribute("phone", rs.getString("PHONENO"));
                request.setAttribute("address", rs.getString("ADDRESS"));
                request.setAttribute("hireDate", rs.getDate("HIREDATE"));
                request.setAttribute("icNumber", rs.getString("IC_NUMBER"));
                request.setAttribute("gender", rs.getString("GENDER"));
                request.setAttribute("profilePic", rs.getString("PROFILE_PICTURE"));
            }

            request.getRequestDispatcher("profile.jsp").forward(request, response);

        } catch (Exception e) {
            throw new ServletException("Error loading profile", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empid") == null) {
            response.sendRedirect("login.jsp?error=Please login.");
            return;
        }

        int empid = Integer.parseInt(String.valueOf(session.getAttribute("empid")));

        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");

        // basic validate
        if (email == null || email.isBlank()) {
            response.sendRedirect("ProfileServlet?edit=1&error=Email is required.");
            return;
        }

        String newProfilePicPath = null;

        // handle upload (optional)
        Part profilePicPart = request.getPart("profilePic");
        if (profilePicPart != null && profilePicPart.getSize() > 0) {

            // allow only images
            String contentType = profilePicPart.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.sendRedirect("ProfileServlet?edit=1&error=Profile picture must be an image.");
                return;
            }

            String uploadsDir = getUploadDir(request);
            File dir = new File(uploadsDir);
            if (!dir.exists()) dir.mkdirs();

            String submitted = Paths.get(profilePicPart.getSubmittedFileName()).getFileName().toString();
            String ext = "";
            int dot = submitted.lastIndexOf('.');
            if (dot > -1) ext = submitted.substring(dot);

            String fileName = "emp_" + empid + "_" + System.currentTimeMillis() + ext;
            String fullPath = uploadsDir + File.separator + fileName;

            profilePicPart.write(fullPath);

            // store relative path for web access
            newProfilePicPath = "uploads/" + fileName;
        }

        // update SQL
        String sqlNoPic = "UPDATE USERS SET EMAIL=?, PHONENO=?, ADDRESS=? WHERE EMPID=?";
        String sqlWithPic = "UPDATE USERS SET EMAIL=?, PHONENO=?, ADDRESS=?, PROFILE_PICTURE=? WHERE EMPID=?";

        try (Connection con = DatabaseConnection.getConnection()) {

            if (newProfilePicPath == null) {
                try (PreparedStatement ps = con.prepareStatement(sqlNoPic)) {
                    ps.setString(1, email);
                    ps.setString(2, phone);
                    ps.setString(3, address);
                    ps.setInt(4, empid);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = con.prepareStatement(sqlWithPic)) {
                    ps.setString(1, email);
                    ps.setString(2, phone);
                    ps.setString(3, address);
                    ps.setString(4, newProfilePicPath);
                    ps.setInt(5, empid);
                    ps.executeUpdate();
                }

                // ✅ update session for topbar avatar usage (optional)
                session.setAttribute("profilePic", newProfilePicPath);
            }

            // ✅ update session name/email if you want
            // session.setAttribute("fullname", ...);

            response.sendRedirect("ProfileServlet?msg=Profile updated successfully.");
        } catch (SQLIntegrityConstraintViolationException dup) {
            response.sendRedirect("ProfileServlet?edit=1&error=Email already exists.");
        } catch (Exception e) {
            throw new ServletException("Error updating profile", e);
        }
    }
}
