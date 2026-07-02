# Phase Plan

## Phase 1: Foundation

- Create backend/frontend project structure.
- Add database migrations.
- Add JWT authentication foundation.
- Add branded React app shell.
- Add employee login and admin login UI.
- Add employee today screen and admin dashboard placeholder backed by API contracts.
- Add PWA manifest and offline attendance queue scaffolding.

## Phase 2: Daily Check-in Windows

- Replace complex event management with a simple Daily Check-in Window workflow.
- Admin creates a daily check-in window for a selected date.
- Admin sets check-in open and close times, for example 06:50-07:10.
- Admin selects which employees are allowed to check in that day.
- Admin can manually open check-in, manually close check-in, edit the window, or cancel the window.
- Employees only see whether today's check-in is open or closed.
- Employees can check in only during the allowed window.
- Employees can check out after they have checked in.
- No event title, location, venue, or complex event page is required for now.
- Continue storing optional GPS/device metadata and support offline queueing.

## Phase 3: Admin Operations

- Employee CRUD.
- Teams and departments only if needed operationally.
- Daily check-in window history and corrections.
- Supervisor/admin review of late, absent, checked-in, and checked-out workers.

## Phase 4: Reports and Exports

- Attendance reports.
- Employee history.
- Late/absence analytics.
- Excel, CSV, PDF export.

## Phase 5: Notifications and Payroll Preparation

- Push notifications.
- Shift reminders.
- Announcements.
- Payroll summaries and exports.
