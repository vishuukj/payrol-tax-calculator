-- =====================================================================
-- Payroll & Tax Calculator with Persistence & Analytics
-- MySQL Database Schema
-- =====================================================================

DROP DATABASE IF EXISTS payroll_hrms;
CREATE DATABASE payroll_hrms CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE payroll_hrms;

-- ---------------------------------------------------------------------
-- USERS  (application logins for HR admins / staff)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    user_id         INT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,          -- BCrypt hash, never plaintext
    full_name       VARCHAR(100) NOT NULL,
    role            ENUM('ADMIN','HR_MANAGER','VIEWER') NOT NULL DEFAULT 'HR_MANAGER',
    email           VARCHAR(120),
    is_active       BOOLEAN DEFAULT TRUE,
    last_login      DATETIME NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- DEPARTMENTS
-- ---------------------------------------------------------------------
CREATE TABLE departments (
    department_id   INT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(80) NOT NULL UNIQUE,
    department_head VARCHAR(100),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- TAX SLABS  (editable at runtime from the Tax Calculator screen)
-- ---------------------------------------------------------------------
CREATE TABLE tax_slabs (
    slab_id         INT AUTO_INCREMENT PRIMARY KEY,
    slab_name       VARCHAR(50) NOT NULL,
    min_amount      DECIMAL(14,2) NOT NULL,
    max_amount      DECIMAL(14,2) NULL,             -- NULL = no upper bound
    tax_percent     DECIMAL(5,2) NOT NULL,
    is_active       BOOLEAN DEFAULT TRUE,
    UNIQUE KEY uq_slab_range (min_amount, max_amount)
);

-- ---------------------------------------------------------------------
-- EMPLOYEES
-- ---------------------------------------------------------------------
CREATE TABLE employees (
    employee_id     INT AUTO_INCREMENT PRIMARY KEY,
    employee_code   VARCHAR(20) NOT NULL UNIQUE,     -- e.g. EMP-0001, human friendly
    first_name      VARCHAR(60)  NOT NULL,
    last_name       VARCHAR(60)  NOT NULL,
    gender          ENUM('Male','Female','Other') NOT NULL,
    email           VARCHAR(120) NOT NULL UNIQUE,
    phone           VARCHAR(20)  NOT NULL,
    department_id   INT NOT NULL,
    designation     VARCHAR(80)  NOT NULL,
    joining_date    DATE NOT NULL,
    basic_salary    DECIMAL(14,2) NOT NULL,
    bonus           DECIMAL(14,2) NOT NULL DEFAULT 0,
    allowances      DECIMAL(14,2) NOT NULL DEFAULT 0,
    pf_percent      DECIMAL(5,2)  NOT NULL DEFAULT 12.00,
    other_deductions DECIMAL(14,2) NOT NULL DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(department_id)
        ON UPDATE CASCADE ON DELETE RESTRICT
);

-- ---------------------------------------------------------------------
-- PAYROLL  (one row per employee per pay period)
-- ---------------------------------------------------------------------
CREATE TABLE payroll (
    payroll_id      INT AUTO_INCREMENT PRIMARY KEY,
    employee_id     INT NOT NULL,
    pay_period      VARCHAR(7) NOT NULL,             -- format YYYY-MM
    gross_salary    DECIMAL(14,2) NOT NULL,
    pf_deduction    DECIMAL(14,2) NOT NULL,
    income_tax      DECIMAL(14,2) NOT NULL,
    other_deductions DECIMAL(14,2) NOT NULL DEFAULT 0,
    net_salary      DECIMAL(14,2) NOT NULL,
    generated_on    DATETIME DEFAULT CURRENT_TIMESTAMP,
    generated_by    INT NULL,
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE CASCADE,
    FOREIGN KEY (generated_by) REFERENCES users(user_id) ON DELETE SET NULL,
    UNIQUE KEY uq_employee_period (employee_id, pay_period)
);

-- ---------------------------------------------------------------------
-- SALARY HISTORY  (audit trail of every change to an employee's pay)
-- ---------------------------------------------------------------------
CREATE TABLE salary_history (
    history_id      INT AUTO_INCREMENT PRIMARY KEY,
    employee_id     INT NOT NULL,
    old_basic_salary DECIMAL(14,2),
    new_basic_salary DECIMAL(14,2),
    old_bonus       DECIMAL(14,2),
    new_bonus       DECIMAL(14,2),
    changed_by      INT NULL,
    change_reason   VARCHAR(255),
    changed_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(user_id) ON DELETE SET NULL
);

-- ---------------------------------------------------------------------
-- ACTIVITY LOG  (recent activity feed shown on dashboard)
-- ---------------------------------------------------------------------
CREATE TABLE activity_log (
    log_id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NULL,
    action          VARCHAR(255) NOT NULL,
    entity_type     VARCHAR(50),
    entity_id       INT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
);

-- =====================================================================
-- SEED DATA
-- =====================================================================

-- The default admin login (username: admin / password: Admin@123) is created
-- automatically the first time the application starts (see
-- com.hrms.payroll.service.AppBootstrap), so that its BCrypt hash is always
-- generated correctly at runtime rather than pasted in as a fixed string here.

INSERT INTO departments (department_name, department_head) VALUES
('Engineering', 'Aditi Rao'),
('Human Resources', 'Karan Mehta'),
('Sales & Marketing', 'Priya Sharma'),
('Finance', 'Rohan Gupta'),
('Operations', 'Neha Verma');

INSERT INTO tax_slabs (slab_name, min_amount, max_amount, tax_percent) VALUES
('Slab 1', 0,        250000,   0.00),
('Slab 2', 250001,   500000,   5.00),
('Slab 3', 500001,   1000000,  20.00),
('Slab 4', 1000001,  NULL,     30.00);

INSERT INTO employees
(employee_code, first_name, last_name, gender, email, phone, department_id, designation, joining_date, basic_salary, bonus, allowances, pf_percent, other_deductions)
VALUES
('EMP-0001', 'Aarav',  'Sharma', 'Male',   'aarav.sharma@hrms.local',  '9810000001', 1, 'Senior Software Engineer', '2021-03-14', 95000, 8000, 6000, 12.00, 500),
('EMP-0002', 'Isha',   'Kapoor', 'Female', 'isha.kapoor@hrms.local',   '9810000002', 3, 'Marketing Manager',       '2020-07-01', 78000, 5000, 4500, 12.00, 300),
('EMP-0003', 'Vivaan', 'Nair',   'Male',   'vivaan.nair@hrms.local',   '9810000003', 4, 'Financial Analyst',       '2022-01-19', 62000, 3000, 3000, 12.00, 200),
('EMP-0004', 'Anaya',  'Iyer',   'Female', 'anaya.iyer@hrms.local',    '9810000004', 2, 'HR Executive',            '2023-05-10', 48000, 2000, 2500, 12.00, 150),
('EMP-0005', 'Reyansh','Joshi',  'Male',   'reyansh.joshi@hrms.local', '9810000005', 5, 'Operations Lead',         '2019-11-23', 88000, 6000, 5000, 12.00, 400);

-- Helpful indexes for search/filter performance
CREATE INDEX idx_employee_name ON employees (first_name, last_name);
CREATE INDEX idx_employee_dept ON employees (department_id);
CREATE INDEX idx_employee_salary ON employees (basic_salary);
CREATE INDEX idx_payroll_period ON payroll (pay_period);
