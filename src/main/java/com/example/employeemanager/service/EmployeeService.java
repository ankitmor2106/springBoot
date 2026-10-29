package com.example.employeemanager.service;

import com.example.employeemanager.Database;
import com.example.employeemanager.model.Employee;
import com.example.employeemanager.model.Project;
import com.example.employeemanager.model.ProjectStatus;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EmployeeService {
    public List<Employee> allEmployees() {
        return Database.transaction(session -> session.createSelectionQuery(
                        "select distinct e from Employee e left join fetch e.projects order by e.employeeName",
                        Employee.class).getResultList());
    }

    public Employee findEmployee(long id) {
        return Database.transaction(session -> session.createSelectionQuery(
                        "select distinct e from Employee e left join fetch e.projects where e.employeeId = :id",
                        Employee.class).setParameter("id", id).uniqueResult());
    }

    public void saveEmployee(Long id, String name, String email, String designation, BigDecimal salary) {
        Database.transaction(session -> {
            Employee employee = id == null ? new Employee(name, email, designation, salary)
                    : requireEmployee(session, id);
            Employee duplicate = session.createSelectionQuery(
                            "from Employee e where lower(e.email) = :email and e.employeeId <> :id",
                            Employee.class)
                    .setParameter("email", email.toLowerCase(Locale.ROOT))
                    .setParameter("id", id == null ? -1L : id)
                    .uniqueResult();
            if (duplicate != null) {
                throw new IllegalArgumentException("An employee with that email already exists.");
            }
            employee.setEmployeeName(name);
            employee.setEmail(email);
            employee.setDesignation(designation);
            employee.setSalary(salary);
            if (id == null) {
                session.persist(employee);
            }
            return null;
        });
    }

    public void deleteEmployee(long id) {
        Database.transaction(session -> {
            Employee employee = session.createSelectionQuery(
                            "select distinct e from Employee e left join fetch e.projects where e.employeeId = :id",
                            Employee.class)
                    .setParameter("id", id)
                    .uniqueResult();
            if (employee != null) {
                for (Project project : Set.copyOf(employee.getProjects())) {
                    employee.removeProject(project);
                }
                session.remove(employee);
            }
            return null;
        });
    }

    public List<Project> allProjects() {
        return Database.transaction(session -> session.createSelectionQuery(
                        "select distinct p from Project p left join fetch p.employees order by p.projectName",
                        Project.class).getResultList());
    }

    public Project findProject(long id) {
        return Database.transaction(session -> session.createSelectionQuery(
                        "select distinct p from Project p left join fetch p.employees where p.projectId = :id",
                        Project.class).setParameter("id", id).uniqueResult());
    }

    public void saveProject(Long id, String name, String client, LocalDate startDate, BigDecimal budget,
                            int maximumTeamSize, ProjectStatus status) {
        Database.transaction(session -> {
            Project project = id == null ? new Project(name, client, startDate, budget, maximumTeamSize, status)
                    : session.createSelectionQuery(
                                    "select distinct p from Project p left join fetch p.employees where p.projectId = :id",
                                    Project.class)
                            .setParameter("id", id)
                            .uniqueResult();
            if (project == null) {
                throw new IllegalArgumentException("Project was not found.");
            }
            if (maximumTeamSize < project.getEmployees().size()) {
                throw new IllegalArgumentException("Maximum team size cannot be less than the current team size.");
            }
            project.setProjectName(name);
            project.setClientName(client);
            project.setStartDate(startDate);
            project.setBudget(budget);
            project.setMaximumTeamSize(maximumTeamSize);
            project.setStatus(status);
            if (id == null) {
                session.persist(project);
            }
            return null;
        });
    }

    public void allocate(long employeeId, List<Long> projectIds) {
        Set<Long> uniqueProjectIds = new LinkedHashSet<>(projectIds);
        if (uniqueProjectIds.size() != projectIds.size()) {
            throw new IllegalArgumentException("The same project was selected more than once.");
        }
        Database.transaction(session -> {
            Employee employee = requireEmployee(session, employeeId);
            for (Long projectId : uniqueProjectIds) {
                Project project = session.createSelectionQuery(
                                "select distinct p from Project p left join fetch p.employees where p.projectId = :id",
                                Project.class)
                        .setParameter("id", projectId)
                        .uniqueResult();
                if (project == null) {
                    throw new IllegalArgumentException("A selected project no longer exists.");
                }
                if (employee.getProjects().contains(project)) {
                    throw new IllegalArgumentException(
                            employee.getEmployeeName() + " is already allocated to " + project.getProjectName() + ".");
                }
                if (project.getStatus() == ProjectStatus.COMPLETED) {
                    throw new IllegalArgumentException(
                            "Completed project " + project.getProjectName() + " cannot accept new employees.");
                }
                if (project.getEmployees().size() >= project.getMaximumTeamSize()) {
                    throw new IllegalArgumentException(
                            project.getProjectName() + " has reached its maximum team size.");
                }
                employee.addProject(project);
            }
            return null;
        });
    }

    public List<Project> projectsForEmployee(long employeeId) {
        return Database.transaction(session -> session.createSelectionQuery(
                        "select distinct p from Project p join p.employees e where e.employeeId = :id order by p.projectName",
                        Project.class).setParameter("id", employeeId).getResultList());
    }

    private Employee requireEmployee(Session session, long id) {
        Employee employee = session.get(Employee.class, id);
        if (employee == null) {
            throw new IllegalArgumentException("Employee was not found.");
        }
        return employee;
    }
}
