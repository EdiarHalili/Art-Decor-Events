# Windows Local Testing Guide

Use this guide to verify Phase 1 and Phase 2 locally on Windows.

## Requirements

- Java 21
- Maven
- Node.js 20 or newer
- Docker Desktop

## 1. Start PostgreSQL

From the project root:

```powershell
cd "C:\Users\1\Documents\Art Decor Events"
docker compose up -d postgres
docker compose ps
```

The local database uses:

```powershell
DB_URL=jdbc:postgresql://localhost:5432/artdecor_workforce
DB_USERNAME=artdecor
DB_PASSWORD=artdecor_dev_password
```

## 2. Start the backend

Open a new PowerShell window:

```powershell
cd "C:\Users\1\Documents\Art Decor Events\backend"
$env:APP_BOOTSTRAP_ENABLED="true"
$env:APP_BOOTSTRAP_ADMIN_EMAIL="admin@artdecor.local"
$env:APP_BOOTSTRAP_ADMIN_PASSWORD="ChangeMe123!"
$env:APP_BOOTSTRAP_EMPLOYEE_CODE="EMP001"
$env:APP_BOOTSTRAP_EMPLOYEE_PIN="1234"
$env:JWT_SECRET="replace-this-with-a-long-local-development-secret-value-123456"
mvn spring-boot:run
```

Check health in another PowerShell window:

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health
```

Expected result: HTTP 200.

## 3. Start the frontend

Open another PowerShell window:

```powershell
cd "C:\Users\1\Documents\Art Decor Events\frontend"
$env:VITE_API_BASE_URL="http://localhost:8080/api/v1"
npm.cmd install
npm.cmd run dev
```

Open:

```text
http://localhost:5173
```

## 4. Test admin login

Use:

```text
Email: admin@artdecor.local
Password: ChangeMe123!
```

Verify:

- Dashboard loads.
- Employees page lists the bootstrap employee.
- You can create an additional employee with a 4-digit PIN.

## 5. Test Daily Check-in Windows

In the admin app:

1. Open **Daily windows**.
2. Select today or tomorrow.
3. Set an opening time and closing time.
4. Select allowed active employees.
5. Click **Create window**.
6. Click **Open** to manually open check-in.
7. Click **Close** to manually close check-in.
8. Click **Edit** to change date, times, or employee selection.
9. Click **Cancel** to cancel the window.

Expected behavior:

- Only one active non-cancelled window can exist for a date.
- Cancelled windows cannot be opened again.
- Manual **Open** allows check-in even outside the scheduled time.
- Manual **Close** blocks new check-ins immediately.

## 6. Test employee check-in/check-out

Use a private browser window or log out from admin.

Use:

```text
Employee ID: EMP001
PIN: 1234
```

Verify:

- If no daily window is assigned, the employee sees no active window.
- If a window is assigned but closed, Check In is disabled.
- If the window is open and the employee is assigned, Check In is enabled.
- After successful Check In, Check Out becomes available.
- Duplicate Check In and duplicate Check Out are rejected by the backend.

## 7. Run verification commands

Backend:

```powershell
cd "C:\Users\1\Documents\Art Decor Events\backend"
mvn test
```

Frontend:

```powershell
cd "C:\Users\1\Documents\Art Decor Events\frontend"
npm.cmd run build
```
