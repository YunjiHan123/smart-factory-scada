USE scada;

-- Demo account password for all seeded users: Password123!

SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

DELETE FROM chatbot_messages WHERE user_id BETWEEN 1001 AND 1007 OR plant_id BETWEEN 1 AND 6;
DELETE FROM simulation_results WHERE user_id BETWEEN 1001 AND 1007 OR plant_id BETWEEN 1 AND 6;
DELETE FROM alarms WHERE plant_id BETWEEN 1 AND 6;
DELETE FROM esg_scores WHERE plant_id BETWEEN 1 AND 6;
DELETE FROM energy_summaries WHERE plant_id BETWEEN 1 AND 6;
DELETE FROM energy_measurements WHERE plant_id BETWEEN 1 AND 6;
DELETE FROM facilities WHERE plant_id BETWEEN 1 AND 6;
DELETE FROM users WHERE id BETWEEN 1001 AND 1007;
DELETE FROM plants WHERE id BETWEEN 1 AND 6;

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;

INSERT INTO plants (id, name, company_type, address, latitude, longitude) VALUES
    (1, '기아 화성', 'KIA', '경기도 화성시 우정읍 기아자동차로 95', 37.0215590, 126.7831110),
    (2, '기아 광명', 'KIA', '경기도 광명시 기아로 113', 37.4302030, 126.8789450),
    (3, '기아 광주', 'KIA', '광주광역시 서구 화운로 277', 35.1601080, 126.8826180),
    (4, '현대 울산', 'HYUNDAI', '울산광역시 북구 염포로 700', 35.5383770, 129.3765130),
    (5, '현대 아산', 'HYUNDAI', '충청남도 아산시 인주면 현대로 1077', 36.8385080, 126.8815930),
    (6, '현대 전주', 'HYUNDAI', '전라북도 완주군 봉동읍 완주산단5로 163', 35.9565430, 127.1345060);

