import { FormEvent, useEffect, useMemo, useState } from "react";
import { CalendarClock, Check, Edit3, Power, PowerOff, Search, Trash2, X } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import {
  cancelCheckInWindow,
  closeCheckInWindow,
  createCheckInWindow,
  deleteCheckInWindow,
  listCheckInWindows,
  listEmployees,
  openCheckInWindow,
  updateCheckInWindow,
  updateSettings,
  type AppSettings,
  type DailyCheckInWindow,
  type Employee,
} from "../../lib/api";

type DailyCheckInWindowPageProps = {
  accessToken: string;
  settings: AppSettings | null;
  onSettingsUpdated: (settings: AppSettings) => void;
};

type WindowForm = {
  workDate: string;
  checkInOpensAt: string;
  checkInClosesAt: string;
  autoCheckoutEnabled: boolean;
  employeeIds: string[];
};

const statusLabels: Record<DailyCheckInWindow["status"], string> = {
  DRAFT: "Në përgatitje",
  PUBLISHED: "Planifikuar",
  CHECK_IN_OPEN: "Hapur",
  CHECK_IN_CLOSED: "Mbyllur",
  CANCELLED: "Anuluar",
  COMPLETED: "Përfunduar",
};

export function DailyCheckInWindowPage({ accessToken, settings, onSettingsUpdated }: DailyCheckInWindowPageProps) {
  const [windows, setWindows] = useState<DailyCheckInWindow[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [form, setForm] = useState<WindowForm>(() => defaultForm(settings));
  const [formOpen, setFormOpen] = useState(false);
  const [editingWindowId, setEditingWindowId] = useState<string | null>(null);
  const [employeeQuery, setEmployeeQuery] = useState("");
  const [openModeCloseTime, setOpenModeCloseTime] = useState(toTimeInput(settings?.defaultCheckInCloseTime ?? "23:59:00"));
  const [openModeUnlimitedCheckout, setOpenModeUnlimitedCheckout] = useState(Boolean(settings?.openModeUnlimitedCheckout));
  const [editingOpenModeTime, setEditingOpenModeTime] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [savingOpenModeTime, setSavingOpenModeTime] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    void loadData();
  }, [accessToken]);

  useEffect(() => {
    if (!settings) {
      return;
    }
    setOpenModeCloseTime(toTimeInput(settings.defaultCheckInCloseTime));
    setOpenModeUnlimitedCheckout(settings.openModeUnlimitedCheckout);
    if (!formOpen) {
      setForm(defaultForm(settings));
    }
  }, [settings, formOpen]);

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
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Oraret nuk mund të ngarkoheshin.");
    } finally {
      setLoading(false);
    }
  }

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
  const selectedEmployees = useMemo(() => new Set(form.employeeIds), [form.employeeIds]);
  const todayModeStatus = useMemo(() => dailyModeStatus(windows), [windows]);
  const overnightNotice = isOvernightWindow(form)
    ? `Ky orar kalon mesnatën dhe mbyllet nesër në ${form.checkInClosesAt}.`
    : "";

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setMessage("");

    try {
      const validationMessage = validateWindowForm(form);
      if (validationMessage) {
        setMessage(validationMessage);
        return;
      }

      const payload = {
        workDate: form.workDate,
        checkInOpensAt: localDateTimeToIso(form.workDate, form.checkInOpensAt, settings?.timezone),
        checkInClosesAt: localDateTimeToIso(windowCloseDate(form), form.checkInClosesAt, settings?.timezone),
        autoCheckoutEnabled: form.autoCheckoutEnabled,
        employeeIds: form.employeeIds,
      };

      const saved = editingWindowId
        ? await updateCheckInWindow(accessToken, editingWindowId, payload)
        : await createCheckInWindow(accessToken, payload);

      try {
        setWindows(await listCheckInWindows(accessToken));
      } catch {
        setWindows((current) => upsertWindow(current, saved));
      }
      closeForm();
      setMessage(overnightNotice || (editingWindowId ? "Orari u përditësua." : "Orari u krijua."));
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Orari nuk mund të ruhej.");
    } finally {
      setSaving(false);
    }
  }

  async function saveOpenModeCloseTime(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!settings) {
      setMessage("Cilësimet nuk janë ngarkuar ende.");
      return;
    }
    setSavingOpenModeTime(true);
    setMessage("");
    try {
      const updated = await updateSettings(accessToken, {
        ...settings,
        defaultCheckInCloseTime: normalizeTime(openModeCloseTime),
        openModeUnlimitedCheckout,
      });
      onSettingsUpdated(updated);
      setEditingOpenModeTime(false);
      setMessage("Cilësimet e mënyrës së hapur u përditësuan.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Koha e mbylljes nuk mund të ruhej.");
    } finally {
      setSavingOpenModeTime(false);
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
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Veprimi nuk mund të kryhej.");
    }
  }

  async function deleteCancelled(windowId: string) {
    const confirmed = globalThis.confirm("A dëshironi ta fshini përgjithmonë këtë orar të anuluar?");
    if (!confirmed) {
      return;
    }

    setMessage("");
    try {
      await deleteCheckInWindow(accessToken, windowId);
      setWindows((current) => current.filter((window) => window.id !== windowId));
      if (editingWindowId === windowId) {
        closeForm();
      }
      setMessage("Orari i anuluar u fshi.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Orari i anuluar nuk mund të fshihej.");
    }
  }

  function openCreateForm() {
    setForm(defaultForm(settings));
    setEditingWindowId(null);
    setEmployeeQuery("");
    setFormOpen(true);
    setMessage("");
  }

  function editWindow(window: DailyCheckInWindow) {
    setEditingWindowId(window.id);
    setForm({
      workDate: window.workDate,
      checkInOpensAt: isoToLocalTime(window.checkInOpensAt),
      checkInClosesAt: isoToLocalTime(window.checkInClosesAt),
      autoCheckoutEnabled: window.autoCheckoutEnabled,
      employeeIds: window.employeeIds,
    });
    setEmployeeQuery("");
    setFormOpen(true);
    setMessage("");
  }

  function closeForm() {
    setForm(defaultForm(settings));
    setEditingWindowId(null);
    setEmployeeQuery("");
    setFormOpen(false);
  }

  function toggleEmployee(employeeId: string) {
    setForm((current) => ({
      ...current,
      employeeIds: current.employeeIds.includes(employeeId)
        ? current.employeeIds.filter((id) => id !== employeeId)
        : [...current.employeeIds, employeeId],
    }));
  }

  function selectAllEmployees() {
    setForm((current) => ({ ...current, employeeIds: activeEmployees.map((employee) => employee.id) }));
  }

  function clearEmployees() {
    setForm((current) => ({ ...current, employeeIds: [] }));
  }

  return (
    <div className="grid gap-5">
      <Card className="p-5">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <span className={`rounded-md px-2 py-1 text-xs font-semibold ${todayModeStatus.scheduled ? "bg-primary/15 text-primary" : "bg-accent/15 text-accent"}`}>
                {todayModeStatus.scheduled ? "Orari i planifikuar" : "Mënyra e hapur është aktive"}
              </span>
            </div>
            <h2 className="mt-3 text-xl font-semibold">{todayModeStatus.scheduled ? "Sot përdoret orari i planifikuar" : "Mënyra e hapur është aktive"}</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              {todayModeStatus.scheduled
                ? "Punëtorët kontrollohen sipas orarit dhe listës së zgjedhur."
                : "Pa orar të planifikuar, punëtorët mund të regjistrojnë hyrje dhe dalje."}
            </p>
          </div>

          <div className="grid gap-2 sm:grid-cols-2 lg:min-w-[360px]">
            <div className="rounded-md border border-border px-3 py-2">
              <p className="text-xs text-muted-foreground">Koha e mbylljes</p>
              <p className="mt-1 font-semibold">{openModeUnlimitedCheckout ? "Pa kufi" : openModeCloseTime}</p>
            </div>
            <Button type="button" variant="secondary" onClick={() => setEditingOpenModeTime((current) => !current)}>
              Cilësimet e mënyrës së hapur
            </Button>
          </div>
        </div>

        {openModeUnlimitedCheckout && (
          <p className="mt-4 rounded-md bg-primary/10 px-3 py-2 text-sm text-primary">
            Dalja pa kufi është aktive. Punëtorët nuk do të marrin dalje automatike derisa kjo mënyrë të çaktivizohet ose administratori të kryejë daljen.
          </p>
        )}

        {editingOpenModeTime && (
          <form className="mt-4 grid gap-3 sm:max-w-xl" onSubmit={saveOpenModeCloseTime}>
            <label className="flex items-center gap-3 rounded-md border border-border px-3 py-3 text-sm font-medium">
              <input
                type="checkbox"
                className="h-4 w-4 accent-primary"
                checked={openModeUnlimitedCheckout}
                onChange={(event) => setOpenModeUnlimitedCheckout(event.target.checked)}
              />
              Dalje pa kufi
            </label>
            {!openModeUnlimitedCheckout && (
              <label className="space-y-1 text-sm font-medium">
                <span>Koha e mbylljes</span>
                <Input type="time" value={openModeCloseTime} onChange={(event) => setOpenModeCloseTime(event.target.value)} required />
              </label>
            )}
            <div className="grid gap-2 sm:grid-cols-2">
              <Button disabled={savingOpenModeTime}>{savingOpenModeTime ? "Duke ruajtur..." : "Ruaj"}</Button>
              <Button type="button" variant="ghost" onClick={() => setEditingOpenModeTime(false)}>
                Anulo
              </Button>
            </div>
          </form>
        )}

        <div className="mt-4 flex flex-col gap-2 sm:flex-row">
          <Button type="button" onClick={openCreateForm}>
            <CalendarClock size={18} />
            Krijo orar të planifikuar
          </Button>
        </div>
      </Card>

      {message && <p className="rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}

      {formOpen && (
        <Card className="p-5">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <h2 className="font-semibold">{editingWindowId ? "Ndrysho orarin" : "Krijo orar"}</h2>
              <p className="text-sm text-muted-foreground">Zgjidh datën, kohën dhe punëtorët e lejuar.</p>
            </div>
            <Button type="button" variant="ghost" onClick={closeForm}>
              <X size={17} />
              Mbyll
            </Button>
          </div>

          <form className="mt-5 space-y-4" onSubmit={submit}>
            <div className="grid gap-3 md:grid-cols-3">
              <label className="space-y-1 text-sm font-medium">
                <span>Data</span>
                <Input type="date" value={form.workDate} onChange={(event) => setForm({ ...form, workDate: event.target.value })} required />
              </label>
              <label className="space-y-1 text-sm font-medium">
                <span>Ora e hapjes</span>
                <Input type="time" value={form.checkInOpensAt} onChange={(event) => setForm({ ...form, checkInOpensAt: event.target.value })} required />
              </label>
              <label className="space-y-1 text-sm font-medium">
                <span>Ora e mbylljes</span>
                <Input type="time" value={form.checkInClosesAt} onChange={(event) => setForm({ ...form, checkInClosesAt: event.target.value })} required />
              </label>
            </div>

            {overnightNotice && <p className="rounded-md bg-primary/10 px-3 py-2 text-sm text-primary">{overnightNotice}</p>}

            <label className="flex items-center gap-3 rounded-md border border-border px-3 py-3 text-sm font-medium">
              <input
                type="checkbox"
                className="h-4 w-4 accent-primary"
                checked={form.autoCheckoutEnabled}
                onChange={(event) => setForm({ ...form, autoCheckoutEnabled: event.target.checked })}
              />
              Bëj dalje automatike në fund të turnit
            </label>

            <div className="rounded-lg border border-border">
              <div className="border-b border-border p-3">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p className="text-sm font-semibold">Punëtorët e lejuar</p>
                    <p className="text-xs text-muted-foreground">{form.employeeIds.length} të zgjedhur</p>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Button type="button" variant="secondary" className="h-9 px-3" onClick={selectAllEmployees}>
                      <Check size={16} />
                      Zgjidhi të gjithë
                    </Button>
                    <Button type="button" variant="ghost" className="h-9 px-3" onClick={clearEmployees}>
                      <X size={16} />
                      Pastro
                    </Button>
                  </div>
                </div>
                <div className="relative mt-3">
                  <Search className="absolute left-3 top-3 text-muted-foreground" size={17} />
                  <Input className="pl-10" placeholder="Kërko punëtorë" value={employeeQuery} onChange={(event) => setEmployeeQuery(event.target.value)} />
                </div>
              </div>
              <div className="max-h-72 divide-y divide-border overflow-y-auto">
                {filteredEmployees.length === 0 && <p className="p-4 text-sm text-muted-foreground">Nuk u gjetën punëtorë aktivë.</p>}
                {filteredEmployees.map((employee) => (
                  <label key={employee.id} className="flex cursor-pointer items-center gap-3 px-3 py-3 text-sm transition hover:bg-muted">
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

            <div className="grid gap-2 sm:grid-cols-2">
              <Button disabled={saving || form.employeeIds.length === 0}>
                {saving ? "Duke ruajtur..." : editingWindowId ? "Ruaj ndryshimet" : "Krijo orar"}
              </Button>
              <Button type="button" variant="secondary" onClick={closeForm}>
                Anulo
              </Button>
            </div>
          </form>
        </Card>
      )}

      <Card className="p-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="font-semibold">Oraret e planifikuara</h2>
            <p className="text-sm text-muted-foreground">Hap, mbyll, ndrysho ose anulo një orar.</p>
          </div>
          <div className="rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{windows.length} orare</div>
        </div>

        <div className="mt-5 space-y-3">
          {loading && <p className="rounded-md border border-border p-4 text-sm text-muted-foreground">Po ngarkohen oraret...</p>}
          {!loading && windows.length === 0 && (
            <div className="rounded-lg border border-dashed border-border p-8 text-center">
              <CalendarClock className="mx-auto text-primary" size={30} />
              <p className="mt-3 font-medium">Nuk ka orare të planifikuara</p>
              <p className="mt-1 text-sm text-muted-foreground">Mënyra e hapur mbetet aktive automatikisht.</p>
            </div>
          )}
          {windows.map((window) => (
            <div key={window.id} className="rounded-lg border border-border p-4">
              <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
                <div className="min-w-0">
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="font-semibold">{formatDate(window.workDate)}</p>
                    <span className={`rounded-md px-2 py-1 text-xs font-semibold ${statusClass(window.status)}`}>{statusLabels[window.status]}</span>
                  </div>
                  <p className="mt-2 text-sm text-muted-foreground">
                    {formatWindowRange(window, settings?.timezone)} · {window.autoCheckoutEnabled ? "dalje automatike" : "pa dalje automatike"} · {window.allowedEmployeeCount} punëtorë
                  </p>
                </div>

                <div className="grid gap-2 sm:grid-cols-2 lg:flex lg:flex-wrap lg:justify-end">
                  {window.status === "CANCELLED" ? (
                    <Button type="button" variant="danger" onClick={() => void deleteCancelled(window.id)}>
                      <Trash2 size={16} />
                      Fshi të anuluarin
                    </Button>
                  ) : (
                    <>
                      <Button type="button" variant="secondary" onClick={() => void runWindowAction(window.id, openCheckInWindow, "Hyrja u hap manualisht.")} disabled={window.status === "CHECK_IN_OPEN"}>
                        <Power size={16} />
                        Hap
                      </Button>
                      <Button type="button" variant="secondary" onClick={() => void runWindowAction(window.id, closeCheckInWindow, "Hyrja u mbyll manualisht.")} disabled={window.status === "CHECK_IN_CLOSED"}>
                        <PowerOff size={16} />
                        Mbyll
                      </Button>
                      <Button type="button" variant="ghost" onClick={() => editWindow(window)}>
                        <Edit3 size={16} />
                        Ndrysho
                      </Button>
                      <Button type="button" variant="danger" onClick={() => void runWindowAction(window.id, cancelCheckInWindow, "Orari u anulua.")}>
                        <X size={16} />
                        Anulo
                      </Button>
                    </>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}

function defaultForm(settings: AppSettings | null): WindowForm {
  return {
    workDate: localDateInputValue(),
    checkInOpensAt: toTimeInput(settings?.defaultCheckInOpenTime ?? "06:50:00"),
    checkInClosesAt: toTimeInput(settings?.defaultCheckInCloseTime ?? "07:10:00"),
    autoCheckoutEnabled: true,
    employeeIds: [],
  };
}

function validateWindowForm(form: WindowForm) {
  if (!form.employeeIds.length) {
    return "Ju lutemi zgjidhni të paktën një punëtor.";
  }
  if (!form.workDate || !form.checkInOpensAt || !form.checkInClosesAt) {
    return "Data, ora e hapjes dhe ora e mbylljes janë të detyrueshme.";
  }
  return "";
}

function dailyModeStatus(windows: DailyCheckInWindow[]) {
  const today = localDateInputValue();
  const scheduled = windows.some((window) =>
    window.workDate === today && !["CANCELLED", "COMPLETED"].includes(window.status),
  );
  return { scheduled };
}

function isOvernightWindow(form: WindowForm) {
  return Boolean(form.workDate && form.checkInOpensAt && form.checkInClosesAt && form.checkInClosesAt <= form.checkInOpensAt);
}

function windowCloseDate(form: WindowForm) {
  return isOvernightWindow(form) ? addDays(form.workDate, 1) : form.workDate;
}

function addDays(dateValue: string, days: number) {
  const [year, month, day] = dateValue.split("-").map(Number);
  const date = new Date(Date.UTC(year, month - 1, day + days, 12, 0, 0));
  return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, "0")}-${String(date.getUTCDate()).padStart(2, "0")}`;
}

function localDateTimeToIso(date: string, time: string, timezone = "Europe/Berlin") {
  const utcGuess = new Date(`${date}T${time}:00Z`);
  const offset = getTimeZoneOffsetMs(timezone, utcGuess);
  return new Date(utcGuess.getTime() - offset).toISOString();
}

function getTimeZoneOffsetMs(timezone: string, date: Date) {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone: timezone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).formatToParts(date);
  const values = Object.fromEntries(parts.filter((part) => part.type !== "literal").map((part) => [part.type, part.value]));
  const asUtc = Date.UTC(
    Number(values.year),
    Number(values.month) - 1,
    Number(values.day),
    Number(values.hour),
    Number(values.minute),
    Number(values.second),
  );
  return asUtc - date.getTime();
}

function localDateInputValue() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function isoToLocalTime(value: string) {
  const date = new Date(value);
  return `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function toTimeInput(value: string) {
  return value.slice(0, 5);
}

function normalizeTime(value: string) {
  return value.length === 5 ? `${value}:00` : value;
}

function upsertWindow(windows: DailyCheckInWindow[], updated: DailyCheckInWindow) {
  const next = windows.some((window) => window.id === updated.id)
    ? windows.map((window) => (window.id === updated.id ? updated : window))
    : [updated, ...windows];

  return next.sort((left, right) => right.workDate.localeCompare(left.workDate));
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat(undefined, { weekday: "short", day: "2-digit", month: "2-digit", year: "numeric" }).format(
    new Date(`${value}T12:00:00`),
  );
}

function formatTime(value: string, timezone = "Europe/Berlin") {
  return new Intl.DateTimeFormat(undefined, { hour: "2-digit", minute: "2-digit", timeZone: timezone }).format(new Date(value));
}

function formatWindowRange(window: DailyCheckInWindow, timezone = "Europe/Berlin") {
  const opensAt = new Date(window.checkInOpensAt);
  const closesAt = new Date(window.checkInClosesAt);
  const range = `${formatTime(window.checkInOpensAt, timezone)} - ${formatTime(window.checkInClosesAt, timezone)}`;
  return localDateInTimezone(opensAt, timezone) === localDateInTimezone(closesAt, timezone)
    ? range
    : `${range} (mbyllet nesër)`;
}

function localDateInTimezone(date: Date, timezone: string) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: timezone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(date);
  const values = Object.fromEntries(parts.filter((part) => part.type !== "literal").map((part) => [part.type, part.value]));
  return `${values.year}-${values.month}-${values.day}`;
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
