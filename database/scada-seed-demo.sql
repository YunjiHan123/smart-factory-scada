-- Smart Factory SCADA demo data.
-- Run after database/scada-schema.sql.
-- Demo password for seeded users: Password123!

USE scada;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE chatbot_messages;
TRUNCATE TABLE simulation_results;
TRUNCATE TABLE alarms;
TRUNCATE TABLE esg_scores;
TRUNCATE TABLE energy_interval_summaries;
TRUNCATE TABLE energy_latest_measurements;
TRUNCATE TABLE energy_summaries;
TRUNCATE TABLE energy_measurements;
TRUNCATE TABLE facilities;
TRUNCATE TABLE users;
TRUNCATE TABLE plants;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO plants (id, name, company_type, address, latitude, longitude) VALUES
    (1, 'Kia Hwaseong Plant', 'KIA', '95 Kia Motors-ro, Hwaseong-si, Gyeonggi-do', 37.0215590, 126.7831110),
    (2, 'Kia Gwangmyeong Plant', 'KIA', '113 Kia-ro, Gwangmyeong-si, Gyeonggi-do', 37.4302030, 126.8789450),
    (3, 'Kia Gwangju Plant', 'KIA', '277 Hwaseong-ro, Seo-gu, Gwangju', 35.1601080, 126.8826180),
    (4, 'Hyundai Ulsan Plant', 'HYUNDAI', '700 Yeompo-ro, Buk-gu, Ulsan', 35.5383770, 129.3765130),
    (5, 'Hyundai Asan Plant', 'HYUNDAI', '1077 Hyundai-ro, Asan-si, Chungcheongnam-do', 36.8385080, 126.8815930),
    (6, 'Hyundai Jeonju Plant', 'HYUNDAI', '163 Jeonju Industrial Complex 5-ro, Jeonju-si', 35.9565430, 127.1345060);

INSERT INTO users (
    id, email, password_hash, name, phone, role, plant_id, status, note, last_login_at
) VALUES
    (1001, 'admin@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', 'System Admin', '010-1000-0001', 'ADMIN', NULL, 'ACTIVE', 'Global administrator account', NOW() - INTERVAL 10 MINUTE),
    (1002, 'manager.hwaseong@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', 'Hwaseong Manager', '010-1000-0002', 'MANAGER', 1, 'ACTIVE', 'Energy manager for Kia Hwaseong', NOW() - INTERVAL 25 MINUTE),
    (1003, 'operator.gwangmyeong@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', 'Gwangmyeong Operator', '010-1000-0003', 'OPERATOR', 2, 'ACTIVE', 'Line operator for Kia Gwangmyeong', NOW() - INTERVAL 1 HOUR),
    (1004, 'viewer.ulsan@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', 'Ulsan Viewer', '010-1000-0004', 'VIEWER', 4, 'ACTIVE', 'Read-only user for Hyundai Ulsan', NOW() - INTERVAL 2 HOUR),
    (1005, 'locked@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', 'Locked User', '010-1000-0005', 'VIEWER', 1, 'LOCKED', 'Locked test account', NULL),
    (1006, 'inactive@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', 'Inactive User', '010-1000-0006', 'OPERATOR', 3, 'INACTIVE', 'Inactive test account', NULL);

