USE scada;

SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

SET @summary_from = TIMESTAMP('2026-04-15 00:00:00');
SET @summary_to = TIMESTAMP('2026-05-16 00:00:00');
SET @month_from = TIMESTAMP(DATE_FORMAT(@summary_from, '%Y-%m-01'));
SET @month_to = TIMESTAMP(DATE_FORMAT(@summary_to, '%Y-%m-01')) + INTERVAL 1 MONTH;

DROP TEMPORARY TABLE IF EXISTS tmp_energy_measurements_backfill;
DROP TEMPORARY TABLE IF EXISTS tmp_energy_measurements_backfill_prev;
DROP TEMPORARY TABLE IF EXISTS tmp_monthly_plant_summaries;

CREATE TEMPORARY TABLE tmp_energy_measurements_backfill (
    id BIGINT NOT NULL,
    plant_id BIGINT NOT NULL,
    facility_id BIGINT NOT NULL,
    measured_at DATETIME NOT NULL,
    electricity_kwh DECIMAL(18, 2) NOT NULL,
    gas_m3 DECIMAL(18, 2) NOT NULL,
    water_ton DECIMAL(18, 2) NOT NULL,
    solar_kwh DECIMAL(18, 2) NOT NULL,
    peak_kw DECIMAL(18, 2) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_tmp_energy_backfill_lookup (plant_id, facility_id, measured_at),
    INDEX idx_tmp_energy_backfill_summary_at (measured_at)
) ENGINE=InnoDB;

INSERT INTO tmp_energy_measurements_backfill (
    id,
    plant_id,
    facility_id,
    measured_at,
    electricity_kwh,
    gas_m3,
    water_ton,
    solar_kwh,
    peak_kw
)
SELECT
    id,
    plant_id,
    facility_id,
    measured_at,
    electricity_kwh,
    gas_m3,
    water_ton,
    solar_kwh,
    peak_kw
FROM energy_measurements
WHERE measured_at >= @summary_from
  AND measured_at < @summary_to;

CREATE TEMPORARY TABLE tmp_energy_measurements_backfill_prev (
    id BIGINT NOT NULL,
    plant_id BIGINT NOT NULL,
    facility_id BIGINT NOT NULL,
    measured_at DATETIME NOT NULL,
    electricity_kwh DECIMAL(18, 2) NOT NULL,
    gas_m3 DECIMAL(18, 2) NOT NULL,
    water_ton DECIMAL(18, 2) NOT NULL,
    solar_kwh DECIMAL(18, 2) NOT NULL,
    peak_kw DECIMAL(18, 2) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_tmp_energy_backfill_prev_lookup (plant_id, facility_id, measured_at)
) ENGINE=InnoDB;

INSERT INTO tmp_energy_measurements_backfill_prev
SELECT *
FROM tmp_energy_measurements_backfill;

DELETE FROM energy_summaries
WHERE summary_type IN ('HOURLY', 'DAILY')
  AND summary_at >= @summary_from
  AND summary_at < @summary_to;

DELETE FROM energy_summaries
WHERE summary_type = 'MONTHLY'
  AND summary_at >= @month_from
  AND summary_at < @month_to;

