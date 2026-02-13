-- Create Lab Test Catalog Table
CREATE TABLE IF NOT EXISTS lab_test_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    price DECIMAL(10, 2) NOT NULL,
    reference_range TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active BIT(1) NOT NULL DEFAULT 1,
    is_deleted BIT(1) NOT NULL DEFAULT 0
);

-- Create Lab Requests Table
CREATE TABLE IF NOT EXISTS lab_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    encounter_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    test_name VARCHAR(255) NOT NULL, -- Snapshot of test name or custom test
    lab_test_id BIGINT, -- Link to catalog
    status VARCHAR(50) NOT NULL, -- ORDERED, SAMPLED, COMPLETED, CANCELLED
    technician_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lab_request_encounter FOREIGN KEY (encounter_id) REFERENCES encounters(id),
    CONSTRAINT fk_lab_request_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_lab_request_catalog FOREIGN KEY (lab_test_id) REFERENCES lab_test_catalog(id)
);

-- Create Lab Results Table
CREATE TABLE IF NOT EXISTS lab_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lab_request_id BIGINT NOT NULL,
    parameter_name VARCHAR(255) NOT NULL,
    result_value VARCHAR(255) NOT NULL,
    unit VARCHAR(50),
    reference_range VARCHAR(255),
    is_abnormal BIT(1) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_lab_result_request FOREIGN KEY (lab_request_id) REFERENCES lab_requests(id) ON DELETE CASCADE
);

-- Add Indexes
CREATE INDEX idx_lab_request_status ON lab_requests(status);
CREATE INDEX idx_lab_request_encounter ON lab_requests(encounter_id);
CREATE INDEX idx_lab_request_patient ON lab_requests(patient_id);