INSERT INTO facilities (id, plant_id, name, facility_type, status) VALUES
    (101, 1, 'Hwaseong Press Line 1', 'PRESS', 'RUNNING'),
    (102, 1, 'Hwaseong Body Line 2', 'BODY', 'RUNNING'),
    (103, 1, 'Hwaseong Paint Booth A', 'PAINT', 'WARNING'),
    (104, 1, 'Hwaseong Assembly Line 3', 'ASSEMBLY', 'RUNNING'),
    (105, 1, 'Hwaseong HVAC System', 'HVAC', 'RUNNING'),
    (201, 2, 'Gwangmyeong Press Line 1', 'PRESS', 'RUNNING'),
    (202, 2, 'Gwangmyeong Body Welding', 'BODY', 'RUNNING'),
    (203, 2, 'Gwangmyeong Paint Oven', 'PAINT', 'RUNNING'),
    (204, 2, 'Gwangmyeong Assembly Line 1', 'ASSEMBLY', 'WARNING'),
    (205, 2, 'Gwangmyeong Inspection Line', 'INSPECTION', 'RUNNING'),
    (301, 3, 'Gwangju Press Line 2', 'PRESS', 'RUNNING'),
    (302, 3, 'Gwangju Body Line 1', 'BODY', 'RUNNING'),
    (303, 3, 'Gwangju Paint Booth B', 'PAINT', 'RUNNING'),
    (304, 3, 'Gwangju Assembly Line 2', 'ASSEMBLY', 'RUNNING'),
    (305, 3, 'Gwangju Utility Center', 'ETC', 'WARNING'),
    (401, 4, 'Ulsan Press Plant 3', 'PRESS', 'RUNNING'),
    (402, 4, 'Ulsan Body Line 5', 'BODY', 'WARNING'),
    (403, 4, 'Ulsan Paint Plant 2', 'PAINT', 'RUNNING'),
    (404, 4, 'Ulsan Assembly Line 4', 'ASSEMBLY', 'RUNNING'),
    (405, 4, 'Ulsan Transformer Room', 'ETC', 'STOPPED'),
    (501, 5, 'Asan Press Line', 'PRESS', 'RUNNING'),
    (502, 5, 'Asan Body Line', 'BODY', 'RUNNING'),
    (503, 5, 'Asan Paint Line', 'PAINT', 'RUNNING'),
    (504, 5, 'Asan Assembly Line', 'ASSEMBLY', 'RUNNING'),
    (505, 5, 'Asan HVAC System', 'HVAC', 'RUNNING'),
    (601, 6, 'Jeonju Press Line', 'PRESS', 'RUNNING'),
    (602, 6, 'Jeonju Body Line', 'BODY', 'RUNNING'),
    (603, 6, 'Jeonju Paint Line', 'PAINT', 'WARNING'),
    (604, 6, 'Jeonju Assembly Line', 'ASSEMBLY', 'RUNNING'),
    (605, 6, 'Jeonju Final Inspection', 'INSPECTION', 'RUNNING');

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
    generated.plant_id,
    generated.facility_id,
    generated.measured_at,
    generated.electricity_kwh,
    generated.gas_m3,
    generated.water_ton,
    generated.solar_kwh,
    generated.peak_kw,
    DATE_ADD(generated.measured_at, INTERVAL 5 SECOND)
FROM (
    SELECT
        f.plant_id,
        f.id AS facility_id,
        TIMESTAMP(DATE_ADD('2026-05-14', INTERVAL d.day_offset DAY), MAKETIME(h.hour_value, 0, 0)) AS measured_at,
        CAST(9000 + f.id * 9 + d.day_offset * 1200 + h.slot_index * (80 + f.plant_id * 5) AS DECIMAL(15, 2)) AS electricity_kwh,
        CAST(1500 + f.id * 2.5 + d.day_offset * 260 + h.slot_index * (18 + f.plant_id) AS DECIMAL(15, 2)) AS gas_m3,
        CAST(90 + f.id * 0.18 + d.day_offset * 22 + h.slot_index * (1.4 + f.plant_id * 0.1) AS DECIMAL(15, 2)) AS water_ton,
        CAST(
            CASE
                WHEN f.facility_type = 'HVAC' THEN 1500 + d.day_offset * 350 + h.slot_index * 90
                ELSE 0
            END
            AS DECIMAL(15, 2)
        ) AS solar_kwh,
        CAST(220 + f.plant_id * 30 + MOD(f.id, 100) * 18 + h.slot_index * 12 AS DECIMAL(15, 2)) AS peak_kw
    FROM facilities f
    JOIN (
        SELECT 0 AS day_offset
        UNION ALL SELECT 1
    ) d
    JOIN (
        SELECT 0 AS slot_index, 8 AS hour_value
        UNION ALL SELECT 1, 9
        UNION ALL SELECT 2, 10
        UNION ALL SELECT 3, 11
        UNION ALL SELECT 4, 12
        UNION ALL SELECT 5, 13
        UNION ALL SELECT 6, 14
        UNION ALL SELECT 7, 15
    ) h
) generated;

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
WHERE ranked.rn = 1;

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
    ROUND(COALESCE(SUM(electricity_usage_kwh), 0), 2),
    ROUND(COALESCE(SUM(gas_usage_m3), 0), 2),
    ROUND(COALESCE(SUM(water_usage_ton), 0), 2),
    ROUND(COALESCE(SUM(solar_usage_kwh), 0), 2),
    ROUND(COALESCE(AVG(peak_kw), 0), 2),
    ROUND(COALESCE(MAX(peak_kw), 0), 2),
    ROUND(COALESCE(SUM(peak_kw), 0), 2),
    COUNT(*),
    MIN(measured_at),
    MAX(measured_at),
    COUNT(*)
FROM bucketed_measurements
GROUP BY plant_id, facility_id, bucket_type, bucket_at;

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
    bucket_at,
    electricity_usage_kwh,
    gas_usage_m3,
    water_usage_ton,
    solar_usage_kwh,
    peak_kw,
    ROUND(electricity_usage_kwh * 0.47, 2)
