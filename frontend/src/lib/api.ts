const API_BASE_URL = resolveApiBaseUrl();

export type AuthResponse = {
  accessToken: string;
  tokenType: "Bearer";
  expiresInSeconds: number;
  role: "ADMINISTRATOR" | "SUPERVISOR" | "EMPLOYEE";
  fullName: string;
  employeeId: string | null;
  employeeCode: string | null;
  passwordMustChange: boolean;
};

export type CurrentUserResponse = {
  userId: string;
  role: "ADMINISTRATOR" | "SUPERVISOR" | "EMPLOYEE";
  fullName: string;
  employeeId: string | null;
  employeeCode: string | null;
  passwordMustChange: boolean;
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
  liveAttendance: AdminLiveAttendanceRow[];
  liveLocations: AdminLiveLocationRow[];
  quickActions: string[];
};

export type AdminLiveAttendanceRow = {
  attendanceRecordId: string;
  employeeId: string;
  employeeCode: string;
  employeeName: string;
  status: "PRESENT" | "LATE" | "ABSENT" | "CHECKED_OUT" | "PENDING_APPROVAL" | "SCHEDULED";
  checkedInAt: string | null;
  checkedOutAt: string | null;
  workedMinutes: number;
  overtimeMinutes: number;
  autoCheckout: boolean;
  checkoutType: CheckoutType;
  late: boolean;
};

export type AdminLiveLocationRow = {
  employeeId: string;
  employeeCode: string;
  employeeName: string;
  attendanceRecordId: string;
  latitude: number;
  longitude: number;
  accuracyMeters: number | null;
  capturedAt: string;
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
  simpleOpenMode: boolean;
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
  autoCheckout: boolean;
  checkoutType: CheckoutType;
  requiresApproval: boolean;
  approvalReason: string | null;
};

export type DailyCheckInWindow = {
  id: string;
  workDate: string;
  checkInOpensAt: string;
  checkInClosesAt: string;
  autoCheckoutEnabled: boolean;
  status: "DRAFT" | "PUBLISHED" | "CHECK_IN_OPEN" | "CHECK_IN_CLOSED" | "CANCELLED" | "COMPLETED";
  employeeIds: string[];
  allowedEmployeeCount: number;
  createdAt: string;
  updatedAt: string | null;
};

export type AttendanceReportRow = {
  workDate: string;
  attendanceRecordId: string | null;
  scheduleId: string;
  employeeId: string;
  employeeCode: string;
  employeeName: string;
  status: "SCHEDULED" | "PRESENT" | "LATE" | "ABSENT" | "CHECKED_OUT" | "PENDING_APPROVAL";
  checkedInAt: string | null;
  checkedOutAt: string | null;
  checkInLatitude: number | null;
  checkInLongitude: number | null;
  checkOutLatitude: number | null;
  checkOutLongitude: number | null;
  checkInDistanceMeters: number | null;
  checkOutDistanceMeters: number | null;
  workedMinutes: number;
  overtimeMinutes: number;
  autoCheckout: boolean;
  checkoutType: CheckoutType | null;
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
  workplaceLatitude: number | null;
  workplaceLongitude: number | null;
  notificationsEnabled: boolean;
  liveLocationTrackingEnabled: boolean;
  liveLocationIntervalMinutes: number;
  sessionTimeoutMinutes: number;
  openModeUnlimitedCheckout: boolean;
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
  autoCheckoutEnabled: boolean;
  employeeIds: string[];
};

export type CheckoutType = "MANUAL_EMPLOYEE" | "AUTO_CHECKED_OUT" | "ADMIN_CHECKED_OUT";

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

