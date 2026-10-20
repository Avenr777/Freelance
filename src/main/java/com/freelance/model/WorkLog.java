package com.freelance.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class WorkLog {
    private int logId;
    private Project project;
    private LocalDate workDate;
    private BigDecimal hoursWorked;
    private String description;

    public WorkLog() {
    }

    public WorkLog(int logId, Project project, LocalDate workDate, BigDecimal hoursWorked, String description) {
        this.logId = logId;
        this.project = project;
        this.workDate = workDate;
        this.hoursWorked = hoursWorked;
        this.description = description;
    }

    public WorkLog(Project project, LocalDate workDate, BigDecimal hoursWorked, String description) {
        this(0, project, workDate, hoursWorked, description);
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public BigDecimal getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(BigDecimal hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal calculateAmount() {
        if (project != null && project.getHourlyRate() != null && hoursWorked != null) {
            return hoursWorked.multiply(project.getHourlyRate());
        }
        return BigDecimal.ZERO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkLog workLog = (WorkLog) o;
        return logId == workLog.logId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(logId);
    }
}
