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

Backend:

```powershell
cd backend
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
