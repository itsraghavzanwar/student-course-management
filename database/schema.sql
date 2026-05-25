-- ============================================================
-- Student & Course Management System - Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS scm_db;
USE scm_db;

-- ============================================================
-- TABLE: roles
-- ============================================================
CREATE TABLE IF NOT EXISTS roles (
    id      BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name    VARCHAR(20)  NOT NULL UNIQUE
);

INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_STUDENT');

-- ============================================================
-- TABLE: courses
-- ============================================================
CREATE TABLE IF NOT EXISTS courses (
    course_id       BIGINT       AUTO_INCREMENT PRIMARY KEY,
    course_name     VARCHAR(150) NOT NULL,
    course_code     VARCHAR(20)  NOT NULL UNIQUE,
    course_duration INT          NOT NULL COMMENT 'Duration in weeks',
    description     TEXT,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: users  (auth table)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- TABLE: user_roles  (join)
-- ============================================================
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id)   ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id)   ON DELETE CASCADE
);

-- ============================================================
-- TABLE: students
-- ============================================================
CREATE TABLE IF NOT EXISTS students (
    student_id    BIGINT       AUTO_INCREMENT PRIMARY KEY,
    first_name    VARCHAR(80)  NOT NULL,
    last_name     VARCHAR(80)  NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    phone         VARCHAR(20),
    date_of_birth DATE,
    course_id     BIGINT,
    user_id       BIGINT UNIQUE,
    enrolled_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES courses(course_id) ON DELETE SET NULL,
    FOREIGN KEY (user_id)   REFERENCES users(id)          ON DELETE SET NULL
);

-- ============================================================
-- STORED PROCEDURE: sp_insert_student
-- ============================================================
DROP PROCEDURE IF EXISTS sp_insert_student;
DELIMITER $$
CREATE PROCEDURE sp_insert_student(
    IN  p_first_name    VARCHAR(80),
    IN  p_last_name     VARCHAR(80),
    IN  p_email         VARCHAR(100),
    IN  p_phone         VARCHAR(20),
    IN  p_dob           DATE,
    IN  p_course_id     BIGINT,
    OUT p_student_id    BIGINT,
    OUT p_message       VARCHAR(255)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_student_id = -1;
        SET p_message = 'Error: Transaction rolled back.';
    END;

    START TRANSACTION;

    -- Validate course exists
    IF p_course_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM courses WHERE course_id = p_course_id
    ) THEN
        SET p_student_id = -1;
        SET p_message = 'Error: Course does not exist.';
        ROLLBACK;
    ELSE
        INSERT INTO students (first_name, last_name, email, phone, date_of_birth, course_id)
        VALUES (p_first_name, p_last_name, p_email, p_phone, p_dob, p_course_id);

        SET p_student_id = LAST_INSERT_ID();
        SET p_message = 'Success: Student inserted.';
        COMMIT;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- STORED PROCEDURE: sp_update_student
-- ============================================================
DROP PROCEDURE IF EXISTS sp_update_student;
DELIMITER $$
CREATE PROCEDURE sp_update_student(
    IN  p_student_id    BIGINT,
    IN  p_first_name    VARCHAR(80),
    IN  p_last_name     VARCHAR(80),
    IN  p_email         VARCHAR(100),
    IN  p_phone         VARCHAR(20),
    IN  p_dob           DATE,
    IN  p_course_id     BIGINT,
    OUT p_rows_affected INT,
    OUT p_message       VARCHAR(255)
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_rows_affected = 0;
        SET p_message = 'Error: Transaction rolled back.';
    END;

    START TRANSACTION;

    IF NOT EXISTS (SELECT 1 FROM students WHERE student_id = p_student_id) THEN
        SET p_rows_affected = 0;
        SET p_message = 'Error: Student not found.';
        ROLLBACK;
    ELSEIF p_course_id IS NOT NULL AND NOT EXISTS (
        SELECT 1 FROM courses WHERE course_id = p_course_id
    ) THEN
        SET p_rows_affected = 0;
        SET p_message = 'Error: Course not found.';
        ROLLBACK;
    ELSE
        UPDATE students
        SET first_name = p_first_name,
            last_name  = p_last_name,
            email      = p_email,
            phone      = p_phone,
            date_of_birth = p_dob,
            course_id  = p_course_id
        WHERE student_id = p_student_id;

        SET p_rows_affected = ROW_COUNT();
        SET p_message = 'Success: Student updated.';
        COMMIT;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- STORED PROCEDURE: sp_delete_student
-- ============================================================
DROP PROCEDURE IF EXISTS sp_delete_student;
DELIMITER $$
CREATE PROCEDURE sp_delete_student(
    IN  p_student_id    BIGINT,
    IN  p_force         TINYINT,   -- 1 = force delete even if enrolled, 0 = restrict
    OUT p_rows_affected INT,
    OUT p_message       VARCHAR(255)
)
BEGIN
    DECLARE v_course_id BIGINT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_rows_affected = 0;
        SET p_message = 'Error: Transaction rolled back.';
    END;

    START TRANSACTION;

    SELECT course_id INTO v_course_id
    FROM students WHERE student_id = p_student_id;

    IF v_course_id IS NOT NULL AND p_force = 0 THEN
        SET p_rows_affected = 0;
        SET p_message = 'Error: Student is enrolled in a course. Use force=true to delete.';
        ROLLBACK;
    ELSE
        DELETE FROM students WHERE student_id = p_student_id;
        SET p_rows_affected = ROW_COUNT();
        SET p_message = 'Success: Student deleted.';
        COMMIT;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- Seed Data
-- ============================================================
INSERT IGNORE INTO courses (course_name, course_code, course_duration, description) VALUES
('Bachelor of Computer Science',  'BCS-101', 208, 'Comprehensive CS degree covering algorithms, data structures, and software engineering.'),
('Full Stack Web Development',    'FSWD-201', 24,  'End-to-end web development with React, Node.js, and cloud deployment.'),
('Data Science & Machine Learning','DSML-301', 32, 'Statistics, Python, ML models, and real-world data pipelines.'),
('Cybersecurity Fundamentals',    'CYBR-401', 20, 'Network security, ethical hacking, and incident response.'),
('Cloud Computing & DevOps',      'CCDO-501', 16, 'AWS/GCP, Docker, Kubernetes, and CI/CD pipelines.');
