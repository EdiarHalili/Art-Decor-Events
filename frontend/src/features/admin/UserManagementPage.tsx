import { FormEvent, useEffect, useMemo, useState } from "react";
import { ShieldCheck, UserMinus } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import {
  createAdminUser,
  deactivateAdminUser,
  listAdminUsers,
  type AdminUser,
} from "../../lib/api";

type UserManagementPageProps = {
  accessToken: string;
};

export function UserManagementPage({ accessToken }: UserManagementPageProps) {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [query, setQuery] = useState("");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<"ADMINISTRATOR" | "SUPERVISOR">("SUPERVISOR");
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);

  async function loadUsers() {
    setMessage("");
    try {
      setUsers(await listAdminUsers(accessToken));
    } catch {
      setMessage("Users could not be loaded. Administrator role is required.");
    }
  }

  useEffect(() => {
    void loadUsers();
  }, []);

  const filteredUsers = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) {
      return users;
    }

    return users.filter((user) =>
      [user.fullName, user.email, user.role].some((value) => value.toLowerCase().includes(normalized)),
    );
  }, [query, users]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setMessage("");

    try {
      const user = await createAdminUser(accessToken, { fullName, email, password, role });
      setUsers((current) => [user, ...current]);
      setFullName("");
      setEmail("");
      setPassword("");
      setRole("SUPERVISOR");
      setMessage("User created successfully.");
    } catch {
      setMessage("User could not be created. Check the email and password.");
    } finally {
      setSaving(false);
    }
  }

  async function deactivate(userId: string) {
    setMessage("");
    try {
      const updated = await deactivateAdminUser(accessToken, userId);
      setUsers((current) => current.map((user) => (user.id === userId ? updated : user)));
    } catch {
      setMessage("User could not be deactivated.");
    }
  }

  return (
    <div className="grid gap-5 xl:grid-cols-[380px_1fr]">
      <Card className="p-5">
        <div className="flex items-center gap-3">
          <div className="rounded-md bg-primary/15 p-2 text-primary">
            <ShieldCheck size={21} />
          </div>
          <div>
            <h2 className="font-semibold">Create admin user</h2>
            <p className="text-sm text-muted-foreground">Grant administrator or supervisor access.</p>
          </div>
        </div>

        <form className="mt-5 space-y-3" onSubmit={submit}>
          <Input placeholder="Full name" value={fullName} onChange={(event) => setFullName(event.target.value)} required />
          <Input
            placeholder="Email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />
          <Input
            placeholder="Temporary password"
            type="password"
            minLength={8}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
          <select
            className="h-11 w-full rounded-md border border-border bg-background px-3 text-sm"
            value={role}
            onChange={(event) => setRole(event.target.value as "ADMINISTRATOR" | "SUPERVISOR")}
          >
            <option value="SUPERVISOR">Supervisor</option>
            <option value="ADMINISTRATOR">Administrator</option>
          </select>
          <Button className="w-full" disabled={saving}>
            {saving ? "Saving..." : "Create user"}
          </Button>
        </form>
      </Card>

      <Card className="p-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="font-semibold">System users</h2>
            <p className="text-sm text-muted-foreground">{users.length} admin and supervisor accounts</p>
          </div>
          <Input
            className="sm:w-72"
            placeholder="Search users"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </div>

        {message && <p className="mt-4 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}

        <div className="mt-5 divide-y divide-border rounded-lg border border-border">
          {filteredUsers.length === 0 && <p className="p-4 text-sm text-muted-foreground">No users found.</p>}
          {filteredUsers.map((user) => (
            <div key={user.id} className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="font-medium">{user.fullName}</p>
                <p className="text-sm text-muted-foreground">{user.email}</p>
              </div>
              <div className="flex items-center gap-3">
                <span className="rounded-md bg-muted px-2 py-1 text-xs font-semibold">{user.role}</span>
                <span className={user.status === "ACTIVE" ? "text-sm text-accent" : "text-sm text-muted-foreground"}>
                  {user.status}
                </span>
                <Button
                  type="button"
                  variant="ghost"
                  disabled={user.status === "INACTIVE"}
                  onClick={() => void deactivate(user.id)}
                  aria-label={`Deactivate ${user.fullName}`}
                >
                  <UserMinus size={17} />
                </Button>
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}
