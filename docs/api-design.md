# API Design

Base path: `/api/v1`

## Authentication

- `POST /auth/admin/login`
- `POST /auth/employee/login`
- `POST /auth/refresh`
- `GET /auth/me`

## Employee Portal

- `GET /employee/today`
- `POST /employee/attendance/check-in`
- `POST /employee/attendance/check-out`
- `POST /employee/attendance/sync`

## Admin

- `GET /admin/dashboard`
- `GET /admin/employees`
- `POST /admin/employees`
- `PATCH /admin/employees/{id}`
- `POST /admin/employees/{id}/deactivate`
- `GET /admin/check-in-windows`
- `POST /admin/check-in-windows`
- `PATCH /admin/check-in-windows/{id}`
- `POST /admin/check-in-windows/{id}/open`
- `POST /admin/check-in-windows/{id}/close`
- `POST /admin/check-in-windows/{id}/cancel`
- `PUT /admin/check-in-windows/{id}/employees`

## Reports

- `GET /reports/daily`
- `GET /reports/weekly`
- `GET /reports/monthly`
- `GET /reports/employees/{id}`
- `GET /reports/export?format=xlsx|csv|pdf`

## Error Format

```json
{
  "code": "ATTENDANCE_WINDOW_CLOSED",
  "message": "Check-in is closed for this daily window.",
  "details": {}
}
```
