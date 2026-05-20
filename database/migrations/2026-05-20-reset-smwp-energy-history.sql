USE scada;

SET SQL_SAFE_UPDATES = 0;
SET SESSION cte_max_recursion_depth = 2000;

INSERT INTO facilities (id, plant_id, name, facility_type, status)
SELECT
    p.id * 10000 + e.seq AS id,
    p.id AS plant_id,
    CONCAT('F-', LPAD(e.seq, 3, '0')) AS name,
    e.facility_type,
    CASE
        WHEN e.seq IN (6, 12, 18, 24) THEN 'WARNING'
        ELSE 'RUNNING'
    END AS status
FROM plants p
JOIN (
    SELECT 1 AS seq, 'PRESS' AS facility_type
    UNION ALL SELECT 2, 'PRESS'
    UNION ALL SELECT 3, 'PRESS'
    UNION ALL SELECT 4, 'PRESS'
    UNION ALL SELECT 5, 'PRESS'
    UNION ALL SELECT 6, 'PRESS'
    UNION ALL SELECT 7, 'BODY'
    UNION ALL SELECT 8, 'BODY'
    UNION ALL SELECT 9, 'BODY'
    UNION ALL SELECT 10, 'BODY'
    UNION ALL SELECT 11, 'BODY'
    UNION ALL SELECT 12, 'BODY'
    UNION ALL SELECT 13, 'ASSEMBLY'
    UNION ALL SELECT 14, 'ASSEMBLY'
    UNION ALL SELECT 15, 'ASSEMBLY'
    UNION ALL SELECT 16, 'ASSEMBLY'
    UNION ALL SELECT 17, 'ASSEMBLY'
    UNION ALL SELECT 18, 'ASSEMBLY'
    UNION ALL SELECT 19, 'PAINT'
    UNION ALL SELECT 20, 'PAINT'
    UNION ALL SELECT 21, 'PAINT'
    UNION ALL SELECT 22, 'PAINT'
    UNION ALL SELECT 23, 'PAINT'
    UNION ALL SELECT 24, 'PAINT'
) e
WHERE NOT EXISTS (
    SELECT 1
    FROM facilities f
    WHERE f.id = p.id * 10000 + e.seq
);

DELETE FROM energy_summaries;
DELETE FROM energy_measurements;

ALTER TABLE energy_summaries AUTO_INCREMENT = 1;
ALTER TABLE energy_measurements AUTO_INCREMENT = 1;

DROP TEMPORARY TABLE IF EXISTS tmp_smwp_hours;
CREATE TEMPORARY TABLE tmp_smwp_hours (
    hour_at DATETIME NOT NULL PRIMARY KEY
) ENGINE=InnoDB;

INSERT INTO tmp_smwp_hours (hour_at)
WITH RECURSIVE hours AS (
    SELECT TIMESTAMP('2026-04-01 00:00:00') AS hour_at
    UNION ALL
    SELECT hour_at + INTERVAL 1 HOUR
    FROM hours
    WHERE hour_at < TIMESTAMP('2026-05-20 11:00:00')
)
SELECT hour_at
FROM hours;

DROP TEMPORARY TABLE IF EXISTS tmp_smwp_facilities;
CREATE TEMPORARY TABLE tmp_smwp_facilities (
    plant_id BIGINT NOT NULL,
    facility_id BIGINT NOT NULL,
    equipment_seq INT NOT NULL,
    PRIMARY KEY (plant_id, facility_id)
) ENGINE=InnoDB;

INSERT INTO tmp_smwp_facilities (plant_id, facility_id, equipment_seq)
SELECT
    plant_id,
    id AS facility_id,
    MOD(id, 10000) AS equipment_seq
FROM facilities
WHERE id >= 10000
  AND MOD(id, 10000) BETWEEN 1 AND 24;

