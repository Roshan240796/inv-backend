package com.synergy.invoicedemo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository lineItemRepository;
    private final InvoiceAttachmentRepository attachmentRepository;

    public InvoiceController(
        InvoiceRepository invoiceRepository,
        InvoiceLineItemRepository lineItemRepository,
        InvoiceAttachmentRepository attachmentRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.lineItemRepository = lineItemRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @GetMapping
    public List<InvoiceResponse> listInvoices() {
        return invoiceRepository.findAll().stream()
            .map(InvoiceController::toResponse)
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public InvoiceDetailResponse getInvoice(@PathVariable Long id) {
        Invoice invoice = findInvoice(id);
        return toDetailResponse(invoice);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        Invoice invoice = invoiceRepository.save(new Invoice(
            null,
            request.customer(),
            request.amount(),
            request.currency(),
            "DRAFT",
            LocalDate.now()
        ));
        return toResponse(invoice);
    }

    @PutMapping("/{id}")
    public InvoiceResponse updateInvoice(@PathVariable Long id, @Valid @RequestBody UpdateInvoiceRequest request) {
        Invoice invoice = findInvoice(id);
        invoice.updateDetails(request.customer(), request.amount(), request.currency());
        return toResponse(invoiceRepository.save(invoice));
    }

    @PutMapping("/{id}/info")
    public InvoiceDetailResponse updateInvoiceInfo(
        @PathVariable Long id,
        @Valid @RequestBody UpdateInvoiceInfoRequest request
    ) {
        Invoice invoice = findInvoice(id);
        invoice.updateInvoiceInfo(
            request.customerAddress(),
            request.customerContactEmail(),
            request.customerContactPhone(),
            request.supplier(),
            request.supplierAddress(),
            request.supplierContactEmail(),
            request.supplierContactPhone(),
            request.dueDate(),
            request.paymentTerms(),
            request.discountAmount(),
            request.discountPercentage(),
            request.taxPercentage(),
            request.notes()
        );
        return toDetailResponse(invoiceRepository.save(invoice));
    }

    @PatchMapping("/{id}/status")
    public InvoiceResponse updateStatus(@PathVariable Long id, @RequestBody String status) {
        Invoice invoice = findInvoice(id);
        String normalizedStatus = status == null ? "" : status.replace("\"", "").trim();
        invoice.updateStatus(normalizedStatus);
        return toResponse(invoiceRepository.save(invoice));
    }

    // Line Items Endpoints
    @PostMapping("/{id}/line-items")
    @ResponseStatus(HttpStatus.CREATED)
    public LineItemResponse addLineItem(
        @PathVariable Long id,
        @Valid @RequestBody CreateLineItemRequest request
    ) {
        Invoice invoice = findInvoice(id);
        InvoiceLineItem lineItem = new InvoiceLineItem(
            invoice,
            null,
            request.description(),
            request.quantity(),
            request.unitPrice(),
            request.taxPercentage(),
            request.discountPercentage()
        );
        lineItem = lineItemRepository.save(lineItem);
        invoice.recalculateTotal();
        invoiceRepository.save(invoice);
        return toLineItemResponse(lineItem);
    }

    @GetMapping("/{id}/line-items")
    public List<LineItemResponse> getLineItems(@PathVariable Long id) {
        findInvoice(id); // Verify invoice exists
        return lineItemRepository.findByInvoiceId(id).stream()
            .map(InvoiceController::toLineItemResponse)
            .collect(Collectors.toList());
    }

    @PutMapping("/{id}/line-items/{itemId}")
    public LineItemResponse updateLineItem(
        @PathVariable Long id,
        @PathVariable Long itemId,
        @Valid @RequestBody UpdateLineItemRequest request
    ) {
        findInvoice(id);
        InvoiceLineItem lineItem = lineItemRepository.findById(itemId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Line item not found"));
        
        lineItem.setDescription(request.description());
        lineItem.setQuantity(request.quantity());
        lineItem.setUnitPrice(request.unitPrice());
        lineItem.setTaxPercentage(request.taxPercentage());
        lineItem.setDiscountPercentage(request.discountPercentage());
        
        lineItem = lineItemRepository.save(lineItem);
        lineItem.getInvoice().recalculateTotal();
        invoiceRepository.save(lineItem.getInvoice());
        
        return toLineItemResponse(lineItem);
    }

    @DeleteMapping("/{id}/line-items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLineItem(@PathVariable Long id, @PathVariable Long itemId) {
        findInvoice(id);
        lineItemRepository.deleteById(itemId);
        Invoice invoice = findInvoice(id);
        invoice.recalculateTotal();
        invoiceRepository.save(invoice);
    }

    // Attachments Endpoints
    @PostMapping("/{id}/attachments")
    @ResponseStatus(HttpStatus.CREATED)
    public AttachmentResponse addAttachment(
        @PathVariable Long id,
        @Valid @RequestBody CreateAttachmentRequest request
    ) {
        Invoice invoice = findInvoice(id);
        InvoiceAttachment attachment = new InvoiceAttachment(
            invoice,
            request.fileName(),
            request.fileType(),
            request.fileSize(),
            request.description(),
            request.filePath()
        );
        attachment = attachmentRepository.save(attachment);
        return toAttachmentResponse(attachment);
    }

    @GetMapping("/{id}/attachments")
    public List<AttachmentResponse> getAttachments(@PathVariable Long id) {
        findInvoice(id); // Verify invoice exists
        return attachmentRepository.findByInvoiceId(id).stream()
            .map(InvoiceController::toAttachmentResponse)
            .collect(Collectors.toList());
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttachment(@PathVariable Long id, @PathVariable Long attachmentId) {
        findInvoice(id);
        attachmentRepository.deleteById(attachmentId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvoice(@PathVariable Long id) {
        invoiceRepository.delete(findInvoice(id));
    }

    private Invoice findInvoice(Long id) {
        return invoiceRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found"));
    }

    private static InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
            invoice.getId(), invoice.getNumber(), invoice.getCustomer(), invoice.getAmount(),
            invoice.getCurrency(), invoice.getStatus(), invoice.getIssuedOn()
        );
    }

    private static InvoiceDetailResponse toDetailResponse(Invoice invoice) {
        return new InvoiceDetailResponse(
            invoice.getId(),
            invoice.getNumber(),
            invoice.getCustomer(),
            invoice.getCustomerAddress(),
            invoice.getCustomerContactEmail(),
            invoice.getCustomerContactPhone(),
            invoice.getSupplier(),
            invoice.getSupplierAddress(),
            invoice.getSupplierContactEmail(),
            invoice.getSupplierContactPhone(),
            invoice.getIssuedOn(),
            invoice.getDueDate(),
            invoice.getSubtotal(),
            invoice.getDiscountAmount(),
            invoice.getDiscountPercentage(),
            invoice.getTaxAmount(),
            invoice.getTaxPercentage(),
            invoice.getAmount(),
            invoice.getCurrency(),
            invoice.getPaymentTerms(),
            invoice.getStatus(),
            invoice.getNotes(),
            invoice.getLineItems().stream().map(InvoiceController::toLineItemResponse).collect(Collectors.toList()),
            invoice.getAttachments().stream().map(InvoiceController::toAttachmentResponse).collect(Collectors.toList())
        );
    }

    private static LineItemResponse toLineItemResponse(InvoiceLineItem item) {
        return new LineItemResponse(
            item.getId(),
            item.getLineNumber(),
            item.getDescription(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getTaxPercentage(),
            item.getDiscountPercentage(),
            item.getLineSubtotal(),
            item.getLineDiscount(),
            item.getLineTax(),
            item.getLineTotal()
        );
    }

    private static AttachmentResponse toAttachmentResponse(InvoiceAttachment attachment) {
        return new AttachmentResponse(
            attachment.getId(),
            attachment.getFileName(),
            attachment.getFileType(),
            attachment.getFileSize(),
            attachment.getDescription(),
            attachment.getUploadedAt()
        );
    }

    // DTOs
    public record CreateInvoiceRequest(
        @NotBlank String customer,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency
    ) {}

    public record UpdateInvoiceRequest(
        @NotBlank String customer,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency
    ) {}

    public record UpdateInvoiceInfoRequest(
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
    ) {}

    public record CreateLineItemRequest(
        @NotBlank String description,
        @NotNull @DecimalMin("0.01") BigDecimal quantity,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
        BigDecimal taxPercentage,
        BigDecimal discountPercentage
    ) {}

    public record UpdateLineItemRequest(
        @NotBlank String description,
        @NotNull @DecimalMin("0.01") BigDecimal quantity,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
        BigDecimal taxPercentage,
        BigDecimal discountPercentage
    ) {}

    public record CreateAttachmentRequest(
        @NotBlank String fileName,
        @NotBlank String fileType,
        @NotNull Long fileSize,
        String description,
        String filePath
    ) {}

    public record InvoiceResponse(
        long id,
        String number,
        String customer,
        BigDecimal amount,
        String currency,
        String status,
        LocalDate issuedOn
    ) {}

    public record InvoiceDetailResponse(
        long id,
        String number,
        String customer,
        String customerAddress,
        String customerContactEmail,
        String customerContactPhone,
        String supplier,
        String supplierAddress,
        String supplierContactEmail,
        String supplierContactPhone,
        LocalDate issuedOn,
        LocalDate dueDate,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal discountPercentage,
        BigDecimal taxAmount,
        BigDecimal taxPercentage,
        BigDecimal amount,
        String currency,
        String paymentTerms,
        String status,
        String notes,
        List<LineItemResponse> lineItems,
        List<AttachmentResponse> attachments
    ) {}

    public record LineItemResponse(
        long id,
        int lineNumber,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal taxPercentage,
        BigDecimal discountPercentage,
        BigDecimal lineSubtotal,
        BigDecimal lineDiscount,
        BigDecimal lineTax,
        BigDecimal lineTotal
    ) {}

    public record AttachmentResponse(
        long id,
        String fileName,
        String fileType,
        long fileSize,
        String description,
        java.time.LocalDateTime uploadedAt
    ) {}
}