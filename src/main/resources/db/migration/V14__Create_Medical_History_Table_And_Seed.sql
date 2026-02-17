CREATE TABLE IF NOT EXISTS medical_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    condition_name VARCHAR(255) NOT NULL,
    diagnosed_date DATE,
    status VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_medical_history_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
);

INSERT INTO
    medical_histories (
        patient_id,
        condition_name,
        diagnosed_date,
        status,
        active,
        deleted,
        created_at,
        updated_at
    )
SELECT id, 'Hypertension', '2023-01-15', 'ONGOING', 1, 0, NOW(), NOW()
FROM patients
WHERE
    id = 1
    AND NOT EXISTS (
        SELECT 1
        FROM medical_histories
        WHERE
            patient_id = 1
            AND condition_name = 'Hypertension'
    );

INSERT INTO
    medical_histories (
        patient_id,
        condition_name,
        diagnosed_date,
        status,
        active,
        deleted,
        created_at,
        updated_at
    )
SELECT id, 'Type 2 Diabetes', '2022-05-20', 'ONGOING', 1, 0, NOW(), NOW()
FROM patients
WHERE
    id = 1
    AND NOT EXISTS (
        SELECT 1
        FROM medical_histories
        WHERE
            patient_id = 1
            AND condition_name = 'Type 2 Diabetes'
    );

INSERT INTO
    medical_histories (
        patient_id,
        condition_name,
        diagnosed_date,
        status,
        active,
        deleted,
        created_at,
        updated_at
    )
SELECT id, 'Appendicitis', '2020-11-10', 'HEALED', 1, 0, NOW(), NOW()
FROM patients
WHERE
    id = 1
    AND NOT EXISTS (
        SELECT 1
        FROM medical_histories
        WHERE
            patient_id = 1
            AND condition_name = 'Appendicitis'
    );