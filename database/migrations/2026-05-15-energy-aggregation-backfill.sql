-- Optional one-time backfill for the aggregation tables from existing raw measurements.
-- Run this after 2026-05-15-energy-aggregation-tables.sql if raw data already exists.

INSERT INTO energy_latest_measurements (
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
    ranked.plant_id,
    ranked.facility_id,
    ranked.measured_at,
    ranked.electricity_kwh,
    ranked.gas_m3,
    ranked.water_ton,
    ranked.solar_kwh,
    ranked.peak_kw
FROM (
    SELECT
        em.*,
        ROW_NUMBER() OVER (
            PARTITION BY em.plant_id, em.facility_id
            ORDER BY em.measured_at DESC, em.id DESC
        ) AS rn
    FROM energy_measurements em
) ranked
WHERE ranked.rn = 1
ON DUPLICATE KEY UPDATE
    electricity_kwh = IF(
        VALUES(measured_at) >= energy_latest_measurements.measured_at,
        VALUES(electricity_kwh),
        energy_latest_measurements.electricity_kwh
    ),
    gas_m3 = IF(
        VALUES(measured_at) >= energy_latest_measurements.measured_at,
        VALUES(gas_m3),
        energy_latest_measurements.gas_m3
    ),
    water_ton = IF(
        VALUES(measured_at) >= energy_latest_measurements.measured_at,
        VALUES(water_ton),
        energy_latest_measurements.water_ton
    ),
    solar_kwh = IF(
        VALUES(measured_at) >= energy_latest_measurements.measured_at,
        VALUES(solar_kwh),
        energy_latest_measurements.solar_kwh
    ),
    peak_kw = IF(
        VALUES(measured_at) >= energy_latest_measurements.measured_at,
        VALUES(peak_kw),
        energy_latest_measurements.peak_kw
    ),
    measured_at = IF(
        VALUES(measured_at) >= energy_latest_measurements.measured_at,
        VALUES(measured_at),
        energy_latest_measurements.measured_at
    );

INSERT INTO energy_interval_summaries (
    plant_id,
    facility_id,
    bucket_type,
    bucket_at,
    electricity_usage_kwh,
    gas_usage_m3,
    water_usage_ton,
    solar_usage_kwh,
    peak_average_kw,
    peak_kw,
    peak_sum_kw,
    peak_sample_count,
    first_measured_at,
    last_measured_at,
    measurement_count
)
WITH ordered_measurements AS (
    SELECT
        em.*,
        LAG(em.electricity_kwh) OVER (
            PARTITION BY em.plant_id, em.facility_id
            ORDER BY em.measured_at ASC, em.id ASC
        ) AS previous_electricity_kwh,
        LAG(em.gas_m3) OVER (
            PARTITION BY em.plant_id, em.facility_id
            ORDER BY em.measured_at ASC, em.id ASC
        ) AS previous_gas_m3,
        LAG(em.water_ton) OVER (
            PARTITION BY em.plant_id, em.facility_id
            ORDER BY em.measured_at ASC, em.id ASC
        ) AS previous_water_ton,
        LAG(em.solar_kwh) OVER (
            PARTITION BY em.plant_id, em.facility_id
            ORDER BY em.measured_at ASC, em.id ASC
        ) AS previous_solar_kwh
    FROM energy_measurements em
),
measurement_deltas AS (
    SELECT
        plant_id,
        facility_id,
        measured_at,
        CASE
            WHEN previous_electricity_kwh IS NULL THEN 0
            WHEN electricity_kwh >= previous_electricity_kwh THEN electricity_kwh - previous_electricity_kwh
            ELSE 0
        END AS electricity_usage_kwh,
        CASE
            WHEN previous_gas_m3 IS NULL THEN 0
            WHEN gas_m3 >= previous_gas_m3 THEN gas_m3 - previous_gas_m3
            ELSE 0
        END AS gas_usage_m3,
        CASE
            WHEN previous_water_ton IS NULL THEN 0
            WHEN water_ton >= previous_water_ton THEN water_ton - previous_water_ton
            ELSE 0
        END AS water_usage_ton,
        CASE
            WHEN previous_solar_kwh IS NULL THEN 0
            WHEN solar_kwh >= previous_solar_kwh THEN solar_kwh - previous_solar_kwh
            ELSE 0
        END AS solar_usage_kwh,
        COALESCE(peak_kw, 0) AS peak_kw
    FROM ordered_measurements
),
bucketed_measurements AS (
    SELECT
        plant_id,
        facility_id,
        'FIFTEEN_MINUTES' AS bucket_type,
        FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(measured_at) / 900) * 900) AS bucket_at,
        electricity_usage_kwh,
        gas_usage_m3,
        water_usage_ton,
        solar_usage_kwh,
        peak_kw,
        measured_at
    FROM measurement_deltas
    UNION ALL
    SELECT
        plant_id,
        facility_id,
        'HOURLY' AS bucket_type,
        TIMESTAMP(DATE(measured_at), MAKETIME(HOUR(measured_at), 0, 0)) AS bucket_at,
        electricity_usage_kwh,
        gas_usage_m3,
        water_usage_ton,
        solar_usage_kwh,
        peak_kw,
        measured_at
    FROM measurement_deltas
    UNION ALL
    SELECT
        plant_id,
        facility_id,
        'DAILY' AS bucket_type,
        CAST(DATE(measured_at) AS DATETIME) AS bucket_at,
        electricity_usage_kwh,
        gas_usage_m3,
        water_usage_ton,
        solar_usage_kwh,
        peak_kw,
        measured_at
    FROM measurement_deltas
)
SELECT
    plant_id,
    facility_id,
    bucket_type,
    bucket_at,
    ROUND(COALESCE(SUM(electricity_usage_kwh), 0), 2) AS electricity_usage_kwh,
    ROUND(COALESCE(SUM(gas_usage_m3), 0), 2) AS gas_usage_m3,
    ROUND(COALESCE(SUM(water_usage_ton), 0), 2) AS water_usage_ton,
    ROUND(COALESCE(SUM(solar_usage_kwh), 0), 2) AS solar_usage_kwh,
    ROUND(COALESCE(AVG(peak_kw), 0), 2) AS peak_average_kw,
    ROUND(COALESCE(MAX(peak_kw), 0), 2) AS peak_kw,
    ROUND(COALESCE(SUM(peak_kw), 0), 2) AS peak_sum_kw,
    COUNT(*) AS peak_sample_count,
    MIN(measured_at) AS first_measured_at,
    MAX(measured_at) AS last_measured_at,
    COUNT(*) AS measurement_count
FROM bucketed_measurements
GROUP BY plant_id, facility_id, bucket_type, bucket_at
ON DUPLICATE KEY UPDATE
    electricity_usage_kwh = VALUES(electricity_usage_kwh),
    gas_usage_m3 = VALUES(gas_usage_m3),
    water_usage_ton = VALUES(water_usage_ton),
    solar_usage_kwh = VALUES(solar_usage_kwh),
    peak_average_kw = VALUES(peak_average_kw),
    peak_kw = VALUES(peak_kw),
    peak_sum_kw = VALUES(peak_sum_kw),
    peak_sample_count = VALUES(peak_sample_count),
    first_measured_at = VALUES(first_measured_at),
    last_measured_at = VALUES(last_measured_at),
    measurement_count = VALUES(measurement_count);
