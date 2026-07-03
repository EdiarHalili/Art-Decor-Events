import { FormEvent, useEffect, useState } from "react";
import { Bell, Landmark, Megaphone, Palette, Save, ShieldCheck, Smartphone } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import {
  getPayrollPreparation,
  listAnnouncements,
  listAuditLogs,
  publishAnnouncement,
  subscribePush,
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
  notificationsEnabled: true,
  sessionTimeoutMinutes: 60,
  updatedAt: null,
};

export function SettingsPage({ accessToken, settings, onSettingsUpdated }: SettingsPageProps) {
  const [form, setForm] = useState<AppSettings>(settings ?? fallbackSettings);
  const [announcements, setAnnouncements] = useState<Announcement[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [payroll, setPayroll] = useState<PayrollPreparation | null>(null);
  const [announcement, setAnnouncement] = useState({ title: "", body: "" });
  const [message, setMessage] = useState("");
  const [pushMessage, setPushMessage] = useState("");
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

  async function enablePushNotifications() {
    setPushMessage("");
    try {
      const publicKey = import.meta.env.VITE_VAPID_PUBLIC_KEY as string | undefined;
      if (!publicKey) {
        setPushMessage("Push delivery is ready, but VITE_VAPID_PUBLIC_KEY must be configured before subscribing this device.");
        return;
      }
      if (!("serviceWorker" in navigator) || !("PushManager" in window)) {
        setPushMessage("This browser does not support web push notifications.");
        return;
      }
      const permission = await Notification.requestPermission();
      if (permission !== "granted") {
        setPushMessage("Notification permission was not granted for this device.");
        return;
      }
      const registration = await navigator.serviceWorker.ready;
      const subscription = await registration.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(publicKey),
      });
      const json = subscription.toJSON();
      if (!json.endpoint || !json.keys?.p256dh || !json.keys?.auth) {
        setPushMessage("The browser did not return a complete push subscription.");
        return;
      }
      await subscribePush(accessToken, {
        endpoint: json.endpoint,
        p256dhKey: json.keys.p256dh,
        authKey: json.keys.auth,
      });
      setPushMessage("This device is subscribed for push notifications.");
    } catch (error) {
      setPushMessage(error instanceof Error ? error.message : "Push notifications could not be enabled.");
    }
  }

  async function publish(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    try {
      const created = await publishAnnouncement(accessToken, announcement);
      setAnnouncements((current) => [created, ...current]);
      setAnnouncement({ title: "", body: "" });
      setMessage("Announcement published.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Announcement could not be published.");
    }
  }

  return (
    <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_380px]">
      <Card className="p-5">
        <div className="flex items-center gap-3">
          <div className="rounded-md bg-primary/15 p-2 text-primary">
            <Palette size={22} />
          </div>
          <div>
            <h2 className="font-semibold">Company settings</h2>
            <p className="text-sm text-muted-foreground">Branding, attendance defaults, GPS, notifications, and session control.</p>
          </div>
        </div>

        {message && <p className="mt-4 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}

        <form className="mt-5 grid gap-4 md:grid-cols-2" onSubmit={submit}>
          <label className="space-y-1 text-sm font-medium md:col-span-2">
            <span>Company name</span>
            <Input value={form.companyName} onChange={(event) => setForm({ ...form, companyName: event.target.value })} required />
          </label>
          <label className="space-y-1 text-sm font-medium md:col-span-2">
            <span>Logo URL</span>
            <Input value={form.logoUrl ?? ""} onChange={(event) => setForm({ ...form, logoUrl: event.target.value })} placeholder="https://..." />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Primary color</span>
            <Input type="color" value={form.primaryColor} onChange={(event) => setForm({ ...form, primaryColor: event.target.value })} />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Accent color</span>
            <Input type="color" value={form.accentColor} onChange={(event) => setForm({ ...form, accentColor: event.target.value })} />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Timezone</span>
            <Input value={form.timezone} onChange={(event) => setForm({ ...form, timezone: event.target.value })} required />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Session timeout minutes</span>
            <Input type="number" min="5" max="1440" value={form.sessionTimeoutMinutes} onChange={(event) => setForm({ ...form, sessionTimeoutMinutes: Number(event.target.value) })} />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Default check-in opens</span>
            <Input type="time" value={toTimeInput(form.defaultCheckInOpenTime)} onChange={(event) => setForm({ ...form, defaultCheckInOpenTime: event.target.value })} />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Default check-in closes</span>
            <Input type="time" value={toTimeInput(form.defaultCheckInCloseTime)} onChange={(event) => setForm({ ...form, defaultCheckInCloseTime: event.target.value })} />
          </label>
          <label className="space-y-1 text-sm font-medium">
            <span>Allowed late minutes</span>
            <Input type="number" min="0" max="240" value={form.allowedLateMinutes} onChange={(event) => setForm({ ...form, allowedLateMinutes: Number(event.target.value) })} />
          </label>
          <div className="grid gap-3 sm:grid-cols-2 md:col-span-2">
            <Toggle label="GPS capture" checked={form.gpsEnabled} onChange={(checked) => setForm({ ...form, gpsEnabled: checked })} />
            <Toggle label="Notifications" checked={form.notificationsEnabled} onChange={(checked) => setForm({ ...form, notificationsEnabled: checked })} />
          </div>
          <Button className="md:col-span-2" disabled={saving}>
            <Save size={18} />
            {saving ? "Saving..." : "Save settings"}
          </Button>
        </form>
      </Card>

      <div className="grid gap-5">
        <Card className="p-5">
          <div className="flex items-center gap-3">
            <Smartphone className="text-primary" size={21} />
            <h2 className="font-semibold">Push device</h2>
          </div>
          <p className="mt-3 text-sm text-muted-foreground">
            Web Push subscription storage is enabled. Delivery requires production VAPID keys and a push sender.
          </p>
          {pushMessage && <p className="mt-3 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{pushMessage}</p>}
          <Button type="button" variant="secondary" className="mt-4 w-full" onClick={() => void enablePushNotifications()}>
            <Bell size={18} />
            Enable on this device
          </Button>
        </Card>

        <Card className="p-5">
          <div className="flex items-center gap-3">
            <Megaphone className="text-primary" size={21} />
            <h2 className="font-semibold">Announcement</h2>
          </div>
          <form className="mt-4 space-y-3" onSubmit={publish}>
            <Input placeholder="Title" value={announcement.title} onChange={(event) => setAnnouncement({ ...announcement, title: event.target.value })} required />
            <textarea className="min-h-24 w-full rounded-md border border-border bg-background px-3 py-3 text-sm outline-none focus:border-primary focus:ring-4 focus:ring-primary/15" placeholder="Message" value={announcement.body} onChange={(event) => setAnnouncement({ ...announcement, body: event.target.value })} required />
            <Button className="w-full">
              <Bell size={18} />
              Publish
            </Button>
          </form>
          <div className="mt-4 space-y-2">
            {announcements.slice(0, 3).map((item) => (
              <div key={item.id} className="rounded-md bg-muted p-3 text-sm">
                <p className="font-medium">{item.title}</p>
                <p className="mt-1 text-muted-foreground">{item.body}</p>
              </div>
            ))}
          </div>
        </Card>

        <Card className="p-5">
          <div className="flex items-center gap-3">
            <Landmark className="text-primary" size={21} />
            <h2 className="font-semibold">Payroll preparation</h2>
          </div>
          <p className="mt-3 text-sm text-muted-foreground">
            {payroll ? `${payroll.employees.length} employees are ready for monthly payroll preparation.` : "Loading payroll preparation data..."}
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
                Important administrator actions will appear here.
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

function urlBase64ToUint8Array(value: string) {
  const padding = "=".repeat((4 - (value.length % 4)) % 4);
  const base64 = (value + padding).replace(/-/g, "+").replace(/_/g, "/");
  const raw = window.atob(base64);
  const output = new Uint8Array(raw.length);
  for (let index = 0; index < raw.length; index += 1) {
    output[index] = raw.charCodeAt(index);
  }
  return output;
}
