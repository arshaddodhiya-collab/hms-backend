-- Create Charge Catalog Table
CREATE TABLE IF NOT EXISTS charge_catalog (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    service_type VARCHAR(50) NOT NULL,
    standard_price DECIMAL(10, 2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create Invoice Table
CREATE TABLE IF NOT EXISTS invoice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    admission_id BIGINT,
    total_amount DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2) DEFAULT 0.00,
    tax_amount DECIMAL(10, 2) DEFAULT 0.00,
    net_amount DECIMAL(10, 2) NOT NULL,
    paid_amount DECIMAL(10, 2) DEFAULT 0.00,
    due_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    generated_by BIGINT,
    issue_date DATETIME,
    due_date DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_invoice_admission FOREIGN KEY (admission_id) REFERENCES admissions (id),
    CONSTRAINT fk_invoice_generated_by FOREIGN KEY (generated_by) REFERENCES users (id)
);

-- Create Charge Table
CREATE TABLE IF NOT EXISTS charge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    source_module VARCHAR(50) NOT NULL,
    source_reference_id BIGINT NOT NULL,
    charge_catalog_id BIGINT,
    item_name VARCHAR(100) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL,
    invoice_id BIGINT,
    version BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_charge_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_charge_catalog FOREIGN KEY (charge_catalog_id) REFERENCES charge_catalog (id),
    CONSTRAINT fk_charge_invoice FOREIGN KEY (invoice_id) REFERENCES invoice (id)
);

-- Create Invoice Item Table
CREATE TABLE IF NOT EXISTS invoice_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    charge_id BIGINT,
    item_description VARCHAR(255) NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_invoice_item_invoice FOREIGN KEY (invoice_id) REFERENCES invoice (id),
    CONSTRAINT fk_invoice_item_charge FOREIGN KEY (charge_id) REFERENCES charge (id)
);

-- Create Payment Table
CREATE TABLE IF NOT EXISTS payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    transaction_reference VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    received_by BIGINT,
    payment_date DATETIME NOT NULL,
    notes TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoice (id),
    CONSTRAINT fk_payment_received_by FOREIGN KEY (received_by) REFERENCES users (id)
);