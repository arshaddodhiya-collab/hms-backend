# Report Service — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Class** | `ReportServiceImpl` |
| **Purpose** | PDF report generation for invoices and lab results using JasperReports |
| **Used By** | `ReportController` |
| **Depends On** | `BillingService`, `LabService`, JasperReports library |

---

## Purpose & Responsibility

ReportService generates **printable PDF documents** from application data. Currently supports:
1. **Invoice PDFs** — formatted billing invoices for patients
2. **Lab Report PDFs** — formatted laboratory result reports

Both use **JasperReports** (`.jrxml` templates) for layout and formatting.

---

## Service Flow Diagrams

### 1. Generate Invoice PDF (`generateInvoicePdf`)

```
Client GET /api/reports/invoice/{invoiceId}/pdf
        │
        ▼
┌──────────────────────────────────┐
│ Fetch InvoiceResponse            │
│  via BillingService              │
│  .getInvoiceById()               │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Load JRXML template:             │
│  reports/invoice_report.jrxml    │
│  (from classpath/resources)      │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Set Parameters:                  │
│  - hospitalName                  │
│  - patientName                   │
│  - invoiceNumber                 │
│  - issueDate                     │
│  - totalAmount, discountAmount   │
│  - taxAmount, netAmount          │
│  - paidAmount, dueAmount         │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Set Data Source:                  │
│  InvoiceItems list →             │
│  JRBeanCollectionDataSource      │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Compile → Fill → Export to PDF   │
│  JasperCompile → JasperFill      │
│  → JasperExportManager           │
└─────────┬────────────────────────┘
          ▼
     byte[] (PDF binary data)
```

### 2. Generate Lab Report PDF (`generateLabReportPdf`)

```
Client GET /api/reports/lab/{labRequestId}/pdf
        │
        ▼
┌──────────────────────────────────┐
│ Fetch LabRequest entity          │
│  via LabService                  │
│  .getLabRequestById()            │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Load JRXML template:             │
│  reports/lab_report.jrxml        │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Set Parameters:                  │
│  - hospitalName                  │
│  - patientName (First + Last)    │
│  - testName, testCode            │
│  - requestDate                   │
│  - technicianNotes               │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Wrap results in DTO:             │
│  LabResult → LabResultDtoWrapper │
│  (parameterName, resultValue,    │
│   unit, referenceRange,          │
│   isAbnormal)                    │
│                                  │
│ Set as JRBeanCollection          │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Compile → Fill → Export to PDF   │
└─────────┬────────────────────────┘
          ▼
     byte[] (PDF binary data)
```

---

## Template Locations

| Template | Path |
|----------|------|
| Invoice Report | `src/main/resources/reports/invoice_report.jrxml` |
| Lab Report | `src/main/resources/reports/lab_report.jrxml` |

---

## Technical Details

- **JasperReports** compiles `.jrxml` XML templates at runtime
- Data is passed via `JRBeanCollectionDataSource` (Java bean → table rows)
- The `LabResultDtoWrapper` inner class adapts entity fields to JasperReports-compatible JavaBean format
- Output is a raw `byte[]` — the controller sets `Content-Type: application/pdf` and streams it to the client

---

## Where This Service Is Used

| Consumer | Operations |
|----------|-----------|
| `ReportController` | `/api/reports/invoice/{id}/pdf`, `/api/reports/lab/{id}/pdf` |
