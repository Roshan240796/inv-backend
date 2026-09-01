package com.synergy.invoicedemo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceAttachmentRepository extends JpaRepository<InvoiceAttachment, Long> {
    List<InvoiceAttachment> findByInvoiceId(Long invoiceId);
    void deleteByInvoiceId(Long invoiceId);
}
