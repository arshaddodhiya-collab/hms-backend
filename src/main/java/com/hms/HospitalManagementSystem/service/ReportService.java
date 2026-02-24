package com.hms.HospitalManagementSystem.service;

public interface ReportService {
    byte[] generateInvoicePdf(Long invoiceId) throws Exception;

    byte[] generateLabReportPdf(Long labRequestId) throws Exception;
}
