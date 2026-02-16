-- Add admission_id column to encounters table
ALTER TABLE encounters
ADD COLUMN admission_id BIGINT;

-- Add foreign key constraint for admission_id
ALTER TABLE encounters
ADD CONSTRAINT fk_encounter_admission
FOREIGN KEY (admission_id)
REFERENCES admissions (id);

-- Modify appointment_id to be nullable (optional for IPD rounds)
ALTER TABLE encounters
MODIFY COLUMN appointment_id BIGINT NULL;

-- Create index for admission_id for faster lookups
CREATE INDEX idx_encounter_admission ON encounters (admission_id);
