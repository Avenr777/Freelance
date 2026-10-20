package com.freelance.dao;

import com.freelance.db.DatabaseFactory;
import com.freelance.model.WorkLog;
import com.freelance.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WorkLogDao {
    private static final Logger logger = LoggerFactory.getLogger(WorkLogDao.class);
    private final ProjectDao projectDao = new ProjectDao();

    public WorkLog save(WorkLog log) throws SQLException {
        String sql = "INSERT INTO work_logs (project_id, work_date, hours_worked, description) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, log.getProject().getProjectId());
            stmt.setDate(2, Date.valueOf(log.getWorkDate()));
            stmt.setBigDecimal(3, log.getHoursWorked());
            stmt.setString(4, log.getDescription());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setLogId(rs.getInt(1));
                }
            }
            logger.info("Saved WorkLog with ID: {}", log.getLogId());
            return log;
        }
    }

    public boolean update(WorkLog log) throws SQLException {
        String sql = "UPDATE work_logs SET project_id = ?, work_date = ?, hours_worked = ?, description = ? WHERE log_id = ?";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, log.getProject().getProjectId());
            stmt.setDate(2, Date.valueOf(log.getWorkDate()));
            stmt.setBigDecimal(3, log.getHoursWorked());
            stmt.setString(4, log.getDescription());
            stmt.setInt(5, log.getLogId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int logId) throws SQLException {
        String sql = "DELETE FROM work_logs WHERE log_id = ?";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, logId);
            return stmt.executeUpdate() > 0;
        }
    }

    public Optional<WorkLog> findById(int logId) throws SQLException {
        String sql = "SELECT log_id, project_id, work_date, hours_worked, description FROM work_logs WHERE log_id = ?";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, logId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int projId = rs.getInt("project_id");
                    Optional<Project> projOpt = projectDao.findById(projId);
                    if (projOpt.isPresent()) {
                        WorkLog log = new WorkLog(
                                rs.getInt("log_id"),
                                projOpt.get(),
                                rs.getDate("work_date").toLocalDate(),
                                rs.getBigDecimal("hours_worked"),
                                rs.getString("description")
                        );
                        return Optional.of(log);
                    }
                }
            }
        }
        return Optional.empty();
    }

    public List<WorkLog> findByProjectId(int projectId) throws SQLException {
        List<WorkLog> logs = new ArrayList<>();
        Optional<Project> projOpt = projectDao.findById(projectId);
        if (projOpt.isEmpty()) {
            return logs;
        }
        Project project = projOpt.get();

        String sql = "SELECT log_id, work_date, hours_worked, description FROM work_logs " +
                "WHERE project_id = ? ORDER BY work_date DESC, log_id DESC";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(new WorkLog(
                            rs.getInt("log_id"),
                            project,
                            rs.getDate("work_date").toLocalDate(),
                            rs.getBigDecimal("hours_worked"),
                            rs.getString("description")
                    ));
                }
            }
        }
        return logs;
    }

    public BigDecimal sumHoursByProjectId(int projectId) throws SQLException {
        String sql = "SELECT SUM(hours_worked) FROM work_logs WHERE project_id = ?";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal sum = rs.getBigDecimal(1);
                    return sum != null ? sum : BigDecimal.ZERO;
                }
            }
        }
        return BigDecimal.ZERO;
    }
}