export async function changePassword(
  accessToken: string,
  payload: { currentPassword: string; newPassword: string },
): Promise<AuthResponse> {
  return authorizedRequest<AuthResponse>("/auth/change-password", accessToken, {
    method: "POST",
    body: JSON.stringify(payload),
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
    password: string;
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
    password?: string;
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

export async function deleteEmployee(accessToken: string, employeeId: string): Promise<void> {
  return authorizedRequest<void>(`/admin/employees/${employeeId}`, accessToken, {
    method: "DELETE",
  });
}

export async function forceDeleteEmployee(accessToken: string, employeeId: string): Promise<void> {
  return authorizedRequest<void>(`/admin/employees/${employeeId}/force`, accessToken, {
    method: "DELETE",
  });
}

export async function getEmployeeDeletionPolicy(accessToken: string): Promise<{ forceDeleteAllowed: boolean }> {
  const policy = await authorizedRequest<{ forceDeleteAllowed?: boolean; allowForceDelete?: boolean }>(
    "/admin/employees/deletion-policy",
    accessToken,
  );
  return { forceDeleteAllowed: Boolean(policy.forceDeleteAllowed ?? policy.allowForceDelete) };
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

export async function resetEmployeePassword(
  accessToken: string,
  employeeId: string,
): Promise<{ temporaryPassword: string; passwordMustChange: boolean }> {
  return authorizedRequest<{ temporaryPassword: string; passwordMustChange: boolean }>(
    `/admin/employees/${employeeId}/reset-password`,
    accessToken,
    { method: "POST" },
  );
}

export async function deactivateAdminUser(accessToken: string, userId: string): Promise<AdminUser> {
  return authorizedRequest<AdminUser>(`/admin/users/${userId}/deactivate`, accessToken, {
    method: "POST",
  });
}

export async function getAdminDashboard(accessToken: string): Promise<AdminDashboardSnapshot> {
  const response = await authorizedRequest<AdminDashboardSnapshot>("/admin/dashboard", accessToken);
  response.liveLocations.forEach((location) =>
    debugGpsLog("api returned live location", {
      employeeId: location.employeeId,
      attendanceRecordId: location.attendanceRecordId,
      latitude: location.latitude,
      longitude: location.longitude,
    }),
  );
  return response;
}

export async function getEmployeeToday(accessToken: string): Promise<EmployeeToday> {
  return authorizedRequest<EmployeeToday>("/employee/today", accessToken);
}

export async function checkIn(
  accessToken: string,
  payload: { scheduleId: string; latitude?: number; longitude?: number; capturedAt?: string; device: Record<string, string> },
): Promise<AttendanceResponse> {
  debugGpsLog("check-in API request payload", {
    scheduleId: payload.scheduleId,
    latitude: payload.latitude,
    longitude: payload.longitude,
  });
  return authorizedRequest<AttendanceResponse>("/employee/attendance/check-in", accessToken, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function checkOut(
  accessToken: string,
  payload: { scheduleId: string; latitude?: number; longitude?: number; capturedAt?: string; device: Record<string, string> },
): Promise<AttendanceResponse> {
  debugGpsLog("check-out API request payload", {
    scheduleId: payload.scheduleId,
    latitude: payload.latitude,
    longitude: payload.longitude,
  });
  return authorizedRequest<AttendanceResponse>("/employee/attendance/check-out", accessToken, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export async function getMyAttendanceHistory(
  accessToken: string,
  params: { from: string; to: string },
): Promise<AttendanceReportRow[]> {
  const response = await authorizedRequest<AttendanceReportRow[]>(
    `/employee/attendance/history?from=${encodeURIComponent(params.from)}&to=${encodeURIComponent(params.to)}`,
    accessToken,
  );
  logAttendanceRows("api returned employee own history", response);
  return response;
}

export async function exportMyAttendance(
  accessToken: string,
  params: { from: string; to: string; format: "pdf" },
): Promise<Blob> {
  return authorizedBlobRequest(
    `/employee/attendance/export?from=${encodeURIComponent(params.from)}&to=${encodeURIComponent(params.to)}&format=${params.format}`,
    accessToken,
  );
}

export async function adminCheckout(
  accessToken: string,
  attendanceRecordId: string,
  checkedOutAt: string,
): Promise<AttendanceResponse> {
  return authorizedRequest<AttendanceResponse>(`/admin/attendance/${attendanceRecordId}/checkout`, accessToken, {
    method: "POST",
    body: JSON.stringify({ checkedOutAt }),
  });
}

export async function extendCheckout(
  accessToken: string,
  attendanceRecordId: string,
  extendedUntil: string,
): Promise<AttendanceResponse> {
  return authorizedRequest<AttendanceResponse>(`/admin/attendance/${attendanceRecordId}/extend`, accessToken, {
    method: "POST",
    body: JSON.stringify({ extendedUntil }),
  });
}

export async function recordLiveLocation(
  accessToken: string,
  payload: {
    latitude: number;
    longitude: number;
    accuracyMeters?: number;
    capturedAt: string;
    device: Record<string, string>;
  },
): Promise<void> {
  debugGpsLog("live-location API request payload", {
    latitude: payload.latitude,
    longitude: payload.longitude,
    accuracyMeters: payload.accuracyMeters,
    capturedAt: payload.capturedAt,
  });
  return authorizedRequest<void>("/employee/live-location", accessToken, {
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
  return authorizedRequest<DailyCheckInWindow>(`/admin/check-in-windows/${windowId}/update`, accessToken, {
    method: "POST",
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

export async function deleteCheckInWindow(accessToken: string, windowId: string): Promise<void> {
  return authorizedRequest<void>(`/admin/check-in-windows/${windowId}`, accessToken, {
    method: "DELETE",
  });
}

export async function getAttendanceReport(
  accessToken: string,
  params: { from: string; to: string; period: "daily" | "weekly" | "monthly" },
): Promise<AttendanceReport> {
  const response = await authorizedRequest<AttendanceReport>(
    `/admin/reports/attendance?from=${encodeURIComponent(params.from)}&to=${encodeURIComponent(params.to)}&period=${params.period}`,
    accessToken,
  );
  logAttendanceRows("api returned attendance report", response.rows);
  return response;
}

export async function getEmployeeHistory(
  accessToken: string,
  employeeId: string,
  params: { from: string; to: string },
): Promise<AttendanceReportRow[]> {
  const response = await authorizedRequest<AttendanceReportRow[]>(
    `/admin/reports/employees/${employeeId}/history?from=${encodeURIComponent(params.from)}&to=${encodeURIComponent(params.to)}`,
    accessToken,
  );
  logAttendanceRows("api returned employee history", response);
  return response;
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

export async function exportEmployeeAttendance(
  accessToken: string,
  employeeId: string,
  params: { from: string; to: string; format: "csv" | "xlsx" | "pdf" },
): Promise<Blob> {
  return authorizedBlobRequest(
    `/admin/reports/employees/${employeeId}/export?from=${encodeURIComponent(params.from)}&to=${encodeURIComponent(params.to)}&format=${params.format}`,
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

export async function updateAnnouncement(
  accessToken: string,
  announcementId: string,
  payload: { title: string; body: string; visibleFrom?: string; visibleUntil?: string | null },
): Promise<Announcement> {
  return authorizedRequest<Announcement>(`/admin/announcements/${announcementId}`, accessToken, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export async function deleteAnnouncement(accessToken: string, announcementId: string): Promise<void> {
  return authorizedRequest<void>(`/admin/announcements/${announcementId}`, accessToken, {
    method: "DELETE",
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
    headers: authenticatedHeaders(accessToken, init.headers, true),
  });
}

function authorizedBlobRequest(path: string, accessToken: string, init: RequestInit = {}): Promise<Blob> {
  return requestBlob(path, {
    ...init,
    headers: authenticatedHeaders(accessToken, init.headers, false),
  });
}

function authenticatedHeaders(accessToken: string, headers?: HeadersInit, includeJsonContentType = false) {
  const next = new Headers(headers);
  if (includeJsonContentType && !next.has("Content-Type")) {
    next.set("Content-Type", "application/json");
  }
  next.set("Authorization", `Bearer ${accessToken}`);
  return next;
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers: jsonHeaders(init.headers),
      cache: "no-store",
    });
  } catch {
    throw new Error(networkErrorMessage());
  }

  if (!response.ok) {
    notifySessionExpired(path, response);
    throw new Error(await errorMessage(response));
  }

  if (response.status === 204 || response.headers.get("content-length") === "0") {
    return undefined as T;
  }

  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

async function requestBlob(path: string, init: RequestInit = {}): Promise<Blob> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...init,
      headers: new Headers(init.headers),
      cache: "no-store",
    });
  } catch {
    throw new Error(networkErrorMessage());
  }

  if (!response.ok) {
    notifySessionExpired(path, response);
    throw new Error(await errorMessage(response));
  }

  return response.blob();
}

function jsonHeaders(headers?: HeadersInit) {
  const next = new Headers(headers);
  if (!next.has("Content-Type")) {
    next.set("Content-Type", "application/json");
  }
  return next;
}

function notifySessionExpired(path: string, response: Response) {
  const isLoginRequest = path === "/auth/admin/login" || path === "/auth/employee/login";
  if (response.status === 401 && !isLoginRequest && typeof window !== "undefined") {
    window.dispatchEvent(new CustomEvent("artdecor:session-expired", {
      detail: "Sesioni juaj ka skaduar. Ju lutemi identifikohuni përsëri.",
    }));
  }
}

async function errorMessage(response: Response) {
  if (response.status >= 500) {
    return "Serveri ka një problem për momentin. Ju lutemi provoni përsëri.";
  }
  try {
    const data = (await response.json()) as { message?: string; details?: Record<string, unknown> };
    const fieldMessages = data.details
      ? Object.entries(data.details)
          .map(([field, message]) => readableFieldMessage(field, message))
          .filter(Boolean)
      : [];
    if (fieldMessages.length > 0) {
      return fieldMessages.join(" ");
    }
    return safeErrorMessage(data.message, "Kërkesa nuk mund të përfundohej.");
  } catch {
    return "Kërkesa nuk mund të përfundohej.";
  }
}

function readableFieldMessage(field: string, message: unknown) {
  if (typeof message !== "string" || !message.trim()) {
    return "";
  }
  if (message.toLowerCase() === "must not be blank") {
    return `${fieldLabel(field)} është e detyrueshme.`;
  }
  return message;
}

function fieldLabel(field: string) {
  const labels: Record<string, string> = {
    employeeCode: "ID e punëtorit",
    pin: "PIN",
    username: "ID e punëtorit",
    password: "Fjalëkalimi",
    email: "Email",
    currentPassword: "Fjalëkalimi aktual",
    newPassword: "Fjalëkalimi i ri",
  };
  return labels[field] ?? field.replace(/([A-Z])/g, " $1").replace(/^./, (letter) => letter.toUpperCase());
}

function safeErrorMessage(message: string | undefined, fallback: string) {
  if (!message || looksInternal(message)) {
    return fallback;
  }
  return message;
}

function looksInternal(message: string) {
  const normalized = message.toLowerCase();
  return (
    normalized.includes("rawpassword") ||
    normalized.includes("nullpointerexception") ||
    normalized.includes("illegalargumentexception") ||
    normalized.includes("constraintviolationexception") ||
    normalized.includes("stack trace") ||
    normalized.includes("cannot be null")
  );
}

export function mapLocationUrl(latitude: number, longitude: number) {
  const url = `https://www.openstreetmap.org/?mlat=${latitude}&mlon=${longitude}#map=18/${latitude}/${longitude}`;
  debugGpsLog("map URL generated", { latitude, longitude, url });
  return url;
}

function resolveApiBaseUrl() {
  const configured = import.meta.env.VITE_API_BASE_URL?.trim();
  if (configured) {
    return configured.replace(/\/$/, "");
  }
  if (typeof window === "undefined") {
    return "/api/v1";
  }
  const { protocol, hostname } = window.location;
  return `${protocol}//${hostname}:8080/api/v1`;
}

function networkErrorMessage() {
  return "Nuk mund të lidhemi me serverin. Kontrolloni internetin, adresën e backend-it ose konfigurimin CORS.";
}

export function isTemporaryNetworkError(error: unknown) {
  return error instanceof Error && error.message === networkErrorMessage();
}

export function debugGpsLog(stage: string, values: Record<string, unknown>) {
  if (!gpsDebugEnabled()) {
    return;
  }
  console.debug(`[GPS DEBUG] ${stage}`, values);
}

function gpsDebugEnabled() {
  if (typeof window !== "undefined" && window.localStorage.getItem("artdecor.gpsDebug") === "false") {
    return false;
  }
  return import.meta.env.DEV;
}

function logAttendanceRows(stage: string, rows: AttendanceReportRow[]) {
  rows
    .filter((row) => row.checkInLatitude != null || row.checkOutLatitude != null)
    .forEach((row) =>
      debugGpsLog(stage, {
        attendanceRecordId: row.attendanceRecordId,
        employeeId: row.employeeId,
        checkInLatitude: row.checkInLatitude,
        checkInLongitude: row.checkInLongitude,
        checkOutLatitude: row.checkOutLatitude,
        checkOutLongitude: row.checkOutLongitude,
      }),
    );
}
