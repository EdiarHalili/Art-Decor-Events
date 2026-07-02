import { useEffect, useState } from "react";
import { Bell, CalendarClock, LogOut, MapPin, Wifi, WifiOff } from "lucide-react";
import { BrandMark } from "../../components/BrandMark";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { checkIn, checkOut, getEmployeeToday, type AuthResponse, type EmployeeToday } from "../../lib/api";
import { queueAttendanceAction } from "../../lib/offlineQueue";

type EmployeeHomeProps = {
  session: AuthResponse;
  onLogout: () => void;
};

export function EmployeeHome({ session, onLogout }: EmployeeHomeProps) {
  const [today, setToday] = useState<EmployeeToday | null>(null);
  const [online, setOnline] = useState(navigator.onLine);
  const [message, setMessage] = useState("");
  const [actionLoading, setActionLoading] = useState<"CHECK_IN" | "CHECK_OUT" | null>(null);

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

  async function captureLocation(): Promise<{ latitude?: number; longitude?: number }> {
    if (!("geolocation" in navigator)) {
      return {};
    }

    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (position) =>
          resolve({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          }),
        () => resolve({}),
        { enableHighAccuracy: true, timeout: 5000, maximumAge: 60000 },
      );
    });
  }

  function deviceMetadata() {
    return {
      userAgent: navigator.userAgent,
      platform: navigator.platform,
      language: navigator.language,
    };
  }

  async function submitAttendance(type: "CHECK_IN" | "CHECK_OUT") {
    if (!today?.scheduleId || !session.employeeId) {
      setMessage("No active schedule is available for attendance.");
      return;
    }

    setActionLoading(type);
    setMessage("");
    const location = await captureLocation();
    const payload = { scheduleId: today.scheduleId, ...location, device: deviceMetadata() };

    if (!navigator.onLine) {
      queueAttendanceAction({
        id: crypto.randomUUID(),
        type,
        employeeId: session.employeeId,
        scheduleId: today.scheduleId,
        capturedAt: new Date().toISOString(),
        ...location,
        device: deviceMetadata(),
      });
      setMessage("Attendance saved offline and will sync when internet returns.");
      setActionLoading(null);
      return;
    }

    try {
      const response = type === "CHECK_IN" ? await checkIn(session.accessToken, payload) : await checkOut(session.accessToken, payload);
      setToday({
        ...today,
        status: response.status === "CHECKED_OUT" ? "Checked out." : response.status === "LATE" ? "Checked in late." : "Checked in.",
        checkInOpen: false,
        checkOutAvailable: response.status !== "CHECKED_OUT",
      });
      setMessage(type === "CHECK_IN" ? "Check-in recorded." : "Check-out recorded.");
    } catch {
      setMessage(type === "CHECK_IN" ? "Check-in could not be recorded." : "Check-out could not be recorded.");
    } finally {
      setActionLoading(null);
    }
  }

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
            <Button
              className="h-14 text-base"
              disabled={!today?.checkInOpen || actionLoading !== null}
              onClick={() => void submitAttendance("CHECK_IN")}
            >
              {actionLoading === "CHECK_IN" ? "Recording..." : "Check In"}
            </Button>
            <Button
              className="h-14 text-base"
              variant="secondary"
              disabled={!today?.checkOutAvailable || actionLoading !== null}
              onClick={() => void submitAttendance("CHECK_OUT")}
            >
              {actionLoading === "CHECK_OUT" ? "Recording..." : "Check Out"}
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
