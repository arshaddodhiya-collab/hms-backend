-- Add auditing columns to all entities extending BaseEntity or implementing it manually

ALTER TABLE charge ADD COLUMN created_by VARCHAR(255);

ALTER TABLE charge ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE charge_catalog ADD COLUMN created_by VARCHAR(255);

ALTER TABLE charge_catalog ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE encounters ADD COLUMN created_by VARCHAR(255);

ALTER TABLE encounters ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE invoice_item ADD COLUMN created_by VARCHAR(255);

ALTER TABLE invoice_item ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE appointments ADD COLUMN created_by VARCHAR(255);

ALTER TABLE appointments ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE payment ADD COLUMN created_by VARCHAR(255);

ALTER TABLE payment ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE invoice ADD COLUMN created_by VARCHAR(255);

ALTER TABLE invoice ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE users ADD COLUMN created_by VARCHAR(255);

ALTER TABLE users ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE admissions ADD COLUMN created_by VARCHAR(255);

ALTER TABLE admissions ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE medical_histories ADD COLUMN created_by VARCHAR(255);

ALTER TABLE medical_histories
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE departments ADD COLUMN created_by VARCHAR(255);

ALTER TABLE departments ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE patients ADD COLUMN created_by VARCHAR(255);

ALTER TABLE patients ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE prescriptions ADD COLUMN created_by VARCHAR(255);

ALTER TABLE prescriptions ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE lab_results ADD COLUMN created_by VARCHAR(255);

ALTER TABLE lab_results ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE lab_requests ADD COLUMN created_by VARCHAR(255);

ALTER TABLE lab_requests ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE vitals ADD COLUMN created_by VARCHAR(255);

ALTER TABLE vitals ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE rounds ADD COLUMN created_by VARCHAR(255);

ALTER TABLE rounds ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE lab_test_parameters ADD COLUMN created_by VARCHAR(255);

ALTER TABLE lab_test_parameters
ADD COLUMN last_modified_by VARCHAR(255);

ALTER TABLE lab_test_catalog ADD COLUMN created_by VARCHAR(255);

ALTER TABLE lab_test_catalog
ADD COLUMN last_modified_by VARCHAR(255);

-- For wards and beds, the entities Ward and Bed don't seem to have createdAt/updatedAt directly or extend BaseEntity.
-- I'll add them if they were extending BaseEntity, but since they weren't matched in grep they probably aren't audited.