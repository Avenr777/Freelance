package com.freelance.dao;

import com.freelance.db.DatabaseFactory;
import com.freelance.model.Client;
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

    public Invoice save(Invoice invoice) throws SQLException {
        String sql = "INSERT INTO invoices (project_id, total_amount, issue_date, due_date, status, tax_rate, discount_type, discount_value, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, invoice.getProject().getProjectId());
            stmt.setBigDecimal(2, invoice.getTotalAmount());
            stmt.setDate(3, Date.valueOf(invoice.getIssueDate()));
            stmt.setDate(4, Date.valueOf(invoice.getDueDate()));
            stmt.setString(5, invoice.getStoredStatus().name());
            stmt.setBigDecimal(6, invoice.getTaxRate());
            stmt.setString(7, invoice.getDiscountType().name());
            stmt.setBigDecimal(8, invoice.getDiscountValue());
            stmt.setString(9, invoice.getNotes());
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
        String sql = "UPDATE invoices SET project_id = ?, total_amount = ?, issue_date = ?, due_date = ?, status = ?, tax_rate = ?, discount_type = ?, discount_value = ?, notes = ? WHERE invoice_id = ?";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, invoice.getProject().getProjectId());
            stmt.setBigDecimal(2, invoice.getTotalAmount());
            stmt.setDate(3, Date.valueOf(invoice.getIssueDate()));
            stmt.setDate(4, Date.valueOf(invoice.getDueDate()));
            stmt.setString(5, invoice.getStoredStatus().name());
            stmt.setBigDecimal(6, invoice.getTaxRate());
            stmt.setString(7, invoice.getDiscountType().name());
            stmt.setBigDecimal(8, invoice.getDiscountValue());
            stmt.setString(9, invoice.getNotes());
            stmt.setInt(10, invoice.getInvoiceId());
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
        String sql = "SELECT i.invoice_id, i.total_amount, i.issue_date, i.due_date, i.status, i.tax_rate, i.discount_type, i.discount_value, i.notes, COALESCE((SELECT SUM(amount) FROM payments pay WHERE pay.invoice_id = i.invoice_id), 0) AS amount_paid, " +
                "p.project_id, p.client_id, p.project_name, p.hourly_rate, p.budget_amount, " +
                "c.name AS client_name, c.email AS client_email, c.phone AS client_phone " +
                "FROM invoices i JOIN projects p ON i.project_id = p.project_id " +
                "JOIN clients c ON p.client_id = c.client_id WHERE i.invoice_id = ?";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, invoiceId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToInvoice(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Invoice> findByProjectId(int projectId) throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT i.invoice_id, i.total_amount, i.issue_date, i.due_date, i.status, i.tax_rate, i.discount_type, i.discount_value, i.notes, COALESCE((SELECT SUM(amount) FROM payments pay WHERE pay.invoice_id = i.invoice_id), 0) AS amount_paid, " +
                "p.project_id, p.client_id, p.project_name, p.hourly_rate, p.budget_amount, " +
                "c.name AS client_name, c.email AS client_email, c.phone AS client_phone " +
                "FROM invoices i JOIN projects p ON i.project_id = p.project_id " +
                "JOIN clients c ON p.client_id = c.client_id " +
                "WHERE i.project_id = ? ORDER BY i.issue_date DESC, i.invoice_id DESC";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, projectId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    invoices.add(mapResultSetToInvoice(rs));
                }
            }
        }
        return invoices;
    }

    public List<Invoice> findAll() throws SQLException {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT i.invoice_id, i.total_amount, i.issue_date, i.due_date, i.status, i.tax_rate, i.discount_type, i.discount_value, i.notes, COALESCE((SELECT SUM(amount) FROM payments pay WHERE pay.invoice_id = i.invoice_id), 0) AS amount_paid, " +
                "p.project_id, p.client_id, p.project_name, p.hourly_rate, p.budget_amount, " +
                "c.name AS client_name, c.email AS client_email, c.phone AS client_phone " +
                "FROM invoices i JOIN projects p ON i.project_id = p.project_id " +
                "JOIN clients c ON p.client_id = c.client_id " +
                "ORDER BY i.issue_date DESC, i.invoice_id DESC";
        try (Connection conn = DatabaseFactory.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                invoices.add(mapResultSetToInvoice(rs));
            }
        }
        return invoices;
    }

    private Invoice mapResultSetToInvoice(ResultSet rs) throws SQLException {
        String statusStr = rs.getString("status");
        if (statusStr == null) {
            throw new SQLException("Invoice status is null in database");
        }
        InvoiceStatus status;
        try {
            status = InvoiceStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            throw new SQLException("Invalid invoice status in database: " + statusStr, e);
        }

        Client client = new Client(
                rs.getInt("client_id"),
                rs.getString("client_name"),
                rs.getString("client_email"),
                rs.getString("client_phone")
        );
        Project project = new Project(
                rs.getInt("project_id"),
                client,
                rs.getString("project_name"),
                rs.getBigDecimal("hourly_rate"),
                rs.getBigDecimal("budget_amount")
        );
        Invoice invoice = new Invoice(
                rs.getInt("invoice_id"),
                project,
                rs.getBigDecimal("total_amount"),
                rs.getDate("issue_date").toLocalDate(),
                rs.getDate("due_date").toLocalDate(),
                status
        );
        invoice.setTaxRate(rs.getBigDecimal("tax_rate"));
        invoice.setDiscountType(com.freelance.model.DiscountType.valueOf(rs.getString("discount_type")));
        invoice.setDiscountValue(rs.getBigDecimal("discount_value"));
        invoice.setNotes(rs.getString("notes"));
        invoice.setAmountPaid(rs.getBigDecimal("amount_paid"));
        return invoice;
    }
}
