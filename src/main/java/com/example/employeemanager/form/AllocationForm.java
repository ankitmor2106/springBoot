package com.example.employeemanager.form;

import java.util.ArrayList;
import java.util.List;

public class AllocationForm {
    private Long employeeId;
    private List<Long> projectIds = new ArrayList<>();

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public List<Long> getProjectIds() {
        return projectIds;
    }

    public void setProjectIds(List<Long> projectIds) {
        this.projectIds = projectIds == null ? new ArrayList<>() : projectIds;
    }
}
