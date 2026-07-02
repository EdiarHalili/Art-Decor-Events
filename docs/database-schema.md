# Database Schema

## Core Tables

- `users`: login identity for administrators, supervisors, and employees.
- `employees`: employee profile, public employee ID, department, team, notes, wage metadata.
- `departments`: operational grouping.
- `teams`: seasonal work teams.
- `locations`: warehouses, venues, event locations, future geofencing metadata.
- `work_schedules`: scheduled workdays or events.
- `schedule_assignments`: employees assigned to schedules.
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

- One attendance record per employee per schedule.
- Check-in and check-out are independently timestamped.
- Duplicate check-in/check-out attempts are rejected or treated as idempotent depending on the endpoint.
- Late status is based on schedule rules.
- Pending approval is used for late/offline/manual exceptions.

