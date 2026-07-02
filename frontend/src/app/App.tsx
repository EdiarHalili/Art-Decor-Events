import { useEffect, useMemo, useState } from "react";
import { AdminDashboard } from "../features/admin/AdminDashboard";
import { LoginPage } from "../features/auth/LoginPage";
import { getCurrentUser, type AuthResponse } from "../lib/api";
import { EmployeeHome } from "../features/employee/EmployeeHome";
import { BrandMark } from "../components/BrandMark";

export function App() {
  const [session, setSession] = useState<AuthResponse | null>(() => {
    const raw = localStorage.getItem("artdecor.session");
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  });
  const [validatingSession, setValidatingSession] = useState(Boolean(session));

  useEffect(() => {
    if (!session) {
      setValidatingSession(false);
      return;
    }

    let cancelled = false;

    getCurrentUser(session.accessToken)
      .then((currentUser) => {
        if (cancelled) {
          return;
        }

        const refreshedSession = {
          ...session,
          role: currentUser.role,
          fullName: currentUser.fullName,
          employeeId: currentUser.employeeId,
        };
        localStorage.setItem("artdecor.session", JSON.stringify(refreshedSession));
        setSession(refreshedSession);
      })
      .catch(() => {
        if (!cancelled) {
          localStorage.removeItem("artdecor.session");
          setSession(null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setValidatingSession(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

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

  if (validatingSession) {
    return (
      <main className="brand-surface flex min-h-screen items-center justify-center px-5">
        <div className="flex flex-col items-center gap-5 rounded-lg border border-border bg-card p-8 shadow-corporate">
          <BrandMark />
          <div className="h-1.5 w-48 overflow-hidden rounded-full bg-muted">
            <div className="h-full w-1/2 animate-pulse rounded-full bg-primary" />
          </div>
        </div>
      </main>
    );
  }

  if (mode === "employee" && session) {
    return <EmployeeHome session={session} onLogout={handleLogout} />;
  }

  if (mode === "admin" && session) {
    return <AdminDashboard session={session} onLogout={handleLogout} />;
  }

  return <LoginPage onAuthenticated={handleAuthenticated} />;
}
