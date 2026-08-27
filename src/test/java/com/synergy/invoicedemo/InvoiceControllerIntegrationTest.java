package com.synergy.invoicedemo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvoiceControllerIntegrationTest {

    @Test
    void shouldAllowSupportedLifecycleTransitions() {
        Invoice invoice = new Invoice(null, "Acme Corp", new BigDecimal("2500.00"), "USD", "DRAFT", LocalDate.now());

        invoice.updateStatus("SUBMITTED");
        assertEquals("SUBMITTED", invoice.getStatus());

        invoice.updateStatus("APPROVED");
        assertEquals("APPROVED", invoice.getStatus());

        invoice.updateStatus("PAID");
        assertEquals("PAID", invoice.getStatus());
    }

    @Test
    void shouldRejectUnsupportedStatusTransitions() {
        Invoice invoice = new Invoice(null, "Acme Corp", new BigDecimal("2500.00"), "USD", "DRAFT", LocalDate.now());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> invoice.updateStatus("PAID"));

        assertEquals("Status transition from DRAFT to PAID is not allowed", exception.getMessage());
    }
}
