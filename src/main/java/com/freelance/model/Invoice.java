package com.freelance.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Invoice {
    private int invoiceId;
    private Project project;
    private BigDecimal totalAmount;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private BigDecimal taxRate = BigDecimal.ZERO;
    private DiscountType discountType = DiscountType.NONE;
    private BigDecimal discountValue = BigDecimal.ZERO;
    private String notes;
    private BigDecimal amountPaid = BigDecimal.ZERO;

    public Invoice() {
        this.status = InvoiceStatus.DRAFT;
    }

    public Invoice(int invoiceId, Project project, BigDecimal totalAmount, LocalDate issueDate, LocalDate dueDate, InvoiceStatus status) {
        this.invoiceId = invoiceId;
        this.project = project;
        this.totalAmount = totalAmount;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.status = status != null ? status : InvoiceStatus.DRAFT;
    }

    public Invoice(Project project, BigDecimal totalAmount, LocalDate issueDate, LocalDate dueDate) {
        this(0, project, totalAmount, issueDate, dueDate, InvoiceStatus.DRAFT);
    }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = nonNegative(taxRate); }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType == null ? DiscountType.NONE : discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = nonNegative(discountValue); }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = nonNegative(amountPaid); }

    /** Applies the configured fixed/percentage discount and then the tax rate. */
    public BigDecimal calculateFinalAmount() {
        BigDecimal subtotal = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        BigDecimal discount = discountType == DiscountType.PERCENTAGE
                ? subtotal.multiply(discountValue).movePointLeft(2) : discountValue;
        BigDecimal discounted = subtotal.subtract(discount).max(BigDecimal.ZERO);
        return discounted.multiply(BigDecimal.ONE.add(taxRate.movePointLeft(2))).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal getBalanceDue() { return calculateFinalAmount().subtract(amountPaid).max(BigDecimal.ZERO); }
    public boolean isFullyPaid() { return getBalanceDue().compareTo(BigDecimal.ZERO) == 0; }

    private BigDecimal nonNegative(BigDecimal value) { return value == null || value.signum() < 0 ? BigDecimal.ZERO : value; }

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public InvoiceStatus getStoredStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    /**
     * Returns the derived effective status (returns OVERDUE if SENT & past due date).
     */
    public InvoiceStatus getEffectiveStatus() {
        return InvoiceStatus.resolveEffectiveStatus(status, dueDate);
    }

    public String getFormattedInvoiceNumber() {
        return String.format("#%04d", invoiceId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Invoice invoice = (Invoice) o;
        return invoiceId == invoice.invoiceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(invoiceId);
    }
}
