const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1";

export type AuthResponse = {
  accessToken: string;
  tokenType: "Bearer";
  expiresInSeconds: number;
  role: "ADMINISTRATOR" | "SUPERVISOR" | "EMPLOYEE";
  fullName: string;
  employeeId: string | null;
};

export type CurrentUserResponse = {
  userId: string;
  role: "ADMINISTRATOR" | "SUPERVISOR" | "EMPLOYEE";
  fullName: string;
  employeeId: string | null;
};

export type Employee = {
  id: string;
  employeeCode: string;
  fullName: string;
  phone: string | null;
  profilePhotoUrl: string | null;
  positionTitle: string | null;
  departmentName: string | null;
  teamName: string | null;
  notes: string | null;
  status: "ACTIVE" | "INACTIVE";
  wageType: "HOURLY" | "DAILY" | "MONTHLY";
  baseWage: number;
  overtimeMultiplier: number;
  createdAt: string;
  updatedAt: string | null;
};

export type AdminUser = {
  id: string;
  fullName: string;
  email: string;
  role: "ADMINISTRATOR" | "SUPERVISOR" | "EMPLOYEE";
  status: "ACTIVE" | "INACTIVE";
  createdAt: string;
  updatedAt: string | null;
};

export type AdminDashboardSnapshot = {
  date: string;
  present: number;
  late: number;
  absent: number;
  currentlyWorking: number;
  activeEmployees: number;
  inactiveEmployees: number;
  administrators: number;
  supervisors: number;
  quickActions: string[];
};

export type EmployeeToday = {
  employeeName: string;
  scheduleId: string | null;
  assignment: string;
  status: string;
  checkInOpen: boolean;
  checkOutAvailable: boolean;
  checkInOpensAt: string | null;
  checkInClosesAt: string | null;
  serverNow: string;
  announcements: string[];
};

export type AttendanceResponse = {
  id: string;
  scheduleId: string;
  employeeId: string;
  status: "PRESENT" | "LATE" | "CHECKED_OUT" | "PENDING_APPROVAL";
  checkedInAt: string | null;
  checkedOutAt: string | null;
  workedMinutes: number;
  overtimeMinutes: number;
  requiresApproval: boolean;
  approvalReason: string | null;
};

export type DailyCheckInWindow = {
  id: string;
  workDate: string;
  checkInOpensAt: string;
  checkInClosesAt: string;
  status: "DRAFT" | "PUBLISHED" | "CHECK_IN_OPEN" | "CHECK_IN_CLOSED" | "CANCELLED" | "COMPLETED";
  employeeIds: string[];
  allowedEmployeeCount: number;
  createdAt: string;
  updatedAt: string | null;
};

export type AttendanceReportRow = {
  workDate: string;
  scheduleId: string;
  employeeId: string;
  employeeCode: string;
  employeeName: string;
  status: "SCHEDULED" | "PRESENT" | "LATE" | "ABSENT" | "CHECKED_OUT" | "PENDING_APPROVAL";
  checkedInAt: string | null;
  checkedOutAt: string | null;
  workedMinutes: number;
  overtimeMinutes: number;
  late: boolean;
  absent: boolean;
};

export type AttendanceReportSummary = {
  assigned: number;
  present: number;
  late: number;
  absent: number;
  checkedOut: number;
  workedMinutes: number;
  overtimeMinutes: number;
};

export type AttendanceReportBucket = {
  label: string;
  assigned: number;
  present: number;
  late: number;
  absent: number;
  workedMinutes: number;
  overtimeMinutes: number;
};

export type EmployeeAttendanceSummary = {
  employeeId: string;
  employeeCode: string;
  employeeName: string;
  assigned: number;
  present: number;
  late: number;
  absent: number;
  workedMinutes: number;
  overtimeMinutes: number;
};

export type AttendanceReport = {
  from: string;
  to: string;
  period: "daily" | "weekly" | "monthly";
  summary: AttendanceReportSummary;
  buckets: AttendanceReportBucket[];
  employees: EmployeeAttendanceSummary[];
  rows: AttendanceReportRow[];
};

export type AppSettings = {
  companyName: string;
  logoUrl: string | null;
  primaryColor: string;
  accentColor: string;
  timezone: string;
  defaultCheckInOpenTime: string;
  defaultCheckInCloseTime: string;
  allowedLateMinutes: number;
  gpsEnabled: boolean;
  notificationsEnabled: boolean;
  sessionTimeoutMinutes: number;
  updatedAt: string | null;
};

export type Announcement = {
  id: string;
  title: string;
  body: string;
  visibleFrom: string;
  visibleUntil: string | null;
  createdAt: string;
};

