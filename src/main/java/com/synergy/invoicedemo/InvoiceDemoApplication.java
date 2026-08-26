package com.synergy.invoicedemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootApplication
public class InvoiceDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(InvoiceDemoApplication.class, args);
	}

	@Bean
	CommandLineRunner seedInvoices(InvoiceRepository invoiceRepository) {
		return args -> {
			if (invoiceRepository.count() == 0) {
				invoiceRepository.save(new Invoice(
					"INV-2026-001", "Quadient Services", new BigDecimal("2450.00"),
					"EUR", "DRAFT", LocalDate.now()
				));
			}
		};
	}

}
