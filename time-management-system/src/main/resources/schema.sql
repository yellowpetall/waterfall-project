-- STC Time Management System - PostgreSQL schema
-- Run with: psql -U postgres -d stc -f schema.sql

DROP TABLE IF EXISTS time_entries;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('EMPLOYEE', 'SUPERVISOR', 'HR'))
);

CREATE TABLE time_entries (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_name VARCHAR(100) NOT NULL,
    entry_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_duration DOUBLE PRECISION DEFAULT 0,
    working_hours DOUBLE PRECISION DEFAULT 0,
    comment TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED')),
    rejection_reason TEXT
);

INSERT INTO users (username, password, role) VALUES
    ('sena',    '1234', 'EMPLOYEE'),
    ('hoca',    '456',  'SUPERVISOR'),
    ('stc_hr',  '789',  'HR');

-- If the table already exists and you only need the new column, run:
--   ALTER TABLE time_entries ADD COLUMN IF NOT EXISTS rejection_reason TEXT;
