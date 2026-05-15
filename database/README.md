# SCADA database setup

For a clean local database, run only these two files in order:

1. `scada-schema.sql`
   - Drops and recreates the `scada` database.
   - Creates all application tables, indexes, and aggregation tables.

2. `scada-seed-demo.sql`
   - Inserts demo plants, users, facilities, raw measurements, summaries, ESG data, alarms, simulation data, and chatbot data.
   - Also fills `energy_latest_measurements` and `energy_interval_summaries`.

Demo login:

```text
email: admin@scada.com
password: Password123!
```

The files under `database/migrations/` are incremental migration/backfill helpers for an already existing database. New teammates setting up from scratch should use `scada-schema.sql` and `scada-seed-demo.sql` instead.
