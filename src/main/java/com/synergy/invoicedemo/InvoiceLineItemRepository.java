package com.synergy.invoicedemo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, Long> {
    List<InvoiceLineItem> findByInvoiceId(Long invoiceId);
    void deleteByInvoiceId(Long invoiceId);
}
