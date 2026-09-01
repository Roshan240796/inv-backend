package com.synergy.invoicedemo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_line_items")
public class InvoiceLineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(nullable = false)
    private Integer lineNumber;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false)
    private BigDecimal quantity;

    @Column(nullable = false)
    private BigDecimal unitPrice;

    @Column
    private BigDecimal taxPercentage = BigDecimal.ZERO;

    @Column
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    protected InvoiceLineItem() {
    }

    public InvoiceLineItem(
        Invoice invoice,
        Integer lineNumber,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxPercentage,
        BigDecimal discountPercentage
    ) {
        this.invoice = invoice;
        this.lineNumber = lineNumber;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.taxPercentage = taxPercentage != null ? taxPercentage : BigDecimal.ZERO;
        this.discountPercentage = discountPercentage != null ? discountPercentage : BigDecimal.ZERO;
    }

    @PrePersist
    private void ensureLineNumber() {
        if (lineNumber == null || lineNumber <= 0) {
            if (invoice != null && invoice.getLineItems() != null) {
                lineNumber = invoice.getLineItems().size() + 1;
            } else {
                lineNumber = 1;
            }
        }
    }

    public BigDecimal getLineSubtotal() {
        return quantity.multiply(unitPrice);
    }

    public BigDecimal getLineDiscount() {
        if (discountPercentage.compareTo(BigDecimal.ZERO) > 0) {
            return getLineSubtotal()
                .multiply(discountPercentage)
                .divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getLineTax() {
        BigDecimal afterDiscount = getLineSubtotal().subtract(getLineDiscount());
        if (taxPercentage.compareTo(BigDecimal.ZERO) > 0) {
            return afterDiscount
                .multiply(taxPercentage)
                .divide(new BigDecimal(100), 2, java.math.RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getLineTotal() {
        return getLineSubtotal().subtract(getLineDiscount()).add(getLineTax());
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTaxPercentage() {
        return taxPercentage;
    }

    public void setTaxPercentage(BigDecimal taxPercentage) {
        this.taxPercentage = taxPercentage;
    }

    public BigDecimal getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(BigDecimal discountPercentage) {
        this.discountPercentage = discountPercentage;
    }
}
