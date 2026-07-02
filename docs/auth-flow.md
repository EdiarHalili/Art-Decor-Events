# Authentication Flow

## Administrator and Supervisor

1. User enters email and password.
2. Backend validates password using BCrypt.
3. Backend issues short-lived JWT access token.
4. Frontend stores the token in memory and mirrors session metadata in local storage.
5. Protected API calls require `Authorization: Bearer <token>`.

## Employee

1. Employee enters employee ID and 4-digit PIN.
2. Backend validates the hashed PIN.
3. Employee receives a limited JWT scoped to employee portal actions only.
4. Employee UI shows only today's assignment, announcements, current status, check in, and check out.

## Security Notes

- PIN and password hashes use BCrypt.
- JWT includes subject, role, and employee ID when applicable.
- Role-based permissions are enforced in Spring Security.
- Every login, check-in, check-out, and admin change writes an audit log entry.
- The backend is the source of truth for schedule windows and duplicate prevention.

