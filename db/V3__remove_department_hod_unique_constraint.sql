-- Drop the unique constraint/index on head_of_department_id
-- The constraint name 'UKrfnsv2ay8ellqaesawo78owom' was found from the error log
-- But to be safe and portable, we can try to drop the index by column name if the constraint name varies, 
-- or use the specific name if we are sure (Hibernate generates predictable names usually).

-- Attempt to drop the foreign key first if it exists (usually not needed for valid index drop, but good for cleanup if valid)
-- ALTER TABLE departments DROP FOREIGN KEY FK_DEPT_HOD; 

-- Drop the unique index
DROP INDEX UKrfnsv2ay8ellqaesawo78owom ON departments;

-- Re-add a non-unique index for performance (optional but recommended for foreign keys)
CREATE INDEX IDX_DEPT_HOD ON departments(head_of_department_id);
