CREATE TABLE encounters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    chief_complaint TEXT,
    diagnosis TEXT,
    notes TEXT,
    started_at DATETIME NOT NULL,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_encounter_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_encounter_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_encounter_doctor FOREIGN KEY (doctor_id) REFERENCES users(id)
);

CREATE INDEX idx_encounter_appointment ON encounters(appointment_id);
CREATE INDEX idx_encounter_patient ON encounters(patient_id);
CREATE INDEX idx_encounter_doctor ON encounters(doctor_id);
CREATE INDEX idx_encounter_status ON encounters(status);


CREATE TABLE vitals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    encounter_id BIGINT NOT NULL UNIQUE,
    temperature DECIMAL(5,2),
    systolic INT,
    diastolic INT,
    pulse INT,
    spo2 INT,
    weight DECIMAL(5,2),
    height DECIMAL(5,2),
    bmi DECIMAL(5,2),
    recorded_at DATETIME NOT NULL,
    recorded_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_vitals_encounter FOREIGN KEY (encounter_id) REFERENCES encounters(id),
    CONSTRAINT fk_vitals_recorder FOREIGN KEY (recorded_by) REFERENCES users(id)
);

CREATE INDEX idx_vitals_encounter ON vitals(encounter_id);


CREATE TABLE prescriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    encounter_id BIGINT NOT NULL,
    note TEXT,
    status VARCHAR(20) NOT NULL,
    issued_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_prescription_encounter FOREIGN KEY (encounter_id) REFERENCES encounters(id)
);

CREATE INDEX idx_prescription_encounter ON prescriptions(encounter_id);


CREATE TABLE prescription_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL,
    medicine_name VARCHAR(255) NOT NULL,
    dosage VARCHAR(100),
    frequency VARCHAR(100),
    duration VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_item_prescription FOREIGN KEY (prescription_id) REFERENCES prescriptions(id)
);

CREATE INDEX idx_item_prescription ON prescription_items(prescription_id);
