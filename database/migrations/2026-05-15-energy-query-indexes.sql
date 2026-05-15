-- Speed up dashboard queries that filter by plant/facility and time range.
CREATE INDEX idx_energy_measurements_plant_time_facility_id
    ON energy_measurements (plant_id, measured_at, facility_id, id);

CREATE INDEX idx_energy_measurements_plant_facility_time_id
    ON energy_measurements (plant_id, facility_id, measured_at, id);

CREATE INDEX idx_energy_summaries_plant_facility_type_time
    ON energy_summaries (plant_id, facility_id, summary_type, summary_at);
