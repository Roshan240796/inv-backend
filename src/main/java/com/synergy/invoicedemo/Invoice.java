package com.synergy.invoicedemo;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String number;
    private String customer;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDate issuedOn;

    protected Invoice() {
    }

    public Invoice(String number, String customer, BigDecimal amount, String currency, String status, LocalDate issuedOn) {
        this.number = number;
        this.customer = customer;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.issuedOn = issuedOn;
    }

    @PrePersist
    private void assignNumber() {
        if (number == null || number.isBlank()) {
            number = "INV-2026-%03d".formatted(id);
        }
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