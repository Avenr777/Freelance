package com.freelance.model;

import java.math.BigDecimal;

/** Immutable DTO used by the client-wise revenue report. */
public record ClientRevenue(Client client, BigDecimal invoicedAmount, BigDecimal receivedAmount, BigDecimal outstandingAmount) { }
