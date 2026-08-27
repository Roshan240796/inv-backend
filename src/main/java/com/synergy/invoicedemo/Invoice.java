package com.synergy.invoicedemo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "invoices")
public class Invoice {

    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
        "DRAFT", Set.of("SUBMITTED"),
        "SUBMITTED", Set.of("APPROVED", "REJECTED"),
        "APPROVED", Set.of("PAID"),
        "REJECTED", Set.of("DRAFT", "SUBMITTED"),
        "PAID", Set.of()
    );

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    @Column(nullable = false)
    private String customer;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String status = "DRAFT";

    @Column(nullable = false)
    private LocalDate issuedOn;

    protected Invoice() {
    }

    public Invoice(String number, String customer, BigDecimal amount, String currency, String status, LocalDate issuedOn) {
        this.number = number;
        this.customer = customer;
        this.amount = amount;
        this.currency = normalizeCurrency(currency);
        this.status = normalizeStatus(status);
        this.issuedOn = issuedOn;
    }

    @PrePersist
    private void assignNumber() {
        if (number == null || number.isBlank()) {
            number = "INV-%s-%03d".formatted(LocalDate.now().getYear(), Math.abs(System.nanoTime() % 1000));
        }
        if (issuedOn == null) {
            issuedOn = LocalDate.now();
        }
        if (currency != null) {
            currency = normalizeCurrency(currency);
        }
        if (status == null || status.isBlank()) {
            status = "DRAFT";
        } else {
            status = normalizeStatus(status);
        }
    }

    public void updateStatus(String newStatus) {
        String normalizedStatus = normalizeStatus(newStatus);
        Set<String> allowedNextStates = VALID_TRANSITIONS.getOrDefault(status, Set.of());
        if (!allowedNextStates.contains(normalizedStatus)) {
            throw new IllegalArgumentException(
                "Status transition from %s to %s is not allowed".formatted(status, normalizedStatus)
            );
        }
        this.status = normalizedStatus;
    }

    public void updateDetails(String customer, BigDecimal amount, String currency) {
        this.customer = customer;
        this.amount = amount;
        this.currency = normalizeCurrency(currency);
    }

    public static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "DRAFT";
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("DRAFT", "SUBMITTED", "APPROVED", "REJECTED", "PAID").contains(normalized)) {
            throw new IllegalArgumentException("Unsupported invoice status: " + status);
        }
        return normalized;
    }

    public static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }
        return currency.trim().toUpperCase();
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getCustomer() {
        return customer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }
}