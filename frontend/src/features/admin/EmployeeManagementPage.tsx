import { FormEvent, useEffect, useMemo, useState } from "react";
import { Camera, Clock3, Edit3, FileSpreadsheet, FileText, KeyRound, Search, Trash2, UserMinus, UserPlus } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import {
  adminCheckout,
  createEmployee,
  deleteEmployee,
  deactivateEmployee,
  exportEmployeeAttendance,
  forceDeleteEmployee,
  getEmployeeHistory,
  getEmployeeDeletionPolicy,
  listEmployees,
  mapLocationUrl,
  resetEmployeePassword,
  updateEmployee,
  type AttendanceReportRow,
  type AuthResponse,
  type CheckoutType,
  type Employee,
} from "../../lib/api";

type EmployeeManagementPageProps = {
  accessToken: string;
  role: AuthResponse["role"];
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

export function EmployeeManagementPage({ accessToken, role }: EmployeeManagementPageProps) {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [form, setForm] = useState<EmployeeForm>(initialForm);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [selected, setSelected] = useState<Employee | null>(null);
  const [history, setHistory] = useState<AttendanceReportRow[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [forceDeleteAllowed, setForceDeleteAllowed] = useState(false);
  const [message, setMessage] = useState("");

  async function loadEmployees() {
    setLoading(true);
    setMessage("");
    try {
      setEmployees(await listEmployees(accessToken));
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Punëtorët nuk mund të ngarkoheshin.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadEmployees();
  }, []);

  useEffect(() => {
    if (role !== "ADMINISTRATOR") {
      setForceDeleteAllowed(false);
      return;
    }

    getEmployeeDeletionPolicy(accessToken)
      .then((policy) => setForceDeleteAllowed(policy.forceDeleteAllowed))
      .catch(() => setForceDeleteAllowed(false));
  }, [accessToken, role]);

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
    setMessage("");

    const passwordValidation = validateEmployeePassword(form.password, Boolean(editingId));
    if (passwordValidation) {
      setMessage(passwordValidation);
      return;
    }

    setSaving(true);

    const payload = {
      fullName: form.fullName.trim(),
      password: form.password.trim() || undefined,
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
            employeeCode: form.employeeCode.trim(),
            fullName: form.fullName.trim(),
            password: form.password.trim(),
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
      setMessage(editingId ? "Profili i punëtorit u përditësua." : "Punëtori u krijua me sukses.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Profili i punëtorit nuk mund të ruhej.");
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
      setMessage("Punëtori u çaktivizua.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Punëtori nuk mund të çaktivizohej.");
    }
  }

  async function deleteSelectedEmployee(employeeId: string) {
    setMessage("");
    await deleteEmployee(accessToken, employeeId);
    setEmployees((current) => current.filter((employee) => employee.id !== employeeId));
    setSelected(null);
    setHistory([]);
    if (editingId === employeeId) {
      resetForm();
    }
    setMessage("Punëtori u fshi me sukses.");
  }

  async function forceDeleteSelectedEmployee(employeeId: string) {
    setMessage("");
    await forceDeleteEmployee(accessToken, employeeId);
    setEmployees((current) => current.filter((employee) => employee.id !== employeeId));
    setSelected(null);
    setHistory([]);
    if (editingId === employeeId) {
      resetForm();
    }
    setMessage("Punëtori dhe e gjithë historia e tij u fshinë me sukses.");
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
            <h2 className="font-semibold">{editingId ? "Edito punëtorin" : "Krijo punëtor"}</h2>
            <p className="text-sm text-muted-foreground">Menaxhoni qasjen, pozitën, ekipin dhe shënimet në një profil.</p>
          </div>
        </div>

        <form className="mt-5 space-y-3" onSubmit={submit}>
          <Input
            placeholder="Kodi i punëtorit"
            value={form.employeeCode}
            onChange={(event) => setForm({ ...form, employeeCode: event.target.value })}
            disabled={Boolean(editingId)}
            required
          />
          <Input placeholder="Emri i plotë" value={form.fullName} onChange={(event) => setForm({ ...form, fullName: event.target.value })} required />
          <Input
            placeholder={editingId ? "Fjalëkalim i ri (opsional)" : "Fjalëkalim i përkohshëm"}
            value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })}
            minLength={8}
            maxLength={128}
            type="password"
            required={!editingId}
          />
          <Input placeholder="Telefoni" value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} />
          <Input placeholder="URL e fotos së profilit" value={form.profilePhotoUrl} onChange={(event) => setForm({ ...form, profilePhotoUrl: event.target.value })} />
          <div className="grid gap-2 sm:grid-cols-3 xl:grid-cols-1 2xl:grid-cols-3">
            <Input placeholder="Pozita" value={form.positionTitle} onChange={(event) => setForm({ ...form, positionTitle: event.target.value })} />
            <Input placeholder="Departamenti" value={form.departmentName} onChange={(event) => setForm({ ...form, departmentName: event.target.value })} />
            <Input placeholder="Ekipi" value={form.teamName} onChange={(event) => setForm({ ...form, teamName: event.target.value })} />
          </div>
          <textarea className="min-h-24 w-full rounded-md border border-border bg-background px-3 py-3 text-sm outline-none focus:border-primary focus:ring-4 focus:ring-primary/15" placeholder="Shënime të brendshme" value={form.notes} onChange={(event) => setForm({ ...form, notes: event.target.value })} />
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button className="flex-1" disabled={saving}>{saving ? "Duke ruajtur..." : editingId ? "Ruaj ndryshimet" : "Krijo punëtor"}</Button>
            {editingId && <Button type="button" variant="secondary" onClick={resetForm}>Anulo</Button>}
          </div>
        </form>
      </Card>

      <div className="grid gap-5">
        <Card className="p-5">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="font-semibold">Punëtorët</h2>
              <p className="text-sm text-muted-foreground">{employees.length} profile punëtorësh</p>
            </div>
            <div className="relative sm:w-72">
              <Search className="absolute left-3 top-3 text-muted-foreground" size={18} />
              <Input className="pl-10" placeholder="Kërko punëtorë" value={query} onChange={(event) => setQuery(event.target.value)} />
            </div>
          </div>

          {message && <p className="mt-4 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}

          <div className="mt-5 grid gap-3">
            {loading && <p className="rounded-md border border-border p-4 text-sm text-muted-foreground">Punëtorët po ngarkohen...</p>}
            {!loading && filteredEmployees.length === 0 && <p className="rounded-md border border-dashed border-border p-4 text-sm text-muted-foreground">Nuk u gjet asnjë punëtor.</p>}
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
                      {employee.employeeCode} · {employee.positionTitle || "Pa pozitë"} · {employee.teamName || "Pa ekip"}
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
              onDelete={() => deleteSelectedEmployee(selected.id)}
              onForceDelete={forceDeleteAllowed && role === "ADMINISTRATOR" ? () => forceDeleteSelectedEmployee(selected.id) : null}
              onResetPassword={async () => {
                const response = await resetEmployeePassword(accessToken, selected.id);
                return response.temporaryPassword;
              }}
              onAttendanceChanged={() => void loadHistory(selected.id)}
            />
          ) : (
            <div className="rounded-lg border border-dashed border-border p-8 text-center text-sm text-muted-foreground">
              Zgjidhni një punëtor për të parë profilin dhe historinë e punës.
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}

