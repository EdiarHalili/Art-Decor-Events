# API Reference

Base path: `/api/v1`

All protected endpoints require:

```http
Authorization: Bearer <access-token>
```

All responses include `X-Request-Id`. Clients may send their own `X-Request-Id`; otherwise the server generates one.

## Error Format

```json
{
  "code": "VALIDATION_FAILED",
  "message": "Please check the submitted fields.",
  "details": {
    "pin": "PIN must be 4 digits."
  }
}
```

## Authentication

- `POST /auth/admin/login`
- `POST /auth/employee/login`
- `GET /auth/me`

## Admin

- `GET /admin/dashboard`
- `GET /admin/users`
- `POST /admin/users`
- `POST /admin/users/{userId}/deactivate`
- `GET /admin/employees`
- `POST /admin/employees`
- `PATCH /admin/employees/{employeeId}`
- `POST /admin/employees/{employeeId}/deactivate`

Planned for Phase 2:

- `GET /admin/check-in-windows`
- `POST /admin/check-in-windows`
- `PATCH /admin/check-in-windows/{windowId}`
- `POST /admin/check-in-windows/{windowId}/open`
- `POST /admin/check-in-windows/{windowId}/close`
- `POST /admin/check-in-windows/{windowId}/cancel`
- `PUT /admin/check-in-windows/{windowId}/employees`

## Employee

- `GET /employee/today`
- `POST /employee/attendance/check-in`
- `POST /employee/attendance/check-out`

## Attendance Action Payload

```json
{
  "scheduleId": "uuid",
  "latitude": 42.6629,
  "longitude": 21.1655,
  "device": {
    "platform": "Win32",
    "language": "en-US"
  }
}
```

Note: Phase 1 still names this identifier `scheduleId` internally. Phase 2 will expose it to admins as a Daily Check-in Window, not an event.
