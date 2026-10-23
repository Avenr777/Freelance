package com.freelance.service;

import com.freelance.dao.InvoiceDao;
import com.freelance.dao.WorkLogDao;
import com.freelance.dao.PaymentDao;
import com.freelance.model.Payment;
import com.freelance.model.Invoice;
import com.freelance.model.InvoiceStatus;
import com.freelance.model.Project;
import com.freelance.model.WorkLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class InvoiceService {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);
    private final WorkLogDao workLogDao;
    private final InvoiceDao invoiceDao;
    private final PaymentDao paymentDao;

    public InvoiceService() {
        this.workLogDao = new WorkLogDao();
        this.invoiceDao = new InvoiceDao();
        this.paymentDao = new PaymentDao();
    }

    public InvoiceService(WorkLogDao workLogDao, InvoiceDao invoiceDao) {
        this.workLogDao = workLogDao;
        this.invoiceDao = invoiceDao;
        this.paymentDao = new PaymentDao();
    }

    /**
     * Auto-calculates invoice total from Total Logged Hours x Hourly Rate.
     */
    public BigDecimal calculateProjectTotal(Project project) throws SQLException {
        if (project == null || project.getHourlyRate() == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal totalHours = workLogDao.sumHoursByProjectId(project.getProjectId());
        return totalHours.multiply(project.getHourlyRate()).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generates a new invoice for the given project with issue date today and due date in N days.
     */
    public Invoice generateInvoice(Project project, int dueInDays) throws SQLException {
        BigDecimal totalAmount = calculateProjectTotal(project);
        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(dueInDays > 0 ? dueInDays : 14);

        Invoice invoice = new Invoice(project, totalAmount, issueDate, dueDate);
        return invoiceDao.save(invoice);
    }

    public boolean markAsSent(int invoiceId) throws SQLException {
        return invoiceDao.updateStatus(invoiceId, InvoiceStatus.SENT);
    }

    public boolean markAsPaid(int invoiceId) throws SQLException {
        return invoiceDao.updateStatus(invoiceId, InvoiceStatus.PAID);
    }

    /** Records a partial payment and marks the invoice paid only when its balance reaches zero. */
    public Payment recordPayment(int invoiceId, BigDecimal amount, String note) throws SQLException {
        Invoice invoice = invoiceDao.findById(invoiceId).orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + invoiceId));
        if (amount == null || amount.signum() <= 0 || amount.compareTo(invoice.getBalanceDue()) > 0) {
            throw new IllegalArgumentException("Payment must be greater than zero and no more than the balance due of " + invoice.getBalanceDue());
        }
        Payment saved = paymentDao.save(new Payment(0, invoiceId, amount, LocalDate.now(), note));
        if (amount.compareTo(invoice.getBalanceDue()) == 0) invoiceDao.updateStatus(invoiceId, InvoiceStatus.PAID);
        return saved;
    }

    public List<WorkLog> getWorkLogsForProject(int projectId) throws SQLException {
        return workLogDao.findByProjectId(projectId);
    }
}
