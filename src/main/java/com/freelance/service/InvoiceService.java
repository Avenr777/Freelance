package com.freelance.service;

import com.freelance.dao.InvoiceDao;
import com.freelance.dao.WorkLogDao;
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

    public InvoiceService() {
        this.workLogDao = new WorkLogDao();
        this.invoiceDao = new InvoiceDao();
    }

    public InvoiceService(WorkLogDao workLogDao, InvoiceDao invoiceDao) {
        this.workLogDao = workLogDao;
        this.invoiceDao = invoiceDao;
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

    public List<WorkLog> getWorkLogsForProject(int projectId) throws SQLException {
        return workLogDao.findByProjectId(projectId);
    }
}
