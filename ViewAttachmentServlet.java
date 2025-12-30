import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.sql.*;

@WebServlet("/ViewAttachmentServlet")
public class ViewAttachmentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ✅ security (employee only)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("empid") == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        String idStr = request.getParameter("id");
        if (idStr == null || idStr.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing id");
            return;
        }

        int leaveId;
        try { leaveId = Integer.parseInt(idStr); }
        catch (Exception e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid id");
            return;
        }

        String sql =
            "SELECT FILE_DATA, MIME_TYPE, FILE_NAME " +
            "FROM ( " +
            "   SELECT FILE_DATA, MIME_TYPE, FILE_NAME, UPLOADED_ON, " +
            "          ROW_NUMBER() OVER (ORDER BY UPLOADED_ON DESC) rn " +
            "   FROM LEAVE_REQUEST_ATTACHMENTS " +
            "   WHERE LEAVE_ID = ? " +
            ") WHERE rn = 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, leaveId);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND, "No attachment found");
                    return;
                }

                String contentType = rs.getString("MIME_TYPE");
                if (contentType == null || contentType.isBlank()) contentType = "application/octet-stream";

                Blob blob = rs.getBlob("FILE_DATA");
                String fileName = rs.getString("FILE_NAME");
                if (fileName == null || fileName.isBlank()) fileName = "attachment";

                response.setContentType(contentType);
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("Content-Disposition", "inline; filename=\"" + fileName.replace("\"","") + "\"");

                try (InputStream in = blob.getBinaryStream();
                     OutputStream out = response.getOutputStream()) {

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
            }

        } catch (Exception e) {
            // For debugging sementara:
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Attachment error: " + e.getMessage());
        }
    }
}
