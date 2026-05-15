-- Store the latest measurement per facility so dashboards do not scan raw measurements.
CREATE TABLE IF NOT EXISTS energy_latest_measurements (
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
    INDEX idx_energy_latest_facility_measured_at (facility_id, measured_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- Store pre-aggregated energy usage for fast peak, utility, and ESG dashboards.
CREATE TABLE IF NOT EXISTS energy_interval_summaries (
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
    INDEX idx_energy_interval_type_time (bucket_type, bucket_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
