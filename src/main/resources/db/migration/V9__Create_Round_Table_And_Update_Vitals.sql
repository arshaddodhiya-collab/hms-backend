-- Create Rounds Table
CREATE TABLE rounds IF NOT EXISTS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    encounter_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    notes TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_rounds_encounter FOREIGN KEY (encounter_id) REFERENCES encounters (id),
    CONSTRAINT fk_rounds_doctor FOREIGN KEY (doctor_id) REFERENCES users (id)
);

CREATE INDEX idx_rounds_encounter ON rounds (encounter_id);

-- Update Vitals to allow multiple records per encounter
-- Drop the unique constraint on encounter_id to allow 1:N relationship
ALTER TABLE vitals DROP INDEX encounter_id;