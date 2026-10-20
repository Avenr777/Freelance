package com.freelance.dao;

import com.freelance.db.DatabaseFactory;
import com.freelance.model.Invoice;
import com.freelance.model.InvoiceStatus;
import com.freelance.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InvoiceDao {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceDao.class);
    private final ProjectDao projectDao = new ProjectDao();

    public Invoice save(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices (project_id, total_amount, issue_date, due_date, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, invoice.getProject().getProjectId());
            stmt.setBigDecimal(2, invoice.getTotalAmount());
            stmt.setDate(3, Date.valueOf(invoice.getIssueDate()));
            stmt.setDate(4, Date.valueOf(invoice.getDueDate()));
            stmt.setString(5, invoice.getStoredStatus().name());
            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    invoice.setInvoiceId(rs.getInt(1));
                }
            }
            logger.info("Saved Invoice with ID: {}", invoice.getInvoiceId());
            return invoice;
        }
    }

    public boolean update(Invoice invoice) throws SQLException {
        String sql = "UPDATE invoices SET project_id = ?, total_amount = ?, issue_date = ?, due_date = ?, status = ? WHERE invoice_id = ?";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, invoice.getProject().getProjectId());
            stmt.setBigDecimal(2, invoice.getTotalAmount());
            stmt.setDate(3, Date.valueOf(invoice.getIssueDate()));
            stmt.setDate(4, Date.valueOf(invoice.getDueDate()));
            stmt.setString(5, invoice.getStoredStatus().name());
            stmt.setInt(6, invoice.getInvoiceId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(int invoiceId, InvoiceStatus newStatus) throws SQLException {
        String sql = "UPDATE invoices SET status = ? WHERE invoice_id = ?";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus.name());
            stmt.setInt(2, invoiceId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int invoiceId) throws SQLException {
        String sql = "DELETE FROM invoices WHERE invoice_id = ?";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, invoiceId);
            return stmt.executeUpdate() > 0;
        }
    }

    public Optional<Invoice> findById(int invoiceId) throws SQLException {
        String sql = "SELECT invoice_id, project_id, total_amount, issue_date, due_date, status FROM invoices WHERE invoice_id = ?";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, invoiceId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int projId = rs.getInt("project_id");
                    Optional<Project> projOpt = projectDao.findById(projId);
                    if (projOpt.isPresent()) {
                        return Optional.of(mapResultSetToInvoice(rs, projOpt.get()));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public List<Invoice> findByProjectId(int projectId) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        Optional<Project> projOpt = projectDao.findById(projectId);
        if (projOpt.isEmpty()) {
            return invoices;
        }
        Project project = projOpt.get();

        String sql = "SELECT invoice_id, total_amount, issue_date, due_date, status FROM invoices " +
                "WHERE project_id = ? ORDER BY issue_date DESC, invoice_id DESC";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToInvoice(rs, project));
                }
            }
        }
        return invoices;
    }

    public List<Invoice> findAll() throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT invoice_id, project_id, total_amount, issue_date, due_date, status FROM invoices ORDER BY issue_date DESC";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                int projId = rs.getInt("project_id");
                Optional<Project> projOpt = projectDao.findById(projId);
                projOpt.ifPresent(project -> {
                    try {
                        invoices.add(mapResultSetToInvoice(rs, project));
                    } catch (SQLException e) {
                        logger.error("Error mapping invoice: {}", e.getMessage());
                    }
                });
            }
        }
        return invoices;
    }

    private Invoice mapResultSetToInvoice(ResultSet rs, Project project) throws SQLException {
        String statusStr = rs.getString("status");
        InvoiceStatus status = InvoiceStatus.DRAFT;
        try {
            status = InvoiceStatus.valueOf(statusStr);
        } catch (Exception ignored) {}

        return new Invoice(
                rs.getInt("invoice_id"),
                project,
                rs.getBigDecimal("total_amount"),
                rs.getDate("issue_date").toLocalDate(),
                rs.getDate("due_date").toLocalDate(),
                status
        );
    }
}