INSERT INTO users (
    id, email, password_hash, name, phone, role, plant_id, status, note, last_login_at
) VALUES
    (1001, 'admin@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '통합 관리자', '010-1000-0001', 'ADMIN', NULL, 'ACTIVE', '전체 사업장 열람 계정', NOW() - INTERVAL 10 MINUTE),
    (1002, 'manager.hwaseong@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '기아 화성 관리자', '010-1000-0002', 'MANAGER', 1, 'ACTIVE', '기아 화성 사업장 담당 계정', NOW() - INTERVAL 25 MINUTE),
    (1003, 'manager.gwangmyeong@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '기아 광명 관리자', '010-1000-0003', 'MANAGER', 2, 'ACTIVE', '기아 광명 사업장 담당 계정', NOW() - INTERVAL 1 HOUR),
    (1004, 'manager.gwangju@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '기아 광주 관리자', '010-1000-0004', 'MANAGER', 3, 'ACTIVE', '기아 광주 사업장 담당 계정', NOW() - INTERVAL 2 HOUR),
    (1005, 'manager.ulsan@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '현대 울산 관리자', '010-1000-0005', 'MANAGER', 4, 'ACTIVE', '현대 울산 사업장 담당 계정', NOW() - INTERVAL 3 HOUR),
    (1006, 'manager.asan@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '현대 아산 관리자', '010-1000-0006', 'MANAGER', 5, 'ACTIVE', '현대 아산 사업장 담당 계정', NOW() - INTERVAL 4 HOUR),
    (1007, 'manager.jeonju@scada.com', '$2a$10$AjgLZMPwXSVx4hIDQ.5fcebBKPCYE/3kLsFMdqlNRxyXQKMQr086W', '현대 전주 관리자', '010-1000-0007', 'MANAGER', 6, 'ACTIVE', '현대 전주 사업장 담당 계정', NOW() - INTERVAL 5 HOUR);

INSERT INTO facilities (id, plant_id, name, facility_type, status) VALUES
    (101, 1, '화성 프레스 1라인', 'PRESS', 'RUNNING'),
    (102, 1, '화성 차체 2라인', 'BODY', 'RUNNING'),
    (103, 1, '화성 도장 부스 A', 'PAINT', 'WARNING'),
    (104, 1, '화성 의장 3라인', 'ASSEMBLY', 'RUNNING'),
    (105, 1, '화성 공조 설비', 'HVAC', 'RUNNING'),
    (201, 2, '광명 프레스 1라인', 'PRESS', 'RUNNING'),
    (202, 2, '광명 차체 용접라인', 'BODY', 'RUNNING'),
    (203, 2, '광명 도장 건조로', 'PAINT', 'RUNNING'),
    (204, 2, '광명 의장 1라인', 'ASSEMBLY', 'WARNING'),
    (205, 2, '광명 최종 검사장', 'INSPECTION', 'RUNNING'),
    (301, 3, '광주 프레스 2라인', 'PRESS', 'RUNNING'),
    (302, 3, '광주 차체 1라인', 'BODY', 'RUNNING'),
    (303, 3, '광주 도장 부스 B', 'PAINT', 'RUNNING'),
    (304, 3, '광주 의장 2라인', 'ASSEMBLY', 'RUNNING'),
    (305, 3, '광주 유틸리티 센터', 'ETC', 'WARNING'),
    (401, 4, '울산 프레스 3공장', 'PRESS', 'RUNNING'),
    (402, 4, '울산 차체 5라인', 'BODY', 'WARNING'),
    (403, 4, '울산 도장 2공장', 'PAINT', 'RUNNING'),
    (404, 4, '울산 의장 4라인', 'ASSEMBLY', 'RUNNING'),
    (405, 4, '울산 변전 설비', 'ETC', 'STOPPED'),
    (501, 5, '아산 프레스 라인', 'PRESS', 'RUNNING'),
    (502, 5, '아산 차체 라인', 'BODY', 'RUNNING'),
    (503, 5, '아산 도장 라인', 'PAINT', 'RUNNING'),
    (504, 5, '아산 의장 라인', 'ASSEMBLY', 'RUNNING'),
    (505, 5, '아산 공조 설비', 'HVAC', 'RUNNING'),
    (601, 6, '전주 프레스 라인', 'PRESS', 'RUNNING'),
    (602, 6, '전주 차체 라인', 'BODY', 'RUNNING'),
    (603, 6, '전주 도장 라인', 'PAINT', 'WARNING'),
    (604, 6, '전주 의장 라인', 'ASSEMBLY', 'RUNNING'),
    (605, 6, '전주 최종 검사장', 'INSPECTION', 'RUNNING');

INSERT INTO energy_summaries (
    plant_id, facility_id, summary_type, summary_at, electricity_kwh, gas_m3, water_ton, solar_kwh, peak_kw, carbon_emission
) VALUES
    (1, NULL, 'DAILY', '2026-05-10 00:00:00', 116820.00, 25840.00, 1290.50, 18240.00, 1340.00, 54820.40),
    (1, NULL, 'DAILY', '2026-05-11 00:00:00', 121450.00, 26310.00, 1324.80, 19120.00, 1385.00, 56610.20),
    (1, NULL, 'DAILY', '2026-05-12 00:00:00', 119780.00, 26020.00, 1311.20, 18760.00, 1368.00, 55742.30),
    (1, NULL, 'DAILY', '2026-05-13 00:00:00', 125630.00, 27150.00, 1368.40, 19680.00, 1425.00, 58680.90),
    (2, NULL, 'DAILY', '2026-05-13 00:00:00', 108420.00, 22410.00, 1122.30, 14220.00, 1260.00, 50620.50),
    (3, NULL, 'DAILY', '2026-05-13 00:00:00', 98750.00, 21460.00, 1018.90, 15880.00, 1185.00, 45870.60),
    (4, NULL, 'DAILY', '2026-05-13 00:00:00', 168900.00, 38200.00, 1840.70, 22850.00, 1810.00, 79230.80),
    (5, NULL, 'DAILY', '2026-05-13 00:00:00', 88420.00, 18920.00, 905.40, 12140.00, 1048.00, 41050.20),
    (6, NULL, 'DAILY', '2026-05-13 00:00:00', 76180.00, 16620.00, 802.60, 10890.00, 965.00, 35420.70),
    (1, 101, 'DAILY', '2026-05-13 00:00:00', 28500.00, 5200.00, 260.00, 0.00, 420.00, 13050.00),
    (1, 102, 'DAILY', '2026-05-13 00:00:00', 24680.00, 4710.00, 224.00, 0.00, 380.00, 11280.00),
    (1, 103, 'DAILY', '2026-05-13 00:00:00', 34200.00, 11240.00, 405.00, 0.00, 510.00, 18470.00),
    (1, 104, 'DAILY', '2026-05-13 00:00:00', 21640.00, 4420.00, 318.00, 0.00, 340.00, 9820.00),
    (1, 105, 'DAILY', '2026-05-13 00:00:00', 16610.00, 1580.00, 161.40, 19680.00, 265.00, 6060.90),
    (1, NULL, 'HOURLY', '2026-05-13 08:00:00', 4210.00, 880.00, 41.50, 980.00, 1040.00, 1960.30),
    (1, NULL, 'HOURLY', '2026-05-13 09:00:00', 4860.00, 1010.00, 48.20, 1450.00, 1165.00, 2240.10),
    (1, NULL, 'HOURLY', '2026-05-13 10:00:00', 5320.00, 1125.00, 55.10, 1830.00, 1248.00, 2475.60),
    (1, NULL, 'HOURLY', '2026-05-13 11:00:00', 5750.00, 1210.00, 61.40, 2180.00, 1315.00, 2678.20),
    (1, NULL, 'HOURLY', '2026-05-13 12:00:00', 6120.00, 1280.00, 66.90, 2490.00, 1360.00, 2842.40),
    (1, NULL, 'HOURLY', '2026-05-13 13:00:00', 6480.00, 1375.00, 70.20, 2720.00, 1415.00, 3026.80),
    (1, NULL, 'HOURLY', '2026-05-13 14:00:00', 6585.00, 1395.00, 72.30, 2650.00, 1425.00, 3074.70),
    (1, NULL, 'HOURLY', '2026-05-13 15:00:00', 6310.00, 1330.00, 69.10, 2310.00, 1378.00, 2940.90);

INSERT INTO energy_measurements (
    plant_id, facility_id, measured_at, electricity_kwh, gas_m3, water_ton, solar_kwh, peak_kw, created_at
) VALUES
    (1, 101, '2026-05-13 10:00:00', 1180.00, 210.00, 10.40, 0.00, 390.00, '2026-05-13 10:00:05'),
    (1, 102, '2026-05-13 10:00:00', 1020.00, 185.00, 8.60, 0.00, 342.00, '2026-05-13 10:00:05'),
    (1, 103, '2026-05-13 10:00:00', 1465.00, 490.00, 17.20, 0.00, 475.00, '2026-05-13 10:00:05'),
    (1, 104, '2026-05-13 10:00:00', 930.00, 176.00, 12.50, 0.00, 288.00, '2026-05-13 10:00:05'),
    (1, 105, '2026-05-13 10:00:00', 725.00, 64.00, 6.40, 1830.00, 210.00, '2026-05-13 10:00:05'),
    (1, 101, '2026-05-13 11:00:00', 1260.00, 235.00, 11.10, 0.00, 405.00, '2026-05-13 11:00:05'),
    (1, 102, '2026-05-13 11:00:00', 1105.00, 202.00, 9.30, 0.00, 358.00, '2026-05-13 11:00:05'),
    (1, 103, '2026-05-13 11:00:00', 1570.00, 528.00, 18.60, 0.00, 498.00, '2026-05-13 11:00:05'),
    (1, 104, '2026-05-13 11:00:00', 995.00, 185.00, 13.20, 0.00, 304.00, '2026-05-13 11:00:05'),
    (1, 105, '2026-05-13 11:00:00', 820.00, 60.00, 9.20, 2180.00, 225.00, '2026-05-13 11:00:05'),
    (1, 101, '2026-05-13 12:00:00', 1340.00, 248.00, 11.70, 0.00, 418.00, '2026-05-13 12:00:05'),
    (1, 102, '2026-05-13 12:00:00', 1180.00, 214.00, 10.00, 0.00, 370.00, '2026-05-13 12:00:05'),
    (1, 103, '2026-05-13 12:00:00', 1665.00, 552.00, 19.80, 0.00, 510.00, '2026-05-13 12:00:05'),
    (1, 104, '2026-05-13 12:00:00', 1080.00, 198.00, 14.10, 0.00, 318.00, '2026-05-13 12:00:05'),
    (1, 105, '2026-05-13 12:00:00', 855.00, 68.00, 11.30, 2490.00, 232.00, '2026-05-13 12:00:05'),
    (1, 101, '2026-05-13 13:00:00', 1410.00, 262.00, 12.10, 0.00, 432.00, '2026-05-13 13:00:05'),
    (1, 102, '2026-05-13 13:00:00', 1235.00, 225.00, 10.40, 0.00, 386.00, '2026-05-13 13:00:05'),
    (1, 103, '2026-05-13 13:00:00', 1780.00, 590.00, 20.40, 0.00, 528.00, '2026-05-13 13:00:05'),
    (1, 104, '2026-05-13 13:00:00', 1125.00, 210.00, 15.00, 0.00, 330.00, '2026-05-13 13:00:05'),
    (1, 105, '2026-05-13 13:00:00', 930.00, 88.00, 12.30, 2720.00, 240.00, '2026-05-13 13:00:05'),
    (1, 101, '2026-05-13 14:00:00', 1435.00, 270.00, 12.40, 0.00, 438.00, '2026-05-13 14:00:05'),
    (1, 102, '2026-05-13 14:00:00', 1260.00, 231.00, 10.70, 0.00, 392.00, '2026-05-13 14:00:05'),
    (1, 103, '2026-05-13 14:00:00', 1810.00, 605.00, 21.10, 0.00, 535.00, '2026-05-13 14:00:05'),
    (1, 104, '2026-05-13 14:00:00', 1160.00, 218.00, 15.30, 0.00, 338.00, '2026-05-13 14:00:05'),
    (1, 105, '2026-05-13 14:00:00', 920.00, 71.00, 12.80, 2650.00, 241.00, '2026-05-13 14:00:05');

INSERT INTO esg_scores (
    plant_id, target_month, electricity_score, gas_score, water_score, solar_score, peak_score, carbon_score, total_score, grade
) VALUES
    (1, '2026-04-01', 8.20, 7.80, 8.50, 9.10, 7.40, 8.00, 8.17, 'AA'),
    (1, '2026-05-01', 8.60, 8.10, 8.70, 9.30, 7.80, 8.40, 8.48, 'AA'),
    (2, '2026-05-01', 7.90, 7.50, 8.20, 7.40, 7.10, 7.70, 7.63, 'A'),
    (3, '2026-05-01', 7.70, 7.20, 7.90, 8.10, 7.30, 7.40, 7.60, 'A'),
    (4, '2026-05-01', 6.80, 6.40, 7.00, 8.20, 6.10, 6.60, 6.85, 'BBB'),
    (5, '2026-05-01', 6.20, 6.00, 6.50, 7.10, 6.30, 6.20, 6.38, 'BBB'),
    (6, '2026-05-01', 5.60, 5.40, 6.10, 6.60, 5.80, 5.50, 5.83, 'BB');

INSERT INTO alarms (
    plant_id, facility_id, alarm_type, alarm_level, message, value, threshold_value, occurred_at, resolved_at, status
) VALUES
    (1, NULL, 'PEAK', 'WARNING', '화성 사업장의 피크 전력이 관리 기준의 95%에 도달했습니다.', 1425.00, 1500.00, '2026-05-13 14:05:00', NULL, 'OCCURRED'),
    (1, 103, 'FACILITY', 'WARNING', '화성 도장 부스 A의 배기 팬 진동 수치가 정상 범위를 초과했습니다.', 78.40, 75.00, '2026-05-13 13:42:00', NULL, 'OCCURRED'),
    (1, 103, 'GAS', 'CRITICAL', '화성 도장 부스 A의 시간당 가스 사용량이 임계값을 초과했습니다.', 605.00, 580.00, '2026-05-13 14:00:00', NULL, 'OCCURRED'),
    (2, 204, 'ELECTRICITY', 'WARNING', '광명 의장 1라인의 전력 사용량이 전일 대비 12% 증가했습니다.', 1320.00, 1200.00, '2026-05-13 11:30:00', NULL, 'OCCURRED'),
    (4, 405, 'FACILITY', 'CRITICAL', '울산 변전 설비가 정지 상태입니다. 현장 점검이 필요합니다.', 0.00, 1.00, '2026-05-13 09:18:00', NULL, 'OCCURRED'),
    (5, NULL, 'ESG', 'INFO', '아산공장의 월간 ESG 등급이 BBB로 유지되었습니다.', 6.38, 6.00, '2026-05-13 08:00:00', NULL, 'OCCURRED'),
    (6, 603, 'WATER', 'WARNING', '전주 도장 라인의 용수 사용량이 기준치를 초과했습니다.', 18.20, 16.00, '2026-05-12 16:10:00', '2026-05-12 17:05:00', 'RESOLVED');

INSERT INTO simulation_results (
    id, user_id, plant_id, base_score, base_grade, electricity_reduction_rate, gas_reduction_rate,
    water_reduction_rate, peak_reduction_rate, solar_increase_rate, expected_score, expected_grade,
    analysis_result, created_at
) VALUES
    (1, 1002, 1, 8.48, 'AA', 5.00, 3.00, 4.00, 6.00, 10.00, 9.32, 'AAA',
     '전력, 가스, 용수, 피크 전력을 함께 절감하고 태양광 발전량을 늘리면 예상 ESG 등급이 AAA로 개선됩니다.',
     '2026-05-13 14:20:00');

INSERT INTO chatbot_messages (
    id, user_id, plant_id, question, answer, referenced_data, created_at
) VALUES
    (1, 1002, 1, '화성 사업장의 오늘 피크 전력 상태를 알려줘.',
     '최근 피크 전력은 1425kW로 기준 1500kW의 약 95%입니다. 화성 도장 부스 A의 가스 사용량도 함께 확인이 필요합니다.',
     JSON_OBJECT('plantId', 1, 'peakKw', 1425.00, 'thresholdKw', 1500.00, 'alarmCount', 3),
     '2026-05-13 14:25:00');
