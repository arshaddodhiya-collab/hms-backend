CREATE TABLE lab_test_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    price DECIMAL(10, 2) NOT NULL,
    reference_range TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_lab_test_code ON lab_test_catalog(code);

CREATE TABLE lab_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    encounter_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    lab_test_id BIGINT,
    test_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    technician_notes TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_lab_request_encounter FOREIGN KEY (encounter_id) REFERENCES encounters(id),
    CONSTRAINT fk_lab_request_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_lab_request_test FOREIGN KEY (lab_test_id) REFERENCES lab_test_catalog(id)
);

CREATE INDEX idx_lab_request_status ON lab_requests(status);
CREATE INDEX idx_lab_request_encounter ON lab_requests(encounter_id);
CREATE INDEX idx_lab_request_patient ON lab_requests(patient_id);

CREATE TABLE lab_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lab_request_id BIGINT NOT NULL,
    parameter_name VARCHAR(255) NOT NULL,
    result_value VARCHAR(255) NOT NULL,
    unit VARCHAR(50),
    reference_range VARCHAR(255),
    is_abnormal BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_result_request FOREIGN KEY (lab_request_id) REFERENCES lab_requests(id)
);

CREATE INDEX idx_result_request ON lab_results(lab_request_id);

-- Populate Lab Test Catalog
INSERT INTO lab_test_catalog (name, code, price, reference_range, is_active) VALUES
('Complete Blood Count', 'CBC', 500.00, 'N/A', TRUE),
('Lipid Profile', 'LIPID', 1200.00, 'Cholesterol < 200 mg/dL', TRUE),
('Liver Function Test', 'LFT', 800.00, 'ALT: 7-55 U/L, AST: 8-48 U/L', TRUE),
('Kidney Function Test', 'KFT', 900.00, 'Creatinine: 0.6-1.2 mg/dL', TRUE),
('Thyroid Profile', 'THYROID', 1500.00, 'TSH: 0.4-4.0 mIU/L', TRUE),
('Blood Sugar Fasting', 'FBS', 200.00, '70-100 mg/dL', TRUE),
('Blood Sugar PP', 'PPBS', 200.00, '< 140 mg/dL', TRUE),
('HbA1c', 'HBA1C', 600.00, '< 5.7%', TRUE),
('Urine Routine', 'URINE_R', 150.00, 'N/A', TRUE),
('Vitamin D', 'VIT_D', 2000.00, '20-50 ng/mL', TRUE),
('Vitamin B12', 'VIT_B12', 1800.00, '200-900 pg/mL', TRUE),
('Dengue NS1 Antigen', 'DENGUE_NS1', 1200.00, 'Negative', TRUE),
('Typhoid Widal', 'WIDAL', 400.00, 'Negative', TRUE),
('Serum Electrolytes', 'ELECTROLYTES', 600.00, 'Na: 135-145, K: 3.5-5.0', TRUE),
('C-Reactive Protein', 'CRP', 500.00, '< 10 mg/L', TRUE);
