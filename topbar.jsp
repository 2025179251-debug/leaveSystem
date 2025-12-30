<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    String fullNameTB = (session.getAttribute("fullname") != null)
            ? session.getAttribute("fullname").toString()
            : "User";

    String roleTB = (session.getAttribute("role") != null)
            ? session.getAttribute("role").toString()
            : "EMPLOYEE";

    String initial = (fullNameTB != null && !fullNameTB.isBlank()) 
            ? ("" + fullNameTB.charAt(0)).toUpperCase() : "U";
%>

<header class="h-16 bg-white/80 backdrop-blur-md border-b border-slate-200 flex items-center justify-between px-6 sticky top-0 z-40 transition-all duration-300">
  
  <div class="flex items-center gap-4">
    <h2 class="text-sm font-extrabold text-slate-800 tracking-tight uppercase">
      <%= roleTB.equalsIgnoreCase("ADMIN") ? "Admin Portal" : "Employee Portal" %>
    </h2>
  </div>

  <div class="flex items-center gap-4">
    
    <button class="w-10 h-10 flex items-center justify-center rounded-full border border-slate-200 bg-white hover:bg-slate-50 transition-colors text-slate-600 relative" title="Notifications">
      <span class="text-lg">🔔</span>
      <span class="absolute top-2 right-2.5 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></span>
    </button>

    <div class="h-8 w-[1px] bg-slate-200 mx-1"></div>

    <a href="ProfileServlet" class="flex items-center gap-3 group transition-all" title="Go to My Profile">
      <div class="text-right hidden sm:block">
        <p class="text-[13px] font-black text-slate-900 leading-none group-hover:text-blue-600 transition-colors">
          <%= fullNameTB %>
        </p>
        <p class="text-[10px] text-slate-500 font-bold uppercase mt-1 tracking-wider">
          <%= roleTB %>
        </p>
      </div>

      <div class="w-10 h-10 rounded-full bg-blue-600 flex items-center justify-center text-white font-black text-sm shadow-md shadow-blue-200 border-2 border-slate-50 group-hover:scale-105 transition-transform">
        <% 
            String profilePic = (session.getAttribute("profilePic") != null) ? session.getAttribute("profilePic").toString() : null;
            if (profilePic != null && !profilePic.isBlank()) { 
        %>
          <img src="<%= profilePic %>" alt="Profile" class="w-full h-full object-cover rounded-full">
        <% } else { %>
          <%= initial %>
        <% } %>
      </div>
    </a>
  </div>
</header>