-- Create users table (for doctors, nurses, admins, etc.)
-- Note: This matches the User entity structure
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    department_id BIGINT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME
);

CREATE INDEX idx_users_username ON users(username);


-- Create patients table
-- Note: This matches the Patient entity which extends PublicEntity
-- Patient has: firstName, lastName, dob, gender, bloodGroup, contact, email, address, allergies, avatar
CREATE TABLE patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    dob DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    blood_group VARCHAR(20),
    contact VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) UNIQUE,
    address TEXT,
    allergies TEXT,
    avatar VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME,
    updated_at DATETIME,
    version BIGINT
);

CREATE INDEX idx_patient_name ON patients(first_name, last_name);
CREATE INDEX idx_patient_contact ON patients(contact);
