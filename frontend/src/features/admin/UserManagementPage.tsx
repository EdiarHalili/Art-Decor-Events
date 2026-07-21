import { FormEvent, useEffect, useMemo, useState } from "react";
import { Eye, Pencil, RotateCcw, ShieldCheck, UserMinus, UserPlus } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import {
  activateAdminUser,
  createAdminUser,
  deactivateAdminUser,
  listAdminUsers,
  resetAdminUserPassword,
  updateAdminUser,
  updateAdminUserRole,
  type AdminUser,
  type UserRole,
} from "../../lib/api";

type AdminRole = "SUPER_ADMIN" | "ADMINISTRATOR" | "SUPERVISOR";

type UserManagementPageProps = {
  accessToken: string;
  role: UserRole;
};

const adminRoles: AdminRole[] = ["SUPER_ADMIN", "ADMINISTRATOR", "SUPERVISOR"];
const superAdminCreatableRoles: AdminRole[] = ["ADMINISTRATOR", "SUPERVISOR"];
const administratorCreatableRoles: AdminRole[] = ["SUPERVISOR"];

export function UserManagementPage({ accessToken, role: currentRole }: UserManagementPageProps) {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<AdminRole>("SUPERVISOR");
  const [editName, setEditName] = useState("");
  const [editEmail, setEditEmail] = useState("");
  const [editRole, setEditRole] = useState<AdminRole>("SUPERVISOR");
  const [resetPassword, setResetPassword] = useState("");
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState<string | null>(null);

  const isSuperAdmin = currentRole === "SUPER_ADMIN";
  const canAccess = currentRole === "SUPER_ADMIN" || currentRole === "ADMINISTRATOR";
  const allowedCreateRoles = isSuperAdmin ? superAdminCreatableRoles : administratorCreatableRoles;

  async function loadUsers() {
    setMessage("");
    try {
      const nextUsers = await listAdminUsers(accessToken);
      setUsers(nextUsers);
      setSelectedUserId((current) => current ?? nextUsers[0]?.id ?? null);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Përdoruesit nuk mund të ngarkoheshin.");
    }
  }

  useEffect(() => {
    if (canAccess) {
      void loadUsers();
    }
  }, [accessToken, canAccess]);

  const selectedUser = users.find((user) => user.id === selectedUserId) ?? null;

  useEffect(() => {
    if (!selectedUser) {
      setEditName("");
      setEditEmail("");
      setEditRole("SUPERVISOR");
      setResetPassword("");
      return;
    }
    setEditName(selectedUser.fullName);
    setEditEmail(selectedUser.email);
    setEditRole(isAdminRole(selectedUser.role) ? selectedUser.role : "SUPERVISOR");
    setResetPassword("");
  }, [selectedUser?.id]);

  const filteredUsers = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    const adminUsers = users.filter((user) => user.role !== "EMPLOYEE");
    if (!normalized) {
      return adminUsers;
    }
    return adminUsers.filter((user) =>
      [user.fullName, user.email, roleLabel(user.role), statusLabel(user.status)].some((value) =>
        value.toLowerCase().includes(normalized),
      ),
    );
  }, [query, users]);

  if (!canAccess) {
    return (
      <Card className="p-5">
        <h2 className="font-semibold">Administrimi i përdoruesve</h2>
        <p className="mt-2 text-sm text-muted-foreground">Nuk keni leje për këtë seksion.</p>
      </Card>
    );
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving("create");
    setMessage("");
    try {
      const user = await createAdminUser(accessToken, { fullName, email, password, role });
      setUsers((current) => [user, ...current]);
      setSelectedUserId(user.id);
      setFullName("");
      setEmail("");
      setPassword("");
      setRole("SUPERVISOR");
      setMessage("Përdoruesi u krijua me sukses.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Përdoruesi nuk mund të krijohej.");
    } finally {
      setSaving(null);
    }
  }

  async function saveDetails() {
    if (!selectedUser) {
      return;
    }
    setSaving("details");
    setMessage("");
    try {
      const updated = await updateAdminUser(accessToken, selectedUser.id, { fullName: editName, email: editEmail });
      replaceUser(updated);
      setMessage("Të dhënat u ruajtën me sukses.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Të dhënat nuk mund të ruheshin.");
    } finally {
      setSaving(null);
    }
  }

  async function saveRole() {
    if (!selectedUser) {
      return;
    }
    setSaving("role");
    setMessage("");
    try {
      const updated = await updateAdminUserRole(accessToken, selectedUser.id, editRole);
      replaceUser(updated);
      setMessage("Roli u përditësua me sukses.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Roli nuk mund të përditësohej.");
    } finally {
      setSaving(null);
    }
  }

  async function resetSelectedPassword() {
    if (!selectedUser) {
      return;
    }
    setSaving("password");
    setMessage("");
    try {
      await resetAdminUserPassword(accessToken, selectedUser.id, resetPassword);
      setResetPassword("");
      setMessage("Fjalëkalimi i përkohshëm u vendos. Përdoruesi duhet ta ndryshojë në hyrjen tjetër.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Fjalëkalimi nuk mund të rivendosej.");
    } finally {
      setSaving(null);
    }
  }

  async function toggleStatus() {
    if (!selectedUser) {
      return;
    }
    setSaving("status");
    setMessage("");
    try {
      const updated =
        selectedUser.status === "ACTIVE"
          ? await deactivateAdminUser(accessToken, selectedUser.id)
          : await activateAdminUser(accessToken, selectedUser.id);
      replaceUser(updated);
      setMessage(updated.status === "ACTIVE" ? "Përdoruesi u aktivizua." : "Përdoruesi u çaktivizua.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Statusi nuk mund të ndryshohej.");
    } finally {
      setSaving(null);
    }
  }

  function replaceUser(updated: AdminUser) {
    setUsers((current) => current.map((user) => (user.id === updated.id ? updated : user)));
  }

  const selectedManageable = selectedUser ? isSuperAdmin || selectedUser.role === "SUPERVISOR" : false;

  return (
    <div className="grid min-w-0 gap-4 sm:gap-5 xl:grid-cols-[minmax(0,380px)_minmax(0,1fr)]">
      <Card className="p-4 sm:p-5">
        <div className="flex items-center gap-3">
          <div className="rounded-md bg-primary/15 p-2 text-primary">
            <ShieldCheck size={21} />
          </div>
          <div className="min-w-0">
            <h2 className="font-semibold">Krijo përdorues administrativ</h2>
            <p className="text-sm text-muted-foreground">Jep qasje për administratorë ose supervisorë.</p>
          </div>
        </div>

        <form className="mt-5 space-y-3" onSubmit={submit}>
          <Input placeholder="Emri i plotë" value={fullName} onChange={(event) => setFullName(event.target.value)} required />
          <Input placeholder="Email" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
          <Input
            placeholder="Fjalëkalim i përkohshëm"
            type="password"
            minLength={8}
            maxLength={128}
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
          <select
            className="h-11 w-full rounded-md border border-border bg-background px-3 text-sm"
            value={role}
            onChange={(event) => setRole(event.target.value as AdminRole)}
          >
            {allowedCreateRoles.map((item) => (
              <option key={item} value={item}>
                {roleLabel(item)}
              </option>
            ))}
          </select>
          <Button className="w-full" disabled={saving === "create"}>
            <UserPlus size={17} />
            {saving === "create" ? "Duke ruajtur..." : "Krijo përdorues"}
          </Button>
        </form>
      </Card>

      <div className="grid min-w-0 gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(300px,380px)]">
        <Card className="p-4 sm:p-5">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 className="font-semibold">Administrimi i përdoruesve</h2>
              <p className="text-sm text-muted-foreground">{users.length} llogari administrative</p>
            </div>
            <Input
              className="sm:w-72"
              placeholder="Kërko përdorues"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </div>

          {message && <p className="mt-4 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}

          <div className="mt-5 divide-y divide-border rounded-lg border border-border">
            {filteredUsers.length === 0 && <p className="p-4 text-sm text-muted-foreground">Nuk u gjet asnjë përdorues.</p>}
            {filteredUsers.map((user) => (
              <button
                key={user.id}
                type="button"
                onClick={() => setSelectedUserId(user.id)}
                className={`flex w-full min-w-0 flex-col gap-3 p-3 text-left transition sm:flex-row sm:items-center sm:justify-between sm:p-4 ${
                  selectedUserId === user.id ? "bg-muted" : "hover:bg-muted/70"
                }`}
              >
                <div className="min-w-0">
                  <p className="break-words font-medium">{user.fullName}</p>
                  <p className="break-all text-sm text-muted-foreground">{user.email}</p>
                  <p className="mt-1 text-xs text-muted-foreground">Krijuar: {dateLabel(user.createdAt)}</p>
                </div>
                <div className="flex min-w-0 flex-wrap items-center gap-2">
                  <span className="rounded-md bg-card px-2 py-1 text-xs font-semibold">{roleLabel(user.role)}</span>
                  <span className={user.status === "ACTIVE" ? "text-sm text-accent" : "text-sm text-muted-foreground"}>
                    {statusLabel(user.status)}
                  </span>
                  <Eye size={17} />
                </div>
              </button>
            ))}
          </div>
        </Card>

        <Card className="p-4 sm:p-5">
          {!selectedUser && <p className="text-sm text-muted-foreground">Zgjidhni një përdorues për të parë detajet.</p>}
          {selectedUser && (
            <div className="space-y-5">
              <div>
                <h2 className="font-semibold">Detajet e përdoruesit</h2>
                <p className="mt-1 break-all text-sm text-muted-foreground">{selectedUser.email}</p>
              </div>

              <div className="grid gap-2 text-sm">
                <p>Roli: {roleLabel(selectedUser.role)}</p>
                <p>Statusi: {statusLabel(selectedUser.status)}</p>
                <p>Krijuar: {dateLabel(selectedUser.createdAt)}</p>
                <p>Përditësuar: {dateLabel(selectedUser.updatedAt)}</p>
              </div>

              <div className="space-y-3">
                <Input value={editName} onChange={(event) => setEditName(event.target.value)} placeholder="Emri i plotë" />
                <Input value={editEmail} onChange={(event) => setEditEmail(event.target.value)} placeholder="Email" type="email" />
                <Button className="w-full" variant="secondary" disabled={!selectedManageable || saving === "details"} onClick={saveDetails}>
                  <Pencil size={17} />
                  {saving === "details" ? "Duke ruajtur..." : "Ruaj detajet"}
                </Button>
              </div>

              <div className="space-y-3">
                <select
                  className="h-11 w-full rounded-md border border-border bg-background px-3 text-sm"
                  value={editRole}
                  disabled={!selectedManageable}
                  onChange={(event) => setEditRole(event.target.value as AdminRole)}
                >
                  {(isSuperAdmin ? adminRoles : (["SUPERVISOR"] as AdminRole[])).map((item) => (
                    <option key={item} value={item}>
                      {roleLabel(item)}
                    </option>
                  ))}
                </select>
                <Button className="w-full" variant="secondary" disabled={!selectedManageable || saving === "role"} onClick={saveRole}>
                  <ShieldCheck size={17} />
                  {saving === "role" ? "Duke ruajtur..." : "Ndrysho rolin"}
                </Button>
              </div>

              <div className="space-y-3">
                <Input
                  value={resetPassword}
                  onChange={(event) => setResetPassword(event.target.value)}
                  placeholder="Fjalëkalim i ri i përkohshëm"
                  type="password"
                  minLength={8}
                  maxLength={128}
                />
                <Button className="w-full" variant="secondary" disabled={!selectedManageable || saving === "password"} onClick={resetSelectedPassword}>
                  <RotateCcw size={17} />
                  {saving === "password" ? "Duke rivendosur..." : "Rivendos fjalëkalimin"}
                </Button>
              </div>

              <Button
                className="w-full"
                variant={selectedUser.status === "ACTIVE" ? "danger" : "secondary"}
                disabled={!selectedManageable || saving === "status"}
                onClick={toggleStatus}
              >
                <UserMinus size={17} />
                {saving === "status"
                  ? "Duke përditësuar..."
                  : selectedUser.status === "ACTIVE"
                    ? "Çaktivizo përdoruesin"
                    : "Aktivizo përdoruesin"}
              </Button>

              {!selectedManageable && (
                <p className="rounded-md bg-muted p-3 text-sm text-muted-foreground">
                  Vetëm Super Admin mund të ndryshojë këtë llogari administrative.
                </p>
              )}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}

function isAdminRole(role: UserRole): role is AdminRole {
  return role === "SUPER_ADMIN" || role === "ADMINISTRATOR" || role === "SUPERVISOR";
}

function roleLabel(role: UserRole) {
  const labels: Record<UserRole, string> = {
    SUPER_ADMIN: "Super Admin",
    ADMINISTRATOR: "Administrator",
    SUPERVISOR: "Supervisor",
    EMPLOYEE: "Punëtor",
  };
  return labels[role];
}

function statusLabel(status: AdminUser["status"]) {
  return status === "ACTIVE" ? "Aktiv" : "Joaktiv";
}

function dateLabel(value: string | null) {
  if (!value) {
    return "-";
  }
  return new Intl.DateTimeFormat("sq-AL", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}
