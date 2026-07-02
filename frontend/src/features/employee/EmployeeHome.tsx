import { useEffect, useState } from "react";
import { Bell, CalendarClock, LogOut, MapPin, Wifi, WifiOff } from "lucide-react";
import { BrandMark } from "../../components/BrandMark";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { getEmployeeToday, type AuthResponse, type EmployeeToday } from "../../lib/api";

type EmployeeHomeProps = {
  session: AuthResponse;
  onLogout: () => void;
};

export function EmployeeHome({ session, onLogout }: EmployeeHomeProps) {
  const [today, setToday] = useState<EmployeeToday | null>(null);
  const [online, setOnline] = useState(navigator.onLine);
  const [message, setMessage] = useState("");

  useEffect(() => {
    function syncOnlineState() {
      setOnline(navigator.onLine);
    }

    window.addEventListener("online", syncOnlineState);
    window.addEventListener("offline", syncOnlineState);

    getEmployeeToday(session.accessToken)
      .then(setToday)
      .catch(() => {
        setMessage("Today's assignment could not be loaded. Attendance actions will be queued if needed.");
      });

    return () => {
      window.removeEventListener("online", syncOnlineState);
      window.removeEventListener("offline", syncOnlineState);
    };
  }, [session.accessToken]);

  const employeeName = today?.employeeName ?? session.fullName;

  return (
    <main className="brand-surface min-h-screen px-4 py-5">
      <div className="mx-auto flex max-w-md flex-col gap-5">
        <header className="flex items-center justify-between">
          <BrandMark compact />
          <div className="flex items-center gap-2 rounded-full border border-border bg-card px-3 py-2 text-xs text-muted-foreground">
            {online ? <Wifi size={15} /> : <WifiOff size={15} />}
            {online ? "Online" : "Offline"}
          </div>
        </header>

        <section>
          <p className="text-sm text-muted-foreground">Welcome</p>
          <h1 className="mt-1 text-2xl font-semibold">{employeeName}</h1>
        </section>

        <Card className="p-5">
          <div className="flex items-start gap-3">
            <div className="rounded-md bg-primary/15 p-2 text-primary">
              <CalendarClock size={22} />
            </div>
            <div>
              <h2 className="text-lg font-semibold">Today's assignment</h2>
              <p className="mt-1 text-sm text-muted-foreground">
                {today?.assignment ?? "Loading today's assignment..."}
              </p>
            </div>
          </div>

          <div className="mt-5 rounded-md bg-muted p-4 text-sm">
            <div className="flex items-center gap-2 font-medium">
              <MapPin size={17} />
              Status
            </div>
            <p className="mt-2 text-muted-foreground">{today?.status ?? "Checking current status..."}</p>
          </div>

          {message && <p className="mt-4 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}

          <div className="mt-5 grid gap-3">
            <Button className="h-14 text-base" disabled={!today?.checkInOpen}>
              Check In
            </Button>
            <Button className="h-14 text-base" variant="secondary" disabled={!today?.checkOutAvailable}>
              Check Out
            </Button>
          </div>
        </Card>

        <Card className="p-5">
          <div className="flex items-center gap-3">
            <Bell className="text-primary" size={21} />
            <h2 className="font-semibold">Announcements</h2>
          </div>
          <div className="mt-3 space-y-2 text-sm text-muted-foreground">
            {(today?.announcements ?? ["Loading announcements..."]).map((announcement) => (
              <p key={announcement}>{announcement}</p>
            ))}
          </div>
        </Card>

        <Button variant="ghost" onClick={onLogout}>
          <LogOut size={18} />
          Sign out
        </Button>
      </div>
    </main>
  );
}
