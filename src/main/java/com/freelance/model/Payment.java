package com.freelance.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A recorded partial or full payment against an invoice. */
public record Payment(int paymentId, int invoiceId, BigDecimal amount, LocalDate paymentDate, String note) { }
