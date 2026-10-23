package com.freelance.dao;

import com.freelance.db.DatabaseFactory;
import com.freelance.model.Client;
import com.freelance.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectDao {
    private static final Logger logger = LoggerFactory.getLogger(ProjectDao.class);
    private final ClientDao clientDao = new ClientDao();

    public Project save(Project project) throws SQLException {
        String sql = "INSERT INTO projects (client_id, project_name, hourly_rate, budget_amount) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, project.getClient().getClientId());
            stmt.setString(2, project.getProjectName());
            stmt.setBigDecimal(3, project.getHourlyRate());
            stmt.setBigDecimal(4, project.getBudgetAmount());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    project.setProjectId(rs.getInt(1));
                }
            }
            logger.info("Saved Project with ID: {}", project.getProjectId());
            return project;
        }
    }

    public boolean update(Project project) throws SQLException {
        String sql = "UPDATE projects SET client_id = ?, project_name = ?, hourly_rate = ?, budget_amount = ? WHERE project_id = ?";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, project.getClient().getClientId());
            stmt.setString(2, project.getProjectName());
            stmt.setBigDecimal(3, project.getHourlyRate());
            stmt.setBigDecimal(4, project.getBudgetAmount());
            stmt.setInt(5, project.getProjectId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int projectId) throws SQLException {
        String sql = "DELETE FROM projects WHERE project_id = ?";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            return stmt.executeUpdate() > 0;
        }
    }

    public Optional<Project> findById(int projectId) throws SQLException {
        String sql = "SELECT p.project_id, p.client_id, p.project_name, p.hourly_rate, p.budget_amount, " +
                "c.name as client_name, c.email as client_email, c.phone as client_phone " +
                "FROM projects p JOIN clients c ON p.client_id = c.client_id WHERE p.project_id = ?";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProject(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Project> findByClientId(int clientId) throws SQLException {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT p.project_id, p.client_id, p.project_name, p.hourly_rate, p.budget_amount, " +
                "c.name as client_name, c.email as client_email, c.phone as client_phone " +
                "FROM projects p JOIN clients c ON p.client_id = c.client_id " +
                "WHERE p.client_id = ? ORDER BY p.project_name ASC";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, clientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    projects.add(mapResultSetToProject(rs));
                }
            }
        }
        return projects;
    }

    public List<Project> findAll() throws SQLException {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT p.project_id, p.client_id, p.project_name, p.hourly_rate, p.budget_amount, " +
                "c.name as client_name, c.email as client_email, c.phone as client_phone " +
                "FROM projects p JOIN clients c ON p.client_id = c.client_id ORDER BY p.project_name ASC";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                projects.add(mapResultSetToProject(rs));
            }
        }
        return projects;
    }

    private Project mapResultSetToProject(ResultSet rs) throws SQLException {
        Client client = new Client(
                rs.getInt("client_id"),
                rs.getString("client_name"),
                rs.getString("client_email"),
                rs.getString("client_phone")
        );
        return new Project(
                rs.getInt("project_id"),
                client,
                rs.getString("project_name"),
                rs.getBigDecimal("hourly_rate"),
                rs.getBigDecimal("budget_amount")
        );
    }
}
