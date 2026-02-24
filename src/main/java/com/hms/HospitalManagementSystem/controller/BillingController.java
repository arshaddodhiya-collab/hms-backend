package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.request.InvoiceRequest;
import com.hms.HospitalManagementSystem.dto.request.PaymentRequest;
import com.hms.HospitalManagementSystem.dto.response.BillingSummaryResponse;
import com.hms.HospitalManagementSystem.dto.response.InvoiceResponse;
import com.hms.HospitalManagementSystem.dto.response.PaymentResponse;
import com.hms.HospitalManagementSystem.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;
    private final com.hms.HospitalManagementSystem.service.ReportService reportService;

    @PostMapping("/invoices")
    @PreAuthorize("hasAuthority('CMP_INVOICE_GENERATE')")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        return new ResponseEntity<>(billingService.createInvoice(request), HttpStatus.CREATED);
    }

    @PostMapping("/payments")
    @PreAuthorize("hasAuthority('CMP_PAYMENT_RECEIPT')")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        return new ResponseEntity<>(billingService.processPayment(request), HttpStatus.CREATED);
    }

    @GetMapping("/summary/{patientId}")
    @PreAuthorize("hasAuthority('CMP_BILLING_SUMMARY')")
    public ResponseEntity<BillingSummaryResponse> getBillingSummary(@PathVariable Long patientId) {
        return ResponseEntity.ok(billingService.getBillingSummary(patientId));
    }

    @GetMapping("/outstanding")
    @PreAuthorize("hasAuthority('CMP_BILLING_SUMMARY')")
    public ResponseEntity<List<InvoiceResponse>> getOutstandingInvoices(@RequestParam Long patientId) {
        return ResponseEntity.ok(billingService.getOutstandingInvoices(patientId));
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('CMP_INVOICE_GENERATE') or hasAuthority('CMP_BILLING_SUMMARY')")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id) {
        return ResponseEntity.ok(billingService.getInvoiceById(id));
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('CMP_BILLING_SUMMARY')")
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        return ResponseEntity.ok(billingService.getAllInvoices());
    }

    @GetMapping("/invoices/{id}/pdf")
    @PreAuthorize("hasAuthority('CMP_INVOICE_GENERATE') or hasAuthority('CMP_BILLING_SUMMARY')")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long id) {
        try {
            byte[] pdf = reportService.generateInvoicePdf(id);
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "invoice_" + id + ".pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