function validateEmployeePassword(password: string, editing: boolean) {
  const normalized = password.trim();
  if (!editing && !normalized) {
    return "Fjalëkalimi është i detyrueshëm.";
  }
  if (normalized && normalized.length < 8) {
    return "Fjalëkalimi duhet të ketë të paktën 8 karaktere.";
  }
  if (normalized.length > 128) {
    return "Fjalekalimi duhet te kete 128 karaktere ose me pak.";
  }
  return "";
}

function EmployeeProfile({
  accessToken,
  employee,
  history,
  historyLoading,
  onEdit,
  onDeactivate,
  onDelete,
  onForceDelete,
  onResetPassword,
  onAttendanceChanged,
}: {
  accessToken: string;
  employee: Employee;
  history: AttendanceReportRow[];
  historyLoading: boolean;
  onEdit: () => void;
  onDeactivate: () => void;
  onDelete: () => Promise<void>;
  onForceDelete: (() => Promise<void>) | null;
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
  const [checkoutRecord, setCheckoutRecord] = useState<AttendanceReportRow | null>(null);
  const [checkoutValue, setCheckoutValue] = useState("");
  const [checkoutSaving, setCheckoutSaving] = useState(false);
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [deleteConfirmation, setDeleteConfirmation] = useState("");
  const [deleteSaving, setDeleteSaving] = useState(false);
  const [forceDeleteModalOpen, setForceDeleteModalOpen] = useState(false);
  const [forceDeleteCode, setForceDeleteCode] = useState("");
  const [forceDeleteWord, setForceDeleteWord] = useState("");
  const [forceDeleteSaving, setForceDeleteSaving] = useState(false);
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
      setExportMessage("Zgjidhni një interval të vlefshëm për eksport.");
      return;
    }

    setExporting(format);
    try {
      const blob = await exportEmployeeAttendance(accessToken, employee.id, { from: exportFrom, to: exportTo, format });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = format === "pdf"
        ? employeePdfFilename(employee.employeeCode, exportFrom, exportTo)
        : `${employee.employeeCode}-attendance-${exportFrom}-to-${exportTo}.${format}`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
      setExportMessage("Eksporti i historisë së punës u shkarkua.");
    } catch (error) {
      setExportMessage(error instanceof Error ? error.message : "Eksporti nuk mund të shkarkohej.");
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
      setExportMessage("Fjalëkalimi i përkohshëm u krijua. Shfaqet vetëm një herë.");
    } catch (error) {
      setExportMessage(error instanceof Error ? error.message : "Fjalëkalimi nuk mund të rivendosej.");
    } finally {
      setResettingPassword(false);
    }
  }

  async function checkoutFromProfile(row: AttendanceReportRow) {
    if (!row.attendanceRecordId) {
      setExportMessage("Regjistrimi i punës mungon.");
      return;
    }
    setCheckoutRecord(row);
    setCheckoutValue(toDateTimeInput(new Date()));
  }

  async function submitCheckout() {
    if (!checkoutRecord?.attendanceRecordId || !checkoutValue) {
      setExportMessage("Zgjidhni datën dhe orën e daljes.");
      return;
    }
    setCheckoutSaving(true);
    setExportMessage("");
    try {
      const parsed = new Date(checkoutValue);
      if (Number.isNaN(parsed.getTime())) {
        throw new Error("Zgjidhni një datë dhe orë të vlefshme.");
      }
      await adminCheckout(accessToken, checkoutRecord.attendanceRecordId, parsed.toISOString());
      setExportMessage("Dalja u regjistrua nga administratori.");
      setCheckoutRecord(null);
      setCheckoutValue("");
      onAttendanceChanged();
    } catch (error) {
      setExportMessage(error instanceof Error ? error.message : "Dalja nuk mund të regjistrohej.");
    } finally {
      setCheckoutSaving(false);
    }
  }

  async function confirmDelete() {
    if (deleteConfirmation.trim() !== employee.employeeCode || deleteSaving) {
      return;
    }
    setDeleteSaving(true);
    setExportMessage("");
    try {
      await onDelete();
      setDeleteModalOpen(false);
      setDeleteConfirmation("");
    } catch (error) {
      setExportMessage(error instanceof Error ? error.message : "Punëtori nuk mund të fshihej.");
    } finally {
      setDeleteSaving(false);
    }
  }

  async function confirmForceDelete() {
    if (!onForceDelete || forceDeleteSaving || forceDeleteCode.trim() !== employee.employeeCode || forceDeleteWord.trim() !== "FSHI") {
      return;
    }
    setForceDeleteSaving(true);
    setExportMessage("");
    try {
      await onForceDelete();
      setForceDeleteModalOpen(false);
      setForceDeleteCode("");
      setForceDeleteWord("");
    } catch (error) {
      setExportMessage(error instanceof Error ? error.message : "Punëtori nuk mund të fshihej me historinë.");
    } finally {
      setForceDeleteSaving(false);
    }
  }

  return (
    <div>
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div className="flex items-center gap-4">
          <Avatar employee={employee} large />
          <div>
            <h2 className="text-xl font-semibold">{employee.fullName}</h2>
            <p className="mt-1 text-sm text-muted-foreground">{employee.employeeCode} · {employee.status === "ACTIVE" ? "Aktiv" : "Joaktiv"}</p>
          </div>
        </div>
        <div className="flex flex-wrap gap-2 sm:justify-end">
          <Button type="button" variant="secondary" onClick={onEdit}><Edit3 size={17} />Edito</Button>
          <Button type="button" variant="secondary" disabled={resettingPassword} onClick={() => void resetPassword()}>
            <KeyRound size={17} />
            {resettingPassword ? "Duke ruajtur..." : "Rivendos fjalëkalimin"}
          </Button>
          <Button type="button" variant="danger" disabled={deleteSaving} onClick={() => setDeleteModalOpen(true)}>
            <Trash2 size={17} />
            Fshi punëtorin
          </Button>
          {onForceDelete && (
            <Button
              type="button"
              variant="secondary"
              className="border-destructive/50 bg-destructive/10 text-destructive hover:bg-destructive/15"
              disabled={forceDeleteSaving}
              onClick={() => setForceDeleteModalOpen(true)}
            >
              <Trash2 size={17} />
              Fshi me gjithë historinë
            </Button>
          )}
          <Button type="button" variant="ghost" disabled={employee.status === "INACTIVE"} onClick={onDeactivate}><UserMinus size={17} /></Button>
        </div>
      </div>
      {onForceDelete && (
        <p className="mt-3 max-w-2xl rounded-md border border-destructive/20 bg-destructive/5 px-3 py-2 text-sm text-destructive">
          Fshirja me gjithë historinë është vetëm për pastrim të të dhënave testuese.
        </p>
      )}

      {temporaryPassword && (
        <div className="mt-4 rounded-md border border-primary/30 bg-primary/10 p-4 text-sm">
          <p className="font-semibold text-primary">Fjalëkalim i përkohshëm</p>
          <p className="mt-2 font-mono text-base">{temporaryPassword}</p>
          <p className="mt-2 text-muted-foreground">Jepjani këtë fjalëkalim punëtorit tani. Nuk do të shfaqet përsëri.</p>
        </div>
      )}

      <div className="mt-5 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <ProfileField label="Telefoni" value={employee.phone || "Nuk është vendosur"} />
        <ProfileField label="Pozita" value={employee.positionTitle || "Nuk është vendosur"} />
        <ProfileField label="Departamenti" value={employee.departmentName || "Nuk është vendosur"} />
        <ProfileField label="Ekipi" value={employee.teamName || "Nuk është vendosur"} />
      </div>

      <div className="mt-4 rounded-md bg-muted p-4 text-sm">
        <p className="font-medium">Shënime</p>
        <p className="mt-2 text-muted-foreground">{employee.notes || "Nuk ka shënime për këtë punëtor."}</p>
      </div>

      <div className="mt-5 rounded-lg border border-border p-4">
        <div className="flex flex-col gap-3 xl:flex-row xl:items-end xl:justify-between">
          <div>
            <p className="font-semibold">Eksporto historinë e punës</p>
            <p className="mt-1 text-sm text-muted-foreground">Përmbledhje e thjeshtë e orëve dhe regjistrimeve të punës.</p>
          </div>
          <div className="grid gap-2 sm:grid-cols-[150px_150px_150px] xl:flex xl:items-end">
            <label className="space-y-1 text-sm font-medium">
              <span>Periudha</span>
              <select
                className="h-11 w-full rounded-md border border-border bg-background px-3 text-sm"
                value={rangePreset}
                onChange={(event) => updateRange(event.target.value as ExportRangePreset)}
              >
                <option value="this-week">Kjo javë</option>
                <option value="this-month">Ky muaj</option>
                <option value="last-month">Muaji i kaluar</option>
                <option value="custom">Interval datash</option>
              </select>
            </label>
            <label className="space-y-1 text-sm font-medium">
              <span>Nga</span>
              <Input type="date" value={exportFrom} onChange={(event) => setExportFrom(event.target.value)} disabled={rangePreset !== "custom"} />
            </label>
            <label className="space-y-1 text-sm font-medium">
              <span>Deri</span>
              <Input type="date" value={exportTo} onChange={(event) => setExportTo(event.target.value)} disabled={rangePreset !== "custom"} />
            </label>
          </div>
        </div>
        <div className="mt-3 flex flex-col gap-2 sm:flex-row">
          <Button type="button" variant="secondary" disabled={Boolean(exporting)} onClick={() => void downloadEmployeeExport("pdf")}>
            <FileText size={17} />
            {exporting === "pdf" ? "Duke eksportuar..." : "Eksporto PDF"}
          </Button>
          <Button type="button" variant="secondary" disabled={Boolean(exporting)} onClick={() => void downloadEmployeeExport("csv")}>
            <FileSpreadsheet size={17} />
            {exporting === "csv" ? "Duke eksportuar..." : "Eksporto Excel/CSV"}
          </Button>
        </div>
        {exportMessage && <p className="mt-3 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{exportMessage}</p>}
      </div>

      <div className="mt-5">
        <div className="flex items-center justify-between">
          <h3 className="font-semibold">Ky muaj</h3>
          <div className="flex items-center gap-2 text-sm text-muted-foreground">
            <Clock3 size={16} />
            {formatMinutes(workedMinutes)} punuar
          </div>
        </div>
        <div className="mt-3 divide-y divide-border rounded-lg border border-border">
          {historyLoading && <p className="p-4 text-sm text-muted-foreground">Historia po ngarkohet...</p>}
          {!historyLoading && history.length === 0 && <p className="p-4 text-sm text-muted-foreground">Nuk ka regjistrime për këtë muaj.</p>}
          {history.slice(0, 12).map((row) => (
            <div key={`${row.scheduleId}-${row.workDate}`} className="grid gap-3 p-4 text-sm lg:grid-cols-[1fr_1.4fr_auto] lg:items-center">
              <div>
                <p className="font-semibold">{formatDate(row.workDate)}</p>
                <p className="mt-1 text-xs text-muted-foreground">{row.employeeCode}</p>
              </div>
              <div className="grid gap-2 text-muted-foreground sm:grid-cols-2">
                <span>Hyrja: {formatTime(row.checkedInAt)}</span>
                <span>Dalja: {checkoutLabel(row.checkoutType, row.checkedOutAt)}</span>
                <GpsCell label="GPS hyrje" latitude={row.checkInLatitude} longitude={row.checkInLongitude} distanceMeters={row.checkInDistanceMeters} />
                <GpsCell label="GPS dalje" latitude={row.checkOutLatitude} longitude={row.checkOutLongitude} distanceMeters={row.checkOutDistanceMeters} />
                <span>Orët: {formatMinutes(row.workedMinutes)}</span>
                <span>Shtesë: {row.overtimeMinutes > 0 ? formatMinutes(row.overtimeMinutes) : "-"}</span>
              </div>
              <div className="flex flex-wrap gap-2 lg:justify-end">
                <span className={`rounded-md px-2 py-1 text-xs font-medium ${row.absent ? "bg-destructive/10 text-destructive" : "bg-accent/10 text-accent"}`}>
                  Statusi: {checkoutTypeLabel(row.checkoutType, row.status)}
                </span>
                {!row.checkedOutAt && row.attendanceRecordId && (
                  <Button type="button" variant="secondary" className="h-9 px-3" onClick={() => void checkoutFromProfile(row)}>
                    Regjistro dalje
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
      {checkoutRecord && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 px-4">
          <div className="w-full max-w-md rounded-lg border border-border bg-card p-5 shadow-xl">
            <h3 className="text-lg font-semibold">Regjistro daljen</h3>
            <p className="mt-2 text-sm text-muted-foreground">
              Zgjidhni datën dhe orën kur ky punëtor ka përfunduar punën.
            </p>
            <label className="mt-4 block space-y-1 text-sm font-medium">
              <span>Data dhe ora</span>
              <Input type="datetime-local" value={checkoutValue} onChange={(event) => setCheckoutValue(event.target.value)} />
            </label>
            <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:justify-end">
              <Button
                type="button"
                variant="secondary"
                disabled={checkoutSaving}
                onClick={() => {
                  setCheckoutRecord(null);
                  setCheckoutValue("");
                }}
              >
                Anulo
              </Button>
              <Button type="button" disabled={checkoutSaving} onClick={() => void submitCheckout()}>
                {checkoutSaving ? "Duke ruajtur..." : "Ruaj daljen"}
              </Button>
            </div>
          </div>
        </div>
      )}
      {deleteModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 px-4">
          <div className="w-full max-w-md rounded-lg border border-border bg-card p-5 shadow-xl">
            <div className="flex items-center gap-3 text-destructive">
              <div className="rounded-md bg-destructive/10 p-2">
                <Trash2 size={20} />
              </div>
              <h3 className="text-lg font-semibold">Fshi punëtorin</h3>
            </div>
            <p className="mt-4 text-sm text-muted-foreground">
              A jeni të sigurt që dëshironi ta fshini këtë punëtor?
              <br />
              Ky veprim nuk mund të zhbëhet.
            </p>
            <label className="mt-4 block space-y-1 text-sm font-medium">
              <span>Shkruani kodin {employee.employeeCode} për të konfirmuar.</span>
              <Input
                value={deleteConfirmation}
                onChange={(event) => setDeleteConfirmation(event.target.value)}
                disabled={deleteSaving}
                autoFocus
              />
            </label>
            <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:justify-end">
              <Button
                type="button"
                variant="secondary"
                disabled={deleteSaving}
                onClick={() => {
                  setDeleteModalOpen(false);
                  setDeleteConfirmation("");
                }}
              >
                Anulo
              </Button>
              <Button
                type="button"
                variant="danger"
                disabled={deleteSaving || deleteConfirmation.trim() !== employee.employeeCode}
                onClick={() => void confirmDelete()}
              >
                {deleteSaving ? "Duke fshirë..." : "Fshi përgjithmonë"}
              </Button>
            </div>
          </div>
        </div>
      )}
      {forceDeleteModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/45 px-4">
          <div className="w-full max-w-lg rounded-lg border border-destructive/30 bg-card p-5 shadow-xl">
            <div className="flex items-center gap-3 text-destructive">
              <div className="rounded-md bg-destructive/10 p-2">
                <Trash2 size={20} />
              </div>
              <h3 className="text-lg font-semibold">Fshi përgjithmonë punëtorin</h3>
            </div>
            <p className="mt-4 text-sm text-muted-foreground">
              Ky veprim do të fshijë punëtorin dhe të gjithë historinë e tij të punës, GPS-in, raportet dhe të dhënat e lidhura.
              Ky veprim nuk mund të zhbëhet.
            </p>
            <div className="mt-4 grid gap-3">
              <label className="block space-y-1 text-sm font-medium">
                <span>Shkruani kodin {employee.employeeCode} për të konfirmuar.</span>
                <Input
                  value={forceDeleteCode}
                  onChange={(event) => setForceDeleteCode(event.target.value)}
                  disabled={forceDeleteSaving}
                  autoFocus
                />
              </label>
              <label className="block space-y-1 text-sm font-medium">
                <span>Shkruani fjalën FSHI.</span>
                <Input
                  value={forceDeleteWord}
                  onChange={(event) => setForceDeleteWord(event.target.value)}
                  disabled={forceDeleteSaving}
                />
              </label>
            </div>
            <div className="mt-5 flex flex-col gap-2 sm:flex-row sm:justify-end">
              <Button
                type="button"
                variant="secondary"
                disabled={forceDeleteSaving}
                onClick={() => {
                  setForceDeleteModalOpen(false);
                  setForceDeleteCode("");
                  setForceDeleteWord("");
                }}
              >
                Anulo
              </Button>
              <Button
                type="button"
                variant="danger"
                disabled={forceDeleteSaving || forceDeleteCode.trim() !== employee.employeeCode || forceDeleteWord.trim() !== "FSHI"}
                onClick={() => void confirmForceDelete()}
              >
                {forceDeleteSaving ? "Duke fshirë..." : "Fshi përgjithmonë me gjithë historinë"}
              </Button>
            </div>
          </div>
        </div>
      )}
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
    return <span>{label}: -</span>;
  }
  return (
    <span>
      {label}: {latitude.toFixed(5)}, {longitude.toFixed(5)}
      {distanceMeters != null ? ` (${formatDistance(distanceMeters)} nga vendi i punës)` : ""}
      <a className="ml-2 font-medium text-primary" href={mapUrl(latitude, longitude)} target="_blank" rel="noreferrer">
        Hape në hartë
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

function employeePdfFilename(employeeCode: string, from: string, to: string) {
  const fromDate = new Date(`${from}T12:00:00`);
  const toDate = new Date(`${to}T12:00:00`);
  const sameMonth = fromDate.getFullYear() === toDate.getFullYear() && fromDate.getMonth() === toDate.getMonth();
  const period = sameMonth ? from.slice(0, 7) : `${formatFilenameDate(from)}-${formatFilenameDate(to)}`;
  return `Historia_Punes_${safeFilenamePart(employeeCode)}_${period}.pdf`;
}

function formatFilenameDate(value: string) {
  const [year, month, day] = value.split("-");
  return `${day}.${month}.${year}`;
}

function safeFilenamePart(value: string) {
  return value.trim().replace(/[^A-Za-z0-9_-]+/g, "_").replace(/^_+|_+$/g, "") || "Punetori";
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

function checkoutLabel(type: CheckoutType | null, checkedOutAt: string | null) {
  const time = formatTime(checkedOutAt);
  if (type === "AUTO_CHECKED_OUT") {
    return `Dalje automatike: ${time}`;
  }
  if (type === "ADMIN_CHECKED_OUT") {
    return `Dalje nga administratori: ${time}`;
  }
  return time;
}

function checkoutTypeLabel(type: CheckoutType | null, status: string) {
  if (status === "CHECKED_OUT" || type) {
    return "Dalë";
  }
  if (status === "PRESENT" || status === "LATE") {
    return "Në punë";
  }
  return "-";
}

function toDateTimeInput(date: Date) {
  const offset = date.getTimezoneOffset();
  return new Date(date.getTime() - offset * 60_000).toISOString().slice(0, 16);
}

function mapUrl(latitude: number, longitude: number) {
  return mapLocationUrl(latitude, longitude);
}

function formatDistance(meters: number) {
  return meters >= 1000 ? `${(meters / 1000).toFixed(2)} km` : `${meters} m`;
}
