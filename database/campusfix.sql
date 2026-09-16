-- CampusFix database schema + seed data
-- Run this whole file against a MySQL server before starting the app:
--   mysql -u root -p < database/campusfix.sql

DROP DATABASE IF EXISTS campusfix;
CREATE DATABASE campusfix CHARACTER SET utf8mb4;
USE campusfix;

CREATE TABLE departments (
    department_id   INT AUTO_INCREMENT PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL UNIQUE,
    description      VARCHAR(255)
);

CREATE TABLE users (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    password      VARCHAR(100) NOT NULL,
    role          ENUM('STUDENT', 'STAFF', 'ADMIN') NOT NULL,
    department_id INT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (department_id) REFERENCES departments(department_id)
);

CREATE TABLE complaints (
    complaint_id        VARCHAR(20) PRIMARY KEY,
    title               VARCHAR(150) NOT NULL,
    description         TEXT NOT NULL,
    category            VARCHAR(50) NOT NULL,
    location            VARCHAR(100),
    priority            ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    status              ENUM('SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED', 'IN_PROGRESS',
                              'ON_HOLD', 'RESOLVED', 'CLOSED', 'REJECTED') NOT NULL,
    submitted_by        INT NOT NULL,
    assigned_to         INT,
    department_id       INT,
    created_at          TIMESTAMP NOT NULL,
    updated_at          TIMESTAMP NOT NULL,
    resolved_at         TIMESTAMP NULL,
    resolution_remarks  TEXT,
    FOREIGN KEY (submitted_by) REFERENCES users(user_id),
    FOREIGN KEY (assigned_to) REFERENCES users(user_id),
    FOREIGN KEY (department_id) REFERENCES departments(department_id)
);

CREATE TABLE complaint_history (
    history_id   INT AUTO_INCREMENT PRIMARY KEY,
    complaint_id VARCHAR(20) NOT NULL,
    old_status   ENUM('SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED', 'IN_PROGRESS',
                       'ON_HOLD', 'RESOLVED', 'CLOSED', 'REJECTED'),
    new_status   ENUM('SUBMITTED', 'UNDER_REVIEW', 'ASSIGNED', 'IN_PROGRESS',
                       'ON_HOLD', 'RESOLVED', 'CLOSED', 'REJECTED') NOT NULL,
    changed_by   VARCHAR(100) NOT NULL,
    remarks      TEXT,
    changed_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id)
);

CREATE TABLE feedback (
    feedback_id  INT AUTO_INCREMENT PRIMARY KEY,
    complaint_id VARCHAR(20) NOT NULL,
    user_id      INT NOT NULL,
    rating       TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment      TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (complaint_id) REFERENCES complaints(complaint_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE audit_log (
    log_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT,
    action      VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    timestamp   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- seed departments
INSERT INTO departments (department_name, description) VALUES
    ('IT Support', 'Wi-Fi, networking and classroom equipment'),
    ('Electrical Maintenance', 'Wiring, lighting and electrical hazards'),
    ('Hostel Administration', 'Hostel rooms and facilities'),
    ('Housekeeping', 'Cleanliness and water supply issues'),
    ('General Maintenance', 'Furniture and general repairs'),
    ('Library Administration', 'Library-related issues'),
    ('Campus Security', 'Security concerns'),
    ('Transport Office', 'Campus transportation issues');

-- seed users - passwords are plain text here for demo purposes only, never do this in production
INSERT INTO users (name, email, password, role, department_id) VALUES
    ('Admin User', 'admin@campusfix.edu', 'admin123', 'ADMIN', NULL),
    ('Aditi Sharma', 'aditi@campusfix.edu', 'pass123', 'STUDENT', NULL),
    ('Rohan Verma', 'rohan@campusfix.edu', 'pass123', 'STUDENT', NULL),
    ('Meera Iyer', 'meera@campusfix.edu', 'pass123', 'STUDENT', NULL),
    ('Staff102', 'staff102@campusfix.edu', 'staff123', 'STAFF', 1),
    ('Staff201', 'staff201@campusfix.edu', 'staff123', 'STAFF', 2),
    ('Staff301', 'staff301@campusfix.edu', 'staff123', 'STAFF', 4);
