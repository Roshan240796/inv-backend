package com.synergy.invoicedemo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;

    public InvoiceController(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping
    public List<InvoiceResponse> listInvoices() {
        return invoiceRepository.findAll().stream()
            .map(InvoiceController::toResponse)
            .collect(Collectors.toList());
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

    private static InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
            invoice.getId(), invoice.getNumber(), invoice.getCustomer(), invoice.getAmount(),
            invoice.getCurrency(), invoice.getStatus(), invoice.getIssuedOn()
        );
    }

    public record CreateInvoiceRequest(
        @NotBlank String customer,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String currency
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
}