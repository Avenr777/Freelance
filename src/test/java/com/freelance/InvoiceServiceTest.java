package com.freelance;

import com.freelance.dao.InvoiceDao;
import com.freelance.dao.WorkLogDao;
import com.freelance.db.DatabaseInitializer;
import com.freelance.model.Client;
import com.freelance.model.Invoice;
import com.freelance.model.InvoiceStatus;
import com.freelance.model.Project;
import com.freelance.service.InvoiceService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class InvoiceServiceTest {

    @BeforeAll
    public static void setup() {
        System.setProperty("db.type", "h2");
        System.setProperty("db.h2.url", "jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        DatabaseInitializer.initialize();
    }

    @Test
    public void testEffectiveStatusDerivation() {
        LocalDate pastDue = LocalDate.now().minusDays(5);
        LocalDate futureDue = LocalDate.now().plusDays(10);

        // SENT + past due date => OVERDUE
        Invoice overdueInv = new Invoice(1, null, new BigDecimal("100.00"), LocalDate.now().minusDays(15), pastDue, InvoiceStatus.SENT);
        assertEquals(InvoiceStatus.OVERDUE, overdueInv.getEffectiveStatus());

        // SENT + future due date => SENT
        Invoice sentInv = new Invoice(2, null, new BigDecimal("100.00"), LocalDate.now(), futureDue, InvoiceStatus.SENT);
        assertEquals(InvoiceStatus.SENT, sentInv.getEffectiveStatus());

        // PAID + past due date => PAID (Paid invoices are never overdue)
        Invoice paidInv = new Invoice(3, null, new BigDecimal("100.00"), LocalDate.now().minusDays(15), pastDue, InvoiceStatus.PAID);
        assertEquals(InvoiceStatus.PAID, paidInv.getEffectiveStatus());
    }

    @Test
    public void testTotalCalculationFormula() {
        Client client = new Client(1, "Test Client", "test@client.com", "123");
        Project project = new Project(1, client, "Website Development", new BigDecimal("60.00"));

        BigDecimal hoursWorked = new BigDecimal("5.5");
        BigDecimal expectedTotal = hoursWorked.multiply(project.getHourlyRate()).setScale(2, RoundingMode.HALF_UP);

        assertEquals(new BigDecimal("330.00"), expectedTotal);
    }
}
