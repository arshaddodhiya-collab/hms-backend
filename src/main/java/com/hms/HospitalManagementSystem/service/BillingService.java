package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.request.InvoiceRequest;
import com.hms.HospitalManagementSystem.dto.response.InvoiceResponse;
import com.hms.HospitalManagementSystem.entity.*;
import com.hms.HospitalManagementSystem.enums.ChargeStatus;
import com.hms.HospitalManagementSystem.enums.InvoiceStatus;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.mapper.InvoiceMapper;
import com.hms.HospitalManagementSystem.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final ChargeRepository chargeRepository;
    private final PatientRepository patientRepository;
    private final InvoiceMapper invoiceMapper;

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        log.info("Creating invoice for patient: {}", request.getPatientId());

        // 1. Create Invoice Header
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-" + System.currentTimeMillis()); // Simple generation logic for now

        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Patient not found with id: " + request.getPatientId()));
        invoice.setPatient(patient);

        // Let's implement basics
        invoice.setTotalAmount(BigDecimal.ZERO);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setNetAmount(BigDecimal.ZERO);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setDueAmount(BigDecimal.ZERO);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setIssueDate(LocalDateTime.now());

        // 2. Process Unbilled Charges (if any)
        if (request.getChargeIds() != null && !request.getChargeIds().isEmpty()) {
            List<Charge> charges = chargeRepository.findAllById(request.getChargeIds());
            for (Charge charge : charges) {
                if (charge.getStatus() != ChargeStatus.UNBILLED) {
                    throw new IllegalStateException("Charge " + charge.getId() + " is already billed or cancelled");
                }

                // Convert Charge to InvoiceItem (or link logic)
                InvoiceItem item = new InvoiceItem();
                item.setInvoice(invoice);
                item.setCharge(charge);
                item.setItemDescription(charge.getItemName());
                item.setUnitPrice(charge.getAmount());
                item.setQuantity(charge.getQuantity());
                item.setTotalPrice(charge.getAmount().multiply(BigDecimal.valueOf(charge.getQuantity())));

                invoice.getItems().add(item);

                // Update charge status
                charge.setStatus(ChargeStatus.BILLED);
                charge.setInvoice(invoice);
            }
        }

        // 3. Process Manual Items
        if (request.getItems() != null) {
            request.getItems().forEach(itemRequest -> {
                InvoiceItem item = new InvoiceItem();
                item.setInvoice(invoice);
                item.setItemDescription(itemRequest.getDescription());
                item.setUnitPrice(itemRequest.getUnitPrice());
                item.setQuantity(itemRequest.getQuantity());
                item.setTotalPrice(itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));

                invoice.getItems().add(item);
            });
        }

        // 4. Calculate Totals
        BigDecimal total = invoice.getItems().stream()
                .map(InvoiceItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        invoice.setTotalAmount(total);
        invoice.setNetAmount(total.subtract(invoice.getDiscountAmount()).add(invoice.getTaxAmount()));
        invoice.setDueAmount(invoice.getNetAmount().subtract(invoice.getPaidAmount()));

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return invoiceMapper.toResponse(savedInvoice);
    }

    @Transactional
    public void generateBill(Admission admission) {
        log.info("Generating bill for admission ID: {}", admission.getId());

        // Logic to finalize bill for admission
        // 1. Identify all unbilled charges for this patient
        List<Charge> unbilledCharges = chargeRepository.findByPatientIdAndStatus(admission.getPatient().getId(),
                ChargeStatus.UNBILLED);

        if (unbilledCharges.isEmpty()) {
            log.info("No unbilled charges found for admission {}", admission.getId());
            return;
        }

        // 2. Create Invoice request or directly create invoice
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-" + System.currentTimeMillis());
        invoice.setPatient(admission.getPatient());
        invoice.setAdmission(admission);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setIssueDate(LocalDateTime.now());

        // Initialize totals
        BigDecimal total = BigDecimal.ZERO;

        for (Charge charge : unbilledCharges) {
            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setCharge(charge);
            item.setItemDescription(charge.getItemName());
            item.setUnitPrice(charge.getAmount());
            item.setQuantity(charge.getQuantity());
            item.setTotalPrice(charge.getAmount().multiply(BigDecimal.valueOf(charge.getQuantity())));

            invoice.getItems().add(item);

            total = total.add(item.getTotalPrice());

            // Update charge
            charge.setStatus(ChargeStatus.BILLED);
            charge.setInvoice(invoice);
        }

        invoice.setTotalAmount(total);
        invoice.setNetAmount(total); // Add tax/discount logic if needed
        invoice.setDueAmount(total);

        invoiceRepository.save(invoice);
        log.info("Invoice generated for admission {}", admission.getId());
    }
}
