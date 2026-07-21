# Architecture

## Product Shape

Art Decor Events Workforce is a role-based attendance and workforce platform for seasonal decoration workers and administrators.

The product is intentionally split into two UX modes:

- Employee mode: extremely simple, mobile-first, no menus, only today's check-in status, announcements, check in, and check out.
- Admin/Supervisor mode: operational dashboard, employee management, daily check-in windows, attendance review, reporting, exports, and future payroll preparation.

## Recommended System Architecture

Use a modular monolith first. This is the best fit for a real company system at the beginning because it keeps deployment, data consistency, authentication, audit logs, and reporting straightforward while still allowing modules to be extracted later if needed.

The backend is organized by clean architecture boundaries:

- Domain: business concepts and rules, independent of Spring and database details.
- Application: use cases such as login, daily check-in window assignment, check in, check out, report generation.
- Infrastructure: persistence, JWT, hashing, external services, push notifications, export engines.
- API: REST controllers, request/response DTOs, security adapters.

The frontend is organized by product area:

- `app`: routing, providers, layouts.
- `features`: auth, employee, admin, attendance, daily check-in windows, reports.
- `components`: reusable UI primitives and brand components.
- `lib`: API client, offline queue, date/time utilities, storage.
- `assets`: company logo and event photography.

## Phase 1 Scope

Phase 1 builds the foundation:

- Project structure and docs
- Database schema with payroll-ready attendance data
- JWT authentication design
- Password hashing strategy
- Branded React shell
- Employee login/check-in screen foundation
- Admin dashboard foundation
- PWA manifest and offline queue scaffolding

## Phase 2 Product Direction

The product will not include full event management for now. Phase 2 should build a simple Daily Check-in Window workflow:

- select date
- set check-in opening and closing times
- select employees allowed to check in
- open, close, edit, or cancel the window
- let employees see only whether today's check-in is open or closed

No event title, venue, location, or complex event assignment page is required in Phase 2.

## Production Principles

- Passwords are never stored in plain text.
- Every important action is auditable.
- Attendance writes are idempotent where possible to prevent duplicate check-ins and check-outs.
- Daily check-in windows are enforced by the backend, not the client.
- Offline attendance records are queued locally and synchronized with conflict-safe server APIs.
- UI uses company images as brand atmosphere, not decoration clutter.
