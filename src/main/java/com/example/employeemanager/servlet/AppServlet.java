package com.example.employeemanager.servlet;

import com.example.employeemanager.form.AllocationForm;
import com.example.employeemanager.form.EmployeeForm;
import com.example.employeemanager.form.ProjectForm;
import com.example.employeemanager.model.Employee;
import com.example.employeemanager.model.Project;
import com.example.employeemanager.model.ProjectStatus;
import com.example.employeemanager.service.EmployeeService;
import com.example.employeemanager.view.FormBindingDialect;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.servlet.http.HttpSession;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AppServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AppServlet.class.getName());
    private final EmployeeService service = new EmployeeService();
    private TemplateEngine templates;

    @Override
    public void init() throws ServletException {
        try {
            ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
            resolver.setPrefix("templates/");
            resolver.setSuffix(".html");
            resolver.setTemplateMode("HTML");
            resolver.setCharacterEncoding("UTF-8");
            resolver.setCacheable(false);
            templates = new TemplateEngine();
            templates.setTemplateResolver(resolver);
            templates.setDialect(new FormBindingDialect());
        } catch (RuntimeException exception) {
            throw new ServletException("Could not initialize the Thymeleaf template engine.", exception);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = path(request);
        if ("/".equals(path)) {
            request.setAttribute("routePath", "/dashboard");
            request.getRequestDispatcher("/dashboard").forward(request, response);
            return;
        }
        try {
            switch (path) {
                case "/common/header" -> writeHeader(request, response);
                case "/dashboard" -> showDashboard(request, response);
                case "/employee/list" -> showEmployeeList(request, response);
                case "/employee/add" -> showEmployeeForm(request, response, null, null);
                case "/employee/edit" -> showEmployeeForm(request, response, parseId(request, "id"), null);
                case "/employee/projects" -> showEmployeeProjects(request, response);
                case "/project/list" -> showProjectList(request, response);
                case "/project/add" -> showProjectForm(request, response, null, null);
                case "/project/edit" -> showProjectForm(request, response, parseId(request, "id"), null);
                case "/allocation/add" -> showAllocationForm(request, response, null);
                case "/allocation/review" -> showAllocationReview(request, response, null);
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Request failed for " + path, exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "The request could not be completed. Check the application log for details.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String path = path(request);
        try {
            switch (path) {
                case "/employee/add", "/employee/edit" -> saveEmployee(request, response, path);
                case "/employee/delete" -> deleteEmployee(request, response);
                case "/project/add", "/project/edit" -> saveProject(request, response, path);
                case "/allocation/add" -> beginAllocation(request, response);
                case "/allocation/confirm" -> confirmAllocation(request, response);
                default -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Request failed for " + path, exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "The request could not be completed. Check the application log for details.");
        }
    }

    private void showDashboard(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        List<Employee> employees = service.allEmployees();
        List<Project> projects = service.allProjects();
        render(request, response, "dashboard", "Dashboard", model -> {
            model.setVariable("employeeCount", employees.size());
            model.setVariable("projectCount", projects.size());
        });
    }

    private void showEmployeeList(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        List<Employee> employees = service.allEmployees();
        render(request, response, "employee-list", "Employees", model -> {
            model.setVariable("employees", employees);
            model.setVariable("success", request.getParameter("success"));
        });
    }

    private void showEmployeeForm(HttpServletRequest request, HttpServletResponse response, Long id, String error)
            throws IOException, ServletException {
        EmployeeForm form = new EmployeeForm();
        if (id != null) {
            Employee employee = service.findEmployee(id);
            if (employee == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Employee was not found.");
                return;
            }
            form.setEmployeeId(employee.getEmployeeId());
            form.setEmployeeName(employee.getEmployeeName());
            form.setEmail(employee.getEmail());
            form.setDesignation(employee.getDesignation());
            form.setSalary(employee.getSalary().toPlainString());
        }
        renderEmployeeForm(request, response, form, error);
    }

    private void renderEmployeeForm(HttpServletRequest request, HttpServletResponse response, EmployeeForm form,
                                   String error) throws IOException, ServletException {
        String action = request.getContextPath() + (form.getEmployeeId() == null ? "/employee/add"
                : "/employee/edit?id=" + form.getEmployeeId());
        render(request, response, "employee-form", form.getEmployeeId() == null ? "Add employee" : "Edit employee",
                model -> {
                    model.setVariable("employeeForm", form);
                    model.setVariable("formAction", action);
                    model.setVariable("error", error);
                });
    }

    private void saveEmployee(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException, ServletException {
        boolean edit = "/employee/edit".equals(path);
        Long id = edit ? parseId(request, "id") : null;
        EmployeeForm form = new EmployeeForm();
        form.setEmployeeId(id);
        form.setEmployeeName(value(request, "employeeName"));
        form.setEmail(value(request, "email"));
        form.setDesignation(value(request, "designation"));
        form.setSalary(value(request, "salary"));

        try {
            if (form.getEmployeeName().isBlank() || form.getEmail().isBlank() || form.getDesignation().isBlank()) {
                throw new IllegalArgumentException("Name, email, and designation are required.");
            }
            BigDecimal salary = new BigDecimal(form.getSalary());
            if (salary.signum() < 0) {
                throw new IllegalArgumentException("Salary cannot be negative.");
            }
            service.saveEmployee(id, form.getEmployeeName().trim(), form.getEmail().trim(),
                    form.getDesignation().trim(), salary);
        } catch (IllegalArgumentException exception) {
            renderEmployeeForm(request, response, form, exception.getMessage());
            return;
        }
        response.sendRedirect(request.getContextPath() + "/employee/list?success=Employee+saved");
    }

    private void deleteEmployee(HttpServletRequest request, HttpServletResponse response) throws IOException {
        service.deleteEmployee(parseId(request, "id"));
        response.sendRedirect(request.getContextPath() + "/employee/list?success=Employee+deleted");
    }

    private void showProjectList(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        List<Project> projects = service.allProjects();
        render(request, response, "project-list", "Projects", model -> {
            model.setVariable("projects", projects);
            model.setVariable("success", request.getParameter("success"));
        });
    }

    private void showProjectForm(HttpServletRequest request, HttpServletResponse response, Long id, String error)
            throws IOException, ServletException {
        ProjectForm form = new ProjectForm();
        if (id != null) {
            Project project = service.findProject(id);
            if (project == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Project was not found.");
                return;
            }
            form.setProjectId(project.getProjectId());
            form.setProjectName(project.getProjectName());
            form.setClientName(project.getClientName());
            form.setStartDate(project.getStartDate().toString());
            form.setBudget(project.getBudget().toPlainString());
            form.setMaximumTeamSize(project.getMaximumTeamSize().toString());
            form.setStatus(project.getStatus().name());
        }
        renderProjectForm(request, response, form, error);
    }

    private void renderProjectForm(HttpServletRequest request, HttpServletResponse response, ProjectForm form,
                                   String error) throws IOException, ServletException {
        String action = request.getContextPath() + (form.getProjectId() == null ? "/project/add"
                : "/project/edit?id=" + form.getProjectId());
        render(request, response, "project-form", form.getProjectId() == null ? "Add project" : "Edit project",
                model -> {
                    model.setVariable("projectForm", form);
                    model.setVariable("formAction", action);
                    model.setVariable("error", error);
                    model.setVariable("statuses", ProjectStatus.values());
                });
    }

    private void saveProject(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException, ServletException {
        boolean edit = "/project/edit".equals(path);
        Long id = edit ? parseId(request, "id") : null;
        ProjectForm form = new ProjectForm();
        form.setProjectId(id);
        form.setProjectName(value(request, "projectName"));
        form.setClientName(value(request, "clientName"));
        form.setStartDate(value(request, "startDate"));
        form.setBudget(value(request, "budget"));
        form.setMaximumTeamSize(value(request, "maximumTeamSize"));
        form.setStatus(value(request, "status"));
        try {
            if (form.getProjectName().isBlank() || form.getClientName().isBlank()) {
                throw new IllegalArgumentException("Project and client names are required.");
            }
            LocalDate startDate = LocalDate.parse(form.getStartDate());
            BigDecimal budget = new BigDecimal(form.getBudget());
            int maximumTeamSize = Integer.parseInt(form.getMaximumTeamSize());
            ProjectStatus status = ProjectStatus.valueOf(form.getStatus());
            if (budget.signum() < 0 || maximumTeamSize < 1) {
                throw new IllegalArgumentException("Budget cannot be negative and team size must be at least one.");
            }
            service.saveProject(id, form.getProjectName().trim(), form.getClientName().trim(), startDate,
                    budget, maximumTeamSize, status);
        } catch (DateTimeException | IllegalArgumentException exception) {
            renderProjectForm(request, response, form, "Please enter valid project details. " + exception.getMessage());
            return;
        }
        response.sendRedirect(request.getContextPath() + "/project/list?success=Project+saved");
    }

    private void showAllocationForm(HttpServletRequest request, HttpServletResponse response, String error)
            throws IOException, ServletException {
        AllocationForm form = new AllocationForm();
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("pendingAllocation") instanceof PendingAllocation pending) {
            form.setEmployeeId(pending.employeeId());
            form.setProjectIds(pending.projectIds());
        }
        renderAllocationForm(request, response, form, error);
    }

    private void renderAllocationForm(HttpServletRequest request, HttpServletResponse response, AllocationForm form,
                                     String error) throws IOException, ServletException {
        List<Employee> employees = service.allEmployees().stream()
                .toList();
        List<Project> projects = service.allProjects();
        render(request, response, "allocation-form", "Allocate projects", model -> {
            model.setVariable("allocationForm", form);
            model.setVariable("employees", employees);
            model.setVariable("projects", projects);
            model.setVariable("error", error);
        });
    }

    private void beginAllocation(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        try {
            long employeeId = Long.parseLong(value(request, "employeeId"));
            String[] rawIds = request.getParameterValues("projectIds");
            if (rawIds == null || rawIds.length == 0) {
                throw new IllegalArgumentException("Select at least one project.");
            }
            List<Long> projectIds = Arrays.stream(rawIds).map(Long::parseLong).toList();
            request.getSession(true).setAttribute("pendingAllocation",
                    new PendingAllocation(employeeId, projectIds));
            response.sendRedirect(request.getContextPath() + "/allocation/review");
        } catch (IllegalArgumentException exception) {
            AllocationForm form = new AllocationForm();
            try {
                form.setEmployeeId(Long.parseLong(value(request, "employeeId")));
            } catch (NumberFormatException ignored) {
                // Keep the remaining submitted project selections available for correction.
            }
            String[] rawIds = request.getParameterValues("projectIds");
            if (rawIds != null) {
                List<Long> parsedIds = new ArrayList<>();
                for (String rawId : rawIds) {
                    try {
                        parsedIds.add(Long.parseLong(rawId));
                    } catch (NumberFormatException ignored) {
                        // Invalid values cannot be selected in the form; valid selections are retained.
                    }
                }
                form.setProjectIds(parsedIds);
            }
            renderAllocationForm(request, response, form, exception.getMessage());
        }
    }

    private void showAllocationReview(HttpServletRequest request, HttpServletResponse response, String error)
            throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        PendingAllocation pending = session == null ? null
                : (PendingAllocation) session.getAttribute("pendingAllocation");
        if (pending == null) {
            Object storedResult = session == null ? null : session.getAttribute("lastAllocation");
            if ("true".equals(request.getParameter("success")) && storedResult instanceof Map<?, ?> result) {
                render(request, response, "allocation-review", "Allocation complete", model -> {
                    model.setVariable("success", true);
                    model.setVariable("result", result);
                });
                session.removeAttribute("lastAllocation");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/allocation/add");
            return;
        }
        Employee employee = service.findEmployee(pending.employeeId());
        if (employee == null) {
            session.removeAttribute("pendingAllocation");
            showAllocationForm(request, response, "The selected employee no longer exists. Please choose another.");
            return;
        }
        List<Project> selectedProjects = new ArrayList<>();
        for (Long projectId : pending.projectIds()) {
            Project project = service.findProject(projectId);
            if (project != null) {
                selectedProjects.add(project);
            }
        }
        render(request, response, "allocation-review", "Review allocation", model -> {
            model.setVariable("success", false);
            model.setVariable("employee", employee);
            model.setVariable("projects", selectedProjects);
            model.setVariable("error", error);
        });
    }

    private void confirmAllocation(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        HttpSession session = request.getSession(false);
        PendingAllocation pending = session == null ? null
                : (PendingAllocation) session.getAttribute("pendingAllocation");
        if (pending == null) {
            response.sendRedirect(request.getContextPath() + "/allocation/add");
            return;
        }
        try {
            service.allocate(pending.employeeId(), pending.projectIds());
            Employee employee = service.findEmployee(pending.employeeId());
            List<String> projectNames = pending.projectIds().stream()
                    .map(service::findProject)
                    .filter(project -> project != null)
                    .map(Project::getProjectName)
                    .toList();
            session.setAttribute("lastAllocation", Map.of(
                    "employeeName", employee.getEmployeeName(),
                    "projectNames", projectNames));
            session.removeAttribute("pendingAllocation");
            response.sendRedirect(request.getContextPath() + "/allocation/review?success=true");
        } catch (IllegalArgumentException exception) {
            showAllocationReview(request, response, exception.getMessage());
        }
    }

    private void showEmployeeProjects(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        long employeeId = parseId(request, "id");
        Employee employee = service.findEmployee(employeeId);
        if (employee == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Employee was not found.");
            return;
        }
        List<Project> projects = service.projectsForEmployee(employeeId);
        render(request, response, "employee-projects", "Employee projects", model -> {
            model.setVariable("projects", projects);
            model.setVariable("employeeName", employee.getEmployeeName());
        });
    }

    private void render(HttpServletRequest request, HttpServletResponse response, String template, String title,
                        ModelConfigurer configurer) throws IOException, ServletException {
        Context context = new Context(Locale.getDefault());
        context.setVariable("pageTitle", title);
        context.setVariable("contextPath", request.getContextPath());
        HttpSession session = request.getSession(false);
        StringWriter includedHeader = new StringWriter();
        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(response) {
            @Override
            public PrintWriter getWriter() {
                return new PrintWriter(includedHeader);
            }
        };
        RequestDispatcher dispatcher = request.getRequestDispatcher("/common/header");
        dispatcher.include(request, wrapper);
        context.setVariable("navigationHtml", includedHeader.toString());
        if (session != null) {
            context.setVariable("pendingAllocation", session.getAttribute("pendingAllocation"));
        }
        configurer.configure(context);
        response.setContentType("text/html;charset=UTF-8");
        templates.process(template, context, response.getWriter());
    }

    private void writeHeader(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String contextPath = request.getContextPath();
        response.getWriter().write("""
                <header class="topbar">
                  <a class="brand" href="%s/dashboard">Employee Projects</a>
                  <nav>
                    <a href="%s/dashboard">Dashboard</a>
                    <a href="%s/employee/list">Employees</a>
                    <a href="%s/project/list">Projects</a>
                    <a href="%s/allocation/add">Allocations</a>
                  </nav>
                </header>
                """.formatted(contextPath, contextPath, contextPath, contextPath, contextPath));
    }

    private static String path(HttpServletRequest request) {
        Object includedPath = request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
        if (includedPath instanceof String value) {
            return value;
        }
        Object forwardedPath = request.getAttribute("routePath");
        if (forwardedPath instanceof String value) {
            return value;
        }
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private static String value(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    private static long parseId(HttpServletRequest request, String parameter) {
        try {
            return Long.parseLong(request.getParameter(parameter));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("A valid record ID is required.");
        }
    }

    @FunctionalInterface
    private interface ModelConfigurer {
        void configure(Context context);
    }

    private record PendingAllocation(long employeeId, List<Long> projectIds)
            implements java.io.Serializable {
        private PendingAllocation {
            projectIds = List.copyOf(projectIds);
        }
    }

}
