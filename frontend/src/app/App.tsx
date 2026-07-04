import { FormEvent, useEffect, useMemo, useState } from "react";
import { AdminDashboard } from "../features/admin/AdminDashboard";
import { LoginPage } from "../features/auth/LoginPage";
import { changePassword, getCurrentUser, getSettings, type AppSettings, type AuthResponse } from "../lib/api";
import { EmployeeHome } from "../features/employee/EmployeeHome";
import { BrandMark } from "../components/BrandMark";
import { Card } from "../components/ui/Card";
import { Input } from "../components/ui/Input";
import { Button } from "../components/ui/Button";

export function App() {
  const [session, setSession] = useState<AuthResponse | null>(() => {
    const raw = localStorage.getItem("artdecor.session");
    return raw ? (JSON.parse(raw) as AuthResponse) : null;
  });
  const [validatingSession, setValidatingSession] = useState(Boolean(session));
  const [settings, setSettings] = useState<AppSettings | null>(null);

  useEffect(() => {
    getSettings()
      .then((response) => {
        setSettings(response);
        applyBrandColors(response);
      })
      .catch(() => undefined);
  }, []);

  useEffect(() => {
    function handleSessionExpired() {
      localStorage.removeItem("artdecor.session");
      setSession(null);
    }

    window.addEventListener("artdecor:session-expired", handleSessionExpired);
    return () => window.removeEventListener("artdecor:session-expired", handleSessionExpired);
  }, []);

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
          passwordMustChange: currentUser.passwordMustChange,
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
          <BrandMark logoUrl={settings?.logoUrl} companyName={settings?.companyName} />
          <div className="h-1.5 w-48 overflow-hidden rounded-full bg-muted">
            <div className="h-full w-1/2 animate-pulse rounded-full bg-primary" />
          </div>
        </div>
      </main>
    );
  }

  if (session?.passwordMustChange) {
    return <ChangePasswordScreen session={session} settings={settings} onChanged={handleAuthenticated} onLogout={handleLogout} />;
  }

  if (mode === "employee" && session) {
    return <EmployeeHome session={session} settings={settings} onLogout={handleLogout} />;
  }

  if (mode === "admin" && session) {
    return <AdminDashboard session={session} settings={settings} onSettingsUpdated={setSettings} onLogout={handleLogout} />;
  }

  return <LoginPage settings={settings} onAuthenticated={handleAuthenticated} />;
}

function ChangePasswordScreen({
  session,
  settings,
  onChanged,
  onLogout,
}: {
  session: AuthResponse;
  settings: AppSettings | null;
  onChanged: (session: AuthResponse) => void;
  onLogout: () => void;
}) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    if (newPassword.length < 8) {
      setMessage("Password must be at least 8 characters.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setMessage("New passwords do not match.");
      return;
    }

    setSaving(true);
    try {
      const updated = await changePassword(session.accessToken, { currentPassword, newPassword });
      onChanged(updated);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Password could not be changed.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <main className="brand-surface flex min-h-screen items-center justify-center px-5 py-8">
      <Card className="w-full max-w-md p-6 shadow-corporate">
        <BrandMark logoUrl={settings?.logoUrl} companyName={settings?.companyName} />
        <div className="mt-6">
          <h1 className="text-xl font-semibold">Change password</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Your account is using a temporary password. Set a new password before continuing.
          </p>
        </div>
        <form className="mt-5 space-y-3" onSubmit={submit}>
          <Input type="password" placeholder="Current temporary password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} required />
          <Input type="password" placeholder="New password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} minLength={8} required />
          <Input type="password" placeholder="Confirm new password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} minLength={8} required />
          {message && <p className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">{message}</p>}
          <Button className="w-full" disabled={saving}>{saving ? "Saving..." : "Save password"}</Button>
          <Button type="button" variant="ghost" className="w-full" onClick={onLogout}>Sign out</Button>
        </form>
      </Card>
    </main>
  );
}

function applyBrandColors(settings: AppSettings) {
  const primary = hexToHsl(settings.primaryColor);
  const accent = hexToHsl(settings.accentColor);
  if (primary) {
    document.documentElement.style.setProperty("--primary", primary);
  }
  if (accent) {
    document.documentElement.style.setProperty("--accent", accent);
  }
}

function hexToHsl(hex: string) {
  const match = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  if (!match) {
    return null;
  }
  const r = parseInt(match[1], 16) / 255;
  const g = parseInt(match[2], 16) / 255;
  const b = parseInt(match[3], 16) / 255;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  let h = 0;
  let s = 0;
  const l = (max + min) / 2;

  if (max !== min) {
    const delta = max - min;
    s = l > 0.5 ? delta / (2 - max - min) : delta / (max + min);
    if (max === r) {
      h = (g - b) / delta + (g < b ? 6 : 0);
    } else if (max === g) {
      h = (b - r) / delta + 2;
    } else {
      h = (r - g) / delta + 4;
    }
    h /= 6;
  }

  return `${Math.round(h * 360)} ${Math.round(s * 100)}% ${Math.round(l * 100)}%`;
}
