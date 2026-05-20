SET @schema_name = DATABASE();

SET @energy_lookup_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = @schema_name
      AND table_name = 'energy_measurements'
      AND index_name = 'idx_measurements_plant_facility_time'
);

SET @energy_lookup_index_sql = IF(
    @energy_lookup_index_exists = 0,
    'CREATE INDEX idx_measurements_plant_facility_time ON energy_measurements (plant_id, facility_id, measured_at)',
    'SELECT 1'
);

PREPARE energy_lookup_index_stmt FROM @energy_lookup_index_sql;
EXECUTE energy_lookup_index_stmt;
DEALLOCATE PREPARE energy_lookup_index_stmt;