DROP TEMPORARY TABLE IF EXISTS tmp_smwp_hourly_usage;
CREATE TEMPORARY TABLE tmp_smwp_hourly_usage (
    plant_id BIGINT NOT NULL,
    facility_id BIGINT NOT NULL,
    hour_at DATETIME NOT NULL,
    electricity_kwh DECIMAL(12, 4) NOT NULL,
    gas_m3 DECIMAL(12, 4) NOT NULL,
    water_ton DECIMAL(12, 4) NOT NULL,
    solar_kwh DECIMAL(12, 4) NOT NULL,
    peak_kw DECIMAL(12, 4) NOT NULL,
    PRIMARY KEY (plant_id, facility_id, hour_at)
) ENGINE=InnoDB;

INSERT INTO tmp_smwp_hourly_usage (
    plant_id,
    facility_id,
    hour_at,
    electricity_kwh,
    gas_m3,
    water_ton,
    solar_kwh,
    peak_kw
)
SELECT
    f.plant_id,
    f.facility_id,
    h.hour_at,
    ROUND(
        (30.0 + f.plant_id * 1.85 + f.equipment_seq * 0.82)
        * CASE
            WHEN HOUR(h.hour_at) BETWEEN 8 AND 18 THEN 1.18
            WHEN HOUR(h.hour_at) BETWEEN 6 AND 7 THEN 0.82
            WHEN HOUR(h.hour_at) BETWEEN 19 AND 21 THEN 0.72
            ELSE 0.38
          END
        * CASE
            WHEN DAYOFWEEK(h.hour_at) IN (1, 7) THEN 0.72
            ELSE 1.0
          END
        * (0.94 + MOD(DAYOFYEAR(h.hour_at) + f.plant_id + f.equipment_seq, 17) * 0.008),
        4
    ) AS electricity_kwh,
    ROUND(
        (30.0 + f.plant_id * 1.85 + f.equipment_seq * 0.82)
        * CASE
            WHEN HOUR(h.hour_at) BETWEEN 8 AND 18 THEN 1.18
            WHEN HOUR(h.hour_at) BETWEEN 6 AND 7 THEN 0.82
            WHEN HOUR(h.hour_at) BETWEEN 19 AND 21 THEN 0.72
            ELSE 0.38
          END
        * CASE
            WHEN DAYOFWEEK(h.hour_at) IN (1, 7) THEN 0.72
            ELSE 1.0
          END
        * (0.94 + MOD(DAYOFYEAR(h.hour_at) + f.plant_id + f.equipment_seq, 17) * 0.008)
        * (0.071 + MOD(f.equipment_seq, 5) * 0.003),
        4
    ) AS gas_m3,
    ROUND(
        (30.0 + f.plant_id * 1.85 + f.equipment_seq * 0.82)
        * CASE
            WHEN HOUR(h.hour_at) BETWEEN 8 AND 18 THEN 1.18
            WHEN HOUR(h.hour_at) BETWEEN 6 AND 7 THEN 0.82
            WHEN HOUR(h.hour_at) BETWEEN 19 AND 21 THEN 0.72
            ELSE 0.38
          END
        * CASE
            WHEN DAYOFWEEK(h.hour_at) IN (1, 7) THEN 0.72
            ELSE 1.0
          END
        * (0.94 + MOD(DAYOFYEAR(h.hour_at) + f.plant_id + f.equipment_seq, 17) * 0.008)
        * (0.0058 + MOD(f.equipment_seq, 4) * 0.0004),
        4
    ) AS water_ton,
    ROUND(
        CASE
            WHEN HOUR(h.hour_at) BETWEEN 7 AND 18 THEN
                (9.5 + f.plant_id * 0.78 + MOD(f.equipment_seq, 6) * 0.54)
                * SIN(((HOUR(h.hour_at) - 6) / 13.0) * PI())
                * CASE
                    WHEN DAYOFWEEK(h.hour_at) IN (1, 7) THEN 0.82
                    ELSE 1.0
                  END
                * (0.90 + MOD(DAYOFYEAR(h.hour_at) + f.plant_id, 13) * 0.01)
            ELSE 0
        END,
        4
    ) AS solar_kwh,
    ROUND(
        (285.0 + f.plant_id * 7.5 + f.equipment_seq * 3.4)
        * CASE
            WHEN HOUR(h.hour_at) BETWEEN 8 AND 18 THEN
                0.92 + SIN(((HOUR(h.hour_at) - 7) / 12.0) * PI()) * 0.18
            WHEN HOUR(h.hour_at) BETWEEN 6 AND 7 THEN 0.74
            WHEN HOUR(h.hour_at) BETWEEN 19 AND 21 THEN 0.68
            ELSE 0.46
          END
        * CASE
            WHEN DAYOFWEEK(h.hour_at) IN (1, 7) THEN 0.91
            ELSE 1.0
          END
        * (0.96 + MOD(DAYOFYEAR(h.hour_at) + f.plant_id * 3 + f.equipment_seq, 19) * 0.004)
        * (0.98 + MOD(f.plant_id * 5 + f.equipment_seq * 3 + HOUR(h.hour_at), 11) * 0.006),
        4
    ) AS peak_kw
