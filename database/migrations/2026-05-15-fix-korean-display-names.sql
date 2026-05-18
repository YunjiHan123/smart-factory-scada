USE scada;

SET @OLD_SQL_SAFE_UPDATES = @@SQL_SAFE_UPDATES;
SET SQL_SAFE_UPDATES = 0;

UPDATE plants
SET
    name = CASE id
        WHEN 1 THEN '기아 화성'
        WHEN 2 THEN '기아 광명'
        WHEN 3 THEN '기아 광주'
        WHEN 4 THEN '현대 울산'
        WHEN 5 THEN '현대 아산'
        WHEN 6 THEN '현대 전주'
        ELSE name
    END,
    address = CASE id
        WHEN 1 THEN '경기도 화성시 우정읍 기아자동차로 95'
        WHEN 2 THEN '경기도 광명시 기아로 113'
        WHEN 3 THEN '광주광역시 서구 화운로 277'
        WHEN 4 THEN '울산광역시 북구 염포로 700'
        WHEN 5 THEN '충청남도 아산시 인주면 현대로 1077'
        WHEN 6 THEN '전라북도 전주시 완산구 봉동읍 전주산단5로 163'
        ELSE address
    END
WHERE id IN (1, 2, 3, 4, 5, 6);

UPDATE users
SET name = CASE id
    WHEN 1001 THEN '통합 관리자'
    WHEN 1002 THEN '기아 화성 관리자'
    WHEN 1003 THEN '기아 광명 관리자'
    WHEN 1004 THEN '기아 광주 관리자'
    WHEN 1005 THEN '현대 울산 관리자'
    WHEN 1006 THEN '현대 아산 관리자'
    WHEN 1007 THEN '현대 전주 관리자'
    ELSE name
END
WHERE id IN (1001, 1002, 1003, 1004, 1005, 1006, 1007);

UPDATE facilities
SET name = CASE id
    WHEN 101 THEN 'F-101'
    WHEN 102 THEN 'F-102'
    WHEN 103 THEN 'F-103'
    WHEN 104 THEN 'F-104'
    WHEN 105 THEN 'F-105'
    WHEN 201 THEN 'F-201'
    WHEN 202 THEN 'F-202'
    WHEN 203 THEN 'F-203'
    WHEN 204 THEN 'F-204'
    WHEN 205 THEN 'F-205'
    WHEN 301 THEN 'F-301'
    WHEN 302 THEN 'F-302'
    WHEN 303 THEN 'F-303'
    WHEN 304 THEN 'F-304'
    WHEN 305 THEN 'F-305'
    WHEN 401 THEN 'F-401'
    WHEN 402 THEN 'F-402'
    WHEN 403 THEN 'F-403'
    WHEN 404 THEN 'F-404'
    WHEN 405 THEN 'F-405'
    WHEN 501 THEN '기존 아산 프레스 설비'
    WHEN 502 THEN '기존 아산 차체 설비'
    WHEN 503 THEN '기존 아산 도장 설비'
    WHEN 504 THEN '기존 아산 의장 설비'
    WHEN 505 THEN '기존 아산 공조 설비'
    WHEN 601 THEN '기존 전주 프레스 설비'
    WHEN 602 THEN '기존 전주 차체 설비'
    WHEN 603 THEN '기존 전주 도장 설비'
    WHEN 604 THEN '기존 전주 의장 설비'
    WHEN 605 THEN '기존 전주 검사 설비'
    ELSE name
END
WHERE id IN (
    101, 102, 103, 104, 105,
    201, 202, 203, 204, 205,
    301, 302, 303, 304, 305,
    401, 402, 403, 404, 405,
    501, 502, 503, 504, 505,
    601, 602, 603, 604, 605
);

UPDATE facilities f
JOIN (
    SELECT 1 AS seq, 'PRESS' AS facility_type, '메인 프레스기' AS name
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
) e ON e.seq = MOD(f.id, 10000)
SET
    f.name = CONCAT('F-', LPAD(e.seq, 3, '0')),
    f.facility_type = e.facility_type,
    f.status = CASE WHEN e.seq IN (6, 12, 18, 24) THEN 'WARNING' ELSE f.status END
WHERE MOD(f.id, 10000) BETWEEN 1 AND 24;

SET SQL_SAFE_UPDATES = @OLD_SQL_SAFE_UPDATES;
