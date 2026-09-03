package com.synergy.invoicedemo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class XmlInvoiceService {
    private static final Set<String> XML_EXTENSIONS = Set.of(".xml");
    private final InvoiceRepository invoiceRepository;
    private final InvoiceAttachmentRepository attachmentRepository;
    private final Path storageDirectory;

    public XmlInvoiceService(InvoiceRepository invoiceRepository,
                             InvoiceAttachmentRepository attachmentRepository,
                             @Value("${app.xml.storage-dir:uploads/xml}") String storageDirectory) {
        this.invoiceRepository = invoiceRepository;
        this.attachmentRepository = attachmentRepository;
        this.storageDirectory = Path.of(storageDirectory).toAbsolutePath().normalize();
    }

    public InvoiceController.InvoiceDetailResponse importInvoice(MultipartFile file) {
        validateFile(file);
        try {
            byte[] content = file.getBytes();
            Document document = parse(content);
            String customer = required(document, "customer", "customerName", "buyerName");
            String amountText = required(document, "amount", "total", "totalAmount", "payableAmount");
            String currency = required(document, "currency", "documentCurrencyCode");
            Invoice invoice = new Invoice(
                optional(document, "invoiceNumber", "number", "id"),
                customer,
                new java.math.BigDecimal(amountText),
                currency,
                "DRAFT",
                parseDate(optional(document, "issuedOn", "invoiceDate", "issueDate"))
            );
            invoice.updateInvoiceInfo(
                optional(document, "customerAddress", "buyerAddress"),
                optional(document, "customerContactEmail", "buyerEmail"),
                optional(document, "customerContactPhone", "buyerPhone"),
                optional(document, "supplier", "supplierName", "sellerName"),
                optional(document, "supplierAddress", "sellerAddress"),
                optional(document, "supplierContactEmail", "sellerEmail"),
                optional(document, "supplierContactPhone", "sellerPhone"),
                parseDate(optional(document, "dueDate", "paymentDueDate")),
                optional(document, "paymentTerms"),
                null,
                null,
                null,
                optional(document, "notes", "note")
            );
            invoice = invoiceRepository.save(invoice);
            Files.createDirectories(storageDirectory);
            String safeName = java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(content)).substring(0, 16) + ".xml";
            Path storedFile = storageDirectory.resolve(safeName).normalize();
            Files.write(storedFile, content);
            attachmentRepository.save(new InvoiceAttachment(invoice, file.getOriginalFilename(), "application/xml", file.getSize(), "Original XML invoice", storedFile.toString()));
            return new InvoiceController.InvoiceDetailResponse(
                invoice.getId(), invoice.getNumber(), invoice.getCustomer(), invoice.getCustomerAddress(), invoice.getCustomerContactEmail(), invoice.getCustomerContactPhone(),
                invoice.getSupplier(), invoice.getSupplierAddress(), invoice.getSupplierContactEmail(), invoice.getSupplierContactPhone(), invoice.getIssuedOn(), invoice.getDueDate(),
                invoice.getSubtotal(), invoice.getDiscountAmount(), invoice.getDiscountPercentage(), invoice.getTaxAmount(), invoice.getTaxPercentage(), invoice.getAmount(), invoice.getCurrency(),
                invoice.getPaymentTerms(), invoice.getStatus(), invoice.getNotes(), java.util.List.of(), java.util.List.of()
            );
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid XML invoice: " + exception.getMessage(), exception);
        } catch (Exception exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Could not store XML invoice", exception);
        }
    }

    private static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "XML file is required");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (XML_EXTENSIONS.stream().noneMatch(name::endsWith)) throw new ResponseStatusException(BAD_REQUEST, "Only .xml files are supported");
    }

    private static Document parse(byte[] content) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(content));
        document.getDocumentElement().normalize();
        return document;
    }

    private static String required(Document document, String... names) {
        String value = optional(document, names);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing required field: " + names[0]);
        return value;
    }

    private static String optional(Document document, String... names) {
        for (String name : names) {
            NodeList nodes = document.getElementsByTagNameNS("*", name);
            if (nodes.getLength() == 0) nodes = document.getElementsByTagName(name);
            if (nodes.getLength() > 0) {
                Node node = nodes.item(0);
                if (node.getTextContent() != null && !node.getTextContent().isBlank()) return node.getTextContent().trim();
            }
        }
        return null;
    }

    private static LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? LocalDate.now() : LocalDate.parse(value.trim());
    }
}
