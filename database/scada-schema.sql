-- Smart Factory SCADA full schema.
-- WARNING: this script drops and recreates the scada database.
-- Requires MySQL 8.0 or newer.

DROP DATABASE IF EXISTS scada;

CREATE DATABASE scada
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE scada;

CREATE TABLE plants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    company_type ENUM('HYUNDAI', 'KIA') NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_plants_company_name (company_type, name)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(50),
    role ENUM('ADMIN', 'MANAGER', 'OPERATOR', 'VIEWER') NOT NULL DEFAULT 'VIEWER',
    plant_id BIGINT,
    status ENUM('ACTIVE', 'INACTIVE', 'LOCKED') NOT NULL DEFAULT 'ACTIVE',
    note VARCHAR(500),
    last_login_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_email (email),
    INDEX idx_users_role_status (role, status),
    INDEX idx_users_plant_status (plant_id, status),
    CONSTRAINT fk_users_plant
        FOREIGN KEY (plant_id) REFERENCES plants (id)
        ON DELETE SET NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE facilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    facility_type ENUM('PRESS', 'BODY', 'PAINT', 'ASSEMBLY', 'INSPECTION', 'HVAC', 'ETC') NOT NULL,
    status ENUM('RUNNING', 'WARNING', 'STOPPED') NOT NULL DEFAULT 'RUNNING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_facilities_plant_type_name (plant_id, facility_type, name),
    CONSTRAINT fk_facilities_plant
        FOREIGN KEY (plant_id) REFERENCES plants (id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE energy_measurements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    facility_id BIGINT NOT NULL,
    measured_at DATETIME NOT NULL,
    electricity_kwh DECIMAL(15, 2),
    gas_m3 DECIMAL(15, 2),
    water_ton DECIMAL(15, 2),
    solar_kwh DECIMAL(15, 2),
    peak_kw DECIMAL(15, 2),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_energy_measurements_plant_time_facility_id (plant_id, measured_at, facility_id, id),
    INDEX idx_energy_measurements_plant_facility_time_id (plant_id, facility_id, measured_at, id),
    CONSTRAINT fk_energy_measurements_plant
        FOREIGN KEY (plant_id) REFERENCES plants (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_energy_measurements_facility
        FOREIGN KEY (facility_id) REFERENCES facilities (id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE energy_summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    facility_id BIGINT,
    summary_type ENUM('HOURLY', 'DAILY', 'MONTHLY') NOT NULL,
    summary_at DATETIME NOT NULL,
    electricity_kwh DECIMAL(15, 2) NOT NULL DEFAULT 0,
    gas_m3 DECIMAL(15, 2) NOT NULL DEFAULT 0,
    water_ton DECIMAL(15, 2) NOT NULL DEFAULT 0,
    solar_kwh DECIMAL(15, 2) NOT NULL DEFAULT 0,
    peak_kw DECIMAL(15, 2) NOT NULL DEFAULT 0,
    carbon_emission DECIMAL(15, 2) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_energy_summaries_plant_facility_type_time (plant_id, facility_id, summary_type, summary_at),
    INDEX idx_energy_summaries_plant_type_time (plant_id, summary_type, summary_at),
    CONSTRAINT fk_energy_summaries_plant
        FOREIGN KEY (plant_id) REFERENCES plants (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_energy_summaries_facility
        FOREIGN KEY (facility_id) REFERENCES facilities (id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE energy_latest_measurements (
    plant_id BIGINT NOT NULL,
    facility_id BIGINT NOT NULL,
    measured_at DATETIME NOT NULL,
    electricity_kwh DECIMAL(15, 2),
    gas_m3 DECIMAL(15, 2),
    water_ton DECIMAL(15, 2),
    solar_kwh DECIMAL(15, 2),
    peak_kw DECIMAL(15, 2),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (plant_id, facility_id),
    INDEX idx_energy_latest_plant_measured_at (plant_id, measured_at),
    INDEX idx_energy_latest_facility_measured_at (facility_id, measured_at),
    CONSTRAINT fk_energy_latest_plant
        FOREIGN KEY (plant_id) REFERENCES plants (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_energy_latest_facility
        FOREIGN KEY (facility_id) REFERENCES facilities (id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE energy_interval_summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    facility_id BIGINT NOT NULL,
    bucket_type ENUM('FIFTEEN_MINUTES', 'HOURLY', 'DAILY') NOT NULL,
    bucket_at DATETIME NOT NULL,
    electricity_usage_kwh DECIMAL(15, 2) NOT NULL DEFAULT 0,
    gas_usage_m3 DECIMAL(15, 2) NOT NULL DEFAULT 0,
    water_usage_ton DECIMAL(15, 2) NOT NULL DEFAULT 0,
    solar_usage_kwh DECIMAL(15, 2) NOT NULL DEFAULT 0,
    peak_average_kw DECIMAL(15, 2) NOT NULL DEFAULT 0,
    peak_kw DECIMAL(15, 2) NOT NULL DEFAULT 0,
    peak_sum_kw DECIMAL(15, 2) NOT NULL DEFAULT 0,
    peak_sample_count INT NOT NULL DEFAULT 0,
    first_measured_at DATETIME,
    last_measured_at DATETIME,
    measurement_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_energy_interval_summary (plant_id, facility_id, bucket_type, bucket_at),
    INDEX idx_energy_interval_plant_type_time (plant_id, bucket_type, bucket_at),
    INDEX idx_energy_interval_plant_facility_type_time (plant_id, facility_id, bucket_type, bucket_at),
    INDEX idx_energy_interval_type_time (bucket_type, bucket_at),
    CONSTRAINT fk_energy_interval_plant
        FOREIGN KEY (plant_id) REFERENCES plants (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_energy_interval_facility
        FOREIGN KEY (facility_id) REFERENCES facilities (id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE esg_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    target_month DATE NOT NULL,
    electricity_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    gas_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    water_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    solar_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    peak_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    carbon_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    total_score DECIMAL(5, 2) NOT NULL DEFAULT 0,
    grade ENUM('AAA', 'AA', 'A', 'BBB', 'BB', 'B', 'CCC') NOT NULL DEFAULT 'CCC',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_esg_scores_plant_month (plant_id, target_month),
    INDEX idx_esg_scores_month_total (target_month, total_score),
    CONSTRAINT fk_esg_scores_plant
        FOREIGN KEY (plant_id) REFERENCES plants (id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE alarms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    facility_id BIGINT,
    alarm_type ENUM('PEAK', 'ELECTRICITY', 'GAS', 'WATER', 'FACILITY', 'ESG') NOT NULL,
    alarm_level ENUM('INFO', 'WARNING', 'CRITICAL') NOT NULL,
    message VARCHAR(1000) NOT NULL,
    value DECIMAL(15, 2),
    threshold_value DECIMAL(15, 2),
    occurred_at DATETIME NOT NULL,
    resolved_at DATETIME,
    status ENUM('OCCURRED', 'RESOLVED') NOT NULL DEFAULT 'OCCURRED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_alarms_plant_status_time (plant_id, status, occurred_at),
    INDEX idx_alarms_level_status_time (alarm_level, status, occurred_at),
    CONSTRAINT fk_alarms_plant
        FOREIGN KEY (plant_id) REFERENCES plants (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_alarms_facility
        FOREIGN KEY (facility_id) REFERENCES facilities (id)
        ON DELETE SET NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE simulation_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plant_id BIGINT NOT NULL,
    base_score DECIMAL(5, 2) NOT NULL,
    base_grade ENUM('AAA', 'AA', 'A', 'BBB', 'BB', 'B', 'CCC') NOT NULL,
    electricity_reduction_rate DECIMAL(5, 2) NOT NULL DEFAULT 0,
    gas_reduction_rate DECIMAL(5, 2) NOT NULL DEFAULT 0,
    water_reduction_rate DECIMAL(5, 2) NOT NULL DEFAULT 0,
    peak_reduction_rate DECIMAL(5, 2) NOT NULL DEFAULT 0,
    solar_increase_rate DECIMAL(5, 2) NOT NULL DEFAULT 0,
    expected_score DECIMAL(5, 2) NOT NULL,
    expected_grade ENUM('AAA', 'AA', 'A', 'BBB', 'BB', 'B', 'CCC') NOT NULL,
    analysis_result VARCHAR(2000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_simulation_user_plant_time (user_id, plant_id, created_at),
    CONSTRAINT fk_simulation_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_simulation_plant
        FOREIGN KEY (plant_id) REFERENCES plants (id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chatbot_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plant_id BIGINT,
    question VARCHAR(1000) NOT NULL,
    answer VARCHAR(4000) NOT NULL,
    referenced_data JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chatbot_user_plant_time (user_id, plant_id, created_at),
    CONSTRAINT fk_chatbot_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_chatbot_plant
        FOREIGN KEY (plant_id) REFERENCES plants (id)
        ON DELETE SET NULL
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
