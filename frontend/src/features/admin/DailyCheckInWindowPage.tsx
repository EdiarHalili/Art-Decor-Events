import { FormEvent, useEffect, useMemo, useState } from "react";
import { CalendarClock, Check, Edit3, Power, PowerOff, Search, UserRoundCheck, X } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import {
  cancelCheckInWindow,
  closeCheckInWindow,
  createCheckInWindow,
  listCheckInWindows,
  listEmployees,
  openCheckInWindow,
  updateCheckInWindow,
  type DailyCheckInWindow,
  type Employee,
} from "../../lib/api";

type DailyCheckInWindowPageProps = {
  accessToken: string;
};

type WindowForm = {
  workDate: string;
  checkInOpensAt: string;
  checkInClosesAt: string;
  employeeIds: string[];
};

const emptyForm: WindowForm = {
  workDate: localDateInputValue(),
  checkInOpensAt: "06:50",
  checkInClosesAt: "07:10",
  employeeIds: [],
};

const statusLabels: Record<DailyCheckInWindow["status"], string> = {
  DRAFT: "Draft",
  PUBLISHED: "Scheduled",
  CHECK_IN_OPEN: "Open",
  CHECK_IN_CLOSED: "Closed",
  CANCELLED: "Cancelled",
  COMPLETED: "Completed",
};

