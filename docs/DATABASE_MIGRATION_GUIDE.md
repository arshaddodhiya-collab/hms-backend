# Database Migration Guide

> [!IMPORTANT]
> **RULE:** Always create a NEW migration file for ANY change to the database schema. Never modify existing migration files that have already been applied or shared.

## Strategy: Manual Migrations

Currently, the project uses a manual migration strategy. We do **not** use Flyway or Liquibase automatically at this stage, but we follow the discipline of versioned migration files to ensure consistency.

## Workflow

1.  **Draft the SQL Change**: Write your `CREATE TABLE`, `ALTER TABLE`, or `INSERT` statements.
2.  **Create a New File**:
    *   **Location**: `hms-backend/db/migration/` (Create this directory if it doesn't exist) or `hms-backend/db/` if keeping it simple for now.
    *   **Naming Convention**: `V<VERSION>__<DESCRIPTION>.sql`
        *   Example: `V2__create_appointments_table.sql`
        *   Example: `V3__add_status_to_patients.sql`
    *   **Versioning**: Increment the version number sequentially (V1, V2, V3...).

3.  **Apply to Local DB**: Run the SQL script against your local MySQL database.
4.  **Commit**: Add the new SQL file to git.

## Best Practices

*   **One Change per File**: Ideally, keep content focused.
*   **Idempotency**: Use `IF NOT EXISTS` for table creation where possible, but rely on the versioning system to ensure scripts are run only once.
*   **Rollbacks**: (Optional but recommended) Include comments or a separate file describing how to revert the change (e.g., `DROP TABLE xyz`).

## Why this is critical?

*   **Team Sync**: Other developers need to apply your changes to their local DBs.
*   **Production Safety**: We need a clear history of changes to apply to the production database safely.

## Database Reset (Development Only)

If you need to wipe the database and start fresh with the new `Department` module:

1.  **Stop the App**.
2.  **Drop Database**: Log into MySQL and run:
    ```sql
    DROP DATABASE hospital_db;
    CREATE DATABASE hospital_db;
    ```
3.  **Start the App**: `./mvnw spring-boot:run`
    *   Hibernate will automatically recreate the schema (Tables).
    *   `DataInitializer` will automatically seed:
        *   Permissions
        *   Roles
        *   **Standard Departments** (General, Cardiology, etc.)
        *   **Default Users** (admin, doctor, etc.) linked to departments.

