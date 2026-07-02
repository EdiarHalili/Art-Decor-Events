import { useState } from "react";
import { CalendarDays, Clock3, LayoutDashboard, LogOut, ShieldCheck, UserCheck, UserX, UsersRound } from "lucide-react";
import { BrandMark } from "../../components/BrandMark";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import type { AuthResponse } from "../../lib/api";
import detailUrl from "../../assets/brand/event-detail.jpg";
import { EmployeeManagementPage } from "./EmployeeManagementPage";
import { UserManagementPage } from "./UserManagementPage";

type AdminDashboardProps = {
  session: AuthResponse;
  onLogout: () => void;
};

const kpis = [
  { label: "Present", value: "0", icon: UserCheck },
  { label: "Working now", value: "0", icon: UsersRound },
  { label: "Late", value: "0", icon: Clock3 },
  { label: "Absent", value: "0", icon: UserX },
];

type AdminView = "dashboard" | "employees" | "users";

export function AdminDashboard({ session, onLogout }: AdminDashboardProps) {
  const [activeView, setActiveView] = useState<AdminView>("dashboard");

  const navItems = [
    { id: "dashboard" as const, label: "Dashboard", icon: LayoutDashboard },
    { id: "employees" as const, label: "Employees", icon: UsersRound },
    { id: "users" as const, label: "Users & roles", icon: ShieldCheck },
  ];

  return (
    <main className="min-h-screen bg-background">
      <div className="grid min-h-screen lg:grid-cols-[280px_1fr]">
        <aside className="hidden border-r border-border bg-card p-5 lg:block">
          <BrandMark />
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

        <section className="px-4 py-5 sm:px-6 lg:px-8">
          <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="text-sm font-medium text-primary">Operations dashboard</p>
              <h1 className="mt-1 text-2xl font-semibold sm:text-3xl">Today at Art Decor Events</h1>
              <p className="mt-1 text-sm text-muted-foreground">Signed in as {session.fullName}</p>
            </div>
            <div className="flex gap-2">
              <Button variant="secondary">
                <CalendarDays size={18} />
                New schedule
              </Button>
              <Button variant="ghost" onClick={onLogout}>
                <LogOut size={18} />
              </Button>
            </div>
          </header>

          {activeView === "dashboard" && <DashboardOverview />}
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
        </section>
      </div>
    </main>
  );
}

function DashboardOverview() {
  return (
    <>
      <section className="mt-6 overflow-hidden rounded-lg border border-border bg-card shadow-corporate">
            <div className="grid lg:grid-cols-[1fr_360px]">
              <div className="p-6">
                <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">Phase 1 foundation</p>
                <h2 className="mt-3 text-2xl font-semibold">Ready for employee setup and schedule creation</h2>
                <p className="mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
                  The system foundation is prepared for attendance windows, employee assignments, reports, exports,
                  offline sync, GPS capture, and future payroll calculations.
                </p>
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
                Attendance events will appear here when schedules are published.
              </div>
            </Card>
            <Card className="p-5">
              <h2 className="font-semibold">Quick actions</h2>
              <div className="mt-5 grid gap-3">
                <Button variant="secondary">Add employee</Button>
                <Button variant="secondary">Create workday</Button>
                <Button variant="secondary">Post announcement</Button>
              </div>
            </Card>
          </section>
    </>
  );
}
