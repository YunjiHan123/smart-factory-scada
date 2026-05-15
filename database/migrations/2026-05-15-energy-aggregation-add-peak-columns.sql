-- Add peak aggregation columns if energy_interval_summaries was created before these fields existed.
ALTER TABLE energy_interval_summaries
    ADD COLUMN peak_average_kw DECIMAL(15, 2) NOT NULL DEFAULT 0 AFTER solar_usage_kwh,
    ADD COLUMN peak_sum_kw DECIMAL(15, 2) NOT NULL DEFAULT 0 AFTER peak_kw,
    ADD COLUMN peak_sample_count INT NOT NULL DEFAULT 0 AFTER peak_sum_kw;
