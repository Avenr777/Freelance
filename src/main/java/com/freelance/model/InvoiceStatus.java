package com.freelance.model;

import java.time.LocalDate;

public enum InvoiceStatus {
    DRAFT("Draft"),
    SENT("Sent"),
    PAID("Paid"),
    OVERDUE("Overdue");

    private final String displayName;

    InvoiceStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Determines effective status based on base status and due date.
     * OVERDUE is derived when status is SENT and due_date < today.
     */
    public static InvoiceStatus resolveEffectiveStatus(InvoiceStatus storedStatus, LocalDate dueDate) {
        if (storedStatus == SENT && dueDate != null && dueDate.isBefore(LocalDate.now())) {
            return OVERDUE;
        }
        return storedStatus;
    }
}
