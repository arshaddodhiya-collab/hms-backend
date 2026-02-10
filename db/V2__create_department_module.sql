-- Create departments table
CREATE TABLE IF NOT EXISTS departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    head_of_department_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    deleted BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (head_of_department_id) REFERENCES users(id)
);

-- Add department_id to users table
ALTER TABLE users ADD COLUMN department_id BIGINT;
ALTER TABLE users ADD CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id);

-- Migrate existing department strings to new departments table

-- 1. Insert Standard Departments (Seed Data)
INSERT INTO departments (name, description, active) VALUES 
('General', 'General Practice and Triage', true),
('Cardiology', 'Heart and cardiovascular system', true),
('Neurology', 'Brain and nervous system', true),
('Orthopedics', 'Bones and muscles', true),
('Pediatrics', 'Medical care for infants, children, and adolescents', true),
('Gynecology', 'Female reproductive health', true),
('Dermatology', 'Skin, hair, and nails', true),
('Ophthalmology', 'Eye care', true),
('Emergency', 'Urgent critical care', true),
('Radiology', 'Medical imaging', true),
('Pathology', 'Laboratory interaction', true),
('Pharmacy', 'Medicine distribution', true),
('Administration', 'Hospital administration and HR', true);

-- 2. Try to map existing users to these departments (Case Insensitive)
UPDATE users u
JOIN departments d ON LOWER(u.department) = LOWER(d.name)
SET u.department_id = d.id;

-- 3. Insert any custom departments from users that didn't match standard ones
INSERT INTO departments (name, description)
SELECT DISTINCT department, 'Custom Department imported from Users'
FROM users 
WHERE department IS NOT NULL 
  AND department != '' 
  AND department_id IS NULL;

-- 4. Update the remaining users
UPDATE users u
JOIN departments d ON u.department = d.name
SET u.department_id = d.id
WHERE u.department_id IS NULL;


-- 3. Drop old department column (Optional, but cleaner)
-- ALTER TABLE users DROP COLUMN department;
-- Keeping it for now as 'department_legacy' just in case, or renaming it.
ALTER TABLE users CHANGE COLUMN department department_legacy VARCHAR(100);

-- Insert Department Permissions
INSERT INTO permissions (code, module) VALUES
('CMP_ADMIN_DEPT_READ', 'ADMIN'),
('CMP_ADMIN_DEPT_WRITE', 'ADMIN'),
('CMP_ADMIN_DEPT_DELETE', 'ADMIN');


-- Assign permissions to ADMIN role (assuming ID 1 is ADMIN)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.code IN ('CMP_ADMIN_DEPT_READ', 'CMP_ADMIN_DEPT_WRITE', 'CMP_ADMIN_DEPT_DELETE');

