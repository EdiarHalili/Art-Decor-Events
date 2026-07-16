import { FormEvent, useEffect, useState } from "react";
import { Bell, Landmark, Megaphone, Palette, Save, ShieldCheck } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import {
  getPayrollPreparation,
  deleteAnnouncement,
  listAnnouncements,
  listAuditLogs,
  publishAnnouncement,
  updateAnnouncement,
  updateSettings,
  type Announcement,
  type AppSettings,
  type AuditLog,
  type PayrollPreparation,
} from "../../lib/api";

type SettingsPageProps = {
  accessToken: string;
  settings: AppSettings | null;
  onSettingsUpdated: (settings: AppSettings) => void;
};

const fallbackSettings: AppSettings = {
  companyName: "Art Decor Events",
  logoUrl: null,
  primaryColor: "#c9a052",
  accentColor: "#4f7f63",
  timezone: "Europe/Berlin",
  defaultCheckInOpenTime: "06:50:00",
  defaultCheckInCloseTime: "07:10:00",
  allowedLateMinutes: 0,
  gpsEnabled: true,
  workplaceLatitude: null,
  workplaceLongitude: null,
  notificationsEnabled: true,
  liveLocationTrackingEnabled: false,
  liveLocationIntervalMinutes: 10,
  sessionTimeoutMinutes: 60,
  openModeUnlimitedCheckout: false,
  updatedAt: null,
};

