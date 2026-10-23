package com.freelance.dao;

import com.freelance.db.DatabaseFactory;
import com.freelance.model.Payment;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

/** JDBC repository for partial-payment records. */
public class PaymentDao {
    public Payment save(Payment payment) throws SQLException {
        if (payment.amount() == null || payment.amount().signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        String sql = "INSERT INTO payments (invoice_id, amount, payment_date, note) VALUES (?, ?, ?, ?)";
        try (Connection connection = DatabaseFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, payment.invoiceId());
            statement.setBigDecimal(2, payment.amount());
            statement.setDate(3, Date.valueOf(payment.paymentDate() == null ? LocalDate.now() : payment.paymentDate()));
            statement.setString(4, payment.note());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? new Payment(keys.getInt(1), payment.invoiceId(), payment.amount(), payment.paymentDate(), payment.note()) : payment;
            }
        }
    }

    public BigDecimal totalByInvoiceId(int invoiceId) throws SQLException {
        try (Connection connection = DatabaseFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE invoice_id = ?")) {
            statement.setInt(1, invoiceId);
            try (ResultSet results = statement.executeQuery()) {
                return results.next() ? results.getBigDecimal(1) : BigDecimal.ZERO;
            }
        }
    }
}
