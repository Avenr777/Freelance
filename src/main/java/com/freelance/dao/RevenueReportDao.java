package com.freelance.dao;

import com.freelance.db.DatabaseFactory;
import com.freelance.model.Client;
import com.freelance.model.ClientRevenue;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Aggregate report implemented with a parameter-free JDBC query. */
public class RevenueReportDao {
    public List<ClientRevenue> findClientRevenue() throws SQLException {
        String sql = "SELECT c.client_id, c.name, c.email, c.phone, COALESCE(SUM(i.total_amount),0) invoiced, " +
                "COALESCE(SUM((SELECT SUM(pay.amount) FROM payments pay WHERE pay.invoice_id=i.invoice_id)),0) received " +
                "FROM clients c LEFT JOIN projects p ON p.client_id=c.client_id LEFT JOIN invoices i ON i.project_id=p.project_id " +
                "GROUP BY c.client_id, c.name, c.email, c.phone ORDER BY c.name";
        List<ClientRevenue> rows = new ArrayList<>();
        try (Connection connection = DatabaseFactory.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                BigDecimal invoiced = rs.getBigDecimal("invoiced");
                BigDecimal received = rs.getBigDecimal("received");
                rows.add(new ClientRevenue(new Client(rs.getInt("client_id"), rs.getString("name"), rs.getString("email"), rs.getString("phone")), invoiced, received, invoiced.subtract(received)));
            }
        }
        return rows;
    }
}
