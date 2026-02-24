# JasperReports Integration Guide

This document explains the "what, how, and where" of the JasperReports PDF integration implemented for the Laboratory and Billing modules in our Hospital Management System (HMS).

## What We Did
We integrated **JasperReports** to dynamically generate downloadable PDF documents for:
1. **Billing Receipts/Invoices**: A unified view of a patient's bill with an itemized breakdown.
2. **Laboratory Reports**: A structured view of a patient's lab test results, including parameters, reference ranges, and abnormal flags.

## How It Works
The PDF generation process spans both the Spring Boot backend and the Angular frontend.

### 1. Template Design (JRXML)
JasperReports uses XML-based templates (`.jrxml` files) to define the layout, styling, and data bindings of the PDF.
- We structured the templates with `title` (hospital/patient info), `columnHeader` (table columns), `detail` (iterated rows of data), and `summary` (totals/notes).
- Dynamic scalar values (like patient name, total amount) are passed as **Parameters**.
- Iterative data (like invoice items, lab results) is passed as **Fields** mapped through a `JRBeanCollectionDataSource`.

### 2. Backend Compilation and Data Population
When a request is made to download a PDF:
1. The backend loads the `.jrxml` file from the classpath.
2. `JasperCompileManager` compiles the `.jrxml` into a `JasperReport` object.
3. The specific service logic fetches the entity data (e.g., `LabRequest` or `InvoiceResponse`) from the database.
4. Parameters are put into a `Map<String, Object>`.
5. List data is wrapped in a `JRBeanCollectionDataSource`. *Note*: We created specific Data Transfer Object (DTO) wrappers like `LabResultDtoWrapper` to guarantee perfect mapping between Jasper fields and Java getters (e.g., ensuring `isAbnormal` maps to `getIsAbnormal()`).
6. `JasperFillManager` fills the report with data to yield a `JasperPrint`.
7. `JasperExportManager` exports the print to a `byte[]` which is returned to the client as an `application/pdf` response.

### 3. Frontend Blob Handling
Downloading a binary file via XHR requires specific handling in Angular:
1. `ApiService` exposes a generic `getBlob` method that sets `{ responseType: 'blob' }`.
2. The specific feature service (e.g., `BillingService`) calls the PDF endpoint to receive the Blob.
3. The component uses `window.URL.createObjectURL(blob)` to create a temporary download link, simulates a click to trigger the browser's native download dialog, and then revokes the object URL for cleanup.

### 4. Resolving DevTools Classloader Issues
Spring Boot DevTools uses a special restart classloader. This classloader sometimes struggles to find third-party libraries correctly, resulting in `ClassNotFoundException: net.sf.jasperreports.engine.JRDataSource`.
- **Fix**: We added a `spring-devtools.properties` file indicating that JasperReports libraries should be included in the restart classloader.

## Where Everything Is Located

### Backend (`hms-backend`)
- **Dependencies**: `pom.xml` (JasperReports `6.20.6` dependency).
- **Templates**:
  - `src/main/resources/reports/invoice_report.jrxml`
  - `src/main/resources/reports/lab_report.jrxml`
- **Classloader Fix**: `src/main/resources/META-INF/spring-devtools.properties`
- **Business Logic**: `src/main/java/com/hms/HospitalManagementSystem/service/ReportService.java`
- **API Endpoints**:
  - `BillingController.java` (`GET /api/v1/billing/invoices/{id}/pdf`)
  - `LabController.java` (`GET /api/v1/lab-requests/{id}/pdf`)
- **Unit Tests**: `src/test/java/com/hms/HospitalManagementSystem/service/ReportServiceTest.java`

### Frontend (`hms-v3`)
- **API File Transfer**: `src/app/core/services/api.service.ts` (`getBlob` method)
- **Billing Module**:
  - `src/app/features/billing/services/billing.service.ts`
  - `payment-receipt.component.ts` & `.html`
- **Lab Module**:
  - `src/app/features/lab/services/lab.service.ts`
  - `lab-report-view.component.ts` & `.html`

## Important Considerations / Gotchas
1. **Null Checks**: When populating JasperReport Parameters or Fields, any `null` value might cause display issues or `NullPointerException`s if you're trying to execute methods on them (e.g., `.toString()`). It's crucial to apply null-safe fallbacks internally within `ReportService.java`.
2. **Template XML Strictness**: The JasperReports XML schema is very strict. Putting attributes in the wrong element (e.g., `forecolor` inside `<font>` instead of `<textElement>`) will cause a `SAXParseException` internally and result in an HTTP 500 error. Always compile or run unit tests on JRXML files locally to debug template issues.
