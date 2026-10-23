package com.freelance.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Project {
    private int projectId;
    private Client client;
    private String projectName;
    private BigDecimal hourlyRate;
    private BigDecimal budgetAmount;

    public Project() {
    }

    public Project(int projectId, Client client, String projectName, BigDecimal hourlyRate) {
        this(projectId, client, projectName, hourlyRate, BigDecimal.ZERO);
    }

    public Project(int projectId, Client client, String projectName, BigDecimal hourlyRate, BigDecimal budgetAmount) {
        this.projectId = projectId;
        this.client = client;
        this.projectName = projectName;
        this.hourlyRate = hourlyRate;
        this.budgetAmount = budgetAmount == null ? BigDecimal.ZERO : budgetAmount;
    }

    public Project(Client client, String projectName, BigDecimal hourlyRate) {
        this(0, client, projectName, hourlyRate);
    }

    public Project(Client client, String projectName, BigDecimal hourlyRate, BigDecimal budgetAmount) {
        this(0, client, projectName, hourlyRate, budgetAmount);
    }

    public int getProjectId() {
        return projectId;
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public BigDecimal getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(BigDecimal budgetAmount) { this.budgetAmount = budgetAmount == null ? BigDecimal.ZERO : budgetAmount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return projectId == project.projectId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId);
    }

    @Override
    public String toString() {
        return projectName;
    }
}
