import { useEffect, useState } from "react";
import { BarChart3, CalendarClock, CalendarDays, Clock3, LayoutDashboard, LogOut, MapPin, RefreshCw, Settings, ShieldCheck, UserCheck, UserX, UsersRound } from "lucide-react";
import { BrandMark } from "../../components/BrandMark";
import { ThemeToggle } from "../../components/ThemeToggle";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { getAdminDashboard, type AdminDashboardSnapshot, type AppSettings, type AuthResponse } from "../../lib/api";
import detailUrl from "../../assets/brand/event-detail.jpg";
import { DailyCheckInWindowPage } from "./DailyCheckInWindowPage";
import { EmployeeManagementPage } from "./EmployeeManagementPage";
import { ReportsPage } from "./ReportsPage";
import { SettingsPage } from "./SettingsPage";
import { UserManagementPage } from "./UserManagementPage";

type AdminDashboardProps = {
  session: AuthResponse;
  settings: AppSettings | null;
  onSettingsUpdated: (settings: AppSettings) => void;
  onLogout: () => void;
};

type AdminView = "dashboard" | "windows" | "reports" | "employees" | "users" | "settings";

export function AdminDashboard({ session, settings, onSettingsUpdated, onLogout }: AdminDashboardProps) {
  const [activeView, setActiveView] = useState<AdminView>("dashboard");

  const navItems = [
    { id: "dashboard" as const, label: "Dashboard", icon: LayoutDashboard },
    { id: "windows" as const, label: "Daily windows", icon: CalendarClock },
    { id: "reports" as const, label: "Reports", icon: BarChart3 },
    { id: "employees" as const, label: "Employees", icon: UsersRound },
    { id: "users" as const, label: "Users & roles", icon: ShieldCheck },
    { id: "settings" as const, label: "Settings", icon: Settings },
  ];

  return (
    <main className="min-h-screen bg-background pb-[env(safe-area-inset-bottom)] pt-[env(safe-area-inset-top)]">
      <div className="grid min-h-screen lg:grid-cols-[280px_1fr]">
        <aside className="hidden border-r border-border bg-card p-5 lg:block">
          <BrandMark logoUrl={settings?.logoUrl} companyName={settings?.companyName} />
          <nav className="mt-8 space-y-2 text-sm">
            {navItems.map((item) => (
              <button
                key={item.id}
                onClick={() => setActiveView(item.id)}
                className={`flex w-full items-center gap-3 rounded-md px-3 py-2 text-left font-medium transition ${
                  activeView === item.id ? "bg-muted text-foreground" : "text-muted-foreground hover:bg-muted hover:text-foreground"
                }`}
              >
                <item.icon size={17} />
                {item.label}
              </button>
            ))}
          </nav>
        </aside>

        <section className="min-w-0 px-4 py-5 sm:px-6 lg:px-8">
          <header className="sticky top-0 z-20 -mx-4 flex flex-col gap-4 border-b border-border bg-background/90 px-4 py-3 backdrop-blur sm:-mx-6 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:static lg:mx-0 lg:border-b-0 lg:bg-transparent lg:px-0 lg:py-0">
            <div>
              <p className="text-sm font-medium text-primary">Operations dashboard</p>
              <h1 className="mt-1 text-2xl font-semibold sm:text-3xl">Today at {settings?.companyName ?? "Art Decor Events"}</h1>
              <p className="mt-1 text-sm text-muted-foreground">Signed in as {session.fullName}</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <ThemeToggle />
              <Button variant="secondary" onClick={() => setActiveView("windows")}>
                <CalendarDays size={18} />
                New window
              </Button>
              <Button variant="ghost" onClick={onLogout}>
                <LogOut size={18} />
              </Button>
            </div>
          </header>

          <nav className="mt-5 flex gap-2 overflow-x-auto pb-1 lg:hidden">
            {navItems.map((item) => (
              <Button
                key={item.id}
                type="button"
                variant={activeView === item.id ? "primary" : "secondary"}
                onClick={() => setActiveView(item.id)}
                className="shrink-0"
              >
                <item.icon size={17} />
                {item.label}
              </Button>
            ))}
          </nav>

          {activeView === "dashboard" && <DashboardOverview accessToken={session.accessToken} />}
          {activeView === "windows" && (
            <section className="mt-6">
              <DailyCheckInWindowPage accessToken={session.accessToken} settings={settings} />
            </section>
          )}
          {activeView === "reports" && (
            <section className="mt-6">
              <ReportsPage accessToken={session.accessToken} />
            </section>
          )}
          {activeView === "employees" && (
            <section className="mt-6">
              <EmployeeManagementPage accessToken={session.accessToken} />
            </section>
          )}
          {activeView === "users" && (
            <section className="mt-6">
              <UserManagementPage accessToken={session.accessToken} />
            </section>
          )}
          {activeView === "settings" && (
            <section className="mt-6">
              <SettingsPage accessToken={session.accessToken} settings={settings} onSettingsUpdated={onSettingsUpdated} />
            </section>
          )}
        </section>
      </div>
    </main>
  );
}

