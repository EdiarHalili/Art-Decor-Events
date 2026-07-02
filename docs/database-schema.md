# Database Schema

## Core Tables

- `users`: login identity for administrators, supervisors, and employees.
- `employees`: employee profile, public employee ID, department, team, notes, wage metadata.
- `departments`: operational grouping.
- `teams`: seasonal work teams.
- `work_schedules`: daily check-in windows. The table name is generic from Phase 1, but the product workflow is intentionally simple.
- `schedule_assignments`: employees allowed to check in for a daily window.
- `attendance_records`: check-in/check-out facts, status, GPS, device, hours, overtime.
- `announcements`: employee-facing messages.
- `audit_logs`: immutable administrative and security activity trail.

## Payroll-Ready Design

Payroll is not implemented in Phase 1, but the schema stores:

- wage type: hourly, daily, monthly
- base wage amount
- overtime multiplier
- worked minutes
- overtime minutes
- pay period summaries can be added without changing attendance history

## Attendance Rules

- One attendance record per employee per daily check-in window.
- Check-in and check-out are independently timestamped.
- Duplicate check-in/check-out attempts are rejected or treated as idempotent depending on the endpoint.
- Late status is based on daily check-in window rules.
- Pending approval is used for late/offline/manual exceptions.

## Simplified Phase 2 Model

The admin should not manage full events. The only operational object needed now is a Daily Check-in Window:

- date
- check-in opens at
- check-in closes at
- status: draft/open/closed/cancelled
- selected employees allowed to check in

Locations, event titles, venues, and complex event assignment screens are intentionally deferred.
