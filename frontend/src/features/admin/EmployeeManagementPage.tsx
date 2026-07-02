import { FormEvent, useEffect, useMemo, useState } from "react";
import { Search, UserMinus, UserPlus } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import {
  createEmployee,
  deactivateEmployee,
  type Employee,
  listEmployees,
} from "../../lib/api";

type EmployeeManagementPageProps = {
  accessToken: string;
};

type EmployeeForm = {
  employeeCode: string;
  fullName: string;
  pin: string;
  phone: string;
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
  notes: "",
  wageType: "HOURLY",
  baseWage: "0",
  overtimeMultiplier: "1.5",
};

export function EmployeeManagementPage({ accessToken }: EmployeeManagementPageProps) {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [form, setForm] = useState<EmployeeForm>(initialForm);
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState("");

  async function loadEmployees() {
    setLoading(true);
    setMessage("");
    try {
      setEmployees(await listEmployees(accessToken));
    } catch {
      setMessage("Employees could not be loaded.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadEmployees();
  }, []);

  const filteredEmployees = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) {
      return employees;
    }

    return employees.filter((employee) =>
      [employee.fullName, employee.employeeCode, employee.phone ?? ""].some((value) =>
        value.toLowerCase().includes(normalized),
      ),
    );
  }, [employees, query]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setMessage("");

    try {
      const employee = await createEmployee(accessToken, {
        ...form,
        baseWage: Number(form.baseWage),
        overtimeMultiplier: Number(form.overtimeMultiplier),
      });
      setEmployees((current) => [employee, ...current]);
      setForm(initialForm);
      setMessage("Employee created successfully.");
    } catch {
      setMessage("Employee could not be created. Check the employee ID and PIN.");
    } finally {
      setSaving(false);
    }
  }

  async function deactivate(employeeId: string) {
    setMessage("");
    try {
      const updated = await deactivateEmployee(accessToken, employeeId);
      setEmployees((current) => current.map((employee) => (employee.id === employeeId ? updated : employee)));
    } catch {
      setMessage("Employee could not be deactivated.");
    }
  }

  return (
    <div className="grid gap-5 xl:grid-cols-[380px_1fr]">
      <Card className="p-5">
        <div className="flex items-center gap-3">
          <div className="rounded-md bg-primary/15 p-2 text-primary">
            <UserPlus size={21} />
          </div>
          <div>
            <h2 className="font-semibold">Create employee</h2>
            <p className="text-sm text-muted-foreground">Employee ID and PIN are used for simple shift login.</p>
          </div>
        </div>

        <form className="mt-5 space-y-3" onSubmit={submit}>
          <Input
            placeholder="Employee ID"
            value={form.employeeCode}
            onChange={(event) => setForm({ ...form, employeeCode: event.target.value })}
            required
          />
          <Input
            placeholder="Full name"
            value={form.fullName}
            onChange={(event) => setForm({ ...form, fullName: event.target.value })}
            required
          />
          <Input
            placeholder="4-digit PIN"
            value={form.pin}
            onChange={(event) => setForm({ ...form, pin: event.target.value })}
            inputMode="numeric"
            maxLength={4}
            required
          />
          <Input
            placeholder="Phone"
            value={form.phone}
            onChange={(event) => setForm({ ...form, phone: event.target.value })}
          />
          <div className="grid grid-cols-3 gap-2">
            <select
              className="h-11 rounded-md border border-border bg-background px-3 text-sm"
              value={form.wageType}
              onChange={(event) => setForm({ ...form, wageType: event.target.value as Employee["wageType"] })}
            >
              <option value="HOURLY">Hourly</option>
              <option value="DAILY">Daily</option>
              <option value="MONTHLY">Monthly</option>
            </select>
            <Input
              placeholder="Wage"
              type="number"
              min="0"
              step="0.01"
              value={form.baseWage}
              onChange={(event) => setForm({ ...form, baseWage: event.target.value })}
            />
            <Input
              placeholder="OT"
              type="number"
              min="1"
              step="0.01"
              value={form.overtimeMultiplier}
              onChange={(event) => setForm({ ...form, overtimeMultiplier: event.target.value })}
            />
          </div>
          <textarea
            className="min-h-24 w-full rounded-md border border-border bg-background px-3 py-3 text-sm outline-none focus:border-primary focus:ring-4 focus:ring-primary/15"
            placeholder="Internal notes"
            value={form.notes}
            onChange={(event) => setForm({ ...form, notes: event.target.value })}
          />
          <Button className="w-full" disabled={saving}>
            {saving ? "Saving..." : "Create employee"}
          </Button>
        </form>
      </Card>

      <Card className="p-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="font-semibold">Employees</h2>
            <p className="text-sm text-muted-foreground">{employees.length} employee profiles</p>
          </div>
          <div className="relative sm:w-72">
            <Search className="absolute left-3 top-3 text-muted-foreground" size={18} />
            <Input
              className="pl-10"
              placeholder="Search employees"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </div>
        </div>

        {message && <p className="mt-4 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}

        <div className="mt-5 overflow-hidden rounded-lg border border-border">
          <div className="grid grid-cols-[1.2fr_0.8fr_0.7fr_0.7fr_auto] gap-3 bg-muted px-4 py-3 text-xs font-semibold uppercase text-muted-foreground">
            <span>Name</span>
            <span>ID</span>
            <span>Wage</span>
            <span>Status</span>
            <span />
          </div>
          <div className="divide-y divide-border">
            {loading && <p className="p-4 text-sm text-muted-foreground">Loading employees...</p>}
            {!loading && filteredEmployees.length === 0 && (
              <p className="p-4 text-sm text-muted-foreground">No employees found.</p>
            )}
            {filteredEmployees.map((employee) => (
              <div
                key={employee.id}
                className="grid grid-cols-[1.2fr_0.8fr_0.7fr_0.7fr_auto] items-center gap-3 px-4 py-3 text-sm"
              >
                <span className="font-medium">{employee.fullName}</span>
                <span className="text-muted-foreground">{employee.employeeCode}</span>
                <span className="text-muted-foreground">{employee.wageType.toLowerCase()}</span>
                <span className={employee.status === "ACTIVE" ? "text-accent" : "text-muted-foreground"}>
                  {employee.status}
                </span>
                <Button
                  type="button"
                  variant="ghost"
                  disabled={employee.status === "INACTIVE"}
                  onClick={() => void deactivate(employee.id)}
                  aria-label={`Deactivate ${employee.fullName}`}
                >
                  <UserMinus size={17} />
                </Button>
              </div>
            ))}
          </div>
        </div>
      </Card>
    </div>
  );
}