export function DailyCheckInWindowPage({ accessToken }: DailyCheckInWindowPageProps) {
  const [windows, setWindows] = useState<DailyCheckInWindow[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [form, setForm] = useState<WindowForm>(emptyForm);
  const [editingWindowId, setEditingWindowId] = useState<string | null>(null);
  const [employeeQuery, setEmployeeQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  async function loadData() {
    setLoading(true);
    setMessage("");
    try {
      const [windowData, employeeData] = await Promise.all([
        listCheckInWindows(accessToken),
        listEmployees(accessToken),
      ]);
      setWindows(windowData);
      setEmployees(employeeData);
    } catch {
      setMessage("Daily check-in windows could not be loaded.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadData();
  }, []);

  const activeEmployees = useMemo(() => employees.filter((employee) => employee.status === "ACTIVE"), [employees]);

  const filteredEmployees = useMemo(() => {
    const normalized = employeeQuery.trim().toLowerCase();
    if (!normalized) {
      return activeEmployees;
    }

    return activeEmployees.filter((employee) =>
      [employee.fullName, employee.employeeCode].some((value) => value.toLowerCase().includes(normalized)),
    );
  }, [activeEmployees, employeeQuery]);

  const selectedEmployees = useMemo(
    () => new Set(form.employeeIds),
    [form.employeeIds],
  );

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setMessage("");

    try {
      const payload = {
        workDate: form.workDate,
        checkInOpensAt: localDateTimeToIso(form.workDate, form.checkInOpensAt),
        checkInClosesAt: localDateTimeToIso(form.workDate, form.checkInClosesAt),
        employeeIds: form.employeeIds,
      };

      const saved = editingWindowId
        ? await updateCheckInWindow(accessToken, editingWindowId, payload)
        : await createCheckInWindow(accessToken, payload);

      setWindows((current) => upsertWindow(current, saved));
      setForm(emptyForm);
      setEditingWindowId(null);
      setEmployeeQuery("");
      setMessage(editingWindowId ? "Daily check-in window updated." : "Daily check-in window scheduled.");
    } catch {
      setMessage("The window could not be saved. Check the date, times, and selected employees.");
    } finally {
      setSaving(false);
    }
  }

  async function runWindowAction(
    windowId: string,
    action: (token: string, id: string) => Promise<DailyCheckInWindow>,
    successMessage: string,
  ) {
    setMessage("");
    try {
      const updated = await action(accessToken, windowId);
      setWindows((current) => upsertWindow(current, updated));
      setMessage(successMessage);
    } catch {
      setMessage("The window action could not be completed.");
    }
  }

  function toggleEmployee(employeeId: string) {
    setForm((current) => ({
      ...current,
      employeeIds: current.employeeIds.includes(employeeId)
        ? current.employeeIds.filter((id) => id !== employeeId)
        : [...current.employeeIds, employeeId],
    }));
  }

  function selectAllFiltered() {
    const ids = filteredEmployees.map((employee) => employee.id);
    setForm((current) => ({ ...current, employeeIds: Array.from(new Set([...current.employeeIds, ...ids])) }));
  }

  function clearEmployees() {
    setForm((current) => ({ ...current, employeeIds: [] }));
  }

  function editWindow(window: DailyCheckInWindow) {
    setEditingWindowId(window.id);
    setForm({
      workDate: window.workDate,
      checkInOpensAt: isoToLocalTime(window.checkInOpensAt),
      checkInClosesAt: isoToLocalTime(window.checkInClosesAt),
      employeeIds: window.employeeIds,
    });
    setMessage("");
  }

  function resetForm() {
    setForm(emptyForm);
    setEditingWindowId(null);
    setEmployeeQuery("");
  }

  return (
    <div className="grid gap-5 xl:grid-cols-[420px_1fr]">
      <Card className="p-5">
        <div className="flex items-center gap-3">
          <div className="rounded-md bg-primary/15 p-2 text-primary">
            <CalendarClock size={22} />
          </div>
          <div>
            <h2 className="font-semibold">{editingWindowId ? "Edit daily window" : "Schedule daily window"}</h2>
            <p className="text-sm text-muted-foreground">Set the date, check-in times, and allowed workers.</p>
          </div>
        </div>

        <form className="mt-5 space-y-4" onSubmit={submit}>
          <div className="grid gap-3 sm:grid-cols-3 xl:grid-cols-1 2xl:grid-cols-3">
            <label className="space-y-1 text-sm font-medium">
              <span>Date</span>
              <Input
                type="date"
                value={form.workDate}
                onChange={(event) => setForm({ ...form, workDate: event.target.value })}
                required
              />
            </label>
            <label className="space-y-1 text-sm font-medium">
              <span>Opens</span>
              <Input
                type="time"
                value={form.checkInOpensAt}
                onChange={(event) => setForm({ ...form, checkInOpensAt: event.target.value })}
                required
              />
            </label>
            <label className="space-y-1 text-sm font-medium">
              <span>Closes</span>
              <Input
                type="time"
                value={form.checkInClosesAt}
                onChange={(event) => setForm({ ...form, checkInClosesAt: event.target.value })}
                required
              />
            </label>
          </div>

          <div className="rounded-lg border border-border">
            <div className="border-b border-border p-3">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm font-semibold">Allowed workers</p>
                  <p className="text-xs text-muted-foreground">{form.employeeIds.length} selected</p>
                </div>
                <div className="flex gap-2">
                  <Button type="button" variant="secondary" className="h-9 px-3" onClick={selectAllFiltered}>
                    <Check size={16} />
                    Select
                  </Button>
                  <Button type="button" variant="ghost" className="h-9 px-3" onClick={clearEmployees}>
                    <X size={16} />
                    Clear
                  </Button>
                </div>
              </div>
              <div className="relative mt-3">
                <Search className="absolute left-3 top-3 text-muted-foreground" size={17} />
                <Input
                  className="pl-10"
                  placeholder="Search employees"
                  value={employeeQuery}
                  onChange={(event) => setEmployeeQuery(event.target.value)}
                />
              </div>
            </div>
            <div className="max-h-72 divide-y divide-border overflow-y-auto">
              {filteredEmployees.length === 0 && (
                <p className="p-4 text-sm text-muted-foreground">No active employees found.</p>
              )}
              {filteredEmployees.map((employee) => (
                <label
                  key={employee.id}
                  className="flex cursor-pointer items-center gap-3 px-3 py-3 text-sm transition hover:bg-muted"
                >
                  <input
                    type="checkbox"
                    className="h-4 w-4 accent-primary"
                    checked={selectedEmployees.has(employee.id)}
                    onChange={() => toggleEmployee(employee.id)}
                  />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate font-medium">{employee.fullName}</span>
                    <span className="text-xs text-muted-foreground">{employee.employeeCode}</span>
                  </span>
                </label>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-2 sm:flex-row">
            <Button className="flex-1" disabled={saving || form.employeeIds.length === 0}>
              {saving ? "Saving..." : editingWindowId ? "Save changes" : "Create window"}
            </Button>
            {editingWindowId && (
              <Button type="button" variant="secondary" onClick={resetForm}>
                Cancel edit
              </Button>
            )}
          </div>
        </form>
      </Card>

      <Card className="p-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="font-semibold">Daily check-in windows</h2>
            <p className="text-sm text-muted-foreground">Open, close, edit, or cancel a scheduled day.</p>
          </div>
          <div className="rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">
            {windows.length} windows
          </div>
        </div>

        {message && <p className="mt-4 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}

        <div className="mt-5 space-y-3">
          {loading && <p className="rounded-md border border-border p-4 text-sm text-muted-foreground">Loading windows...</p>}
          {!loading && windows.length === 0 && (
            <div className="rounded-lg border border-dashed border-border p-8 text-center">
              <UserRoundCheck className="mx-auto text-primary" size={30} />
              <p className="mt-3 font-medium">No daily check-in windows yet</p>
              <p className="mt-1 text-sm text-muted-foreground">Create tomorrow's window and assign the working team.</p>
            </div>
          )}
          {windows.map((window) => (
            <div key={window.id} className="rounded-lg border border-border p-4">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="font-semibold">{formatDate(window.workDate)}</p>
                    <span className={`rounded-md px-2 py-1 text-xs font-semibold ${statusClass(window.status)}`}>
                      {statusLabels[window.status]}
                    </span>
                  </div>
                  <p className="mt-2 text-sm text-muted-foreground">
                    {formatTime(window.checkInOpensAt)} - {formatTime(window.checkInClosesAt)} -{" "}
                    {window.allowedEmployeeCount} allowed workers
                  </p>
                </div>

                <div className="flex flex-wrap gap-2">
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => void runWindowAction(window.id, openCheckInWindow, "Check-in opened manually.")}
                    disabled={window.status === "CANCELLED" || window.status === "CHECK_IN_OPEN"}
                  >
                    <Power size={16} />
                    Open
                  </Button>
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => void runWindowAction(window.id, closeCheckInWindow, "Check-in closed manually.")}
                    disabled={window.status === "CANCELLED" || window.status === "CHECK_IN_CLOSED"}
                  >
                    <PowerOff size={16} />
                    Close
                  </Button>
                  <Button type="button" variant="ghost" onClick={() => editWindow(window)} disabled={window.status === "CANCELLED"}>
                    <Edit3 size={16} />
                    Edit
                  </Button>
                  <Button
                    type="button"
                    variant="danger"
                    onClick={() => void runWindowAction(window.id, cancelCheckInWindow, "Daily check-in window cancelled.")}
                    disabled={window.status === "CANCELLED"}
                  >
                    <X size={16} />
                    Cancel
                  </Button>
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}

function localDateTimeToIso(date: string, time: string) {
  return new Date(`${date}T${time}:00`).toISOString();
}

function localDateInputValue() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function isoToLocalTime(value: string) {
  const date = new Date(value);
  return `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function upsertWindow(windows: DailyCheckInWindow[], updated: DailyCheckInWindow) {
  const next = windows.some((window) => window.id === updated.id)
    ? windows.map((window) => (window.id === updated.id ? updated : window))
    : [updated, ...windows];

  return next.sort((left, right) => right.workDate.localeCompare(left.workDate));
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { weekday: "short", month: "short", day: "numeric", year: "numeric" }).format(
    new Date(`${value}T12:00:00`),
  );
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat(undefined, { hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}

function statusClass(status: DailyCheckInWindow["status"]) {
  if (status === "CHECK_IN_OPEN") {
    return "bg-accent/15 text-accent";
  }

  if (status === "CANCELLED") {
    return "bg-destructive/15 text-destructive";
  }

  if (status === "CHECK_IN_CLOSED" || status === "COMPLETED") {
    return "bg-muted text-muted-foreground";
  }

  return "bg-primary/15 text-primary";
}
