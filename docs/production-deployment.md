# Production Deployment Guide

This guide prepares Art Decor Events Workforce for production hosting after final QA.

## Required Services

- Java 21 runtime for the Spring Boot backend.
- Node.js 20+ only for building the frontend.
- PostgreSQL 16+ for production data.
- HTTPS termination through a reverse proxy or managed platform.
- Daily encrypted database backups.

## Backend Environment

Set these values in the production environment:

```powershell
$env:SPRING_PROFILES_ACTIVE="prod"
$env:DB_HOST="your-postgres-host"
$env:DB_PORT="5432"
$env:DB_NAME="artdecor_workforce"
$env:DB_USERNAME="artdecor_app"
$env:DB_PASSWORD="replace-with-strong-password"
$env:JWT_SECRET="replace-with-at-least-32-random-characters"
$env:CORS_ALLOWED_ORIGINS="https://your-domain.com"
$env:CORS_ALLOWED_ORIGIN_PATTERNS=""
$env:APP_BUSINESS_ZONE="Europe/Berlin"
```

Use a different database user for migrations if the hosting platform supports it. The runtime user should not own the database.

For production, keep `CORS_ALLOWED_ORIGINS` restricted to the exact deployed frontend origin. Use `CORS_ALLOWED_ORIGIN_PATTERNS` only for temporary LAN or staging testing, not as a broad production wildcard.

## Frontend Environment

Build with:

```powershell
$env:VITE_API_BASE_URL="https://api.your-domain.com/api/v1"
$env:VITE_VAPID_PUBLIC_KEY="your-web-push-public-key"
npm.cmd run build
```

Host `frontend/dist` behind HTTPS. The PWA service worker, install prompt, geolocation, and push APIs require secure context in production.

When opening the app from another device on the local network during QA, set `VITE_API_BASE_URL` to the laptop/backend LAN address before starting or building the frontend, for example `http://192.168.0.41:8080/api/v1`. In production, always point it to the public HTTPS backend API.

## PostgreSQL Setup

Create the database and user:

```sql
CREATE DATABASE artdecor_workforce;
CREATE USER artdecor_app WITH ENCRYPTED PASSWORD 'replace-with-strong-password';
GRANT CONNECT ON DATABASE artdecor_workforce TO artdecor_app;
GRANT USAGE, CREATE ON SCHEMA public TO artdecor_app;
```

Run migrations through the Spring Boot application startup or a controlled deployment job. Always test migrations against a staging copy first.

## HTTPS And Proxy

Configure the reverse proxy to:

- Redirect HTTP to HTTPS.
- Forward `X-Forwarded-For` and `X-Forwarded-Proto`.
- Limit request body size to expected upload needs.
- Serve security headers at the edge in addition to the backend defaults.

## Backups

Minimum recommendation:

- Nightly `pg_dump` backup.
- Keep 7 daily, 4 weekly, and 12 monthly restore points.
- Encrypt backups at rest.
- Store backups outside the application server.
- Test restore at least once per month.

Example:

```powershell
pg_dump --format=custom --file="backup-artdecor-$(Get-Date -Format yyyyMMdd).dump" artdecor_workforce
```

## Push Notifications

Current implementation stores browser push subscriptions and service worker push display support. Production delivery still requires:

- VAPID key pair.
- `VITE_VAPID_PUBLIC_KEY` in the frontend build.
- Backend push sender service using the private VAPID key.
- Scheduled reminder job for upcoming check-in windows.

## Deployment Checklist

- `mvn test` passes.
- `npm.cmd run build` passes.
- Production database migrated.
- Admin account created with a strong password.
- Company settings reviewed in Admin Settings.
- PWA install tested on Android and iPhone.
- Backup and restore process tested.
- HTTPS certificate active.
- CORS restricted to the production frontend domain.
