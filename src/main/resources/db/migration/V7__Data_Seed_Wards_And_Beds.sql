-- Insert Wards
INSERT INTO wards (name, type, capacity, is_active) VALUES ('General Ward - Male', 'GENERAL', 20, TRUE);
INSERT INTO wards (name, type, capacity, is_active) VALUES ('General Ward - Female', 'GENERAL', 20, TRUE);
INSERT INTO wards (name, type, capacity, is_active) VALUES ('ICU', 'ICU', 10, TRUE);
INSERT INTO wards (name, type, capacity, is_active) VALUES ('Private Ward', 'PRIVATE', 5, TRUE);

-- Get IDs for wards (assuming auto-increment starts or continues, but using subqueries for safety)
-- Insert Beds for General Ward - Male
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('GM-101', 'GENERAL', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'General Ward - Male'));
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('GM-102', 'GENERAL', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'General Ward - Male'));
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('GM-103', 'GENERAL', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'General Ward - Male'));
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('GM-104', 'GENERAL', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'General Ward - Male'));
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('GM-105', 'GENERAL', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'General Ward - Male'));

-- Insert Beds for General Ward - Female
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('GF-201', 'GENERAL', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'General Ward - Female'));
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('GF-202', 'GENERAL', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'General Ward - Female'));
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('GF-203', 'GENERAL', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'General Ward - Female'));
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('GF-204', 'GENERAL', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'General Ward - Female'));
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('GF-205', 'GENERAL', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'General Ward - Female'));

-- Insert Beds for ICU
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('ICU-301', 'ICU', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'ICU'));
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('ICU-302', 'ICU', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'ICU'));
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('ICU-303', 'ICU', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'ICU'));

-- Insert Beds for Private Ward
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('PVT-401', 'PRIVATE', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'Private Ward'));
INSERT INTO beds (number, type, is_occupied, is_active, ward_id) VALUES ('PVT-402', 'PRIVATE', FALSE, TRUE, (SELECT id FROM wards WHERE name = 'Private Ward'));