function DashboardOverview({ accessToken }: { accessToken: string }) {
  const [snapshot, setSnapshot] = useState<AdminDashboardSnapshot | null>(null);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    let active = true;

    async function loadDashboard() {
      setLoading(true);
      try {
        const nextSnapshot = await getAdminDashboard(accessToken);
        if (active) {
          setSnapshot(nextSnapshot);
          setMessage("");
        }
      } catch (error) {
        if (active) {
          setMessage(error instanceof Error ? error.message : "Dashboard data could not be loaded.");
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    }

    void loadDashboard();
    const interval = window.setInterval(() => void loadDashboard(), 15000);
    return () => {
      active = false;
      window.clearInterval(interval);
    };
  }, [accessToken]);

  const kpis = [
    { label: "Present", value: snapshot?.present ?? 0, icon: UserCheck },
    { label: "Working now", value: snapshot?.currentlyWorking ?? 0, icon: UsersRound },
    { label: "Late", value: snapshot?.late ?? 0, icon: Clock3 },
    { label: "Absent", value: snapshot?.absent ?? 0, icon: UserX },
  ];

  return (
    <>
      <section className="mt-6 overflow-hidden rounded-lg border border-border bg-card shadow-corporate">
            <div className="grid lg:grid-cols-[1fr_360px]">
              <div className="p-6">
                <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">Phase 1 foundation</p>
                <h2 className="mt-3 text-2xl font-semibold">Ready for employee setup and daily check-in windows</h2>
                <p className="mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
                  {snapshot
                    ? `${snapshot.activeEmployees} active employees, ${snapshot.inactiveEmployees} inactive employees, ${snapshot.administrators} administrators, and ${snapshot.supervisors} supervisors are registered.`
                    : "The system foundation is prepared for attendance windows, employee assignments, reports, exports, offline sync, GPS capture, and future payroll calculations."}
                </p>
                {message && <p className="mt-4 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}
              </div>
              <img src={detailUrl} alt="" className="h-64 w-full object-cover lg:h-full" />
            </div>
          </section>

          <section className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {kpis.map((kpi) => (
              <Card key={kpi.label} className="p-5">
                <div className="flex items-center justify-between">
                  <p className="text-sm text-muted-foreground">{kpi.label}</p>
                  <kpi.icon className="text-primary" size={21} />
                </div>
                <p className="mt-4 text-3xl font-semibold">{kpi.value}</p>
              </Card>
            ))}
          </section>

          <section className="mt-6 grid gap-4 xl:grid-cols-[1.25fr_0.75fr]">
            <Card className="p-5">
              <div className="flex items-center justify-between gap-3">
                <h2 className="font-semibold">Live attendance</h2>
                {loading && <RefreshCw className="animate-spin text-muted-foreground" size={16} />}
              </div>
              <div className="mt-5 divide-y divide-border rounded-lg border border-border">
                {!loading && (snapshot?.liveAttendance.length ?? 0) === 0 && (
                  <div className="p-6 text-center text-sm text-muted-foreground">
                    No employees are checked in yet today.
                  </div>
                )}
                {snapshot?.liveAttendance.map((row) => (
                  <div key={`${row.employeeId}-${row.checkedInAt}`} className="grid gap-3 p-4 text-sm md:grid-cols-[1.2fr_1fr_auto] md:items-center">
                    <div className="min-w-0">
                      <p className="truncate font-semibold">{row.employeeName}</p>
                      <p className="mt-1 text-xs text-muted-foreground">{row.employeeCode}</p>
                    </div>
                    <div className="grid grid-cols-2 gap-2 text-xs text-muted-foreground sm:text-sm">
                      <span>In: {formatTime(row.checkedInAt)}</span>
                      <span>{row.autoCheckout ? "Auto Check Out" : "Out"}: {formatTime(row.checkedOutAt)}</span>
                      <span>Worked: {formatMinutes(row.workedMinutes)}</span>
                      <span>Overtime: {row.overtimeMinutes > 0 ? formatMinutes(row.overtimeMinutes) : "None"}</span>
                    </div>
                    <span className={`rounded-md px-2 py-1 text-xs font-medium ${row.late ? "bg-destructive/10 text-destructive" : "bg-accent/10 text-accent"}`}>
                      {row.autoCheckout ? "AUTO CHECK OUT" : row.status.replaceAll("_", " ")}
                    </span>
                  </div>
                ))}
              </div>
            </Card>
            <Card className="p-5">
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <MapPin className="text-primary" size={19} />
                  <h2 className="font-semibold">Live locations</h2>
                </div>
                <span className="text-xs text-muted-foreground">{snapshot?.liveLocations.length ?? 0} active</span>
              </div>
              <div className="mt-5 divide-y divide-border rounded-lg border border-border">
                {(snapshot?.liveLocations.length ?? 0) === 0 && (
                  <div className="p-5 text-sm text-muted-foreground">
                    No live locations are available. Locations appear only while employees are checked in and tracking is enabled.
                  </div>
                )}
                {snapshot?.liveLocations.map((location) => (
                  <div key={location.attendanceRecordId} className="grid gap-3 p-4 text-sm md:grid-cols-[1fr_auto] md:items-center">
                    <div>
                      <p className="font-semibold">{location.employeeName}</p>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {location.employeeCode} · {location.latitude.toFixed(5)}, {location.longitude.toFixed(5)}
                        {location.accuracyMeters != null ? ` · ±${Math.round(location.accuracyMeters)}m` : ""}
                      </p>
                      <p className="mt-1 text-xs text-muted-foreground">Updated {formatDateTime(location.capturedAt)}</p>
                    </div>
                    <a
                      className="inline-flex h-11 items-center justify-center rounded-md border border-border bg-card px-4 text-sm font-semibold text-card-foreground transition hover:bg-muted"
                      href={`https://maps.google.com/?q=${location.latitude},${location.longitude}`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      View on Map
                    </a>
                  </div>
                ))}
              </div>
            </Card>
            <Card className="p-5">
              <h2 className="font-semibold">Quick actions</h2>
              <div className="mt-5 grid gap-3">
                {(snapshot?.quickActions ?? ["Add employee", "Create check-in window", "Post announcement"]).map((action) => (
                  <Button key={action} variant="secondary">
                    {action}
                  </Button>
                ))}
              </div>
            </Card>
          </section>
    </>
  );
}

function formatTime(value: string | null) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat(undefined, { hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}

function formatDateTime(value: string | null) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function formatMinutes(minutes: number) {
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return `${hours}h ${remainder}m`;
}
