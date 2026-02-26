package com.hms.HospitalManagementSystem.service.impl;

import com.hms.HospitalManagementSystem.dto.request.InvoiceRequest;
import com.hms.HospitalManagementSystem.dto.request.PaymentRequest;
import com.hms.HospitalManagementSystem.dto.response.BillingSummaryResponse;
import com.hms.HospitalManagementSystem.dto.response.InvoiceResponse;
import com.hms.HospitalManagementSystem.dto.response.PaymentResponse;
import com.hms.HospitalManagementSystem.entity.*;
import com.hms.HospitalManagementSystem.enums.ChargeStatus;
import com.hms.HospitalManagementSystem.enums.InvoiceStatus;
import com.hms.HospitalManagementSystem.enums.PaymentMethod;
import com.hms.HospitalManagementSystem.enums.PaymentStatus;
import com.hms.HospitalManagementSystem.exception.ResourceNotFoundException;
import com.hms.HospitalManagementSystem.mapper.InvoiceMapper;
import com.hms.HospitalManagementSystem.mapper.PaymentMapper;
import com.hms.HospitalManagementSystem.repository.*;
import com.hms.HospitalManagementSystem.service.BillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingServiceImpl implements BillingService {

    private final InvoiceRepository invoiceRepository;
    private final ChargeRepository chargeRepository;
    private final PatientRepository patientRepository;
    private final AdmissionRepository admissionRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceMapper invoiceMapper;
    private final PaymentMapper paymentMapper;

    @Override
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

        // Set admission if provided
        if (request.getAdmissionId() != null) {
            Admission admission = admissionRepository.findById(request.getAdmissionId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Admission not found with id: " + request.getAdmissionId()));
            invoice.setAdmission(admission);
        }

        // Let's implement basics
        invoice.setTotalAmount(BigDecimal.ZERO);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setTaxAmount(BigDecimal.ZERO);
        invoice.setNetAmount(BigDecimal.ZERO);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setDueAmount(BigDecimal.ZERO);
        // Set status from request or default to ISSUED
        if (request.getStatus() != null) {
            invoice.setStatus(InvoiceStatus.valueOf(request.getStatus()));
        } else {
            invoice.setStatus(InvoiceStatus.ISSUED);
        }
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

    @Override
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

    @Override
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for invoice ID: {}", request.getInvoiceId());

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Invoice not found with id: " + request.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot process payment for CANCELLED invoice");
        }

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice is already fully PAID");
        }

        if (request.getAmount().compareTo(invoice.getDueAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Payment amount " + request.getAmount() + " exceeds due amount " + invoice.getDueAmount());
        }

        // Create Payment
        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(PaymentMethod.valueOf(request.getPaymentMethod()));
        payment.setTransactionReference(request.getTransactionReference());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setNotes(request.getNotes());

        // TODO: Set receivedBy from SecurityContext
        // payment.setReceivedBy(currentUser);

        paymentRepository.save(payment);

        // Update Invoice
        invoice.setPaidAmount(invoice.getPaidAmount().add(request.getAmount()));
        invoice.setDueAmount(invoice.getNetAmount().subtract(invoice.getPaidAmount()));

        if (invoice.getDueAmount().compareTo(BigDecimal.ZERO) == 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        invoiceRepository.save(invoice);

        return paymentMapper.toResponse(payment);
    }

    @Override
    public BillingSummaryResponse getBillingSummary(Long patientId) {
        log.info("Fetching billing summary for patient ID: {}", patientId);

        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with id: " + patientId);
        }

        List<Invoice> allInvoices = invoiceRepository.findByPatientId(patientId);

        BigDecimal totalBilled = allInvoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaid = allInvoices.stream()
                .map(Invoice::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDue = allInvoices.stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.CANCELLED)
                .map(Invoice::getDueAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Invoice> outstandingInvoices = allInvoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.ISSUED
                        || inv.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                .toList();

        // Sort specifically? Currently just taking recent ones from the full list if
        // needed
        List<InvoiceResponse> recentResponses = allInvoices.stream()
                .sorted((i1, i2) -> i2.getIssueDate().compareTo(i1.getIssueDate()))
                .limit(5)
                .map(invoiceMapper::toResponse)
                .toList();

        return BillingSummaryResponse.builder()
                .patientId(patientId)
                .totalBilledAmount(totalBilled)
                .totalPaidAmount(totalPaid)
                .totalDueAmount(totalDue)
                .outstandingInvoiceCount(outstandingInvoices.size())
                .recentInvoices(recentResponses)
                .build();
    }

    @Override
    public List<InvoiceResponse> getOutstandingInvoices(Long patientId) {
        return invoiceRepository
                .findByPatientIdAndStatusIn(patientId, List.of(InvoiceStatus.ISSUED, InvoiceStatus.PARTIALLY_PAID))
                .stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    public InvoiceResponse getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .map(invoiceMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    @Override
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll()
                .stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }
}
