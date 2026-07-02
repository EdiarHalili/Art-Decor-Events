# Database Operations

## Local PostgreSQL

The application expects PostgreSQL and Flyway migrations.

Default development connection:

```text
DB_URL=jdbc:postgresql://localhost:5432/artdecor_workforce
DB_USERNAME=artdecor
DB_PASSWORD=artdecor_dev_password
```

Use `docker compose up -d postgres` on a machine with Docker installed.

## Migrations

Schema changes belong in `backend/src/main/resources/db/migration`.

Rules:

- Never edit an already-applied production migration.
- Add a new `V<number>__description.sql` migration for each schema change.
- Keep payroll-related historical data immutable once attendance records exist.

## Health

Spring Boot exposes `/actuator/health` for service and database health checks.

## Current Limitation

This workstation does not currently have Docker installed, so live PostgreSQL validation must be run on a machine with PostgreSQL or Docker available. Automated repository mapping tests still run in CI/local Maven tests using an in-memory database.