export type AuditLog = {
  id: string;
  action: string;
  entityType: string;
  entityId: string | null;
  createdAt: string;
};

export type PayrollPreparation = {
  month: string;
  status: string;
  employees: {
    employeeId: string;
    employeeCode: string;
    employeeName: string;
    wageType: Employee["wageType"];
    baseWage: number;
    overtimeMultiplier: number;
    workedMinutes: number;
    overtimeMinutes: number;
  }[];
};

type DailyCheckInWindowPayload = {
  workDate: string;
  checkInOpensAt: string;
  checkInClosesAt: string;
  employeeIds: string[];
};

export async function loginEmployee(employeeCode: string, pin: string): Promise<AuthResponse> {
  return request<AuthResponse>("/auth/employee/login", {
    method: "POST",
    body: JSON.stringify({ employeeCode, pin }),
  });
}

export async function loginAdmin(email: string, password: string): Promise<AuthResponse> {
  return request<AuthResponse>("/auth/admin/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export async function getCurrentUser(accessToken: string): Promise<CurrentUserResponse> {
  return request<CurrentUserResponse>("/auth/me", {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
}

export async function listEmployees(accessToken: string): Promise<Employee[]> {
  return authorizedRequest<Employee[]>("/admin/employees", accessToken);
}

export async function createEmployee(
  accessToken: string,
  payload: {
    employeeCode: string;
    fullName: string;
    pin: string;
    phone?: string;
    profilePhotoUrl?: string;
    positionTitle?: string;
    departmentName?: string;
    teamName?: string;
    notes?: string;
    wageType: Employee["wageType"];
    baseWage: number;
    overtimeMultiplier: number;
  },
): Promise<Employee> {
  return authorizedRequest<Employee>("/admin/employees", accessToken, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateEmployee(
  accessToken: string,
  employeeId: string,
  payload: {
    fullName: string;
    pin?: string;
    phone?: string;
    profilePhotoUrl?: string;
    positionTitle?: string;
    departmentName?: string;
    teamName?: string;
    notes?: string;
    wageType: Employee["wageType"];
    baseWage: number;
    overtimeMultiplier: number;
  },
): Promise<Employee> {
  return authorizedRequest<Employee>(`/admin/employees/${employeeId}`, accessToken, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export async function deactivateEmployee(accessToken: string, employeeId: string): Promise<Employee> {
  return authorizedRequest<Employee>(`/admin/employees/${employeeId}/deactivate`, accessToken, {
    method: "POST",
  });
}

export async function listAdminUsers(accessToken: string): Promise<AdminUser[]> {
  return authorizedRequest<AdminUser[]>("/admin/users", accessToken);
}

export async function createAdminUser(
  accessToken: string,
  payload: {
    fullName: string;
    email: string;
    password: string;
    role: "ADMINISTRATOR" | "SUPERVISOR";
  },
): Promise<AdminUser> {
  return authorizedRequest<AdminUser>("/admin/users", accessToken, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function deactivateAdminUser(accessToken: string, userId: string): Promise<AdminUser> {
  return authorizedRequest<AdminUser>(`/admin/users/${userId}/deactivate`, accessToken, {
    method: "POST",
  });
}

export async function getAdminDashboard(accessToken: string): Promise<AdminDashboardSnapshot> {
  return authorizedRequest<AdminDashboardSnapshot>("/admin/dashboard", accessToken);
}

export async function getEmployeeToday(accessToken: string): Promise<EmployeeToday> {
  return authorizedRequest<EmployeeToday>("/employee/today", accessToken);
}

export async function checkIn(
  accessToken: string,
  payload: { scheduleId: string; latitude?: number; longitude?: number; device: Record<string, string> },
): Promise<AttendanceResponse> {
  return authorizedRequest<AttendanceResponse>("/employee/attendance/check-in", accessToken, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function checkOut(
  accessToken: string,
  payload: { scheduleId: string; latitude?: number; longitude?: number; device: Record<string, string> },
): Promise<AttendanceResponse> {
  return authorizedRequest<AttendanceResponse>("/employee/attendance/check-out", accessToken, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function listCheckInWindows(accessToken: string): Promise<DailyCheckInWindow[]> {
  return authorizedRequest<DailyCheckInWindow[]>("/admin/check-in-windows", accessToken);
}

export async function createCheckInWindow(
  accessToken: string,
  payload: DailyCheckInWindowPayload,
): Promise<DailyCheckInWindow> {
  return authorizedRequest<DailyCheckInWindow>("/admin/check-in-windows", accessToken, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function updateCheckInWindow(
  accessToken: string,
  windowId: string,
  payload: DailyCheckInWindowPayload,
): Promise<DailyCheckInWindow> {
  return authorizedRequest<DailyCheckInWindow>(`/admin/check-in-windows/${windowId}`, accessToken, {
    method: "PATCH",
    body: JSON.stringify(payload),
  });
}

export async function openCheckInWindow(accessToken: string, windowId: string): Promise<DailyCheckInWindow> {
  return authorizedRequest<DailyCheckInWindow>(`/admin/check-in-windows/${windowId}/open`, accessToken, {
    method: "POST",
  });
}

export async function closeCheckInWindow(accessToken: string, windowId: string): Promise<DailyCheckInWindow> {
  return authorizedRequest<DailyCheckInWindow>(`/admin/check-in-windows/${windowId}/close`, accessToken, {
    method: "POST",
  });
}

export async function cancelCheckInWindow(accessToken: string, windowId: string): Promise<DailyCheckInWindow> {
  return authorizedRequest<DailyCheckInWindow>(`/admin/check-in-windows/${windowId}/cancel`, accessToken, {
    method: "POST",
  });
}

export async function getAttendanceReport(
  accessToken: string,
  params: { from: string; to: string; period: "daily" | "weekly" | "monthly" },
): Promise<AttendanceReport> {
  return authorizedRequest<AttendanceReport>(
    `/admin/reports/attendance?from=${encodeURIComponent(params.from)}&to=${encodeURIComponent(params.to)}&period=${params.period}`,
    accessToken,
  );
}

export async function getEmployeeHistory(
  accessToken: string,
  employeeId: string,
  params: { from: string; to: string },
): Promise<AttendanceReportRow[]> {
  return authorizedRequest<AttendanceReportRow[]>(
    `/admin/reports/employees/${employeeId}/history?from=${encodeURIComponent(params.from)}&to=${encodeURIComponent(params.to)}`,
    accessToken,
  );
}

export async function exportAttendanceReport(
  accessToken: string,
  params: { from: string; to: string; format: "csv" | "xlsx" | "pdf" },
): Promise<Blob> {
  return authorizedBlobRequest(
    `/admin/reports/attendance/export?from=${encodeURIComponent(params.from)}&to=${encodeURIComponent(params.to)}&format=${params.format}`,
    accessToken,
  );
}

export async function getSettings(): Promise<AppSettings> {
  return request<AppSettings>("/settings");
}

export async function updateSettings(accessToken: string, payload: AppSettings): Promise<AppSettings> {
  return authorizedRequest<AppSettings>("/admin/settings", accessToken, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function listAnnouncements(accessToken: string): Promise<Announcement[]> {
  return authorizedRequest<Announcement[]>("/admin/announcements", accessToken);
}

export async function publishAnnouncement(
  accessToken: string,
  payload: { title: string; body: string; visibleFrom?: string; visibleUntil?: string },
): Promise<Announcement> {
  return authorizedRequest<Announcement>("/admin/announcements", accessToken, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function listAuditLogs(accessToken: string): Promise<AuditLog[]> {
  return authorizedRequest<AuditLog[]>("/admin/settings/audit-logs", accessToken);
}

export async function getPayrollPreparation(accessToken: string, month: string): Promise<PayrollPreparation> {
  return authorizedRequest<PayrollPreparation>(
    `/admin/payroll/preparation?month=${encodeURIComponent(month)}`,
    accessToken,
  );
}

export async function subscribePush(
  accessToken: string,
  payload: { endpoint: string; p256dhKey: string; authKey: string },
): Promise<void> {
  return authorizedRequest<void>("/notifications/subscribe", accessToken, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

function authorizedRequest<T>(path: string, accessToken: string, init: RequestInit = {}): Promise<T> {
  return request<T>(path, {
    ...init,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      ...init.headers,
    },
  });
}

function authorizedBlobRequest(path: string, accessToken: string, init: RequestInit = {}): Promise<Blob> {
  return requestBlob(path, {
    ...init,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      ...init.headers,
    },
  });
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...init.headers,
    },
  });

  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }

  if (response.status === 204 || response.headers.get("content-length") === "0") {
    return undefined as T;
  }

  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

async function requestBlob(path: string, init: RequestInit = {}): Promise<Blob> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      ...init.headers,
    },
  });

  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }

  return response.blob();
}

async function errorMessage(response: Response) {
  try {
    const data = (await response.json()) as { message?: string; details?: Record<string, string> };
    const fieldMessages = data.details ? Object.values(data.details).filter(Boolean) : [];
    return [data.message, ...fieldMessages].filter(Boolean).join(" ") || "The request could not be completed.";
  } catch {
    return "The request could not be completed.";
  }
}