INSERT INTO energy_summaries (
    plant_id,
    facility_id,
    summary_type,
    summary_at,
    electricity_kwh,
    gas_m3,
    water_ton,
    solar_kwh,
    peak_kw,
    carbon_emission
)
SELECT
    current_row.plant_id,
    current_row.facility_id,
    'HOURLY',
    TIMESTAMP(DATE(current_row.measured_at), MAKETIME(HOUR(current_row.measured_at), 0, 0)),
    COALESCE(SUM(
        CASE
            WHEN previous_row.id IS NULL THEN 0
            WHEN current_row.electricity_kwh >= previous_row.electricity_kwh THEN current_row.electricity_kwh - previous_row.electricity_kwh
            ELSE 0
        END
    ), 0),
    COALESCE(SUM(
        CASE
            WHEN previous_row.id IS NULL THEN 0
            WHEN current_row.gas_m3 >= previous_row.gas_m3 THEN current_row.gas_m3 - previous_row.gas_m3
            ELSE 0
        END
    ), 0),
    COALESCE(SUM(
        CASE
            WHEN previous_row.id IS NULL THEN 0
            WHEN current_row.water_ton >= previous_row.water_ton THEN current_row.water_ton - previous_row.water_ton
            ELSE 0
        END
    ), 0),
    COALESCE(SUM(
        CASE
            WHEN previous_row.id IS NULL THEN 0
            WHEN current_row.solar_kwh >= previous_row.solar_kwh THEN current_row.solar_kwh - previous_row.solar_kwh
            ELSE 0
        END
    ), 0),
    COALESCE(MAX(current_row.peak_kw), 0),
    ROUND(COALESCE(SUM(
        CASE
            WHEN previous_row.id IS NULL THEN 0
            WHEN current_row.electricity_kwh >= previous_row.electricity_kwh THEN current_row.electricity_kwh - previous_row.electricity_kwh
            ELSE 0
        END
    ), 0) * 0.47, 2)
FROM tmp_energy_measurements_backfill current_row
LEFT JOIN tmp_energy_measurements_backfill_prev previous_row
  ON previous_row.plant_id = current_row.plant_id
 AND previous_row.facility_id = current_row.facility_id
 AND previous_row.measured_at = current_row.measured_at - INTERVAL 1 HOUR
GROUP BY
    current_row.plant_id,
    current_row.facility_id,
    TIMESTAMP(DATE(current_row.measured_at), MAKETIME(HOUR(current_row.measured_at), 0, 0));

INSERT INTO energy_summaries (
    plant_id,
    facility_id,
    summary_type,
    summary_at,
    electricity_kwh,
    gas_m3,
    water_ton,
    solar_kwh,
    peak_kw,
    carbon_emission
)
SELECT
    plant_id,
    NULL,
    'HOURLY',
    summary_at,
    COALESCE(SUM(electricity_kwh), 0),
    COALESCE(SUM(gas_m3), 0),
    COALESCE(SUM(water_ton), 0),
    COALESCE(SUM(solar_kwh), 0),
    COALESCE(MAX(peak_kw), 0),
    COALESCE(SUM(carbon_emission), 0)
FROM energy_summaries
WHERE summary_type = 'HOURLY'
  AND facility_id IS NOT NULL
  AND summary_at >= @summary_from
  AND summary_at < @summary_to
GROUP BY plant_id, summary_at;

INSERT INTO energy_summaries (
    plant_id,
    facility_id,
    summary_type,
    summary_at,
    electricity_kwh,
    gas_m3,
    water_ton,
    solar_kwh,
    peak_kw,
    carbon_emission
)
SELECT
    plant_id,
    facility_id,
    'DAILY',
    TIMESTAMP(DATE(summary_at)),
    COALESCE(SUM(electricity_kwh), 0),
    COALESCE(SUM(gas_m3), 0),
    COALESCE(SUM(water_ton), 0),
    COALESCE(SUM(solar_kwh), 0),
    COALESCE(MAX(peak_kw), 0),
    COALESCE(SUM(carbon_emission), 0)
FROM energy_summaries
WHERE summary_type = 'HOURLY'
  AND facility_id IS NOT NULL
  AND summary_at >= @summary_from
  AND summary_at < @summary_to
GROUP BY plant_id, facility_id, TIMESTAMP(DATE(summary_at));

INSERT INTO energy_summaries (
    plant_id,
    facility_id,
    summary_type,
    summary_at,
    electricity_kwh,
    gas_m3,
    water_ton,
    solar_kwh,
    peak_kw,
    carbon_emission
)
SELECT
    plant_id,
    NULL,
    'DAILY',
    summary_at,
    COALESCE(SUM(electricity_kwh), 0),
    COALESCE(SUM(gas_m3), 0),
    COALESCE(SUM(water_ton), 0),
    COALESCE(SUM(solar_kwh), 0),
    COALESCE(MAX(peak_kw), 0),
    COALESCE(SUM(carbon_emission), 0)
