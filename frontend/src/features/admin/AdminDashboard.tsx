import { useEffect, useState } from "react";
import { BarChart3, CalendarClock, CalendarDays, Clock3, LayoutDashboard, LogOut, Settings, ShieldCheck, UserCheck, UserX, UsersRound } from "lucide-react";
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

  useEffect(() => {
    getAdminDashboard(accessToken)
      .then(setSnapshot)
      .catch(() => setMessage("Dashboard data could not be loaded."));
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
              <h2 className="font-semibold">Live attendance</h2>
              <div className="mt-5 rounded-md border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
                Attendance activity will appear here when daily check-in windows are opened.
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