export function SettingsPage({ accessToken, settings, onSettingsUpdated }: SettingsPageProps) {
  const [form, setForm] = useState<AppSettings>(settings ?? fallbackSettings);
  const [announcements, setAnnouncements] = useState<Announcement[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [payroll, setPayroll] = useState<PayrollPreparation | null>(null);
  const [announcement, setAnnouncement] = useState({ title: "", body: "" });
  const [editingAnnouncementId, setEditingAnnouncementId] = useState<string | null>(null);
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (settings) {
      setForm(settings);
    }
  }, [settings]);

  useEffect(() => {
    const month = new Date().toISOString().slice(0, 7);
    Promise.all([
      listAnnouncements(accessToken),
      listAuditLogs(accessToken),
      getPayrollPreparation(accessToken, month),
    ])
      .then(([announcementData, auditData, payrollData]) => {
        setAnnouncements(announcementData);
        setAuditLogs(auditData);
        setPayroll(payrollData);
      })
      .catch((error: Error) => setMessage(error.message));
  }, [accessToken]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setMessage("");
    try {
      const updated = await updateSettings(accessToken, {
        ...form,
        defaultCheckInOpenTime: normalizeTime(form.defaultCheckInOpenTime),
        defaultCheckInCloseTime: normalizeTime(form.defaultCheckInCloseTime),
      });
      onSettingsUpdated(updated);
      setForm(updated);
      setMessage("Settings saved successfully.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Settings could not be saved.");
    } finally {
      setSaving(false);
    }
  }

  async function publish(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    try {
      if (editingAnnouncementId) {
        const updated = await updateAnnouncement(accessToken, editingAnnouncementId, announcement);
        setAnnouncements((current) => current.map((item) => (item.id === updated.id ? updated : item)));
        setEditingAnnouncementId(null);
        setMessage("Njoftimi u përditësua.");
      } else {
        const created = await publishAnnouncement(accessToken, announcement);
        setAnnouncements((current) => [created, ...current]);
        setMessage("Njoftimi u publikua.");
      }
      setAnnouncement({ title: "", body: "" });
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Njoftimi nuk mund të ruhej.");
    }
  }

  function startEditAnnouncement(item: Announcement) {
    setEditingAnnouncementId(item.id);
    setAnnouncement({ title: item.title, body: item.body });
  }

  async function removeAnnouncement(announcementId: string) {
    setMessage("");
    try {
      await deleteAnnouncement(accessToken, announcementId);
      setAnnouncements((current) => current.filter((item) => item.id !== announcementId));
      if (editingAnnouncementId === announcementId) {
        setEditingAnnouncementId(null);
        setAnnouncement({ title: "", body: "" });
      }
      setMessage("Njoftimi u fshi.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Njoftimi nuk mund të fshihej.");
    }
  }

  return (
    <div className="grid min-w-0 gap-4 sm:gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(0,380px)]">
      <Card className="p-4 sm:p-5">
        <div className="flex items-center gap-3">
          <div className="rounded-md bg-primary/15 p-2 text-primary">
            <Palette size={22} />
          </div>
          <div>
            <h2 className="font-semibold">Cilësimet e kompanisë</h2>
            <p className="text-sm text-muted-foreground">Brandimi, GPS, lokacioni live dhe kontrolli i sesionit.</p>
          </div>
        </div>

        {message && <p className="mt-4 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}

        <form className="mt-5 grid min-w-0 gap-4 md:grid-cols-2" onSubmit={submit}>
          <label className="space-y-1 text-sm font-medium md:col-span-2">
            <span>Emri i kompanisë</span>
            <Input value={form.companyName} onChange={(event) => setForm({ ...form, companyName: event.target.value })} required />
          </label>
          <label className="space-y-1 text-sm font-medium md:col-span-2">
            <span>Logo URL</span>
            <Input value={form.logoUrl ?? ""} onChange={(event) => setForm({ ...form, logoUrl: event.target.value })} placeholder="https://..." />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Ngjyra kryesore</span>
            <Input type="color" value={form.primaryColor} onChange={(event) => setForm({ ...form, primaryColor: event.target.value })} />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Ngjyra ndihmëse</span>
            <Input type="color" value={form.accentColor} onChange={(event) => setForm({ ...form, accentColor: event.target.value })} />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Zona kohore</span>
            <Input value={form.timezone} onChange={(event) => setForm({ ...form, timezone: event.target.value })} required />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Sesioni skadon pas minutave</span>
            <Input type="number" min="5" max="1440" value={form.sessionTimeoutMinutes} onChange={(event) => setForm({ ...form, sessionTimeoutMinutes: Number(event.target.value) })} />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Hyrja standarde hapet</span>
            <Input type="time" value={toTimeInput(form.defaultCheckInOpenTime)} onChange={(event) => setForm({ ...form, defaultCheckInOpenTime: event.target.value })} />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Hyrja standarde mbyllet</span>
            <Input type="time" value={toTimeInput(form.defaultCheckInCloseTime)} onChange={(event) => setForm({ ...form, defaultCheckInCloseTime: event.target.value })} />
          </label>
          {isOvernightDefaultTime(form.defaultCheckInOpenTime, form.defaultCheckInCloseTime) && (
            <p className="rounded-md bg-primary/10 px-3 py-2 text-sm text-primary md:col-span-2">
              Ky orar kalon mesnatën dhe mbyllet ditën tjetër në {toTimeInput(form.defaultCheckInCloseTime)}.
            </p>
          )}
          <label className="space-y-1 text-sm font-medium">
            <span>Minutat e tolerancës</span>
            <Input type="number" min="0" max="240" value={form.allowedLateMinutes} onChange={(event) => setForm({ ...form, allowedLateMinutes: Number(event.target.value) })} />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Intervali i lokacionit live</span>
            <Input type="number" min="5" max="60" value={form.liveLocationIntervalMinutes} onChange={(event) => setForm({ ...form, liveLocationIntervalMinutes: Number(event.target.value) })} />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Gjerësia gjeografike e vendit të punës</span>
            <Input
              type="number"
              min="-90"
              max="90"
              step="0.000001"
              value={form.workplaceLatitude ?? ""}
              onChange={(event) => setForm({ ...form, workplaceLatitude: optionalNumber(event.target.value) })}
              placeholder="Optional"
            />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Gjatësia gjeografike e vendit të punës</span>
            <Input
              type="number"
              min="-180"
              max="180"
              step="0.000001"
              value={form.workplaceLongitude ?? ""}
              onChange={(event) => setForm({ ...form, workplaceLongitude: optionalNumber(event.target.value) })}
              placeholder="Optional"
            />
          </label>
          <div className="grid min-w-0 gap-3 sm:grid-cols-2 md:col-span-2">
            <Toggle label="GPS gjatë hyrjes/daljes" checked={form.gpsEnabled} onChange={(checked) => setForm({ ...form, gpsEnabled: checked })} />
            <Toggle label="Lokacion live gjatë punës" checked={form.liveLocationTrackingEnabled} onChange={(checked) => setForm({ ...form, liveLocationTrackingEnabled: checked })} />
          </div>
          <Button className="md:col-span-2" disabled={saving}>
            <Save size={18} />
            {saving ? "Duke ruajtur..." : "Ruaj cilësimet"}
          </Button>
        </form>
      </Card>

      <div className="grid gap-5">
      <Card className="p-4 sm:p-5">
          <div className="flex items-center gap-3">
            <Megaphone className="text-primary" size={21} />
            <h2 className="font-semibold">Njoftime për punëtorët</h2>
          </div>
          <form className="mt-4 space-y-3" onSubmit={publish}>
            <Input placeholder="Titulli" value={announcement.title} onChange={(event) => setAnnouncement({ ...announcement, title: event.target.value })} required />
            <textarea className="min-h-24 w-full rounded-md border border-border bg-background px-3 py-3 text-sm outline-none focus:border-primary focus:ring-4 focus:ring-primary/15" placeholder="Mesazhi" value={announcement.body} onChange={(event) => setAnnouncement({ ...announcement, body: event.target.value })} required />
            <Button className="w-full">
              <Bell size={18} />
              {editingAnnouncementId ? "Ruaj ndryshimet" : "Publiko"}
            </Button>
            {editingAnnouncementId && (
              <Button type="button" variant="ghost" className="w-full" onClick={() => { setEditingAnnouncementId(null); setAnnouncement({ title: "", body: "" }); }}>
                Anulo editimin
              </Button>
            )}
          </form>
          <div className="mt-4 space-y-2">
            {announcements.map((item) => (
              <div key={item.id} className="rounded-md bg-muted p-3 text-sm">
                <p className="font-medium">{item.title}</p>
                <p className="mt-1 text-muted-foreground">{item.body}</p>
                <div className="mt-3 flex gap-2">
                  <Button type="button" variant="secondary" className="h-9 px-3" onClick={() => startEditAnnouncement(item)}>
                    Edito
                  </Button>
                  <Button type="button" variant="ghost" className="h-9 px-3 text-destructive" onClick={() => void removeAnnouncement(item.id)}>
                    Fshi
                  </Button>
                </div>
              </div>
            ))}
            {announcements.length === 0 && <p className="rounded-md border border-dashed border-border p-4 text-sm text-muted-foreground">Nuk ka njoftime të publikuara.</p>}
          </div>
        </Card>

        <Card className="p-5">
          <div className="flex items-center gap-3">
            <Landmark className="text-primary" size={21} />
            <h2 className="font-semibold">Përgatitja e pagave</h2>
          </div>
          <p className="mt-3 text-sm text-muted-foreground">
            {payroll ? `${payroll.employees.length} punëtorë janë gati për përgatitjen mujore të pagave.` : "Po ngarkohen të dhënat e pagave..."}
          </p>
        </Card>

        <Card className="p-5">
          <div className="flex items-center gap-3">
            <ShieldCheck className="text-primary" size={21} />
            <h2 className="font-semibold">Audit log</h2>
          </div>
          <div className="mt-4 space-y-2">
            {auditLogs.slice(0, 6).map((log) => (
              <div key={log.id} className="rounded-md border border-border p-3 text-xs">
                <p className="font-semibold">{log.action.replaceAll("_", " ")}</p>
                <p className="mt-1 text-muted-foreground">{new Date(log.createdAt).toLocaleString()}</p>
              </div>
            ))}
            {auditLogs.length === 0 && (
              <div className="rounded-md border border-dashed border-border p-4 text-sm text-muted-foreground">
                Veprimet e rëndësishme të administratorit do të shfaqen këtu.
              </div>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
}

function Toggle({ label, checked, onChange }: { label: string; checked: boolean; onChange: (checked: boolean) => void }) {
  return (
    <label className="flex items-center justify-between rounded-md border border-border px-3 py-3 text-sm font-medium">
      <span>{label}</span>
      <input type="checkbox" className="h-5 w-5 accent-primary" checked={checked} onChange={(event) => onChange(event.target.checked)} />
    </label>
  );
}

function toTimeInput(value: string) {
  return value.slice(0, 5);
}

function normalizeTime(value: string) {
  return value.length === 5 ? `${value}:00` : value;
}

function isOvernightDefaultTime(openTime: string, closeTime: string) {
  const open = toTimeInput(openTime);
  const close = toTimeInput(closeTime);
  return Boolean(open && close && close <= open);
}

function optionalNumber(value: string) {
  return value.trim() === "" ? null : Number(value);
}
