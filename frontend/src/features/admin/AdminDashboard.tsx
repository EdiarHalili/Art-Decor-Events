import { useEffect, useState } from "react";
import { BarChart3, CalendarClock, CalendarDays, LayoutDashboard, LogOut, MapPin, Menu, RefreshCw, Settings, ShieldCheck, UserCheck, UsersRound, X } from "lucide-react";
import { BrandMark } from "../../components/BrandMark";
import { ThemeToggle } from "../../components/ThemeToggle";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { adminCheckout, extendCheckout, getAdminDashboard, mapLocationUrl, type AdminDashboardSnapshot, type AppSettings, type AuthResponse } from "../../lib/api";
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
  const [menuOpen, setMenuOpen] = useState(false);

  const navItems = [
    { id: "dashboard" as const, label: "Paneli", icon: LayoutDashboard },
    { id: "windows" as const, label: "Dritaret ditore", icon: CalendarClock },
    { id: "reports" as const, label: "Raportet", icon: BarChart3 },
    { id: "employees" as const, label: "Punëtorët", icon: UsersRound },
    { id: "users" as const, label: "Përdoruesit", icon: ShieldCheck },
    { id: "settings" as const, label: "Cilësimet", icon: Settings },
  ];

  return (
    <main className="min-h-screen overflow-x-hidden bg-background pb-[env(safe-area-inset-bottom)] pt-[env(safe-area-inset-top)]">
      <div className="grid min-h-screen min-w-0 lg:grid-cols-[280px_1fr]">
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

        <section className="min-w-0 max-w-full px-3 py-4 sm:px-6 lg:px-8">
          <header className="sticky top-0 z-20 -mx-4 flex flex-col gap-4 border-b border-border bg-background/90 px-4 py-3 backdrop-blur sm:-mx-6 sm:flex-row sm:items-center sm:justify-between sm:px-6 lg:static lg:mx-0 lg:border-b-0 lg:bg-transparent lg:px-0 lg:py-0">
            <div>
              <p className="text-sm font-medium text-primary">Paneli operativ</p>
              <h1 className="mt-1 break-words text-xl font-semibold sm:text-3xl">Sot te {settings?.companyName ?? "Art Decor Events"}</h1>
              <p className="mt-1 text-sm text-muted-foreground">I identifikuar si {session.fullName}</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button variant="secondary" className="lg:hidden" onClick={() => setMenuOpen(true)} aria-label="Hap menynë">
                <Menu size={18} />
                Meny
              </Button>
              <ThemeToggle />
              <Button variant="secondary" onClick={() => setActiveView("windows")}>
                <CalendarDays size={18} />
                Dritare e re
              </Button>
              <Button variant="ghost" onClick={onLogout} aria-label="Dil" title="Dil">
                <LogOut size={18} />
              </Button>
            </div>
          </header>

          {menuOpen && (
            <div className="fixed inset-0 z-50 lg:hidden" role="dialog" aria-modal="true">
              <button className="absolute inset-0 bg-black/45" aria-label="Mbyll menynë" onClick={() => setMenuOpen(false)} />
              <div className="absolute bottom-0 left-0 right-0 max-h-[85vh] overflow-y-auto rounded-t-lg border border-border bg-card p-4 pb-[calc(1rem+env(safe-area-inset-bottom))] shadow-xl">
                <div className="flex items-center justify-between">
                  <BrandMark compact logoUrl={settings?.logoUrl} companyName={settings?.companyName} />
                  <Button variant="ghost" onClick={() => setMenuOpen(false)} aria-label="Mbyll">
                    <X size={18} />
                  </Button>
                </div>
                <nav className="mt-5 grid gap-2">
                  {navItems.map((item) => (
                    <button
                      key={item.id}
                      type="button"
                      onClick={() => {
                        setActiveView(item.id);
                        setMenuOpen(false);
                      }}
                      className={`flex min-h-12 w-full items-center gap-3 rounded-md px-3 py-2 text-left text-sm font-medium transition ${
                        activeView === item.id ? "bg-muted text-foreground" : "text-muted-foreground hover:bg-muted hover:text-foreground"
                      }`}
                    >
                      <item.icon size={17} />
                      {item.label}
                    </button>
                  ))}
                </nav>
              </div>
            </div>
          )}

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
  const [checkoutModal, setCheckoutModal] = useState<{ attendanceRecordId: string; employeeName: string; mode: "checkout" | "extend" } | null>(null);
  const [checkoutValue, setCheckoutValue] = useState("");
  const [checkoutSaving, setCheckoutSaving] = useState(false);

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

  function openAttendanceModal(attendanceRecordId: string, employeeName: string, mode: "checkout" | "extend") {
    setCheckoutModal({ attendanceRecordId, employeeName, mode });
    setCheckoutValue(toDateTimeInput(new Date()));
    setMessage("");
  }

  async function submitAttendanceModal() {
    if (!checkoutModal) {
      return;
    }
    setMessage("");
    setCheckoutSaving(true);
    try {
      const parsed = new Date(checkoutValue);
      if (Number.isNaN(parsed.getTime())) {
        throw new Error("Ju lutemi zgjidhni datën dhe orën.");
      }
      if (checkoutModal.mode === "checkout") {
        await adminCheckout(accessToken, checkoutModal.attendanceRecordId, parsed.toISOString());
      } else {
        await extendCheckout(accessToken, checkoutModal.attendanceRecordId, parsed.toISOString());
      }
      const nextSnapshot = await getAdminDashboard(accessToken);
      setSnapshot(nextSnapshot);
      setMessage(checkoutModal.mode === "checkout" ? "Dalja u regjistrua nga administratori." : "Koha e daljes u vazhdua.");
      setCheckoutModal(null);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Veprimi nuk mund të kryhej.");
    } finally {
      setCheckoutSaving(false);
    }
  }

  const kpis = [
    { label: "Të regjistruar sot", value: snapshot?.present ?? 0, icon: UserCheck },
    { label: "Në punë tani", value: snapshot?.currentlyWorking ?? 0, icon: UsersRound },
    { label: "Punëtorë aktivë", value: snapshot?.activeEmployees ?? 0, icon: UsersRound },
    { label: "Dalje të regjistruara", value: snapshot?.liveAttendance.filter((row) => row.checkedOutAt).length ?? 0, icon: UserCheck },
  ];

  return (
    <>
      <section className="mt-6 overflow-hidden rounded-lg border border-border bg-card shadow-corporate">
            <div className="grid lg:grid-cols-[1fr_360px]">
              <div className="p-6">
                <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">Art Decor Events</p>
                <h2 className="mt-3 text-2xl font-semibold">Gjendja e sotme e punëtorëve</h2>
                <p className="mt-3 max-w-2xl text-sm leading-6 text-muted-foreground">
                  {snapshot
                    ? `${snapshot.activeEmployees} punëtorë aktivë, ${snapshot.inactiveEmployees} joaktivë, ${snapshot.administrators} administratorë dhe ${snapshot.supervisors} mbikëqyrës janë të regjistruar.`
                    : "Po ngarkohen të dhënat operative."}
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
                <h2 className="font-semibold">Prezenca live</h2>
                {loading && <RefreshCw className="animate-spin text-muted-foreground" size={16} />}
              </div>
              <div className="mt-5 divide-y divide-border rounded-lg border border-border">
                {!loading && (snapshot?.liveAttendance.length ?? 0) === 0 && (
                  <div className="p-6 text-center text-sm text-muted-foreground">
                    Nuk ka punëtorë të regjistruar në punë për momentin.
                  </div>
                )}
                {snapshot?.liveAttendance.map((row) => (
                  <div key={`${row.employeeId}-${row.checkedInAt}`} className="grid gap-3 p-4 text-sm md:grid-cols-[1.2fr_1fr_auto] md:items-center">
                    <div className="min-w-0">
                      <p className="truncate font-semibold">{row.employeeName}</p>
                      <p className="mt-1 text-xs text-muted-foreground">{row.employeeCode}</p>
                    </div>
                    <div className="grid grid-cols-2 gap-2 text-xs text-muted-foreground sm:text-sm">
                      <span>Hyrja: {formatTime(row.checkedInAt)}</span>
                      <span>Dalja: {formatTime(row.checkedOutAt)}</span>
                      <span>Punuar: {formatMinutes(row.workedMinutes)}</span>
                      <span>Shtesë: {row.overtimeMinutes > 0 ? formatMinutes(row.overtimeMinutes) : "-"}</span>
                    </div>
                    <div className="flex flex-wrap items-center gap-2 md:justify-end">
                      <span className="rounded-md bg-accent/10 px-2 py-1 text-xs font-medium text-accent">
                        {attendanceStatusLabel(row)}
                      </span>
                      {!row.checkedOutAt && (
                        <Button type="button" variant="secondary" className="h-9 px-3" onClick={() => openAttendanceModal(row.attendanceRecordId, row.employeeName, "checkout")}>
                          Dalje
                        </Button>
                      )}
                      {!row.checkedOutAt && (
                        <Button type="button" variant="ghost" className="h-9 px-3" onClick={() => openAttendanceModal(row.attendanceRecordId, row.employeeName, "extend")}>
                          Vazhdo
                        </Button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </Card>
            <Card className="p-5">
              <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-2">
                  <MapPin className="text-primary" size={19} />
                  <h2 className="font-semibold">Lokacionet live</h2>
                </div>
                <span className="text-xs text-muted-foreground">{snapshot?.liveLocations.length ?? 0} aktivë</span>
              </div>
              <div className="mt-5 divide-y divide-border rounded-lg border border-border">
                {(snapshot?.liveLocations.length ?? 0) === 0 && (
                  <div className="p-5 text-sm text-muted-foreground">
                    Nuk ka lokacione live. Lokacionet shfaqen vetëm kur punëtori është në punë dhe gjurmimi është aktiv.
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
                      <p className="mt-1 text-xs text-muted-foreground">Përditësuar {formatDateTime(location.capturedAt)}</p>
                    </div>
                    <a
                      className="inline-flex h-11 items-center justify-center rounded-md border border-border bg-card px-4 text-sm font-semibold text-card-foreground transition hover:bg-muted"
                      href={mapLocationUrl(location.latitude, location.longitude)}
                      target="_blank"
                      rel="noreferrer"
                    >
                      Hape në hartë
                    </a>
                  </div>
                ))}
              </div>
            </Card>
            <Card className="p-5">
              <h2 className="font-semibold">Veprime të shpejta</h2>
              <div className="mt-5 grid gap-3">
                {(snapshot?.quickActions ?? ["Shto punëtor", "Krijo dritare", "Publiko njoftim"]).map((action) => (
                  <Button key={action} variant="secondary">
                    {action}
                  </Button>
                ))}
              </div>
            </Card>
          </section>
          {checkoutModal && (
            <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/45 px-4 py-4 sm:items-center" role="dialog" aria-modal="true">
              <div className="w-full max-w-md rounded-lg border border-border bg-card p-5 shadow-xl">
                <h2 className="text-lg font-semibold">
                  {checkoutModal.mode === "checkout" ? "Regjistro daljen" : "Vazhdo kohën e daljes"}
                </h2>
                <p className="mt-2 text-sm text-muted-foreground">{checkoutModal.employeeName}</p>
                <label className="mt-4 block space-y-2 text-sm font-medium">
                  <span>Data dhe ora</span>
                  <input
                    type="datetime-local"
                    className="h-11 w-full rounded-md border border-border bg-background px-3 text-sm outline-none focus:border-primary focus:ring-4 focus:ring-primary/15"
                    value={checkoutValue}
                    onChange={(event) => setCheckoutValue(event.target.value)}
                  />
                </label>
                <div className="mt-5 grid gap-2 sm:grid-cols-2">
                  <Button type="button" variant="secondary" disabled={checkoutSaving} onClick={() => setCheckoutModal(null)}>
                    Anulo
                  </Button>
                  <Button type="button" disabled={checkoutSaving} onClick={() => void submitAttendanceModal()}>
                    {checkoutSaving ? "Duke ruajtur..." : "Ruaj"}
                  </Button>
                </div>
              </div>
            </div>
          )}
    </>
  );
}

function toDateTimeInput(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  return `${year}-${month}-${day}T${hours}:${minutes}`;
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

function attendanceStatusLabel(row: { checkedOutAt: string | null }) {
  return row.checkedOutAt ? "Dalë" : "Në punë";
}
