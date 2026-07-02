import { useMemo, useState } from "react";
import { AdminDashboard } from "../features/admin/AdminDashboard";
import { LoginPage } from "../features/auth/LoginPage";
import type { AuthResponse } from "../lib/api";
import { EmployeeHome } from "../features/employee/EmployeeHome";

export function App() {
  const [session, setSession] = useState<AuthResponse | null>(() => {
    const raw = localStorage.getItem("artdecor.session");
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  });

  const mode = useMemo(() => {
    if (!session) {
      return "login";
    }

    return session.role === "EMPLOYEE" ? "employee" : "admin";
  }, [session]);

  function handleAuthenticated(nextSession: AuthResponse) {
    localStorage.setItem("artdecor.session", JSON.stringify(nextSession));
    setSession(nextSession);
  }

  function handleLogout() {
    localStorage.removeItem("artdecor.session");
    setSession(null);
  }

  if (mode === "employee" && session) {
    return <EmployeeHome session={session} onLogout={handleLogout} />;
  }

  if (mode === "admin" && session) {
    return <AdminDashboard session={session} onLogout={handleLogout} />;
  }

  return <LoginPage onAuthenticated={handleAuthenticated} />;
}

