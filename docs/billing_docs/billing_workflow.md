# Billing Module Architecture & Workflow

## Overview
The Billing Module handles the financial aspects of patient visits, including invoice generation, payment processing, and financial reporting. It integrates with the Patient, Admission, and Charge Catalog modules to provide a seamless billing experience.

## Workflow Diagrams

### 1. Invoice Generation Workflow
This workflow describes how an invoice is created, either manually or from an admission.

```mermaid
sequenceDiagram
    participant User as Receptionist/Admin
    participant FE as Frontend (InvoiceGenerate)
    participant BC as BillingController
    participant BS as BillingService
    participant CCS as ChargeCatalogService
    participant DB as Database

    User->>FE: Select Patient & Items
    FE->>CCS: Get Charge Catalog (Items/Prices)
    CCS->>DB: Fetch Active Charges
    DB-->>CCS: Return Charge List
    CCS-->>FE: Populate Item Dropdown

    User->>FE: Add Items & Click "Generate Invoice"
    FE->>BC: POST /api/v1/billing/invoices (InvoiceRequest)
    BC->>BS: createInvoice(request)
    
    BS->>DB: Fetch Patient Details
    
    loop For Each Item
        BS->>BS: Calculate Line Totals
        BS->>BS: Update Stock (Inventory Module - Future)
    end
    
    BS->>BS: Calculate Invoice Grand Total
    BS->>DB: Save Invoice (Status: ISSUED/DRAFT)
    DB-->>BS: Return Saved Invoice
    BS-->>BC: Return InvoiceResponse
    BC-->>FE: Return Success & Invoice Data
    FE->>User: Redirect to Billing Summary / Show Receipt
```

### 2. Payment Processing Workflow
This workflow details how payments are recorded against an existing invoice.

```mermaid
sequenceDiagram
    participant User as Receptionist
    participant FE as Frontend (PaymentReceipt)
    participant BC as BillingController
    participant BS as BillingService
    participant DB as Database

    User->>FE: Click "Pay" / "View Receipt"
    FE->>BC: GET /api/v1/billing/invoices/{id}
    BC->>BS: getInvoiceById(id)
    BS->>DB: Fetch Invoice
    DB-->>BS: Return Invoice
    BS-->>BC: Return InvoiceResponse
    BC-->>FE: Show Invoice Details & Payment Form

    User->>FE: Enter Amount & Method -> Click "Pay"
    FE->>BC: POST /api/v1/billing/payments (PaymentRequest)
    BC->>BS: processPayment(request)
    
    BS->>DB: Fetch Invoice (Lock/Check Status)
    
    opt Validation
        BS->>BS: Verify Due Amount >= Payment Amount
        BS->>BS: Verify Invoice Not Cancelled/Paid
    end

    BS->>DB: Save Payment Record
    BS->>DB: Update Invoice (Paid Amount, Status: PAID/PARTIALLY_PAID)
    
    DB-->>BS: Return Updated Data
    BS-->>BC: Return PaymentResponse
    BC-->>FE: Update UI & Show Success Toast
```

## Database Schema (Simplified)

```mermaid
erDiagram
    PATIENT ||--o{ INVOICE : "incurred"
    ADMISSION ||--o{ INVOICE : "generated_from"
    INVOICE ||--|{ INVOICE_ITEM : "contains"
    INVOICE ||--o{ PAYMENT : "paid_via"
    CHARGE_CATALOG ||--o{ INVOICE_ITEM : "defines"
    
    INVOICE {
        Long id
        String invoiceNumber
        Decimal totalAmount
        Decimal paidAmount
        Decimal dueAmount
        String status
        DateTime issueDate
    }

    INVOICE_ITEM {
        Long id
        String description
        Decimal unitPrice
        Integer quantity
        Decimal totalPrice
    }

    PAYMENT {
        Long id
        Decimal amount
        String paymentMethod
        String status
        DateTime paymentDate
    }
```

## API Endpoints

| Method | Endpoint | Description | Permission |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/billing/invoices` | Create a new invoice | `CMP_INVOICE_GENERATE` |
| `GET` | `/api/v1/billing/invoices/{id}` | Get invoice details | `CMP_INVOICE_GENERATE`, `CMP_BILLING_SUMMARY` |
| `POST` | `/api/v1/billing/payments` | Record a payment | `CMP_PAYMENT_RECEIPT` |
| `GET` | `/api/v1/billing/summary/{patientId}` | Get financial summary for patient | `CMP_BILLING_SUMMARY` |
| `GET` | `/api/v1/billing/invoices` | Get all invoices (Admin/Audit) | `CMP_BILLING_SUMMARY` |