FROM tmp_smwp_facilities f
CROSS JOIN tmp_smwp_hours h;

INSERT INTO energy_measurements (
    plant_id,
    facility_id,
    measured_at,
    electricity_kwh,
    gas_m3,
    water_ton,
    solar_kwh,
    peak_kw,
    created_at
)
SELECT
    usage_with_total.plant_id,
    usage_with_total.facility_id,
    usage_with_total.hour_at + INTERVAL sample.sample_minute MINUTE AS measured_at,
    ROUND(
        usage_with_total.base_electricity
        + usage_with_total.previous_electricity_kwh
        + usage_with_total.electricity_kwh * sample.sample_ratio,
        2
    ) AS electricity_kwh,
    ROUND(
        usage_with_total.base_gas
        + usage_with_total.previous_gas_m3
        + usage_with_total.gas_m3 * sample.sample_ratio,
        2
    ) AS gas_m3,
    ROUND(
        usage_with_total.base_water
        + usage_with_total.previous_water_ton
        + usage_with_total.water_ton * sample.sample_ratio,
        2
    ) AS water_ton,
    ROUND(
        usage_with_total.base_solar
        + usage_with_total.previous_solar_kwh
        + usage_with_total.solar_kwh * sample.sample_ratio,
        2
    ) AS solar_kwh,
    ROUND(usage_with_total.peak_kw, 2) AS peak_kw,
    usage_with_total.hour_at + INTERVAL sample.sample_minute MINUTE AS created_at
FROM (
    SELECT
        u.*,
        8200 + u.plant_id * 650 + MOD(u.facility_id, 10000) * 34 AS base_electricity,
        1800 + u.plant_id * 120 + MOD(u.facility_id, 10000) * 7 AS base_gas,
        95 + u.plant_id * 8 + MOD(u.facility_id, 10000) * 0.55 AS base_water,
        980 + u.plant_id * 90 + MOD(u.facility_id, 10000) * 6 AS base_solar,
        COALESCE(
            SUM(u.electricity_kwh) OVER (
                PARTITION BY u.facility_id
                ORDER BY u.hour_at
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ),
            0
        ) AS previous_electricity_kwh,
        COALESCE(
            SUM(u.gas_m3) OVER (
                PARTITION BY u.facility_id
                ORDER BY u.hour_at
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ),
            0
        ) AS previous_gas_m3,
        COALESCE(
            SUM(u.water_ton) OVER (
                PARTITION BY u.facility_id
                ORDER BY u.hour_at
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ),
            0
        ) AS previous_water_ton,
        COALESCE(
            SUM(u.solar_kwh) OVER (
                PARTITION BY u.facility_id
                ORDER BY u.hour_at
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ),
            0
        ) AS previous_solar_kwh
    FROM tmp_smwp_hourly_usage u
    WHERE u.hour_at >= TIMESTAMP('2026-05-20 00:00:00')
      AND u.hour_at < TIMESTAMP('2026-05-20 12:00:00')
) usage_with_total
JOIN (
    SELECT 0 AS sample_minute, 0.0000 AS sample_ratio
    UNION ALL SELECT 10, 10 / 59.0
    UNION ALL SELECT 20, 20 / 59.0
    UNION ALL SELECT 30, 30 / 59.0
    UNION ALL SELECT 40, 40 / 59.0
    UNION ALL SELECT 50, 50 / 59.0
    UNION ALL SELECT 59, 1.0000
) sample
ORDER BY
    usage_with_total.plant_id,
    usage_with_total.facility_id,
    measured_at;

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
    'HOURLY',
    hour_at,
    ROUND(electricity_kwh, 2),
    ROUND(gas_m3, 2),
    ROUND(water_ton, 2),
    ROUND(solar_kwh, 2),
    ROUND(peak_kw, 2),
    ROUND(electricity_kwh * 0.424 + gas_m3 * 2.15, 2)
