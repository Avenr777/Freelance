package com.freelance;

import com.freelance.dao.InvoiceDao;
import com.freelance.model.Client;
import com.freelance.model.Invoice;
import com.freelance.model.InvoiceStatus;
import com.freelance.model.Project;
import com.freelance.model.ReminderBatch;
import com.freelance.service.PaymentReminderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentReminderServiceTest {
    @Test
    void createsReadableRemindersOnlyForOutstandingSentInvoices() throws Exception {
        Client client = new Client(1, "Acme Corp", "billing@acme.example", "123");
        Project project = new Project(1, client, "Website", new BigDecimal("100"));
        Invoice overdue = new Invoice(10, project, new BigDecimal("200"), LocalDate.now().minusDays(20), LocalDate.now().minusDays(2), InvoiceStatus.SENT);
        overdue.setAmountPaid(new BigDecimal("50"));
        Invoice draft = new Invoice(11, project, new BigDecimal("300"), LocalDate.now(), LocalDate.now().plusDays(10), InvoiceStatus.DRAFT);

        InvoiceDao invoiceDao = new InvoiceDao() {
            @Override
            public List<Invoice> findAll() {
                return List.of(draft, overdue);
            }
        };
        Path destination = Files.createTempFile("reminders-", ".txt");
        ReminderBatch batch = new PaymentReminderService(invoiceDao).createReminderFileAsync(destination).get();
        String content = Files.readString(destination);

        assertEquals(1, batch.reminderCount());
        assertEquals(1, batch.overdueCount());
        assertEquals(new BigDecimal("150.00"), batch.totalOutstanding());
        assertTrue(content.contains("billing@acme.example"));
        assertTrue(content.contains("#0010"));
        assertTrue(content.contains("150.00"));
        assertTrue(!content.contains("#0011"));
    }
}