FROM energy_interval_summaries
WHERE bucket_type = 'DAILY';

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
    ROUND(SUM(electricity_kwh), 2),
    ROUND(SUM(gas_m3), 2),
    ROUND(SUM(water_ton), 2),
    ROUND(SUM(solar_kwh), 2),
    ROUND(MAX(peak_kw), 2),
    ROUND(SUM(carbon_emission), 2)
FROM energy_summaries
WHERE summary_type = 'DAILY'
  AND facility_id IS NOT NULL
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
    NULL,
    'HOURLY',
    bucket_at,
    ROUND(SUM(electricity_usage_kwh), 2),
    ROUND(SUM(gas_usage_m3), 2),
    ROUND(SUM(water_usage_ton), 2),
    ROUND(SUM(solar_usage_kwh), 2),
    ROUND(MAX(peak_kw), 2),
    ROUND(SUM(electricity_usage_kwh) * 0.47, 2)
FROM energy_interval_summaries
WHERE bucket_type = 'HOURLY'
GROUP BY plant_id, bucket_at;

INSERT INTO esg_scores (
    plant_id,
    target_month,
    electricity_score,
    gas_score,
    water_score,
    solar_score,
    peak_score,
    carbon_score,
    total_score,
    grade
) VALUES
    (1, '2026-05-01', 8.60, 8.10, 8.70, 9.30, 7.80, 8.40, 8.48, 'AA'),
    (2, '2026-05-01', 7.90, 7.50, 8.20, 7.40, 7.10, 7.70, 7.63, 'A'),
    (3, '2026-05-01', 7.70, 7.20, 7.90, 8.10, 7.30, 7.40, 7.60, 'A'),
    (4, '2026-05-01', 6.80, 6.40, 7.00, 8.20, 6.10, 6.60, 6.85, 'BBB'),
    (5, '2026-05-01', 6.20, 6.00, 6.50, 7.10, 6.30, 6.20, 6.38, 'BBB'),
    (6, '2026-05-01', 5.60, 5.40, 6.10, 6.60, 5.80, 5.50, 5.83, 'BB');

INSERT INTO alarms (
    plant_id,
    facility_id,
    alarm_type,
    alarm_level,
    message,
    value,
    threshold_value,
    occurred_at,
    resolved_at,
    status
) VALUES
    (1, NULL, 'PEAK', 'WARNING', 'Kia Hwaseong peak power reached 95 percent of the management threshold.', 1425.00, 1500.00, '2026-05-15 14:05:00', NULL, 'OCCURRED'),
    (1, 103, 'GAS', 'CRITICAL', 'Paint Booth A gas usage exceeded the hourly limit.', 605.00, 580.00, '2026-05-15 14:00:00', NULL, 'OCCURRED'),
    (2, 204, 'ELECTRICITY', 'WARNING', 'Assembly Line 1 electricity usage increased by more than 12 percent.', 1320.00, 1200.00, '2026-05-15 11:30:00', NULL, 'OCCURRED'),
    (4, 405, 'FACILITY', 'CRITICAL', 'Transformer room is stopped and requires inspection.', 0.00, 1.00, '2026-05-15 09:18:00', NULL, 'OCCURRED'),
    (5, NULL, 'ESG', 'INFO', 'Hyundai Asan ESG grade remains BBB this month.', 6.38, 6.00, '2026-05-15 08:00:00', NULL, 'OCCURRED'),
    (6, 603, 'WATER', 'WARNING', 'Jeonju Paint Line water usage exceeded the threshold.', 18.20, 16.00, '2026-05-14 16:10:00', '2026-05-14 17:05:00', 'RESOLVED');

INSERT INTO simulation_results (
    id,
    user_id,
    plant_id,
    base_score,
    base_grade,
    electricity_reduction_rate,
    gas_reduction_rate,
    water_reduction_rate,
    peak_reduction_rate,
    solar_increase_rate,
    expected_score,
    expected_grade,
    analysis_result,
    created_at
) VALUES
    (1, 1002, 1, 8.48, 'AA', 5.00, 3.00, 4.00, 6.00, 10.00, 9.32, 'AAA',
     'Reducing electricity, gas, water, and peak power while increasing solar generation can improve the expected ESG grade to AAA.',
     '2026-05-15 14:20:00');

INSERT INTO chatbot_messages (
    id,
    user_id,
    plant_id,
    question,
    answer,
    referenced_data,
    created_at
) VALUES
    (1, 1002, 1, 'Show today peak power status for Hwaseong.',
     'Recent peak power is 1425 kW, which is 95 percent of the 1500 kW threshold. Paint Booth A gas usage should also be checked.',
     JSON_OBJECT('plantId', 1, 'peakKw', 1425.00, 'thresholdKw', 1500.00, 'alarmCount', 2),
     '2026-05-15 14:25:00');