FROM tmp_smwp_hourly_usage;

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
    hour_at,
    ROUND(SUM(electricity_kwh), 2),
    ROUND(SUM(gas_m3), 2),
    ROUND(SUM(water_ton), 2),
    ROUND(SUM(solar_kwh), 2),
    ROUND(MAX(peak_kw), 2),
    ROUND(SUM(electricity_kwh) * 0.424 + SUM(gas_m3) * 2.15, 2)
FROM tmp_smwp_hourly_usage
GROUP BY plant_id, hour_at;

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
    DATE(hour_at),
    ROUND(SUM(electricity_kwh), 2),
    ROUND(SUM(gas_m3), 2),
    ROUND(SUM(water_ton), 2),
    ROUND(SUM(solar_kwh), 2),
    ROUND(MAX(peak_kw), 2),
    ROUND(SUM(electricity_kwh) * 0.424 + SUM(gas_m3) * 2.15, 2)
FROM tmp_smwp_hourly_usage
GROUP BY plant_id, facility_id, DATE(hour_at);

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
    DATE(hour_at),
    ROUND(SUM(electricity_kwh), 2),
    ROUND(SUM(gas_m3), 2),
    ROUND(SUM(water_ton), 2),
    ROUND(SUM(solar_kwh), 2),
    ROUND(MAX(peak_kw), 2),
    ROUND(SUM(electricity_kwh) * 0.424 + SUM(gas_m3) * 2.15, 2)
FROM tmp_smwp_hourly_usage
GROUP BY plant_id, DATE(hour_at);

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
    DATE_FORMAT(hour_at, '%Y-%m-01'),
    ROUND(SUM(electricity_kwh), 2),
    ROUND(SUM(gas_m3), 2),
    ROUND(SUM(water_ton), 2),
    ROUND(SUM(solar_kwh), 2),
    ROUND(MAX(peak_kw), 2),
    ROUND(SUM(electricity_kwh) * 0.424 + SUM(gas_m3) * 2.15, 2)
FROM tmp_smwp_hourly_usage
GROUP BY plant_id, facility_id, DATE_FORMAT(hour_at, '%Y-%m-01');

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
    'MONTHLY',
    DATE_FORMAT(hour_at, '%Y-%m-01'),
    ROUND(SUM(electricity_kwh), 2),
    ROUND(SUM(gas_m3), 2),
    ROUND(SUM(water_ton), 2),
    ROUND(SUM(solar_kwh), 2),
    ROUND(MAX(peak_kw), 2),
    ROUND(SUM(electricity_kwh) * 0.424 + SUM(gas_m3) * 2.15, 2)
FROM tmp_smwp_hourly_usage
GROUP BY plant_id, DATE_FORMAT(hour_at, '%Y-%m-01');

DROP TEMPORARY TABLE IF EXISTS tmp_smwp_hourly_usage;
DROP TEMPORARY TABLE IF EXISTS tmp_smwp_facilities;
DROP TEMPORARY TABLE IF EXISTS tmp_smwp_hours;

SET SQL_SAFE_UPDATES = 1;
