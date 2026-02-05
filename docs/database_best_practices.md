# Database Best Practices for High-Performance HMIS

Building a Hospital Management Information System (HMIS) requires a database that is not only **fast** but also **reliable** and **compliant**. In healthcare, data integrity is non-negotiable, but speed is critical for user adoption.

Here is a comprehensive guide to optimizing your database architecture.

---

## 1. Schema Design & Modeling

### Use 3rd Normal Form (3NF) for Core Data
Transactional data (Patient demographics, Admissions, Billing) should be normalized to avoid anomalies.
*   **Why**: Ensures data consistency. Updating a patient's name in one place reflects everywhere.
*   **Example**: Separate `Patients`, `Visits`, and `Diagnosis` tables rather than one giant flat table.

### Selective Denormalization for Reporting
Do **not** run complex aggregations on your transactional (OLTP) tables during peak hours.
*   **Strategy**: Create specific "Summary Tables" or "Materialized Views" for dashboards (e.g., `DailyRevenue`, `BedOccupancySnapshot`).
*   **Update Mechanism**: Update these via events or nightly jobs, not constantly.

### UUIDs vs. Auto-Increment
For a large-scale/distributed HMIS, consider **UUIDs (v7)** or **TSID**.
*   **Pros**: Secure (unguessable IDs), mergeable across servers.
*   **Cons**: Slightly slower on inserts than simple Integers. 
*   **Recommendation**: Use `Long` (Auto-Increment) for internal foreign keys if running on a single primary instance for performance, but UUIDs for public-facing APIs.

### The "Soft Delete" Pattern
**Never** hard delete clinical data. It is illegal in many jurisdictions and bad for auditing.
*   **Implementation**: Add `deleted_at (TIMESTAMP)` or `is_active (BOOLEAN)` to every table.
*   **Optimization**: Ensure your default queries filter `WHERE deleted_at IS NULL` (use Hibernate `@Where` clause).

---

## 2. Indexing Strategy (The #1 Performance Factor)

### Composite Indexes
Single column indexes are rarely enough. Index based on your **Query Patterns**.
*   **Scenario**: Searching for a patient's visits by date.
*   **Bad**: Index on `patient_id` only.
*   **Good**: Composite Index on `(patient_id, visit_date desc)`.
*   **Why**: The DB jumps to the patient bucket and immediately finds the sorted dates.

### Covering Indexes
Design indexes that contain *all* the columns needed for a specific query.
*   **Example**: `SELECT first_name, last_name FROM patients WHERE phone = ?`
*   **Index**: `(phone, first_name, last_name)`
*   **Benefit**: The DB gets the answer directly from the index (in RAM) without ever touching the heavy table data on disk.

### Index Foreign Keys
Always index Foreign Key columns (`user_id`, `department_id`).
*   **Why**: Prevents full table scans during `JOINS` and avoids table locks during cascading deletions/updates.

---

## 3. Query Optimization

### Pagination: Keyset vs. Offset
Avoid `OFFSET` pagination for large lists (e.g., "Page 5000 of Logs").
*   **Bad**: `OFFSET 50000 LIMIT 10` (DB scans and throws away 50,000 rows).
*   **Good (Keyset)**: `WHERE id < :last_seen_id ORDER BY id DESC LIMIT 10`.
*   **Result**: O(1) complexity. Instant access regardless of data size.

### Optimizing Hibernate/JPA
Since you are using Spring Boot:
1.  **Use Projections**: Don't fetch the full `Patient` entity just to display a name in a list. Use interfaces (`PatientSummary`).
2.  **Avoid N+1**: Use `@EntityGraph` or `JOIN FETCH` (as implemented in your `PostRepository`) for retrieving related data (e.g., Patient + Insurance Details).
3.  **Batch Writes**: Enable JDBC batching to insert 1000 records in 1 network call, not 1000 calls.
    ```properties
    spring.jpa.properties.hibernate.jdbc.batch_size=50
    spring.jpa.properties.hibernate.order_inserts=true
    ```

---

## 4. Architecture & Scaling

### Read/Write Splitting
Separating concerns increases throughput.
*   **Master DB**: Handles `INSERT`, `UPDATE`, `DELETE`.
*   **Read Replicas**: Handle `SELECT` queries for reporting, analytics, and read-heavy views.
*   **Implementation**: Use `@Transactional(readOnly = true)` to route queries to replicas automatically.

### Caching Strategy (Redis)
Don't hit the DB for "Slow Changing Data".
*   **What to limit**: Configuration, Zip Codes, Drug Dictionaries, Department Names.
*   **Tool**: Redis or Hazelcast (2nd Level Cache).
*   **Benefit**: Reduces DB load by 30-50%.

### Partitioning
If you have millions of rows (e.g., `AuditLogs`, `VitalSigns`), partition tables by **Year/Month**.
*   **Benefit**: Queries for "January 2026" only scan that specific partition file, ignoring the terabytes of history from 2020-2025.

---

## 5. Handling Search (Search Engine)
**Do NOT** use `LIKE '%keyword%'` for searching patients or clinical notes.
*   **Why**: It forces a full table scan and is incredibly slow.
*   **Solution**: Offload search to **Elasticsearch** or **Meilisearch**.
*   **Workflow**: Save to DB -> Event fires -> Sync text to Elasticsearch -> Search API queries Elasticsearch -> Returns IDs.

---

## 6. Archival Strategy
HMIS databases grow forever. Define an archival policy early.
*   **Hot Data**: Last 1-2 years (SSD Storage).
*   **Cold Data**: Older than 2 years. Move to a separate "Archive Database" or cheaper storage (HDD/S3).
*   **Compliance**: Ensure cold data is still retrievable if audited, but it doesn't need to be instant.

## Summary Checklist
- [ ] Enable JDBC Batching.
- [ ] Use Projections for lists.
- [ ] Index Foreign Keys.
- [ ] Use Redis for master data.
- [ ] Avoid `LIKE` queries; implementation full-text search.
- [ ] Soft Delete implementation.
