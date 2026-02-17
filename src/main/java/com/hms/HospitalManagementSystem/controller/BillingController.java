package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.request.InvoiceRequest;
import com.hms.HospitalManagementSystem.dto.response.InvoiceResponse;
import com.hms.HospitalManagementSystem.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/invoices")
    @PreAuthorize("hasAuthority('CMP_INVOICE_GENERATE')")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceRequest request) {
        return new ResponseEntity<>(billingService.createInvoice(request), HttpStatus.CREATED);
    }
}
