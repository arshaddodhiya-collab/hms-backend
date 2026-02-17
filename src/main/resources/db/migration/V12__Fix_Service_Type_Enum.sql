-- Fix invalid ServiceType values seeded in V11
UPDATE charge_catalog
SET
    service_type = 'LAB'
WHERE
    service_type = 'LABORATORY';

UPDATE charge_catalog
SET
    service_type = 'PROCEDURE'
WHERE
    service_type = 'RADIOLOGY';