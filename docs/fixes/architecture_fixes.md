# Architecture Updates Explained

This document explains the recent structural changes made to the backend to improve stability and prevent database conflicts. All changes were designed to ensure **100% compatibility** with the existing database.

---

## 1. Fixing Duplicate Database Columns (Entity Consolidation)

**The Problem:**
Several of our core Java classes (`Encounter`, `Appointment`, `PublicEntity`, and `BaseEntity`) were defining the exact same database columns manually—such as `id`, `created_at`, and `updated_at`. This meant if we ever wanted to change how IDs work or how timestamps are generated, we would have to hunt down and change it in many files.

**The Solution:**
We reorganized the code so that everything inherits from one single source of truth: `BaseEntity`. 
- `BaseEntity` now handles the `id`, `created_at`, and `updated_at` logic for everyone.
- `PublicEntity` (which adds `active` and `deleted` flags) now extends `BaseEntity`.
- `Encounter` and `Appointment` now extend `BaseEntity`. 
- We replaced the standard `@Builder` with `@SuperBuilder` so that child classes can inherit and build the fields from their parents properly.

*Why is this safe?* Because we simply moved the definitions to a parent class. The actual database columns remain exactly the same.

---

## 2. Fixing Database Schema Conflicts (Flyway vs Hibernate)

**The Problem:**
We had two different tools fighting to control the database structure:
1. **Flyway:** Our official migration tool (running the scripts in `src/main/resources/db/migration`).
2. **Hibernate:** Configured with `spring.jpa.hibernate.ddl-auto=update`, this tool tries to automatically guess and change the database schema based on our Java code every time the server starts.

When both are running, Hibernate can accidentally overwrite or corrupt the carefully planned Flyway tables.

**The Solution:**
We changed Hibernate's setting to `spring.jpa.hibernate.ddl-auto=validate`. 
Now, Flyway is the *only* tool allowed to create or modify tables. Hibernate will only *check* the tables when the server starts. If our Java code doesn't exactly match the Flyway database, the server will refuse to start, alerting us immediately instead of silently breaking the database.

---

## 3. Discarding Unused Migration Files

**The Problem:**
There was a `/db` directory at the very root of the project (outside of `src`). It contained old, duplicate `.sql` files that were not being used by the application, causing confusion.

**The Solution:**
We deleted the root `/db` folder. The official Flyway migrations inside `src/main/resources/db/migration/` remain untouched and are the only source of truth.

---

## 4. Making CORS (Cross-Origin Resource Sharing) Configurable

**The Problem:**
The backend was hardcoded to only accept requests from `http://localhost:4200` in the `SecurityConfig.java` file. If we deployed this to production, the frontend wouldn't be able to talk to the backend.

**The Solution:**
We moved this setting to `application.properties`:
`cors.allowed-origins=${CORS_ORIGINS:http://localhost:4200}`

Now, by default, it works locally exactly as it did before. But when deploying to production, we can simply set a `CORS_ORIGINS` environment variable (e.g., `https://my-hospital-app.com`) without modifying any Java code!