FROM energy_summaries
WHERE summary_type = 'DAILY'
  AND facility_id IS NOT NULL
  AND summary_at >= @summary_from
  AND summary_at < @summary_to
GROUP BY plant_id, summary_at;

INSERT INTO energy_summaries (
    plant_id,
    facility_id,
    summary_type,
    summary_at,
    electricity_kwh,
    gas_m3,
    water_ton,
    solar_kwh,
    peak_kw,
    carbon_emission
)
SELECT
    plant_id,
    facility_id,
    'MONTHLY',
    TIMESTAMP(DATE_FORMAT(summary_at, '%Y-%m-01')),
    COALESCE(SUM(electricity_kwh), 0),
    COALESCE(SUM(gas_m3), 0),
    COALESCE(SUM(water_ton), 0),
    COALESCE(SUM(solar_kwh), 0),
    COALESCE(MAX(peak_kw), 0),
    COALESCE(SUM(carbon_emission), 0)
FROM energy_summaries
WHERE summary_type = 'DAILY'
  AND facility_id IS NOT NULL
  AND summary_at >= @summary_from
  AND summary_at < @summary_to
GROUP BY plant_id, facility_id, TIMESTAMP(DATE_FORMAT(summary_at, '%Y-%m-01'));

CREATE TEMPORARY TABLE tmp_monthly_plant_summaries (
    id BIGINT NOT NULL AUTO_INCREMENT,
    plant_id BIGINT NOT NULL,
    facility_id BIGINT NULL,
    summary_type VARCHAR(20) NOT NULL,
    summary_at DATETIME NOT NULL,
    electricity_kwh DECIMAL(18, 2) NOT NULL,
    gas_m3 DECIMAL(18, 2) NOT NULL,
    water_ton DECIMAL(18, 2) NOT NULL,
    solar_kwh DECIMAL(18, 2) NOT NULL,
    peak_kw DECIMAL(18, 2) NOT NULL,
    carbon_emission DECIMAL(18, 2) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

INSERT INTO tmp_monthly_plant_summaries (
    plant_id,
    facility_id,
    summary_type,
    summary_at,
    electricity_kwh,
    gas_m3,
    water_ton,
    solar_kwh,
    peak_kw,
    carbon_emission
)
SELECT
    plant_id,
    NULL AS facility_id,
    'MONTHLY' AS summary_type,
    summary_at,
    COALESCE(SUM(electricity_kwh), 0) AS electricity_kwh,
    COALESCE(SUM(gas_m3), 0) AS gas_m3,
    COALESCE(SUM(water_ton), 0) AS water_ton,
    COALESCE(SUM(solar_kwh), 0) AS solar_kwh,
    COALESCE(MAX(peak_kw), 0) AS peak_kw,
    COALESCE(SUM(carbon_emission), 0) AS carbon_emission
FROM energy_summaries
WHERE summary_type = 'MONTHLY'
  AND facility_id IS NOT NULL
  AND summary_at >= @month_from
  AND summary_at < @month_to
GROUP BY plant_id, summary_at;

INSERT INTO energy_summaries (
    plant_id,
    facility_id,
    summary_type,
    summary_at,
    electricity_kwh,
    gas_m3,
    water_ton,
    solar_kwh,
    peak_kw,
    carbon_emission
)
SELECT
    plant_id,
    facility_id,
    summary_type,
    summary_at,
    electricity_kwh,
    gas_m3,
    water_ton,
    solar_kwh,
    peak_kw,
    carbon_emission
FROM tmp_monthly_plant_summaries;

DROP TEMPORARY TABLE IF EXISTS tmp_energy_measurements_backfill;
DROP TEMPORARY TABLE IF EXISTS tmp_energy_measurements_backfill_prev;
DROP TEMPORARY TABLE IF EXISTS tmp_monthly_plant_summaries;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;
