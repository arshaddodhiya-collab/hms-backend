package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.entity.LabRequest;
import com.hms.HospitalManagementSystem.entity.LabResult;
import com.hms.HospitalManagementSystem.entity.LabTestCatalog;
import com.hms.HospitalManagementSystem.entity.Patient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;

@SpringBootTest
public class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @MockBean
    private LabService labService;

    @MockBean
    private BillingService billingService;

    @Test
    public void testGenerateLabReportPdf() throws Exception {
        LabRequest mockRequest = new LabRequest();
        mockRequest.setId(1L);
        Patient patient = new Patient();
        patient.setFirstName("John");
        patient.setLastName("Doe");
        mockRequest.setPatient(patient);
        LabTestCatalog labTest = new LabTestCatalog();
        labTest.setCode("LBC");
        labTest.setName("Complete Blood Count");
        mockRequest.setLabTest(labTest);
        mockRequest.setCreatedAt(LocalDateTime.now());
        mockRequest.setTestName("Complete Blood Count");

        LabResult mockResult = new LabResult();
        mockResult.setParameterName("Hemoglobin");
        mockResult.setResultValue("15.0");
        mockResult.setUnit("g/dL");
        mockResult.setReferenceRange("13.8 - 17.2");
        mockResult.setAbnormal(false);
        mockRequest.setLabResults(List.of(mockResult));

        when(labService.getLabRequestById(1L)).thenReturn(mockRequest);

        byte[] pdf = reportService.generateLabReportPdf(1L);
        System.out.println("Generated PDF of size: " + pdf.length);
    }

    @Test
    public void testGenerateInvoicePdf() throws Exception {
        com.hms.HospitalManagementSystem.dto.response.InvoiceResponse mockInvoice = new com.hms.HospitalManagementSystem.dto.response.InvoiceResponse();
        mockInvoice.setInvoiceNumber("INV-001");
        mockInvoice.setPatientName("John Doe");
        mockInvoice.setTotalAmount(new java.math.BigDecimal("100.00"));

        com.hms.HospitalManagementSystem.dto.response.InvoiceItemResponse mockItem = new com.hms.HospitalManagementSystem.dto.response.InvoiceItemResponse();
        mockItem.setDescription("Consultation");
        mockItem.setQuantity(1);
        mockItem.setUnitPrice(new java.math.BigDecimal("100.00"));
        mockItem.setTotalPrice(new java.math.BigDecimal("100.00"));

        mockInvoice.setItems(List.of(mockItem));

        when(billingService.getInvoiceById(1L)).thenReturn(mockInvoice);

        byte[] pdf = reportService.generateInvoicePdf(1L);
        System.out.println("Generated Invoice PDF of size: " + pdf.length);
    }
}
