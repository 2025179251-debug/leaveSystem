<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ include file="icon.jsp" %>

<%
    String role = (session.getAttribute("role") != null) ? (String) session.getAttribute("role") : "EMPLOYEE";
    String uri = request.getRequestURI();
    // Mendapatkan nama fail semasa untuk logik menu aktif
    String activePage = uri.substring(uri.lastIndexOf("/") + 1);
%>

<script src="https://cdn.tailwindcss.com"></script>

<style>
    .no-scrollbar::-webkit-scrollbar { display: none; }
    #appSidebar { transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
    /* Memastikan font Arial konsisten pada sidebar */
    #appSidebar * { font-family: Arial, sans-serif !important; }
</style>

<aside id="appSidebar" class="fixed left-0 top-0 h-full bg-[#0f172a] text-slate-200 border-r border-slate-800 z-50 flex flex-col w-20 lg:w-64 transition-all duration-300">
    
    <div class="p-4 lg:p-6 border-b border-slate-800 flex items-center gap-3 overflow-hidden shrink-0">
        <div class="min-w-[40px] w-10 h-10 bg-white rounded-lg flex items-center justify-center shrink-0 p-1">
            <img src="https://encrypted-tbn1.gstatic.com/images?q=tbn:ANd9GcRNhLlRcJ19hFyLWQOGP3EWiaxRZiHWupjWp6xtRzs5cdMeCUzu" alt="Logo" class="max-w-full max-h-full object-contain" />
        </div>
        <div class="hidden lg:block">
            <h1 class="text-[12px] font-extrabold text-white leading-tight uppercase">Klinik <br>Dr Mohamad</h1>
        </div>
    </div>

    <nav class="flex-1 px-3 mt-4 space-y-1 no-scrollbar overflow-y-auto">
        <% if (role.equalsIgnoreCase("ADMIN")) { %>
            <a href="AdminDashboardServlet" class="flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 group <%= activePage.equalsIgnoreCase("AdminDashboardServlet") ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800" %>">
                <span class="shrink-0"><%= BriefcaseIcon("w-5 h-5") %></span>
                <span class="hidden lg:block whitespace-nowrap text-sm font-medium">Admin Dashboard</span>
            </a>

            <a href="RegisterEmployeeServlet" class="flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 group <%= activePage.equalsIgnoreCase("RegisterEmployeeServlet") || activePage.equalsIgnoreCase("EmployeeDirectoryServlet") ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800" %>">
                <span class="shrink-0"><%= UsersIcon("w-5 h-5") %></span>
                <span class="hidden lg:block whitespace-nowrap text-sm font-medium">Employees</span>
            </a>

            <a href="LeaveEmpBalancesServlet" class="flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 group <%= activePage.equalsIgnoreCase("LeaveEmpBalancesServlet") ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800" %>">
                <span class="shrink-0"><%= ChartBarIcon("w-5 h-5") %></span>
                <span class="hidden lg:block whitespace-nowrap text-sm font-medium">Leave Balances</span>
            </a>

            <a href="leaveEmpHistoryServlet" class="flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 group <%= activePage.equalsIgnoreCase("leaveEmpHistoryServlet") ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800" %>">
                <span class="shrink-0"><%= ClipboardListIcon("w-5 h-5") %></span>
                <span class="hidden lg:block whitespace-nowrap text-sm font-medium">Leave History</span>
            </a>

            <a href="ManageHolidayServlet" class="flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 group <%= activePage.equalsIgnoreCase("ManageHolidayServlet") ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800" %>">
                <span class="shrink-0"><%= CalendarIcon("w-5 h-5") %></span>
                <span class="hidden lg:block whitespace-nowrap text-sm font-medium">Manage Holidays</span>
            </a>

        <% } else { %>
            <a href="EmployeeDashboardServlet" class="flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 group <%= activePage.equalsIgnoreCase("EmployeeDashboardServlet") ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800" %>">
                <span class="shrink-0"><%= HomeIcon("w-5 h-5") %></span>
                <span class="hidden lg:block whitespace-nowrap text-sm font-medium">Dashboard</span>
            </a>

            <a href="ApplyLeaveServlet" class="flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 group <%= activePage.equalsIgnoreCase("ApplyLeaveServlet") ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800" %>">
                <span class="shrink-0"><%= CalendarIcon("w-5 h-5") %></span>
                <span class="hidden lg:block whitespace-nowrap text-sm font-medium">Apply Leave</span>
            </a>

            <a href="LeaveHistoryServlet" class="flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 group <%= activePage.equalsIgnoreCase("LeaveHistoryServlet") ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-slate-800" %>">
                <span class="shrink-0"><%= ClipboardListIcon("w-5 h-5") %></span>
                <span class="hidden lg:block whitespace-nowrap text-sm font-medium">My History</span>
            </a>
        <% } %>
    </nav>

    <div class="p-3 border-t border-slate-800 bg-[#0f172a]">
        <a href="LogoutServlet" class="flex items-center gap-3 px-3 py-2.5 w-full rounded-xl text-red-400 hover:bg-red-500/10 transition-colors font-medium">
            <span class="shrink-0"><%= LogOutIcon("w-5 h-5") %></span>
            <span class="hidden lg:block text-sm">Sign Out</span>
        </a>
    </div>
</aside>