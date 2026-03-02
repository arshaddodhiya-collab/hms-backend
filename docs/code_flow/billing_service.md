# Billing & Charge Catalog Service — Code Flow Documentation

## Module Overview

| Aspect | Details |
|--------|---------|
| **Package** | `com.hms.HospitalManagementSystem.service.impl` |
| **Classes** | `BillingServiceImpl`, `ChargeCatalogServiceImpl` |
| **Purpose** | Hospital billing — charge catalog management, invoice generation, payment processing, billing summaries |
| **Used By** | `BillingController`, `AdmissionServiceImpl`, `ReportServiceImpl` |
| **Depends On** | `InvoiceRepository`, `ChargeRepository`, `PatientRepository`, `AdmissionRepository`, `PaymentRepository`, `ChargeCatalogRepository`, `InvoiceMapper`, `PaymentMapper`, `ChargeCatalogMapper` |

---

## ChargeCatalogService — Purpose

ChargeCatalogService manages the **master price list** for hospital services. Each catalog entry has:
- A unique **code** (e.g., `ROOM-GEN`, `LAB-CBC`)
- A service type, name, description
- A standard price
- An active/inactive flag

### Operations

| Method | Purpose |
|--------|---------|
| `createCharge()` | Add new charge to catalog (validates unique code) |
| `getAllCharges()` | List entire catalog |
| `getChargeById()` | Fetch single entry |
| `updateCharge()` | Update name, description, price, type, active status |

> The charge catalog is the foundation of billing — all billable items reference entries from this master list.

---

## BillingService — Purpose & Responsibility

BillingService is the **financial engine** of the HMS. It manages:
1. **Invoice Creation** — building invoices from charges and manual items
2. **Auto-Bill Generation** — triggered on patient discharge
3. **Payment Processing** — recording payments against invoices
4. **Billing Summaries** — aggregated financial view per patient
5. **Outstanding Invoices** — tracking unpaid bills

---

## Invoice Status Lifecycle

```
  DRAFT ──────► ISSUED ──────► PARTIALLY_PAID ──────► PAID
                   │
                   └──────────────────────────► CANCELLED
```

| Status | Description |
|--------|-------------|
| `DRAFT` | Auto-generated on discharge (needs review) |
| `ISSUED` | Manual creation or after review |
| `PARTIALLY_PAID` | Some payment received, balance remaining |
| `PAID` | Fully paid, due amount = 0 |
| `CANCELLED` | Invoice voided |

---

## Service Flow Diagrams

### 1. Create Invoice (`createInvoice`)

```
Client POST /api/billing/invoices
  { patientId, admissionId?, chargeIds[], items[], status? }
        │
        ▼
┌──────────────────────────────────┐
│ 1. Create Invoice Header         │
│  - Generate invoice number       │
│    (INV-{timestamp})             │
│  - Link patient, admission       │
│  - Initialize amounts to ZERO    │
│  - Status: from request or ISSUED│
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 2. Process Charge IDs:           │
│  For each charge:                │
│  - Validate: status == UNBILLED  │ ── Not? → IllegalStateException
│  - Create InvoiceItem from       │
│    charge data                   │
│  - Mark charge → BILLED          │
│  - Link charge → invoice         │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 3. Process Manual Items:         │
│  For each item in request:       │
│  - Create InvoiceItem            │
│  - Calculate totalPrice =        │
│    unitPrice × quantity          │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ 4. Calculate Totals:             │
│  totalAmount = sum of all items  │
│  netAmount = total - discount    │
│               + tax              │
│  dueAmount = net - paid          │
└─────────┬────────────────────────┘
          ▼
     Save → Return InvoiceResponse
```

### 2. Auto-Generate Bill on Discharge (`generateBill`)

```
Called by AdmissionService.dischargePatient()
        │
        ▼
┌──────────────────────────────────┐
│ Find all UNBILLED charges        │
│  for this patient                │
└─────────┬────────────────────────┘
          │
     No charges? → Return (no invoice)
          │
          ▼
┌──────────────────────────────────┐
│ Create Invoice:                  │
│  status = DRAFT                  │
│  links: patient, admission       │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ For each unbilled charge:        │
│  - Create InvoiceItem            │
│  - Mark charge → BILLED          │
│  - Calculate total               │
└─────────┬────────────────────────┘
          ▼
     Save Invoice (DRAFT)
```

> **Discharge trigger**: This is called automatically when `AdmissionService.dischargePatient()` is executed. The invoice starts as `DRAFT` for staff review.

### 3. Process Payment (`processPayment`)

```
Client POST /api/billing/payments
  { invoiceId, amount, paymentMethod, transactionReference, notes }
        │
        ▼
┌──────────────────────────────────┐
│ Validate:                        │
│  - Invoice exists?               │ ── No → ResourceNotFoundException
│  - Not CANCELLED?                │ ── Yes → IllegalStateException
│  - Not already PAID?             │ ── Yes → IllegalStateException
│  - Amount ≤ dueAmount?           │ ── No → IllegalArgumentException
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Create Payment record:           │
│  - Link to invoice               │
│  - method: CASH/CARD/UPI/etc.    │
│  - status = SUCCESS              │
│  - paymentDate = now             │
└─────────┬────────────────────────┘
          ▼
┌──────────────────────────────────┐
│ Update Invoice:                  │
│  paidAmount += payment amount    │
│  dueAmount = net - paid          │
│  If due == 0 → status = PAID     │
│  Else → status = PARTIALLY_PAID  │
└─────────┬────────────────────────┘
          ▼
     Return PaymentResponse
```

### 4. Get Billing Summary (`getBillingSummary`)

```
Client GET /api/billing/summary/{patientId}
        │
        ▼
┌──────────────────────────────────┐
│ Fetch all invoices for patient   │
│ Calculate:                       │
│  - totalBilled: sum of totals    │
│  - totalPaid: sum of paid        │
│  - totalDue: sum of due          │
│    (excluding cancelled)         │
│  - outstandingCount: ISSUED +    │
│    PARTIALLY_PAID count          │
│  - recentInvoices: top 5 by date │
└─────────┬────────────────────────┘
          ▼
     BillingSummaryResponse
```

---

## Charge → Invoice → Payment Flow

```
ChargeCatalog (master prices)
        │
        ▼
Charges (unbilled line items per patient)
        │
        ▼ (createInvoice or auto-generate on discharge)
Invoice (DRAFT → ISSUED)
  ├── InvoiceItem 1 (linked to Charge)
  ├── InvoiceItem 2 (linked to Charge)
  └── InvoiceItem 3 (manual entry)
        │
        ▼ (processPayment)
Payment record → Invoice status updated
```

---

## Where These Services Are Used

| Consumer | Operations |
|----------|-----------|
| `BillingController` | Invoice CRUD, payments, summaries |
| `AdmissionServiceImpl.dischargePatient()` | `generateBill()` — auto-creates invoice on discharge |
| `ReportServiceImpl` | `getInvoiceById()` — fetches data for PDF generation |
| `DashboardServiceImpl` | `invoiceRepository.sumNetAmount()` for today's revenue |
