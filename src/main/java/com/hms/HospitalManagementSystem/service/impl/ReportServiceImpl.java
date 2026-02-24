package com.hms.HospitalManagementSystem.service.impl;

import com.hms.HospitalManagementSystem.dto.response.InvoiceItemResponse;
import com.hms.HospitalManagementSystem.dto.response.InvoiceResponse;
import com.hms.HospitalManagementSystem.service.BillingService;
import com.hms.HospitalManagementSystem.service.LabService;
import com.hms.HospitalManagementSystem.service.ReportService;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private BillingService billingService;

    @Autowired
    private LabService labService;

    @Override
    public byte[] generateInvoicePdf(Long invoiceId) throws Exception {
        InvoiceResponse invoice = billingService.getInvoiceById(invoiceId);

        // Load JRXML
        InputStream jrxmlInput = new ClassPathResource("reports/invoice_report.jrxml").getInputStream();
        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlInput);

        // Set parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hospitalName", "HMS General Hospital");
        parameters.put("patientName", invoice.getPatientName());
        parameters.put("invoiceNumber", invoice.getInvoiceNumber());
        parameters.put("issueDate", invoice.getIssueDate() != null ? invoice.getIssueDate().toString() : "");
        parameters.put("totalAmount", invoice.getTotalAmount() != null ? invoice.getTotalAmount().toString() : "0.00");
        parameters.put("discountAmount",
                invoice.getDiscountAmount() != null ? invoice.getDiscountAmount().toString() : "0.00");
        parameters.put("taxAmount", invoice.getTaxAmount() != null ? invoice.getTaxAmount().toString() : "0.00");
        parameters.put("netAmount", invoice.getNetAmount() != null ? invoice.getNetAmount().toString() : "0.00");
        parameters.put("paidAmount", invoice.getPaidAmount() != null ? invoice.getPaidAmount().toString() : "0.00");
        parameters.put("dueAmount", invoice.getDueAmount() != null ? invoice.getDueAmount().toString() : "0.00");

        // Set list data
        List<InvoiceItemResponse> items = invoice.getItems();
        if (items == null) {
            items = java.util.Collections.emptyList();
        }
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(items);

        // Generate report in memory
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    @Override
    public byte[] generateLabReportPdf(Long labRequestId) throws Exception {
        // We need a response object that contains results. LabController returns
        // mapToResponse,
        // we might need to recreate mapping or just use entity. For simplicity, we can
        // fetch entity.
        // But to reuse logic, let's fetch entity and map here or just use entity
        // directly.
        com.hms.HospitalManagementSystem.entity.LabRequest labRequest = labService.getLabRequestById(labRequestId);

        InputStream jrxmlInput = new ClassPathResource("reports/lab_report.jrxml").getInputStream();
        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlInput);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("hospitalName", "HMS General Hospital");

        String patientName = "N/A";
        if (labRequest.getPatient() != null) {
            patientName = labRequest.getPatient().getFirstName() + " " + labRequest.getPatient().getLastName();
        }
        parameters.put("patientName", patientName);

        parameters.put("testName", labRequest.getTestName());
        parameters.put("testCode", labRequest.getLabTest() != null ? labRequest.getLabTest().getCode() : "N/A");
        parameters.put("requestDate", labRequest.getCreatedAt() != null ? labRequest.getCreatedAt().toString() : "");
        parameters.put("technicianNotes",
                labRequest.getTechnicianNotes() == null ? "" : labRequest.getTechnicianNotes());

        List<com.hms.HospitalManagementSystem.entity.LabResult> results = labRequest.getLabResults();
        if (results == null) {
            results = java.util.Collections.emptyList();
        }

        List<LabResultDtoWrapper> resultDtos = results.stream().map(r -> new LabResultDtoWrapper(r)).toList();
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(resultDtos);

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    public static class LabResultDtoWrapper {
        private String parameterName;
        private String resultValue;
        private String unit;
        private String referenceRange;
        private Boolean isAbnormal;

        public LabResultDtoWrapper(com.hms.HospitalManagementSystem.entity.LabResult r) {
            this.parameterName = r.getParameterName();
            this.resultValue = r.getResultValue();
            this.unit = r.getUnit();
            this.referenceRange = r.getReferenceRange();
            this.isAbnormal = r.getAbnormal();
        }

        public String getParameterName() {
            return parameterName;
        }

        public String getResultValue() {
            return resultValue;
        }

        public String getUnit() {
            return unit;
        }

        public String getReferenceRange() {
            return referenceRange;
        }

        public Boolean getIsAbnormal() {
            return isAbnormal;
        }
    }
}
