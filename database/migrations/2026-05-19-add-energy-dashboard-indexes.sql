-- Dashboard aggregation queries scan all plants by time range for comparison and ESG views.
-- The existing plant-first indexes help single-plant lookups, but not measured_at-only ranges.
CREATE INDEX idx_measurements_time_plant_facility
    ON energy_measurements (measured_at, plant_id, facility_id);
