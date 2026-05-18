USE scada;

SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

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
    SELECT 1 AS seq, 'PRESS' AS facility_type, '메인 프레스기' AS process_name
    UNION ALL SELECT 2, 'PRESS', '블랭킹 프레스'
    UNION ALL SELECT 3, 'PRESS', '코일 피더'
    UNION ALL SELECT 4, 'PRESS', '금형 교환기'
    UNION ALL SELECT 5, 'PRESS', '유압 펌프'
    UNION ALL SELECT 6, 'PRESS', '배출 컨베이어'
    UNION ALL SELECT 7, 'BODY', '용접 로봇'
    UNION ALL SELECT 8, 'BODY', '지그 시스템'
    UNION ALL SELECT 9, 'BODY', '이송 컨베이어'
    UNION ALL SELECT 10, 'BODY', '스폿 용접기'
    UNION ALL SELECT 11, 'BODY', '검사 카메라'
    UNION ALL SELECT 12, 'BODY', '보정 장치'
    UNION ALL SELECT 13, 'ASSEMBLY', '조립 로봇'
    UNION ALL SELECT 14, 'ASSEMBLY', '체결 토크 장치'
    UNION ALL SELECT 15, 'ASSEMBLY', '부품 공급 장치'
    UNION ALL SELECT 16, 'ASSEMBLY', '도어 장착 장치'
    UNION ALL SELECT 17, 'ASSEMBLY', '시트 장착 리프트'
    UNION ALL SELECT 18, 'ASSEMBLY', '최종 검사기'
    UNION ALL SELECT 19, 'PAINT', '전처리 탱크'
    UNION ALL SELECT 20, 'PAINT', '분사 로봇'
    UNION ALL SELECT 21, 'PAINT', '건조 오븐'
    UNION ALL SELECT 22, 'PAINT', '배기 처리 설비'
    UNION ALL SELECT 23, 'PAINT', '도료 공급 펌프'
    UNION ALL SELECT 24, 'PAINT', '도막 검사기'
) e
WHERE NOT EXISTS (
    SELECT 1
    FROM facilities f
    WHERE f.id = p.id * 10000 + e.seq
);

DELETE em
FROM energy_measurements em
JOIN facilities f ON f.id = em.facility_id
WHERE f.id >= 10000
  AND MOD(f.id, 10000) BETWEEN 1 AND 24
  AND em.measured_at >= TIMESTAMP('2026-04-15 00:00:00')
  AND em.measured_at < TIMESTAMP('2026-05-16 00:00:00');

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
WITH RECURSIVE days(day_offset) AS (
    SELECT 0
    UNION ALL
    SELECT day_offset + 1 FROM days WHERE day_offset < 30
),
hours(hour_offset) AS (
    SELECT 0
    UNION ALL
    SELECT hour_offset + 1 FROM hours WHERE hour_offset < 23
),
line_equipment AS (
    SELECT
        f.plant_id,
        f.id AS facility_id,
        f.facility_type,
        MOD(f.id, 10000) AS equipment_seq
    FROM facilities f
    WHERE f.id >= 10000
      AND MOD(f.id, 10000) BETWEEN 1 AND 24
),
usage_points AS (
    SELECT
        e.plant_id,
        e.facility_id,
        e.facility_type,
        e.equipment_seq,
        TIMESTAMP(DATE_ADD('2026-04-15', INTERVAL d.day_offset DAY), MAKETIME(h.hour_offset, 0, 0)) AS measured_at,
        d.day_offset,
        h.hour_offset,
        ROUND(
            (
                CASE e.facility_type
                    WHEN 'PRESS' THEN 18.5
                    WHEN 'BODY' THEN 15.2
                    WHEN 'ASSEMBLY' THEN 12.4
                    WHEN 'PAINT' THEN 21.6
                    ELSE 10.0
                END
                + e.plant_id * 1.15
                + e.equipment_seq * 0.42
            )
            * (
                0.78
                + CASE
                    WHEN h.hour_offset BETWEEN 8 AND 18 THEN 0.62
                    WHEN h.hour_offset BETWEEN 6 AND 22 THEN 0.32
                    ELSE 0.12
                  END
                + MOD(d.day_offset * 11 + e.equipment_seq * 7 + e.plant_id * 5, 13) * 0.028
                + ABS(SIN((h.hour_offset + e.equipment_seq) / 24 * PI())) * 0.18
            ),
            3
        ) AS electricity_delta,
        ROUND(
            (
                CASE WHEN e.facility_type = 'PAINT' THEN 2.9 ELSE 0.82 END
                + e.plant_id * 0.06
                + e.equipment_seq * 0.025
            )
            * (0.72 + MOD(d.day_offset * 5 + h.hour_offset + e.equipment_seq, 9) * 0.035),
            3
        ) AS gas_delta,
        ROUND(
            (
                CASE WHEN e.facility_type IN ('PAINT', 'ASSEMBLY') THEN 0.18 ELSE 0.075 END
                + e.plant_id * 0.006
                + e.equipment_seq * 0.002
            )
            * (0.78 + MOD(d.day_offset * 3 + h.hour_offset + e.equipment_seq, 7) * 0.04),
            3
        ) AS water_delta,
        ROUND(
            CASE
                WHEN e.facility_type = 'ASSEMBLY' AND h.hour_offset BETWEEN 7 AND 18
                    THEN (8.5 + e.plant_id * 0.9 + e.equipment_seq * 0.18)
                         * SIN((h.hour_offset - 6) / 13 * PI())
                ELSE 0
            END,
            3
        ) AS solar_delta,
        ROUND(
            CASE e.facility_type
                WHEN 'PRESS' THEN 210 + h.hour_offset * 7 + e.equipment_seq * 5
                WHEN 'BODY' THEN 170 + h.hour_offset * 5 + e.equipment_seq * 4
                WHEN 'ASSEMBLY' THEN 140 + h.hour_offset * 4 + e.equipment_seq * 3
                WHEN 'PAINT' THEN 260 + h.hour_offset * 8 + e.equipment_seq * 5
                ELSE 120
            END
            * (0.88 + MOD(d.day_offset * 7 + e.plant_id + e.equipment_seq, 11) * 0.018),
            2
        ) AS peak_kw
    FROM line_equipment e
    JOIN days d
    JOIN hours h
),
cumulative_points AS (
    SELECT
        plant_id,
        facility_id,
        measured_at,
        SUM(electricity_delta) OVER (
            PARTITION BY plant_id, facility_id
            ORDER BY measured_at
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) AS electricity_kwh,
        SUM(gas_delta) OVER (
            PARTITION BY plant_id, facility_id
            ORDER BY measured_at
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) AS gas_m3,
        SUM(water_delta) OVER (
            PARTITION BY plant_id, facility_id
            ORDER BY measured_at
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) AS water_ton,
        SUM(solar_delta) OVER (
            PARTITION BY plant_id, facility_id
            ORDER BY measured_at
            ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
        ) AS solar_kwh,
        peak_kw
    FROM usage_points
)
SELECT
    plant_id,
    facility_id,
    measured_at,
    ROUND(electricity_kwh, 2),
    ROUND(gas_m3, 2),
    ROUND(water_ton, 2),
    ROUND(solar_kwh, 2),
    peak_kw,
    measured_at + INTERVAL 5 SECOND
FROM cumulative_points;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;
