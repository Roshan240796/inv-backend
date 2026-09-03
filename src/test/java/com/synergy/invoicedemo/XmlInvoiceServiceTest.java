package com.synergy.invoicedemo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class XmlInvoiceServiceTest {
    @TempDir
    Path storage;

    @Test
    void importsAndStoresValidXmlInvoice() {
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        InvoiceAttachmentRepository attachments = mock(InvoiceAttachmentRepository.class);
        Invoice saved = new Invoice("XML-1", "XML Customer", new java.math.BigDecimal("125.50"), "EUR", "DRAFT", java.time.LocalDate.of(2026, 9, 3));
        try {
            var id = Invoice.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(saved, 1L);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
        when(invoices.save(any(Invoice.class))).thenReturn(saved);
        XmlInvoiceService service = new XmlInvoiceService(invoices, attachments, storage.toString());
        MockMultipartFile file = new MockMultipartFile("file", "invoice.xml", "application/xml", "<Invoice><invoiceNumber>XML-1</invoiceNumber><customer>XML Customer</customer><amount>125.50</amount><currency>EUR</currency></Invoice>".getBytes());

        InvoiceController.InvoiceDetailResponse result = service.importInvoice(file);

        assertEquals("XML Customer", result.customer());
        assertEquals("EUR", result.currency());
        org.junit.jupiter.api.Assertions.assertTrue(storage.toFile().listFiles().length == 1);
    }

    @Test
    void rejectsXmlWithDoctype() {
        XmlInvoiceService service = new XmlInvoiceService(mock(InvoiceRepository.class), mock(InvoiceAttachmentRepository.class), storage.toString());
        MockMultipartFile file = new MockMultipartFile("file", "invoice.xml", "application/xml", "<!DOCTYPE Invoice [<!ENTITY xxe SYSTEM 'file:///etc/passwd'>]><Invoice><customer>&xxe;</customer></Invoice>".getBytes());

        assertThrows(Exception.class, () -> service.importInvoice(file));
    }
}
