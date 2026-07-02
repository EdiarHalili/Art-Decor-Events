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

function authorizedRequest<T>(path: string, accessToken: string, init: RequestInit = {}): Promise<T> {
  return request<T>(path, {
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
    throw new Error("The request could not be completed.");
  }

  return response.json() as Promise<T>;
}
