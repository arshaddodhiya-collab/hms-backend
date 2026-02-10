-- Create Patients Table
CREATE TABLE IF NOT EXISTS patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BIT(1) Not NULL DEFAULT 1,
    deleted BIT(1) NOT NULL DEFAULT 0,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    dob DATE NOT NULL,
    gender VARCHAR(50) NOT NULL,
    blood_group VARCHAR(20),
    contact VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    address TEXT,
    allergies TEXT,
    avatar VARCHAR(255),
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_patient_contact UNIQUE (contact),
    CONSTRAINT uk_patient_email UNIQUE (email)
);

-- Create Indexes for Patients
CREATE INDEX idx_patient_name ON patients(first_name, last_name);
CREATE INDEX idx_patient_contact ON patients(contact);

-- Create Medical History Table
CREATE TABLE IF NOT EXISTS medical_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BIT(1) NOT NULL DEFAULT 1,
    deleted BIT(1) NOT NULL DEFAULT 0,
    patient_id BIGINT NOT NULL,
    condition_name VARCHAR(255) NOT NULL,
    diagnosed_date DATE,
    status VARCHAR(50),
    CONSTRAINT fk_medical_history_patient FOREIGN KEY (patient_id) REFERENCES patients(id) ON DELETE CASCADE
);
