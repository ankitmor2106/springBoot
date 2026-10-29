package com.example.employeemanager.form;

public class ProjectForm {
    private Long projectId;
    private String projectName = "";
    private String clientName = "";
    private String startDate = "";
    private String budget = "";
    private String maximumTeamSize = "";
    private String status = "PLANNED";

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getMaximumTeamSize() {
        return maximumTeamSize;
    }

    public void setMaximumTeamSize(String maximumTeamSize) {
        this.maximumTeamSize = maximumTeamSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
