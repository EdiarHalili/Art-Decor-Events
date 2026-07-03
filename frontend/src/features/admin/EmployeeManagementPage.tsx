import { FormEvent, useEffect, useMemo, useState } from "react";
import { Camera, Clock3, Edit3, Search, UserMinus, UserPlus } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import {
  createEmployee,
  deactivateEmployee,
  getEmployeeHistory,
  listEmployees,
  updateEmployee,
  type AttendanceReportRow,
  type Employee,
} from "../../lib/api";

type EmployeeManagementPageProps = {
  accessToken: string;
};

type EmployeeForm = {
  employeeCode: string;
  fullName: string;
  pin: string;
  phone: string;
  profilePhotoUrl: string;
  positionTitle: string;
  departmentName: string;
  teamName: string;
  notes: string;
  wageType: Employee["wageType"];
  baseWage: string;
  overtimeMultiplier: string;
};

const initialForm: EmployeeForm = {
  employeeCode: "",
  fullName: "",
  pin: "",
  phone: "",
  profilePhotoUrl: "",
  positionTitle: "",
  departmentName: "",
  teamName: "",
  notes: "",
  wageType: "HOURLY",
  baseWage: "0",
  overtimeMultiplier: "1.5",
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

    const now = new Date();
    const from = new Date(now.getFullYear(), now.getMonth(), 1);
    const to = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    setHistoryLoading(true);
    getEmployeeHistory(accessToken, selected.id, {
      from: dateOnly(from),
      to: dateOnly(to),
    })
      .then(setHistory)
      .catch(() => setHistory([]))
      .finally(() => setHistoryLoading(false));
  }, [accessToken, selected?.id]);

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
      pin: form.pin || undefined,
      phone: form.phone,
      profilePhotoUrl: form.profilePhotoUrl,
      positionTitle: form.positionTitle,
      departmentName: form.departmentName,
      teamName: form.teamName,
      notes: form.notes,
      wageType: form.wageType,
      baseWage: Number(form.baseWage),
      overtimeMultiplier: Number(form.overtimeMultiplier),
    };

    try {
      const employee = editingId
        ? await updateEmployee(accessToken, editingId, payload)
        : await createEmployee(accessToken, {
            employeeCode: form.employeeCode,
            fullName: form.fullName,
            pin: form.pin,
            phone: form.phone,
            profilePhotoUrl: form.profilePhotoUrl,
            positionTitle: form.positionTitle,
            departmentName: form.departmentName,
            teamName: form.teamName,
            notes: form.notes,
            wageType: form.wageType,
            baseWage: Number(form.baseWage),
            overtimeMultiplier: Number(form.overtimeMultiplier),
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
      pin: "",
      phone: employee.phone ?? "",
      profilePhotoUrl: employee.profilePhotoUrl ?? "",
      positionTitle: employee.positionTitle ?? "",
      departmentName: employee.departmentName ?? "",
      teamName: employee.teamName ?? "",
      notes: employee.notes ?? "",
      wageType: employee.wageType,
      baseWage: String(employee.baseWage),
      overtimeMultiplier: String(employee.overtimeMultiplier),
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
            <p className="text-sm text-muted-foreground">Keep employee access, role, team, and wage details in one profile.</p>
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
            placeholder={editingId ? "New 4-digit PIN (optional)" : "4-digit PIN"}
            value={form.pin}
            onChange={(event) => setForm({ ...form, pin: event.target.value })}
            inputMode="numeric"
            maxLength={4}
            required={!editingId}
          />
          <Input placeholder="Phone" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} />
          <Input placeholder="Profile photo URL" value={form.profilePhotoUrl} onChange={(event) => setForm({ ...form, profilePhotoUrl: event.target.value })} />
          <div className="grid gap-2 sm:grid-cols-3 xl:grid-cols-1 2xl:grid-cols-3">
            <Input placeholder="Position" value={form.positionTitle} onChange={(event) => setForm({ ...form, positionTitle: event.target.value })} />
            <Input placeholder="Department" value={form.departmentName} onChange={(event) => setForm({ ...form, departmentName: event.target.value })} />
            <Input placeholder="Team" value={form.teamName} onChange={(event) => setForm({ ...form, teamName: event.target.value })} />
          </div>
          <div className="grid grid-cols-3 gap-2">
            <select className="h-11 rounded-md border border-border bg-background px-3 text-sm" value={form.wageType} onChange={(event) => setForm({ ...form, wageType: event.target.value as Employee["wageType"] })}>
              <option value="HOURLY">Hourly</option>
              <option value="DAILY">Daily</option>
              <option value="MONTHLY">Monthly</option>
            </select>
            <Input placeholder="Wage" type="number" min="0" step="0.01" value={form.baseWage} onChange={(event) => setForm({ ...form, baseWage: event.target.value })} />
            <Input placeholder="OT" type="number" min="1" step="0.01" value={form.overtimeMultiplier} onChange={(event) => setForm({ ...form, overtimeMultiplier: event.target.value })} />
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
              employee={selected}
              history={history}
              historyLoading={historyLoading}
              onEdit={() => startEdit(selected)}
              onDeactivate={() => void deactivate(selected.id)}
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
  employee,
  history,
  historyLoading,
  onEdit,
  onDeactivate,
}: {
  employee: Employee;
  history: AttendanceReportRow[];
  historyLoading: boolean;
  onEdit: () => void;
  onDeactivate: () => void;
}) {
  const workedMinutes = history.reduce((total, row) => total + row.workedMinutes, 0);

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
          <Button type="button" variant="ghost" disabled={employee.status === "INACTIVE"} onClick={onDeactivate}><UserMinus size={17} /></Button>
        </div>
      </div>

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
                <span>Check Out: {formatTime(row.checkedOutAt)}</span>
                <span>Worked: {formatMinutes(row.workedMinutes)}</span>
                <span>Overtime: {row.overtimeMinutes > 0 ? formatMinutes(row.overtimeMinutes) : "None"}</span>
              </div>
              <div className="flex flex-wrap gap-2 lg:justify-end">
                <span className={`rounded-md px-2 py-1 text-xs font-medium ${row.absent ? "bg-destructive/10 text-destructive" : "bg-accent/10 text-accent"}`}>
                  Status: {formatStatus(row.status)}
                </span>
                <span className={`rounded-md px-2 py-1 text-xs font-medium ${row.late ? "bg-destructive/10 text-destructive" : "bg-muted text-muted-foreground"}`}>
                  {row.late ? "Late" : "On time"}
                </span>
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

function upsertEmployee(employees: Employee[], updated: Employee) {
  return employees.some((employee) => employee.id === updated.id)
    ? employees.map((employee) => (employee.id === updated.id ? updated : employee))
    : [updated, ...employees];
}

function dateOnly(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
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
