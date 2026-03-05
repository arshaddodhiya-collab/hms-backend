package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.InvoiceRequest;
import com.hms.HospitalManagementSystem.dto.request.PaymentRequest;
import com.hms.HospitalManagementSystem.dto.response.BillingSummaryResponse;
import com.hms.HospitalManagementSystem.dto.response.InvoiceResponse;
import com.hms.HospitalManagementSystem.dto.response.PaymentResponse;
import com.hms.HospitalManagementSystem.entity.Admission;

import java.util.List;

public interface BillingService {
    InvoiceResponse createInvoice(InvoiceRequest request);

    void generateBill(Admission admission);

    PaymentResponse processPayment(PaymentRequest request);

    BillingSummaryResponse getBillingSummary(Long patientId);

    org.springframework.data.domain.Slice<InvoiceResponse> getOutstandingInvoices(Long patientId,
            org.springframework.data.domain.Pageable pageable);

    InvoiceResponse getInvoiceById(Long id);

    org.springframework.data.domain.Slice<InvoiceResponse> getAllInvoices(
            org.springframework.data.domain.Pageable pageable);
}
