INSERT INTO
    charge_catalog (
        code,
        name,
        description,
        service_type,
        standard_price,
        is_active
    )
VALUES (
        'CONS-001',
        'General Consultation',
        'Standard OPD Consultation',
        'CONSULTATION',
        50.00,
        true
    ),
    (
        'CONS-002',
        'Specialist Consultation',
        'Specialist Doctor Consultation',
        'CONSULTATION',
        100.00,
        true
    ),
    (
        'LAB-001',
        'Complete Blood Count (CBC)',
        'Basic blood test panel',
        'LAB',
        25.00,
        true
    ),
    (
        'LAB-002',
        'Urinalysis',
        'Routine urine test',
        'LAB',
        15.00,
        true
    ),
    (
        'RAD-001',
        'X-Ray Chest PA',
        'Chest X-Ray Posterior-Anterior View',
        'PROCEDURE',
        40.00,
        true
    ),
    (
        'RAD-002',
        'MRI Brain',
        'Magnetic Resonance Imaging of Brain',
        'PROCEDURE',
        250.00,
        true
    ),
    (
        'PROC-001',
        'Wound Dressing',
        'Minor wound dressing and cleaning',
        'PROCEDURE',
        30.00,
        true
    ),
    (
        'PROC-002',
        'Suture Removal',
        'Removal of surgical sutures',
        'PROCEDURE',
        20.00,
        true
    ),
    (
        'PHARM-001',
        'Paracetamol 500mg',
        'Pain reliever tablet',
        'PHARMACY',
        0.50,
        true
    ),
    (
        'PHARM-002',
        'Amoxicillin 500mg',
        'Antibiotic capsule',
        'PHARMACY',
        1.00,
        true
    ),
    (
        'BED-001',
        'General Ward Bed Charge',
        'Daily charge for General Ward',
        'BED_CHARGE',
        15.00,
        true
    ),
    (
        'BED-002',
        'ICU Bed Charge',
        'Daily charge for ICU',
        'BED_CHARGE',
        100.00,
        true
    );