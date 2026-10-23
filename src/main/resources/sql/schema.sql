-- Database Schema for Freelancer Invoice Generator

CREATE TABLE IF NOT EXISTS clients (
    client_id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS projects (
    project_id SERIAL PRIMARY KEY,
    client_id INT NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    hourly_rate NUMERIC(10, 2) NOT NULL,
    budget_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    FOREIGN KEY (client_id) REFERENCES clients(client_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS work_logs (
    log_id SERIAL PRIMARY KEY,
    project_id INT NOT NULL,
    work_date DATE NOT NULL,
    hours_worked NUMERIC(8, 2) NOT NULL,
    description TEXT,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS invoices (
    invoice_id SERIAL PRIMARY KEY,
    project_id INT NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 0,
    discount_type VARCHAR(12) NOT NULL DEFAULT 'NONE',
    discount_value NUMERIC(12, 2) NOT NULL DEFAULT 0,
    notes TEXT,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id SERIAL PRIMARY KEY,
    invoice_id INT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    payment_date DATE NOT NULL,
    note TEXT,
    FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id) ON DELETE CASCADE
);

-- Safe migrations for databases created by earlier versions of the project.
ALTER TABLE projects ADD COLUMN IF NOT EXISTS budget_amount NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS tax_rate NUMERIC(5, 2) NOT NULL DEFAULT 0;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS discount_type VARCHAR(12) NOT NULL DEFAULT 'NONE';
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS discount_value NUMERIC(12, 2) NOT NULL DEFAULT 0;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS notes TEXT;
