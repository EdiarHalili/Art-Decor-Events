import { FormEvent, useEffect, useMemo, useState } from "react";
import { Camera, Clock3, Edit3, FileSpreadsheet, FileText, KeyRound, Search, UserMinus, UserPlus } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import {
  adminCheckout,
  createEmployee,
  deactivateEmployee,
  exportEmployeeAttendance,
  getEmployeeHistory,
  listEmployees,
  resetEmployeePassword,
  updateEmployee,
  type AttendanceReportRow,
  type CheckoutType,
  type Employee,
} from "../../lib/api";

type EmployeeManagementPageProps = {
  accessToken: string;
};

type EmployeeForm = {
  employeeCode: string;
  fullName: string;
  password: string;
  phone: string;
  profilePhotoUrl: string;
  positionTitle: string;
  departmentName: string;
  teamName: string;
  notes: string;
};

type ExportRangePreset = "this-week" | "this-month" | "last-month" | "custom";

const initialForm: EmployeeForm = {
  employeeCode: "",
  fullName: "",
  password: "",
  phone: "",
  profilePhotoUrl: "",
  positionTitle: "",
  departmentName: "",
  teamName: "",
  notes: "",
};

export function EmployeeManagementPage({ accessToken }: EmployeeManagementPageProps) {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [form, setForm] = useState<EmployeeForm>(initialForm);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [selected, setSelected] = useState<Employee | null>(null);
  const [history, setHistory] = useState<AttendanceReportRow[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [message, setMessage] = useState("");

  async function loadEmployees() {
    setLoading(true);
    setMessage("");
    try {
      setEmployees(await listEmployees(accessToken));
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Employees could not be loaded.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadEmployees();
  }, []);

  useEffect(() => {
    if (!selected) {
      setHistory([]);
      return;
    }

    void loadHistory(selected.id);
  }, [accessToken, selected?.id]);

  async function loadHistory(employeeId: string) {
    const now = new Date();
    const from = new Date(now.getFullYear(), now.getMonth(), 1);
    const to = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    setHistoryLoading(true);
    getEmployeeHistory(accessToken, employeeId, {
      from: dateOnly(from),
      to: dateOnly(to),
    })
      .then(setHistory)
      .catch(() => setHistory([]))
      .finally(() => setHistoryLoading(false));
  }

  const filteredEmployees = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) {
      return employees;
    }

    return employees.filter((employee) =>
      [
        employee.fullName,
        employee.employeeCode,
        employee.phone ?? "",
        employee.positionTitle ?? "",
        employee.departmentName ?? "",
        employee.teamName ?? "",
      ].some((value) => value.toLowerCase().includes(normalized)),
    );
  }, [employees, query]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setMessage("");

    const payload = {
      fullName: form.fullName,
      password: form.password || undefined,
      phone: form.phone,
      profilePhotoUrl: form.profilePhotoUrl,
      positionTitle: form.positionTitle,
      departmentName: form.departmentName,
      teamName: form.teamName,
      notes: form.notes,
      wageType: "HOURLY" as Employee["wageType"],
      baseWage: 0,
      overtimeMultiplier: 1.5,
    };

    try {
      const employee = editingId
        ? await updateEmployee(accessToken, editingId, payload)
        : await createEmployee(accessToken, {
            employeeCode: form.employeeCode,
            fullName: form.fullName,
            password: form.password,
            phone: form.phone,
            profilePhotoUrl: form.profilePhotoUrl,
            positionTitle: form.positionTitle,
            departmentName: form.departmentName,
            teamName: form.teamName,
            notes: form.notes,
            wageType: "HOURLY",
            baseWage: 0,
            overtimeMultiplier: 1.5,
          });

      setEmployees((current) => upsertEmployee(current, employee));
      setSelected(employee);
      resetForm();
      setMessage(editingId ? "Employee profile updated." : "Employee created successfully.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Employee profile could not be saved.");
    } finally {
      setSaving(false);
    }
  }

  async function deactivate(employeeId: string) {
    setMessage("");
    try {
      const updated = await deactivateEmployee(accessToken, employeeId);
      setEmployees((current) => current.map((employee) => (employee.id === employeeId ? updated : employee)));
      setSelected((current) => (current?.id === employeeId ? updated : current));
      setMessage("Employee deactivated.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Employee could not be deactivated.");
    }
  }

  function startEdit(employee: Employee) {
    setEditingId(employee.id);
    setSelected(employee);
    setForm({
      employeeCode: employee.employeeCode,
      fullName: employee.fullName,
      password: "",
      phone: employee.phone ?? "",
      profilePhotoUrl: employee.profilePhotoUrl ?? "",
      positionTitle: employee.positionTitle ?? "",
      departmentName: employee.departmentName ?? "",
      teamName: employee.teamName ?? "",
      notes: employee.notes ?? "",
    });
  }

  function resetForm() {
    setForm(initialForm);
    setEditingId(null);
  }

  return (
    <div className="grid gap-5 xl:grid-cols-[400px_1fr]">
      <Card className="p-5">
        <div className="flex items-center gap-3">
          <div className="rounded-md bg-primary/15 p-2 text-primary">
            <UserPlus size={21} />
          </div>
          <div>
            <h2 className="font-semibold">{editingId ? "Edit employee" : "Create employee"}</h2>
            <p className="text-sm text-muted-foreground">Keep employee access, role, team, and notes in one profile.</p>
          </div>
        </div>

        <form className="mt-5 space-y-3" onSubmit={submit}>
          <Input
            placeholder="Employee ID"
            value={form.employeeCode}
            onChange={(event) => setForm({ ...form, employeeCode: event.target.value })}
            disabled={Boolean(editingId)}
            required
          />
          <Input placeholder="Full name" value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} required />
          <Input
            placeholder={editingId ? "New password (optional)" : "Temporary password"}
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
            minLength={8}
            type="password"
            required={!editingId}
          />
          <Input placeholder="Phone" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} />
          <Input placeholder="Profile photo URL" value={form.profilePhotoUrl} onChange={(event) => setForm({ ...form, profilePhotoUrl: event.target.value })} />
          <div className="grid gap-2 sm:grid-cols-3 xl:grid-cols-1 2xl:grid-cols-3">
            <Input placeholder="Position" value={form.positionTitle} onChange={(event) => setForm({ ...form, positionTitle: event.target.value })} />
            <Input placeholder="Department" value={form.departmentName} onChange={(event) => setForm({ ...form, departmentName: event.target.value })} />
            <Input placeholder="Team" value={form.teamName} onChange={(event) => setForm({ ...form, teamName: event.target.value })} />
          </div>
          <textarea className="min-h-24 w-full rounded-md border border-border bg-background px-3 py-3 text-sm outline-none focus:border-primary focus:ring-4 focus:ring-primary/15" placeholder="Internal notes" value={form.notes} onChange={(event) => setForm({ ...form, notes: event.target.value })} />
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button className="flex-1" disabled={saving}>{saving ? "Saving..." : editingId ? "Save changes" : "Create employee"}</Button>
            {editingId && <Button type="button" variant="secondary" onClick={resetForm}>Cancel</Button>}
          </div>
        </form>
      </Card>

      <div className="grid gap-5">
        <Card className="p-5">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="font-semibold">Employees</h2>
              <p className="text-sm text-muted-foreground">{employees.length} employee profiles</p>
            </div>
            <div className="relative sm:w-72">
              <Search className="absolute left-3 top-3 text-muted-foreground" size={18} />
              <Input className="pl-10" placeholder="Search employees" value={query} onChange={(event) => setQuery(event.target.value)} />
            </div>
          </div>

          {message && <p className="mt-4 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}

          <div className="mt-5 grid gap-3">
            {loading && <p className="rounded-md border border-border p-4 text-sm text-muted-foreground">Loading employees...</p>}
            {!loading && filteredEmployees.length === 0 && <p className="rounded-md border border-dashed border-border p-4 text-sm text-muted-foreground">No employees found.</p>}
            {filteredEmployees.map((employee) => (
              <button
                key={employee.id}
                type="button"
                onClick={() => setSelected(employee)}
                className={`rounded-lg border p-4 text-left transition hover:bg-muted ${selected?.id === employee.id ? "border-primary bg-primary/10" : "border-border bg-card"}`}
              >
                <div className="flex items-center gap-3">
                  <Avatar employee={employee} />
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-semibold">{employee.fullName}</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {employee.employeeCode} · {employee.positionTitle || "No position"} · {employee.teamName || "No team"}
                    </p>
                  </div>
                  <span className={employee.status === "ACTIVE" ? "text-sm font-medium text-accent" : "text-sm text-muted-foreground"}>{employee.status}</span>
                </div>
              </button>
            ))}
          </div>
        </Card>

        <Card className="p-5">
          {selected ? (
            <EmployeeProfile
              accessToken={accessToken}
              employee={selected}
              history={history}
              historyLoading={historyLoading}
              onEdit={() => startEdit(selected)}
              onDeactivate={() => void deactivate(selected.id)}
              onResetPassword={async () => {
                const response = await resetEmployeePassword(accessToken, selected.id);
                return response.temporaryPassword;
              }}
              onAttendanceChanged={() => void loadHistory(selected.id)}
            />
          ) : (
            <div className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
              Select an employee to view profile details and attendance history.
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}

function EmployeeProfile({
  accessToken,
  employee,
  history,
  historyLoading,
  onEdit,
  onDeactivate,
  onResetPassword,
  onAttendanceChanged,
}: {
  accessToken: string;
  employee: Employee;
  history: AttendanceReportRow[];
  historyLoading: boolean;
  onEdit: () => void;
  onDeactivate: () => void;
  onResetPassword: () => Promise<string>;
  onAttendanceChanged: () => void;
}) {
  const defaultRange = exportRange("this-month");
  const [rangePreset, setRangePreset] = useState<ExportRangePreset>("this-month");
  const [exportFrom, setExportFrom] = useState(defaultRange.from);
  const [exportTo, setExportTo] = useState(defaultRange.to);
  const [exporting, setExporting] = useState<"pdf" | "csv" | null>(null);
  const [exportMessage, setExportMessage] = useState("");
  const [temporaryPassword, setTemporaryPassword] = useState("");
  const [resettingPassword, setResettingPassword] = useState(false);
  const workedMinutes = history.reduce((total, row) => total + row.workedMinutes, 0);

  function updateRange(preset: ExportRangePreset) {
    setRangePreset(preset);
    if (preset === "custom") {
      return;
    }
    const next = exportRange(preset);
    setExportFrom(next.from);
    setExportTo(next.to);
  }

  async function downloadEmployeeExport(format: "pdf" | "csv") {
    setExportMessage("");
    if (!exportFrom || !exportTo || exportTo < exportFrom) {
      setExportMessage("Choose a valid export date range.");
      return;
    }

    setExporting(format);
    try {
      const blob = await exportEmployeeAttendance(accessToken, employee.id, { from: exportFrom, to: exportTo, format });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `${employee.employeeCode}-attendance-${exportFrom}-to-${exportTo}.${format}`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
      setExportMessage("Employee attendance export downloaded.");
    } catch (error) {
      setExportMessage(error instanceof Error ? error.message : "Employee attendance export could not be downloaded.");
    } finally {
      setExporting(null);
    }
  }

  async function resetPassword() {
    setExportMessage("");
    setTemporaryPassword("");
    setResettingPassword(true);
    try {
      const password = await onResetPassword();
      setTemporaryPassword(password);
      setExportMessage("Temporary password created. It is shown here only once.");
    } catch (error) {
      setExportMessage(error instanceof Error ? error.message : "Password could not be reset.");
    } finally {
      setResettingPassword(false);
    }
  }

  async function checkoutFromProfile(row: AttendanceReportRow) {
    if (!row.attendanceRecordId) {
      setExportMessage("Attendance record is missing.");
      return;
    }
    const value = globalThis.prompt("Checkout time (YYYY-MM-DD HH:mm)");
    if (!value) {
      return;
    }
    try {
      const parsed = new Date(value.replace(" ", "T"));
      if (Number.isNaN(parsed.getTime())) {
        throw new Error("Enter checkout time as YYYY-MM-DD HH:mm.");
      }
      await adminCheckout(accessToken, row.attendanceRecordId, parsed.toISOString());
      setExportMessage("Admin checkout recorded.");
      onAttendanceChanged();
    } catch (error) {
      setExportMessage(error instanceof Error ? error.message : "Admin checkout could not be recorded.");
    }
  }

  return (
    <div>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-center gap-4">
          <Avatar employee={employee} large />
          <div>
            <h2 className="text-xl font-semibold">{employee.fullName}</h2>
            <p className="mt-1 text-sm text-muted-foreground">{employee.employeeCode} · {employee.status}</p>
          </div>
        </div>
        <div className="flex gap-2">
          <Button type="button" variant="secondary" onClick={onEdit}><Edit3 size={17} />Edit</Button>
          <Button type="button" variant="secondary" disabled={resettingPassword} onClick={() => void resetPassword()}>
            <KeyRound size={17} />
            {resettingPassword ? "Resetting..." : "Reset Password"}
          </Button>
          <Button type="button" variant="ghost" disabled={employee.status === "INACTIVE"} onClick={onDeactivate}><UserMinus size={17} /></Button>
        </div>
      </div>

      {temporaryPassword && (
        <div className="mt-4 rounded-md border border-primary/30 bg-primary/10 p-4 text-sm">
          <p className="font-semibold text-primary">Temporary password</p>
          <p className="mt-2 font-mono text-base">{temporaryPassword}</p>
          <p className="mt-2 text-muted-foreground">Give this password to the employee now. It will not be shown again.</p>
        </div>
      )}

      <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <ProfileField label="Phone" value={employee.phone || "Not set"} />
        <ProfileField label="Position" value={employee.positionTitle || "Not set"} />
        <ProfileField label="Department" value={employee.departmentName || "Not set"} />
        <ProfileField label="Team" value={employee.teamName || "Not set"} />
      </div>

      <div className="mt-4 rounded-md bg-muted p-4 text-sm">
        <p className="font-medium">Notes</p>
        <p className="mt-2 text-muted-foreground">{employee.notes || "No notes saved for this employee."}</p>
      </div>

      <div className="mt-5 rounded-lg border border-border p-4">
        <div className="flex flex-col gap-3 xl:flex-row xl:items-end xl:justify-between">
          <div>
            <p className="font-semibold">Export work progress</p>
            <p className="mt-1 text-sm text-muted-foreground">Simple employee hours summary with attendance table.</p>
          </div>
          <div className="grid gap-2 sm:grid-cols-[150px_150px_150px] xl:flex xl:items-end">
            <label className="space-y-1 text-sm font-medium">
              <span>Range</span>
              <select
                className="h-11 w-full rounded-md border border-border bg-background px-3 text-sm"
                value={rangePreset}
                onChange={(event) => updateRange(event.target.value as ExportRangePreset)}
              >
                <option value="this-week">This week</option>
                <option value="this-month">This month</option>
                <option value="last-month">Last month</option>
                <option value="custom">Custom date range</option>
              </select>
            </label>
            <label className="space-y-1 text-sm font-medium">
              <span>From</span>
              <Input type="date" value={exportFrom} onChange={(event) => setExportFrom(event.target.value)} disabled={rangePreset !== "custom"} />
            </label>
            <label className="space-y-1 text-sm font-medium">
              <span>To</span>
              <Input type="date" value={exportTo} onChange={(event) => setExportTo(event.target.value)} disabled={rangePreset !== "custom"} />
            </label>
          </div>
        </div>
        <div className="mt-3 flex flex-col gap-2 sm:flex-row">
          <Button type="button" variant="secondary" disabled={Boolean(exporting)} onClick={() => void downloadEmployeeExport("pdf")}>
            <FileText size={17} />
            {exporting === "pdf" ? "Exporting..." : "Export PDF"}
          </Button>
          <Button type="button" variant="secondary" disabled={Boolean(exporting)} onClick={() => void downloadEmployeeExport("csv")}>
            <FileSpreadsheet size={17} />
            {exporting === "csv" ? "Exporting..." : "Export Excel/CSV"}
          </Button>
        </div>
        {exportMessage && <p className="mt-3 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{exportMessage}</p>}
      </div>

      <div className="mt-5">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold">This month</h3>
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Clock3 size={16} />
            {formatMinutes(workedMinutes)} worked
          </div>
        </div>
        <div className="mt-3 divide-y divide-border rounded-lg border border-border">
          {historyLoading && <p className="p-4 text-sm text-muted-foreground">Loading attendance history...</p>}
          {!historyLoading && history.length === 0 && <p className="p-4 text-sm text-muted-foreground">No attendance records for this month.</p>}
          {history.slice(0, 12).map((row) => (
            <div key={`${row.scheduleId}-${row.workDate}`} className="grid gap-3 p-4 text-sm lg:grid-cols-[1fr_1.4fr_auto] lg:items-center">
              <div>
                <p className="font-semibold">{formatDate(row.workDate)}</p>
                <p className="mt-1 text-xs text-muted-foreground">{row.employeeCode}</p>
              </div>
              <div className="grid gap-2 text-muted-foreground sm:grid-cols-2">
                <span>Check In: {formatTime(row.checkedInAt)}</span>
                <span>Check Out: {checkoutLabel(row.checkoutType, row.checkedOutAt)}</span>
                <GpsCell label="Check In GPS" latitude={row.checkInLatitude} longitude={row.checkInLongitude} distanceMeters={row.checkInDistanceMeters} />
                <GpsCell label="Check Out GPS" latitude={row.checkOutLatitude} longitude={row.checkOutLongitude} distanceMeters={row.checkOutDistanceMeters} />
                <span>Worked: {formatMinutes(row.workedMinutes)}</span>
                <span>Overtime: {row.overtimeMinutes > 0 ? formatMinutes(row.overtimeMinutes) : "None"}</span>
              </div>
              <div className="flex flex-wrap gap-2 lg:justify-end">
                <span className={`rounded-md px-2 py-1 text-xs font-medium ${row.absent ? "bg-destructive/10 text-destructive" : "bg-accent/10 text-accent"}`}>
                  Status: {checkoutTypeLabel(row.checkoutType, row.status)}
                </span>
                <span className={`rounded-md px-2 py-1 text-xs font-medium ${row.late ? "bg-destructive/10 text-destructive" : "bg-muted text-muted-foreground"}`}>
                  {row.late ? "Late" : "On time"}
                </span>
                {!row.checkedOutAt && row.attendanceRecordId && (
                  <Button type="button" variant="secondary" className="h-9 px-3" onClick={() => void checkoutFromProfile(row)}>
                    Admin checkout
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function Avatar({ employee, large = false }: { employee: Employee; large?: boolean }) {
  const sizeClass = large ? "h-16 w-16" : "h-11 w-11";
  if (employee.profilePhotoUrl) {
    return <img src={employee.profilePhotoUrl} alt="" className={`${sizeClass} rounded-full border border-border object-cover`} />;
  }
  return (
    <div className={`${sizeClass} flex items-center justify-center rounded-full border border-border bg-muted text-primary`}>
      <Camera size={large ? 24 : 18} />
    </div>
  );
}

function ProfileField({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-border p-3">
      <p className="text-xs uppercase text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm font-medium">{value}</p>
    </div>
  );
}

function GpsCell({
  label,
  latitude,
  longitude,
  distanceMeters,
}: {
  label: string;
  latitude: number | null;
  longitude: number | null;
  distanceMeters: number | null;
}) {
  if (latitude == null || longitude == null) {
    return <span>{label}: Not captured</span>;
  }
  return (
    <span>
      {label}: {latitude.toFixed(5)}, {longitude.toFixed(5)}
      {distanceMeters != null ? ` (${formatDistance(distanceMeters)} from workplace)` : ""}
      <a className="ml-2 font-medium text-primary" href={mapUrl(latitude, longitude)} target="_blank" rel="noreferrer">
        View on Map
      </a>
    </span>
  );
}

function upsertEmployee(employees: Employee[], updated: Employee) {
  return employees.some((employee) => employee.id === updated.id)
    ? employees.map((employee) => (employee.id === updated.id ? updated : employee))
    : [updated, ...employees];
}

function dateOnly(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function exportRange(preset: Exclude<ExportRangePreset, "custom">) {
  const now = new Date();
  if (preset === "this-week") {
    const day = now.getDay() === 0 ? 7 : now.getDay();
    const from = new Date(now);
    from.setDate(now.getDate() - day + 1);
    const to = new Date(from);
    to.setDate(from.getDate() + 6);
    return { from: dateOnly(from), to: dateOnly(to) };
  }

  if (preset === "last-month") {
    return {
      from: dateOnly(new Date(now.getFullYear(), now.getMonth() - 1, 1)),
      to: dateOnly(new Date(now.getFullYear(), now.getMonth(), 0)),
    };
  }

  return {
    from: dateOnly(new Date(now.getFullYear(), now.getMonth(), 1)),
    to: dateOnly(new Date(now.getFullYear(), now.getMonth() + 1, 0)),
  };
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { day: "2-digit", month: "2-digit", year: "numeric" }).format(new Date(`${value}T12:00:00`));
}

function formatTime(value: string | null) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat(undefined, { hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}

function formatMinutes(minutes: number) {
  const hours = Math.floor(minutes / 60);
  const remainder = minutes % 60;
  return `${hours}h ${remainder}m`;
}

function formatStatus(status: string) {
  return status.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function checkoutLabel(type: CheckoutType | null, checkedOutAt: string | null) {
  const time = formatTime(checkedOutAt);
  if (type === "AUTO_CHECKED_OUT") {
    return `Auto Check Out: ${time}`;
  }
  if (type === "ADMIN_CHECKED_OUT") {
    return `Admin Check Out: ${time}`;
  }
  return time;
}

function checkoutTypeLabel(type: CheckoutType | null, status: string) {
  if (type === "AUTO_CHECKED_OUT") {
    return "Auto Check Out";
  }
  if (type === "ADMIN_CHECKED_OUT") {
    return "Admin Check Out";
  }
  if (status === "CHECKED_OUT") {
    return "Manual Employee Check Out";
  }
  return formatStatus(status);
}

function mapUrl(latitude: number, longitude: number) {
  return `https://www.openstreetmap.org/?mlat=${latitude}&mlon=${longitude}#map=18/${latitude}/${longitude}`;
}

function formatDistance(meters: number) {
  return meters >= 1000 ? `${(meters / 1000).toFixed(2)} km` : `${meters} m`;
}
