SET SQL_SAFE_UPDATES = 0;

DELETE FROM alarms
WHERE alarm_type = 'FACILITY'
  AND (
      message LIKE '%누적 계측값이 직전 수집값보다 감소%'
      OR message LIKE '%계측기 리셋%'
  );

SET SQL_SAFE_UPDATES = 1;
