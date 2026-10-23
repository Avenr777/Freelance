package com.freelance;

import com.freelance.dao.ClientDao;
import com.freelance.dao.InvoiceDao;
import com.freelance.dao.ProjectDao;
import com.freelance.dao.WorkLogDao;
import com.freelance.db.DatabaseInitializer;
import com.freelance.model.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DaoTest {

    @BeforeAll
    public static void setup() throws Exception {
        System.setProperty("db.type", "h2");
        System.setProperty("db.h2.url", "jdbc:h2:mem:daotestdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        DatabaseInitializer.initialize();
    }

    @Test
    public void testFullCrudCycle() throws Exception {
        ClientDao clientDao = new ClientDao();
        ProjectDao projectDao = new ProjectDao();
        WorkLogDao workLogDao = new WorkLogDao();
        InvoiceDao invoiceDao = new InvoiceDao();

        // 1. Create Client
        Client client = new Client("Acme Corp", "billing@acme.com", "+91 98765 43210");
        client = clientDao.save(client);
        assertTrue(client.getClientId() > 0);

        // 2. Create Project
        Project project = new Project(client, "Mobile App", new BigDecimal("75.00"));
        project = projectDao.save(project);
        assertTrue(project.getProjectId() > 0);

        // 3. Log Work Hours
        WorkLog log1 = new WorkLog(project, LocalDate.now().minusDays(2), new BigDecimal("4.0"), "UI Design");
        WorkLog log2 = new WorkLog(project, LocalDate.now().minusDays(1), new BigDecimal("2.5"), "API Integration");
        workLogDao.save(log1);
        workLogDao.save(log2);

        List<WorkLog> logs = workLogDao.findByProjectId(project.getProjectId());
        assertEquals(2, logs.size());
        assertEquals(project.getProjectId(), workLogDao.findById(log1.getLogId()).orElseThrow().getProject().getProjectId());

        BigDecimal totalHours = workLogDao.sumHoursByProjectId(project.getProjectId());
        assertEquals(new BigDecimal("6.50"), totalHours.setScale(2));

        // 4. Create Invoice
        BigDecimal totalAmount = totalHours.multiply(project.getHourlyRate());
        Invoice invoice = new Invoice(project, totalAmount, LocalDate.now(), LocalDate.now().plusDays(14));
        invoice = invoiceDao.save(invoice);
        assertTrue(invoice.getInvoiceId() > 0);

        Optional<Invoice> retrieved = invoiceDao.findById(invoice.getInvoiceId());
        assertTrue(retrieved.isPresent());
        assertEquals(new BigDecimal("487.50"), retrieved.get().getTotalAmount().setScale(2));
        assertEquals(1, invoiceDao.findByProjectId(project.getProjectId()).size());
        assertEquals(1, invoiceDao.findAll().size());

        // 5. Update status
        boolean updated = invoiceDao.updateStatus(invoice.getInvoiceId(), InvoiceStatus.PAID);
        assertTrue(updated);
        Optional<Invoice> paidInv = invoiceDao.findById(invoice.getInvoiceId());
        assertTrue(paidInv.isPresent());
        assertEquals(InvoiceStatus.PAID, paidInv.get().getStoredStatus());
    }
}
