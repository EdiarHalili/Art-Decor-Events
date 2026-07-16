import { FormEvent, useEffect, useState } from "react";
import { Edit3, Megaphone, Trash2 } from "lucide-react";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import {
  deleteAnnouncement,
  listAnnouncements,
  publishAnnouncement,
  updateAnnouncement,
  type Announcement,
} from "../../lib/api";

type AnnouncementsPageProps = {
  accessToken: string;
};

export function AnnouncementsPage({ accessToken }: AnnouncementsPageProps) {
  const [announcements, setAnnouncements] = useState<Announcement[]>([]);
  const [form, setForm] = useState({ title: "", body: "" });
  const [editingId, setEditingId] = useState<string | null>(null);
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    void loadAnnouncements();
  }, [accessToken]);

  async function loadAnnouncements() {
    setLoading(true);
    setMessage("");
    try {
      setAnnouncements(await listAnnouncements(accessToken));
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Njoftimet nuk mund të ngarkoheshin.");
    } finally {
      setLoading(false);
    }
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setMessage("");
    try {
      if (editingId) {
        const updated = await updateAnnouncement(accessToken, editingId, form);
        setAnnouncements((current) => current.map((item) => (item.id === updated.id ? updated : item)));
        setMessage("Njoftimi u përditësua.");
      } else {
        const created = await publishAnnouncement(accessToken, form);
        setAnnouncements((current) => [created, ...current]);
        setMessage("Njoftimi u publikua.");
      }
      setEditingId(null);
      setForm({ title: "", body: "" });
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Njoftimi nuk mund të ruhej.");
    } finally {
      setSaving(false);
    }
  }

  function editAnnouncement(item: Announcement) {
    setEditingId(item.id);
    setForm({ title: item.title, body: item.body });
    setMessage("");
  }

  async function removeAnnouncement(announcementId: string) {
    setMessage("");
    try {
      await deleteAnnouncement(accessToken, announcementId);
      setAnnouncements((current) => current.filter((item) => item.id !== announcementId));
      if (editingId === announcementId) {
        setEditingId(null);
        setForm({ title: "", body: "" });
      }
      setMessage("Njoftimi u fshi.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Njoftimi nuk mund të fshihej.");
    }
  }

  return (
    <div className="grid min-w-0 gap-4 sm:gap-5 xl:grid-cols-[minmax(0,420px)_minmax(0,1fr)]">
      <Card className="p-4 sm:p-5">
        <div className="flex items-center gap-3">
          <div className="rounded-md bg-primary/15 p-2 text-primary">
            <Megaphone size={21} />
          </div>
          <div>
            <h2 className="font-semibold">Njoftimet</h2>
            <p className="text-sm text-muted-foreground">Mesazhe të thjeshta për punëtorët.</p>
          </div>
        </div>

        {message && <p className="mt-4 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}

        <form className="mt-5 space-y-3" onSubmit={submit}>
          <Input
            placeholder="Titulli"
            value={form.title}
            onChange={(event) => setForm({ ...form, title: event.target.value })}
            required
          />
          <textarea
            className="min-h-28 w-full rounded-md border border-border bg-background px-3 py-3 text-sm outline-none focus:border-primary focus:ring-4 focus:ring-primary/15"
            placeholder="Mesazhi"
            value={form.body}
            onChange={(event) => setForm({ ...form, body: event.target.value })}
            required
          />
          <div className="grid min-w-0 gap-2 sm:grid-cols-2">
            <Button disabled={saving}>{saving ? "Duke ruajtur..." : editingId ? "Ruaj ndryshimet" : "Publiko"}</Button>
            {editingId && (
              <Button
                type="button"
                variant="secondary"
                disabled={saving}
                onClick={() => {
                  setEditingId(null);
                  setForm({ title: "", body: "" });
                }}
              >
                Anulo
              </Button>
            )}
          </div>
        </form>
      </Card>

      <Card className="p-4 sm:p-5">
        <div className="flex items-center justify-between gap-3">
          <h2 className="font-semibold">Njoftimet e publikuara</h2>
          <span className="rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{announcements.length}</span>
        </div>
        <div className="mt-5 space-y-3">
          {loading && <p className="rounded-md border border-border p-4 text-sm text-muted-foreground">Po ngarkohen njoftimet...</p>}
          {!loading && announcements.length === 0 && (
            <p className="rounded-md border border-dashed border-border p-6 text-center text-sm text-muted-foreground">
              Nuk ka njoftime të publikuara.
            </p>
          )}
          {announcements.map((item) => (
            <div key={item.id} className="rounded-lg border border-border p-3 sm:p-4">
              <div className="flex min-w-0 flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                <div className="min-w-0">
                  <p className="break-words font-semibold">{item.title}</p>
                  <p className="mt-1 whitespace-pre-wrap break-words text-sm text-muted-foreground">{item.body}</p>
                  <p className="mt-2 text-xs text-muted-foreground">{formatDateTime(item.createdAt)}</p>
                </div>
                <div className="grid shrink-0 grid-cols-2 gap-2 min-[420px]:flex">
                  <Button type="button" variant="secondary" className="min-h-9 px-3" onClick={() => editAnnouncement(item)}>
                    <Edit3 size={16} />
                    Edito
                  </Button>
                  <Button type="button" variant="ghost" className="min-h-9 px-3 text-destructive" onClick={() => void removeAnnouncement(item.id)}>
                    <Trash2 size={16} />
                    Fshi
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

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
