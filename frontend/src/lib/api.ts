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
