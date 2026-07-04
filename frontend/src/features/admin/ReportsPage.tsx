import { useEffect, useMemo, useState } from "react";
import { BarChart3, Download, FileSpreadsheet, FileText, History, PieChart, Search } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import {
  exportAttendanceReport,
  getAttendanceReport,
  getEmployeeHistory,
  mapLocationUrl,
  type AttendanceReport,
  type AttendanceReportRow,
  type EmployeeAttendanceSummary,
} from "../../lib/api";

type ReportsPageProps = {
  accessToken: string;
};

type Period = "daily" | "weekly" | "monthly";

export function ReportsPage({ accessToken }: ReportsPageProps) {
  const [from, setFrom] = useState(startOfMonth());
  const [to, setTo] = useState(today());
  const [period, setPeriod] = useState<Period>("daily");
  const [report, setReport] = useState<AttendanceReport | null>(null);
  const [selectedEmployee, setSelectedEmployee] = useState<EmployeeAttendanceSummary | null>(null);
  const [history, setHistory] = useState<AttendanceReportRow[]>([]);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  async function loadReport() {
    setLoading(true);
    setMessage("");
    try {
      const nextReport = await getAttendanceReport(accessToken, { from, to, period });
      setReport(nextReport);
      setSelectedEmployee(null);
      setHistory([]);
    } catch {
      setMessage("Reports could not be loaded.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadReport();
  }, []);

  const filteredRows = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    const rows = selectedEmployee ? history : report?.rows ?? [];
    if (!normalized) {
      return rows;
    }
    return rows.filter((row) =>
      [row.employeeName, row.employeeCode, row.status, row.workDate].some((value) => value.toLowerCase().includes(normalized)),
    );
  }, [history, query, report, selectedEmployee]);

  async function selectEmployee(employee: EmployeeAttendanceSummary) {
    setSelectedEmployee(employee);
    setMessage("");
    try {
      setHistory(await getEmployeeHistory(accessToken, employee.employeeId, { from, to }));
    } catch {
      setMessage("Employee history could not be loaded.");
    }
  }

  async function download(format: "csv" | "xlsx" | "pdf") {
    setMessage("");
    try {
      const blob = await exportAttendanceReport(accessToken, { from, to, format });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `art-decor-attendance-${from}-to-${to}.${format === "xlsx" ? "xls" : format}`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch {
      setMessage("Export could not be downloaded.");
    }
  }

  const summary = report?.summary;
  const maxAssigned = Math.max(1, ...(report?.buckets.map((bucket) => bucket.assigned) ?? [1]));

  return (
    <div className="space-y-5">
      <Card className="p-5">
        <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
          <div>
            <div className="flex items-center gap-3">
              <div className="rounded-md bg-primary/15 p-2 text-primary">
                <BarChart3 size={22} />
              </div>
              <div>
                <h2 className="font-semibold">Attendance reports</h2>
                <p className="text-sm text-muted-foreground">Daily, weekly, and monthly attendance with export-ready data.</p>
              </div>
            </div>
          </div>

          <div className="grid gap-2 sm:grid-cols-[150px_150px_150px_auto]">
            <label className="space-y-1 text-sm font-medium">
              <span>From</span>
              <Input type="date" value={from} onChange={(event) => setFrom(event.target.value)} />
            </label>
            <label className="space-y-1 text-sm font-medium">
              <span>To</span>
              <Input type="date" value={to} onChange={(event) => setTo(event.target.value)} />
            </label>
            <label className="space-y-1 text-sm font-medium">
              <span>Period</span>
              <select
                className="h-11 w-full rounded-md border border-border bg-background px-3 text-sm"
                value={period}
                onChange={(event) => setPeriod(event.target.value as Period)}
              >
                <option value="daily">Daily</option>
                <option value="weekly">Weekly</option>
                <option value="monthly">Monthly</option>
              </select>
            </label>
            <Button className="self-end" onClick={() => void loadReport()} disabled={loading}>
              {loading ? "Loading..." : "Apply"}
            </Button>
          </div>
        </div>

        {message && <p className="mt-4 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}
      </Card>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Metric label="Assigned" value={summary?.assigned ?? 0} />
        <Metric label="Present" value={summary?.present ?? 0} />
        <Metric label="Late" value={summary?.late ?? 0} />
        <Metric label="Absent" value={summary?.absent ?? 0} />
      </section>

      <section className="grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">
        <Card className="p-5">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-semibold">Attendance trend</h2>
              <p className="text-sm text-muted-foreground">Present, late, and absent totals grouped by {period}.</p>
            </div>
            <PieChart className="text-primary" size={22} />
          </div>
          <div className="mt-5 space-y-4">
            {(report?.buckets ?? []).length === 0 && (
              <p className="rounded-md border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
                No report data for this range.
              </p>
            )}
            {report?.buckets.map((bucket) => (
              <div key={bucket.label}>
                <div className="mb-1 flex items-center justify-between text-sm">
                  <span className="font-medium">{bucket.label}</span>
                  <span className="text-muted-foreground">{bucket.assigned} assigned</span>
                </div>
                <div className="grid h-3 overflow-hidden rounded-md bg-muted" style={{ gridTemplateColumns: `${Math.max(0, bucket.present) + 1}fr ${Math.max(0, bucket.late) + 1}fr ${Math.max(0, bucket.absent) + 1}fr` }}>
                  <div className="bg-accent" style={{ opacity: bucket.present ? 1 : 0 }} />
                  <div className="bg-primary" style={{ opacity: bucket.late ? 1 : 0 }} />
                  <div className="bg-destructive" style={{ opacity: bucket.absent ? 1 : 0 }} />
                </div>
                <div className="mt-1 h-1 rounded bg-muted">
                  <div className="h-1 rounded bg-foreground/35" style={{ width: `${(bucket.assigned / maxAssigned) * 100}%` }} />
                </div>
              </div>
            ))}
          </div>
        </Card>

        <Card className="p-5">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-semibold">Employee history</h2>
              <p className="text-sm text-muted-foreground">Select a worker to inspect their attendance history.</p>
            </div>
            <History className="text-primary" size={22} />
          </div>
          <div className="mt-5 max-h-80 divide-y divide-border overflow-y-auto rounded-lg border border-border">
            {(report?.employees ?? []).length === 0 && (
              <p className="p-4 text-sm text-muted-foreground">No employee records found.</p>
            )}
            {report?.employees.map((employee) => (
              <button
                key={employee.employeeId}
                className={`flex w-full items-center justify-between gap-3 px-4 py-3 text-left text-sm transition hover:bg-muted ${
                  selectedEmployee?.employeeId === employee.employeeId ? "bg-muted" : ""
                }`}
                onClick={() => void selectEmployee(employee)}
              >
                <span>
                  <span className="block font-medium">{employee.employeeName}</span>
                  <span className="text-xs text-muted-foreground">{employee.employeeCode}</span>
                </span>
                <span className="text-right text-xs text-muted-foreground">
                  {hours(employee.workedMinutes)}
                  <br />
                  {employee.absent} absent
                </span>
              </button>
            ))}
          </div>
        </Card>
      </section>

      <Card className="p-5">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 className="font-semibold">{selectedEmployee ? `${selectedEmployee.employeeName} history` : "Attendance records"}</h2>
            <p className="text-sm text-muted-foreground">
              {filteredRows.length} rows · {hours(summary?.workedMinutes ?? 0)} worked · {hours(summary?.overtimeMinutes ?? 0)} overtime
            </p>
          </div>
          <div className="flex flex-col gap-2 sm:flex-row">
            <div className="relative sm:w-64">
              <Search className="absolute left-3 top-3 text-muted-foreground" size={18} />
              <Input className="pl-10" placeholder="Search records" value={query} onChange={(event) => setQuery(event.target.value)} />
            </div>
            <Button variant="secondary" onClick={() => void download("csv")}>
              <Download size={17} />
              CSV
            </Button>
            <Button variant="secondary" onClick={() => void download("xlsx")}>
              <FileSpreadsheet size={17} />
              Excel
            </Button>
            <Button variant="secondary" onClick={() => void download("pdf")}>
              <FileText size={17} />
              PDF
            </Button>
          </div>
        </div>

        <div className="mt-5 overflow-x-auto rounded-lg border border-border">
          <table className="w-full min-w-[1120px] text-left text-sm">
            <thead className="bg-muted text-xs uppercase text-muted-foreground">
              <tr>
                <th className="px-4 py-3">Date</th>
                <th className="px-4 py-3">Employee</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3">Check In</th>
                <th className="px-4 py-3">Check In GPS</th>
                <th className="px-4 py-3">Check Out</th>
                <th className="px-4 py-3">Check Out GPS</th>
                <th className="px-4 py-3">Worked</th>
                <th className="px-4 py-3">Overtime</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {filteredRows.length === 0 && (
                <tr>
                  <td colSpan={9} className="px-4 py-6 text-center text-muted-foreground">
                    No records match this report.
                  </td>
                </tr>
              )}
              {filteredRows.map((row) => (
                <tr key={`${row.scheduleId}-${row.employeeId}`} className="align-top">
                  <td className="px-4 py-3">{row.workDate}</td>
                  <td className="px-4 py-3">
                    <span className="font-medium">{row.employeeName}</span>
                    <span className="block text-xs text-muted-foreground">{row.employeeCode}</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className={`rounded-md px-2 py-1 text-xs font-semibold ${statusClass(row.status, row.checkoutType)}`}>
                      {checkoutTypeLabel(row.checkoutType, row.status)}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">{time(row.checkedInAt)}</td>
                  <td className="px-4 py-3 text-muted-foreground">
                    <GpsLink latitude={row.checkInLatitude} longitude={row.checkInLongitude} distanceMeters={row.checkInDistanceMeters} />
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    {time(row.checkedOutAt)}
                    {row.checkoutType && row.checkoutType !== "MANUAL_EMPLOYEE" && (
                      <span className="mt-1 block text-xs text-primary">{checkoutTypeLabel(row.checkoutType, row.status)}</span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">
                    <GpsLink latitude={row.checkOutLatitude} longitude={row.checkOutLongitude} distanceMeters={row.checkOutDistanceMeters} />
                  </td>
                  <td className="px-4 py-3">{hours(row.workedMinutes)}</td>
                  <td className="px-4 py-3">{hours(row.overtimeMinutes)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: number }) {
  return (
    <Card className="p-5">
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="mt-3 text-3xl font-semibold">{value}</p>
    </Card>
  );
}

function GpsLink({
  latitude,
  longitude,
  distanceMeters,
}: {
  latitude: number | null;
  longitude: number | null;
  distanceMeters: number | null;
}) {
  if (latitude == null || longitude == null) {
    return <span>-</span>;
  }
  return (
    <span>
      <span className="block">{latitude.toFixed(5)}, {longitude.toFixed(5)}</span>
      {distanceMeters != null && <span className="block text-xs">{formatDistance(distanceMeters)} from workplace</span>}
      <a className="font-medium text-primary" href={mapUrl(latitude, longitude)} target="_blank" rel="noreferrer">
        View on Map
      </a>
    </span>
  );
}

function today() {
  const date = new Date();
  return dateValue(date);
}

function startOfMonth() {
  const date = new Date();
  date.setDate(1);
  return dateValue(date);
}

function dateValue(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function hours(minutes: number) {
  return `${(minutes / 60).toFixed(2)}h`;
}

function time(value: string | null) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat(undefined, { hour: "2-digit", minute: "2-digit" }).format(new Date(value));
}

function label(status: AttendanceReportRow["status"]) {
  return status.replace("_", " ").toLowerCase();
}

function checkoutTypeLabel(type: AttendanceReportRow["checkoutType"], status: AttendanceReportRow["status"]) {
  if (type === "AUTO_CHECKED_OUT") {
    return "Auto Check Out";
  }
  if (type === "ADMIN_CHECKED_OUT") {
    return "Admin Check Out";
  }
  if (status === "CHECKED_OUT") {
    return "Manual Employee Check Out";
  }
  return label(status);
}

function statusClass(status: AttendanceReportRow["status"], checkoutType: AttendanceReportRow["checkoutType"] = null) {
  if (checkoutType === "AUTO_CHECKED_OUT" || checkoutType === "ADMIN_CHECKED_OUT") {
    return "bg-primary/15 text-primary";
  }
  if (status === "ABSENT") {
    return "bg-destructive/15 text-destructive";
  }
  if (status === "LATE") {
    return "bg-primary/15 text-primary";
  }
  if (status === "CHECKED_OUT") {
    return "bg-muted text-muted-foreground";
  }
  return "bg-accent/15 text-accent";
}

function mapUrl(latitude: number, longitude: number) {
  return mapLocationUrl(latitude, longitude);
}

function formatDistance(meters: number) {
  return meters >= 1000 ? `${(meters / 1000).toFixed(2)} km` : `${meters} m`;
}
