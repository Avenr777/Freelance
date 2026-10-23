package com.freelance.model;

import java.math.BigDecimal;
import java.nio.file.Path;

/** Result of generating a payment-reminder queue. */
public record ReminderBatch(Path file, int reminderCount, int overdueCount, BigDecimal totalOutstanding) { }
