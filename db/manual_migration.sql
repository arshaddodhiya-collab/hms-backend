-- Manual Migration Script
-- All database changes should be appended here.
-- Do not modify existing SQL statements once applied to production.

-- =========================================================================================
-- [2024-02-09] Security Module Schema
-- =========================================================================================

-- 1. Permissions Table
CREATE TABLE IF NOT EXISTS permissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    module VARCHAR(50) NOT NULL
);

-- 2. Roles Table
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- 3. Role-Permissions Join Table
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- 4. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    department_id BIGINT,
    active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 5. User-Roles Join Table
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- =========================================================================================
-- [2024-02-09] Seed Data (Roles & Permissions)
-- =========================================================================================

-- Insert Permissions (Modules)
INSERT IGNORE INTO permissions (code, module) VALUES 
('MOD_DASHBOARD', 'DASHBOARD'),
('MOD_PATIENTS', 'PATIENTS'),
('MOD_APPOINTMENTS', 'APPOINTMENTS'),
('MOD_TRIAGE', 'TRIAGE'),
('MOD_CONSULTATION', 'CONSULTATION'),
('MOD_LAB', 'LAB'),
('MOD_BILLING', 'BILLING'),
('MOD_ADMIN', 'ADMIN'),
('MOD_VOICE', 'VOICE');

-- Insert Permissions (Actions & Components)
INSERT IGNORE INTO permissions (code, module) VALUES 
('ACT_VIEW', 'ACTION'),
('ACT_CREATE', 'ACTION'),
('ACT_EDIT', 'ACTION'),
('ACT_DELETE', 'ACTION'),
('CMP_VITALS_WRITE', 'COMPONENT'),
('CMP_VITALS_READ', 'COMPONENT'),
('CMP_PATIENT_ADD', 'COMPONENT'),
('CMP_PATIENT_LIST', 'COMPONENT'),
('CMP_PATIENT_VIEW', 'COMPONENT'),
('CMP_PATIENT_EDIT', 'COMPONENT'),
('CMP_APPOINTMENT_LIST', 'COMPONENT'),
('CMP_APPOINTMENT_CREATE', 'COMPONENT'),
('CMP_APPOINTMENT_VIEW', 'COMPONENT'),
('CMP_APPOINTMENT_EDIT', 'COMPONENT'),
('CMP_CONSULTATION_READ', 'COMPONENT'),
('CMP_CONSULTATION_WRITE', 'COMPONENT'),
('CMP_LAB_ENTRY', 'COMPONENT'),
('CMP_LAB_READ', 'COMPONENT'),
('CMP_INVOICE_GENERATE', 'COMPONENT'),
('CMP_PAYMENT_RECEIPT', 'COMPONENT'),
('CMP_BILLING_SUMMARY', 'COMPONENT'),
('CMP_ADMIN_DEPT_READ', 'COMPONENT'),
('CMP_ADMIN_DEPT_WRITE', 'COMPONENT'),
('CMP_ADMIN_USER_READ', 'COMPONENT'),
('CMP_ADMIN_USER_WRITE', 'COMPONENT'),
('CMP_ADMIN_ROLE_WRITE', 'COMPONENT');

-- Insert Roles
INSERT IGNORE INTO roles (name, description) VALUES 
('ADMIN', 'Administrator with full access'),
('DOCTOR', 'Medical Doctor'),
('NURSE', 'Nurse and Triage'),
('RECEPTION', 'Front Desk'),
('LAB_TECH', 'Lab Technician');

-- Helper procedure to assign all permissions to ADMIN (Optional, but useful)
-- For now, we will map them manually or let the app handle it via an API later.
-- Or just insert a few key ones.

-- Map all permissions to ADMIN role (assuming ADMIN is id=1)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ADMIN';

-- Insert Default Admin User (password: admin123 -> bcrypt needed)
-- $2a$10$r.7... is irrelevant here, user needs to generate one.
-- I'll insert a placeholder or skip user insertion to avoid bad passwords.
-- Users should be created via API or manually with known hash.
