CREATE DATABASE IF NOT EXISTS scada
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE scada;

SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS alarms;
DROP TABLE IF EXISTS chatbot_messages;
DROP TABLE IF EXISTS simulation_results;
DROP TABLE IF EXISTS esg_scores;
DROP TABLE IF EXISTS energy_summaries;
DROP TABLE IF EXISTS energy_measurements;
DROP TABLE IF EXISTS facilities;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS plants;

SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;

CREATE TABLE plants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    company_type ENUM('HYUNDAI', 'KIA') NOT NULL,
    address VARCHAR(255),
    latitude DECIMAL(10, 7),
    longitude DECIMAL(10, 7),

    INDEX idx_plants_company_type (company_type)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
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

    CONSTRAINT fk_users_plant
        FOREIGN KEY (plant_id) REFERENCES plants(id)
        ON DELETE SET NULL,

    INDEX idx_users_plant_id (plant_id),
    INDEX idx_users_role_status (role, status)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE facilities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    facility_type ENUM('PRESS', 'BODY', 'PAINT', 'ASSEMBLY', 'INSPECTION', 'HVAC', 'ETC') NOT NULL,
    status ENUM('RUNNING', 'WARNING', 'STOPPED') NOT NULL DEFAULT 'RUNNING',

    CONSTRAINT fk_facilities_plant
        FOREIGN KEY (plant_id) REFERENCES plants(id),

    INDEX idx_facilities_plant_id (plant_id),
    INDEX idx_facilities_type_status (facility_type, status)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE energy_measurements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    facility_id BIGINT,
    measured_at DATETIME NOT NULL,
    electricity_kwh DECIMAL(12, 2),
    gas_m3 DECIMAL(12, 2),
    water_ton DECIMAL(12, 2),
    solar_kwh DECIMAL(12, 2),
    peak_kw DECIMAL(12, 2),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_measurements_plant
        FOREIGN KEY (plant_id) REFERENCES plants(id),

    CONSTRAINT fk_measurements_facility
        FOREIGN KEY (facility_id) REFERENCES facilities(id),

    INDEX idx_measurements_plant_time (plant_id, measured_at),
    INDEX idx_measurements_facility_time (facility_id, measured_at),
    INDEX idx_measurements_plant_facility_time (plant_id, facility_id, measured_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE energy_summaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    facility_id BIGINT,
    summary_type ENUM('HOURLY', 'DAILY', 'MONTHLY') NOT NULL,
    summary_at DATETIME NOT NULL,
    electricity_kwh DECIMAL(12, 2),
    gas_m3 DECIMAL(12, 2),
    water_ton DECIMAL(12, 2),
    solar_kwh DECIMAL(12, 2),
    peak_kw DECIMAL(12, 2),
    carbon_emission DECIMAL(12, 2),

    CONSTRAINT fk_summaries_plant
        FOREIGN KEY (plant_id) REFERENCES plants(id),

    CONSTRAINT fk_summaries_facility
        FOREIGN KEY (facility_id) REFERENCES facilities(id),

    UNIQUE KEY uk_summary (plant_id, facility_id, summary_type, summary_at),
    INDEX idx_summary_plant_time (plant_id, summary_at),
    INDEX idx_summary_plant_type_time (plant_id, summary_type, summary_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE esg_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    target_month DATE NOT NULL,
    electricity_score DECIMAL(5, 2),
    gas_score DECIMAL(5, 2),
    water_score DECIMAL(5, 2),
    solar_score DECIMAL(5, 2),
    peak_score DECIMAL(5, 2),
    carbon_score DECIMAL(5, 2),
    total_score DECIMAL(5, 2),
    grade ENUM('AAA', 'AA', 'A', 'BBB', 'BB', 'B', 'CCC') NOT NULL,

    CONSTRAINT fk_esg_scores_plant
        FOREIGN KEY (plant_id) REFERENCES plants(id),

    UNIQUE KEY uk_esg_score_month (plant_id, target_month),
    INDEX idx_esg_scores_month_score (target_month, total_score)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE simulation_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plant_id BIGINT NOT NULL,
    base_score DECIMAL(5, 2),
    base_grade VARCHAR(10),
    electricity_reduction_rate DECIMAL(5, 2),
    gas_reduction_rate DECIMAL(5, 2),
    water_reduction_rate DECIMAL(5, 2),
    peak_reduction_rate DECIMAL(5, 2),
    solar_increase_rate DECIMAL(5, 2),
    expected_score DECIMAL(5, 2),
    expected_grade VARCHAR(10),
    analysis_result TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_simulation_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_simulation_plant
        FOREIGN KEY (plant_id) REFERENCES plants(id),

    INDEX idx_simulation_user_created_at (user_id, created_at),
    INDEX idx_simulation_plant_created_at (plant_id, created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE chatbot_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plant_id BIGINT,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    referenced_data JSON,
    chart_spec JSON,
    image_data LONGTEXT,
    external_sources JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_chatbot_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_chatbot_plant
        FOREIGN KEY (plant_id) REFERENCES plants(id),

    INDEX idx_chatbot_user_created_at (user_id, created_at),
    INDEX idx_chatbot_plant_created_at (plant_id, created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

CREATE TABLE alarms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plant_id BIGINT NOT NULL,
    facility_id BIGINT,
    alarm_type ENUM('PEAK', 'ELECTRICITY', 'GAS', 'WATER', 'FACILITY', 'ESG') NOT NULL,
    alarm_level ENUM('INFO', 'WARNING', 'CRITICAL') NOT NULL,
    message VARCHAR(500) NOT NULL,
    value DECIMAL(12, 2),
    threshold_value DECIMAL(12, 2),
    occurred_at DATETIME NOT NULL,
    resolved_at DATETIME,
    status ENUM('OCCURRED', 'RESOLVED') NOT NULL DEFAULT 'OCCURRED',

    CONSTRAINT fk_alarms_plant
        FOREIGN KEY (plant_id) REFERENCES plants(id),

    CONSTRAINT fk_alarms_facility
        FOREIGN KEY (facility_id) REFERENCES facilities(id),

    INDEX idx_alarms_plant_status (plant_id, status),
    INDEX idx_alarms_occurred_at (occurred_at),
    INDEX idx_alarms_level_status (alarm_level, status)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
