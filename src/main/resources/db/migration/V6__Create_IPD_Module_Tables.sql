CREATE TABLE IF NOT EXISTS wards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    capacity INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS beds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    number VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ward_id BIGINT NOT NULL,
    CONSTRAINT fk_bed_ward FOREIGN KEY (ward_id) REFERENCES wards(id) ON DELETE CASCADE,
    UNIQUE (ward_id, number)
);

CREATE TABLE IF NOT EXISTS admissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admission_date DATETIME NOT NULL,
    discharge_date DATETIME,
    status VARCHAR(20) NOT NULL,
    diagnosis TEXT,
    discharge_summary TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    bed_id BIGINT NOT NULL,
    CONSTRAINT fk_admission_patient FOREIGN KEY (patient_id) REFERENCES patients(id),
    CONSTRAINT fk_admission_doctor FOREIGN KEY (doctor_id) REFERENCES users(id),
    CONSTRAINT fk_admission_bed FOREIGN KEY (bed_id) REFERENCES beds(id)
);
