CREATE TABLE lab_test_parameters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lab_test_id BIGINT NOT NULL,
    parameter_name VARCHAR(255) NOT NULL,
    unit VARCHAR(50),
    reference_range VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_parameter_test FOREIGN KEY (lab_test_id) REFERENCES lab_test_catalog(id)
);

CREATE INDEX idx_parameter_test ON lab_test_parameters(lab_test_id);

-- Populate Data for CBC (Code: CBC) - Assuming ID 1
INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'Hemoglobin', 'g/dL', '13.0 - 17.0' FROM lab_test_catalog WHERE code = 'CBC';

INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'RBC Count', 'mill/mm3', '4.5 - 5.5' FROM lab_test_catalog WHERE code = 'CBC';

INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'WBC Count', 'cells/mm3', '4000 - 11000' FROM lab_test_catalog WHERE code = 'CBC';

INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'Platelet Count', 'lakh/mm3', '1.5 - 4.5' FROM lab_test_catalog WHERE code = 'CBC';

INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'Hematocrit (PCV)', '%', '40 - 50' FROM lab_test_catalog WHERE code = 'CBC';

-- Populate Data for Lipid Profile (Code: LIPID) - Assuming ID 2
INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'Total Cholesterol', 'mg/dL', '< 200' FROM lab_test_catalog WHERE code = 'LIPID';

INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'HDL Cholesterol', 'mg/dL', '> 40' FROM lab_test_catalog WHERE code = 'LIPID';

INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'LDL Cholesterol', 'mg/dL', '< 100' FROM lab_test_catalog WHERE code = 'LIPID';

INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'Triglycerides', 'mg/dL', '< 150' FROM lab_test_catalog WHERE code = 'LIPID';

-- Populate Data for LFT (Code: LFT)
INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'Total Bilirubin', 'mg/dL', '0.1 - 1.2' FROM lab_test_catalog WHERE code = 'LFT';

INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'Direct Bilirubin', 'mg/dL', '< 0.3' FROM lab_test_catalog WHERE code = 'LFT';

INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'SGOT (AST)', 'U/L', '0 - 35' FROM lab_test_catalog WHERE code = 'LFT';

INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'SGPT (ALT)', 'U/L', '0 - 45' FROM lab_test_catalog WHERE code = 'LFT';

INSERT INTO lab_test_parameters (lab_test_id, parameter_name, unit, reference_range)
SELECT id, 'Alkaline Phosphatase', 'U/L', '30 - 120' FROM lab_test_catalog WHERE code = 'LFT';
