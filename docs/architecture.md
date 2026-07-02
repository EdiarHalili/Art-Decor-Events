# Architecture

## Product Shape

Art Decor Events Workforce is a role-based attendance and workforce platform for seasonal decoration workers and administrators.

The product is intentionally split into two UX modes:

- Employee mode: extremely simple, mobile-first, no menus, only today's assignment, announcements, check in, check out, and current status.
- Admin/Supervisor mode: operational dashboard, employee management, scheduling, attendance review, reporting, exports, and future payroll preparation.

## Recommended System Architecture

Use a modular monolith first. This is the best fit for a real company system at the beginning because it keeps deployment, data consistency, authentication, audit logs, and reporting straightforward while still allowing modules to be extracted later if needed.

The backend is organized by clean architecture boundaries:

- Domain: business concepts and rules, independent of Spring and database details.
- Application: use cases such as login, check in, check out, schedule assignment, report generation.
- Infrastructure: persistence, JWT, hashing, external services, push notifications, export engines.
- API: REST controllers, request/response DTOs, security adapters.

The frontend is organized by product area:

- `app`: routing, providers, layouts.
- `features`: auth, employee, admin, attendance, schedules, reports.
- `components`: reusable UI primitives and brand components.
- `lib`: API client, offline queue, date/time utilities, storage.
- `assets`: company logo and event photography.

## Phase 1 Scope

Phase 1 builds the foundation:

- Project structure and docs
- Database schema with payroll-ready attendance data
- JWT authentication design
- PIN/password hashing strategy
- Branded React shell
- Employee login/check-in screen foundation
- Admin dashboard foundation
- PWA manifest and offline queue scaffolding

## Production Principles

- PINs and passwords are never stored in plain text.
- Every important action is auditable.
- Attendance writes are idempotent where possible to prevent duplicate check-ins and check-outs.
- Time windows are enforced by the backend, not the client.
- Offline attendance records are queued locally and synchronized with conflict-safe server APIs.
- UI uses company images as brand atmosphere, not decoration clutter.

