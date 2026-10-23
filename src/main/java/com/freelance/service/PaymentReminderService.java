package com.freelance.service;

import com.freelance.dao.InvoiceDao;
import com.freelance.model.Invoice;
import com.freelance.model.InvoiceStatus;
import com.freelance.model.ReminderBatch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Creates a reminder queue on a background thread so the JavaFX UI remains responsive. */
public class PaymentReminderService {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private final InvoiceDao invoiceDao;

    public PaymentReminderService(InvoiceDao invoiceDao) { this.invoiceDao = invoiceDao; }

    public CompletableFuture<ReminderBatch> createReminderFileAsync(Path outputFile) {
        CompletableFuture<ReminderBatch> result = new CompletableFuture<>();
        Thread reminderThread = new Thread(() -> {
            try {
                List<Invoice> invoices = invoiceDao.findAll().stream()
                        .filter(this::needsReminder)
                        .sorted(Comparator.comparing(Invoice::getDueDate).thenComparing(Invoice::getInvoiceId))
                        .toList();
                BigDecimal totalOutstanding = invoices.stream().map(Invoice::getBalanceDue).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
                int overdueCount = (int) invoices.stream().filter(invoice -> invoice.getDueDate().isBefore(LocalDate.now())).count();

                String reminders = buildReminderFile(invoices, totalOutstanding, overdueCount);
                Files.writeString(outputFile, reminders.toString(), StandardCharsets.UTF_8);
                result.complete(new ReminderBatch(outputFile, invoices.size(), overdueCount, totalOutstanding));
            } catch (Exception exception) {
                result.completeExceptionally(new IllegalStateException("Could not create payment reminder file", exception));
            }
        }, "payment-reminder-generator");
        reminderThread.setDaemon(true);
        reminderThread.start();
        return result;
    }

    /** Creates a timestamped queue in the working directory to prevent accidental overwrites. */
    public CompletableFuture<ReminderBatch> createReminderFileAsync() {
        Path output = Path.of("payment-reminders-" + FILE_TIMESTAMP.format(LocalDateTime.now()) + ".txt").toAbsolutePath();
        return createReminderFileAsync(output);
    }

    private boolean needsReminder(Invoice invoice) {
        InvoiceStatus status = invoice.getEffectiveStatus();
        return (status == InvoiceStatus.SENT || status == InvoiceStatus.OVERDUE) && invoice.getBalanceDue().signum() > 0;
    }

    private String buildReminderFile(List<Invoice> invoices, BigDecimal totalOutstanding, int overdueCount) {
        String line = "=".repeat(72);
        StringBuilder result = new StringBuilder()
                .append(line).append(System.lineSeparator())
                .append("PAYMENT REMINDER QUEUE").append(System.lineSeparator())
                .append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM uuuu, HH:mm"))).append(System.lineSeparator())
                .append("Reminders: ").append(invoices.size()).append(" | Overdue: ").append(overdueCount)
                .append(" | Total outstanding: INR ").append(totalOutstanding).append(System.lineSeparator())
                .append(line).append(System.lineSeparator()).append(System.lineSeparator());

        if (invoices.isEmpty()) {
            return result.append("No sent or overdue invoices currently require a payment reminder.").append(System.lineSeparator()).toString();
        }

        for (Invoice invoice : invoices) {
            String clientName = invoice.getProject().getClient().getName();
            String email = invoice.getProject().getClient().getEmail();
            result.append("TO: ").append(clientName);
            if (email != null && !email.isBlank()) result.append(" <").append(email.trim()).append(">");
            result.append(System.lineSeparator())
                    .append("SUBJECT: Payment reminder for invoice ").append(invoice.getFormattedInvoiceNumber()).append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append("Hello ").append(clientName).append(',').append(System.lineSeparator()).append(System.lineSeparator())
                    .append("This is a friendly reminder that invoice ").append(invoice.getFormattedInvoiceNumber())
                    .append(" for project \"").append(invoice.getProject().getProjectName()).append("\" has an outstanding balance of INR ")
                    .append(invoice.getBalanceDue().setScale(2, RoundingMode.HALF_UP)).append(".").append(System.lineSeparator())
                    .append("Due date: ").append(invoice.getDueDate()).append(" (").append(dueLabel(invoice.getDueDate())).append(").").append(System.lineSeparator())
                    .append("Amount paid: INR ").append(invoice.getAmountPaid().setScale(2, RoundingMode.HALF_UP)).append(System.lineSeparator())
                    .append(System.lineSeparator()).append("Please arrange payment at your earliest convenience. Thank you.")
                    .append(System.lineSeparator()).append(System.lineSeparator()).append(line).append(System.lineSeparator()).append(System.lineSeparator());
        }
        return result.toString();
    }

    private String dueLabel(LocalDate dueDate) {
        long days = Duration.between(dueDate.atStartOfDay(), LocalDate.now().atStartOfDay()).toDays();
        if (days > 0) return days + (days == 1 ? " day overdue" : " days overdue");
        if (days == 0) return "due today";
        days = Math.abs(days);
        return "due in " + days + (days == 1 ? " day" : " days");
    }
}
