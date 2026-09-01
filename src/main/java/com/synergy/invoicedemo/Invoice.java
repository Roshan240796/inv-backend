package com.synergy.invoicedemo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

    // Customer Information
    @Column(nullable = false)
    private String customer;

    @Column(length = 500)
    private String customerAddress;

    @Column(length = 200)
    private String customerContactEmail;

    @Column(length = 20)
    private String customerContactPhone;

    // Supplier Information
    @Column(length = 200)
    private String supplier;

    @Column(length = 500)
    private String supplierAddress;

    @Column(length = 200)
    private String supplierContactEmail;

    @Column(length = 20)
    private String supplierContactPhone;

    // Invoice Dates
    @Column(nullable = false)
    private LocalDate issuedOn;

    @Column
    private LocalDate dueDate;

    // Amounts
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = true)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @Column
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Column(nullable = false)
    private String currency;

    // Payment Terms
    @Column(length = 200)
    private String paymentTerms;

    // Status
    @Column(nullable = false)
    private String status = "DRAFT";

    // Notes
    @Column(length = 2000)
    private String notes;

    // Line Items and Attachments
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<InvoiceLineItem> lineItems = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<InvoiceAttachment> attachments = new ArrayList<>();

    protected Invoice() {
    }

    public Invoice(String number, String customer, BigDecimal amount, String currency, String status, LocalDate issuedOn) {
        this.number = number;
        this.customer = customer;
        this.subtotal = amount;
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
        recalculateTotal();
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
        this.subtotal = amount;
        this.amount = amount;
        this.currency = normalizeCurrency(currency);
        recalculateTotal();
    }

    public void updateInvoiceInfo(
        String customerAddress,
        String customerContactEmail,
        String customerContactPhone,
        String supplier,
        String supplierAddress,
        String supplierContactEmail,
        String supplierContactPhone,
        LocalDate dueDate,
        String paymentTerms,
        BigDecimal discountAmount,
        BigDecimal discountPercentage,
        BigDecimal taxPercentage,
        String notes
    ) {
        this.customerAddress = customerAddress;
        this.customerContactEmail = customerContactEmail;
        this.customerContactPhone = customerContactPhone;
        this.supplier = supplier;
        this.supplierAddress = supplierAddress;
        this.supplierContactEmail = supplierContactEmail;
        this.supplierContactPhone = supplierContactPhone;
        this.dueDate = dueDate;
        this.paymentTerms = paymentTerms;
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
        this.discountPercentage = discountPercentage != null ? discountPercentage : BigDecimal.ZERO;
        this.taxPercentage = taxPercentage != null ? taxPercentage : BigDecimal.ZERO;
        this.notes = notes;
        recalculateTotal();
    }

    public void recalculateTotal() {
        // Calculate subtotal from line items if available, otherwise use provided subtotal
        if (lineItems != null && !lineItems.isEmpty()) {
            subtotal = lineItems.stream()
                .map(item -> item.getLineTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Apply discount
        BigDecimal afterDiscount = subtotal;
        if (discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discountCalc = subtotal.multiply(discountPercentage).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
            afterDiscount = subtotal.subtract(discountCalc);
            this.discountAmount = discountCalc;
        } else if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
            afterDiscount = subtotal.subtract(discountAmount);
        }

        // Calculate tax
        if (taxPercentage.compareTo(BigDecimal.ZERO) > 0) {
            this.taxAmount = afterDiscount.multiply(taxPercentage).divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
        }

        // Calculate total
        this.amount = afterDiscount.add(taxAmount != null ? taxAmount : BigDecimal.ZERO);
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

    // Getters
    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getCustomer() {
        return customer;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public String getCustomerContactEmail() {
        return customerContactEmail;
    }

    public String getCustomerContactPhone() {
        return customerContactPhone;
    }

    public String getSupplier() {
        return supplier;
    }

    public String getSupplierAddress() {
        return supplierAddress;
    }

    public String getSupplierContactEmail() {
        return supplierContactEmail;
    }

    public String getSupplierContactPhone() {
        return supplierContactPhone;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTaxPercentage() {
        return taxPercentage;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public String getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public List<InvoiceLineItem> getLineItems() {
        return lineItems;
    }

    public List<InvoiceAttachment> getAttachments() {
        return attachments;
    }

    // Setters for collections
    public void setLineItems(List<InvoiceLineItem> lineItems) {
        this.lineItems = lineItems;
    }

    public void setAttachments(List<InvoiceAttachment> attachments) {
        this.attachments = attachments;
    }
}