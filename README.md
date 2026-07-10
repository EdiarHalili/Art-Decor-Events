# Art Decor Events Workforce

Production workforce and attendance management system for Art Decor Events.

## Stack

- Frontend: React, TypeScript, Vite, Tailwind CSS, shadcn-style components, PWA
- Backend: Java 21, Spring Boot, Spring Security, JWT
- Database: PostgreSQL with Flyway migrations
- Architecture: modular clean architecture with domain, application, infrastructure, and API layers

## Workspace

- `docs/` - architecture, database, API, auth, and UX planning
- `backend/` - Spring Boot API
- `frontend/` - employee/admin web app

## Local Development

Recommended Windows startup:

```powershell
.\scripts\start-local.ps1
```

For phone testing on the same Wi-Fi, pass your laptop IPv4 address:

```powershell
.\scripts\start-local.ps1 -BackendHost "192.168.0.41"
```

Backend:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="dev"
$env:DB_URL="jdbc:postgresql://localhost:5432/artdecor_workforce"
$env:DB_USERNAME="artdecor"
$env:DB_PASSWORD="artdecor_dev_password"
mvn spring-boot:run
```

Frontend:

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

Database:

```powershell
docker compose up -d postgres
```

Health:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health
```

Full Windows testing checklist:

- [docs/windows-local-testing.md](docs/windows-local-testing.md)

PWA installation:

- [docs/pwa-installation.md](docs/pwa-installation.md)
