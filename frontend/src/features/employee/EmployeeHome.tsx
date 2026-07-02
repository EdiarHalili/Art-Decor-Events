import { useEffect, useState } from "react";
import { Bell, CalendarClock, LogOut, MapPin, Wifi, WifiOff } from "lucide-react";
import { BrandMark } from "../../components/BrandMark";
import { PwaInstallPrompt } from "../../components/PwaInstallPrompt";
import { ThemeToggle } from "../../components/ThemeToggle";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { checkIn, checkOut, getEmployeeToday, type AuthResponse, type EmployeeToday } from "../../lib/api";
import { getQueuedAttendanceActions, queueAttendanceAction, syncQueuedAttendanceActions } from "../../lib/offlineQueue";

type EmployeeHomeProps = {
  session: AuthResponse;
  onLogout: () => void;
};

export function EmployeeHome({ session, onLogout }: EmployeeHomeProps) {
  const [today, setToday] = useState<EmployeeToday | null>(() => getCachedToday(session.employeeId));
  const [online, setOnline] = useState(navigator.onLine);
  const [message, setMessage] = useState("");
  const [actionLoading, setActionLoading] = useState<"CHECK_IN" | "CHECK_OUT" | null>(null);
  const [queuedCount, setQueuedCount] = useState(getQueuedAttendanceActions().length);

  useEffect(() => {
    async function syncOnlineState() {
      const nextOnline = navigator.onLine;
      setOnline(nextOnline);
      setQueuedCount(getQueuedAttendanceActions().length);
      if (nextOnline) {
        await syncPendingActions();
      }
    }

    window.addEventListener("online", syncOnlineState);
    window.addEventListener("offline", syncOnlineState);

    getEmployeeToday(session.accessToken)
      .then((response) => {
        setToday(response);
        cacheToday(session.employeeId, response);
      })
      .catch(() => {
        setMessage("Today's assignment could not be loaded. Attendance actions will be queued if needed.");
      });

    if (navigator.onLine) {
      void syncPendingActions();
    }

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
      setMessage("No active daily check-in window is available for attendance.");
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
      setQueuedCount(getQueuedAttendanceActions().length);
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
      cacheToday(session.employeeId, {
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

  async function syncPendingActions() {
    const before = getQueuedAttendanceActions().length;
    if (before === 0) {
      setQueuedCount(0);
      return;
    }

    const result = await syncQueuedAttendanceActions(session.accessToken);
    setQueuedCount(result.remaining);
    if (result.synced > 0) {
      setMessage(result.remaining === 0 ? "Offline attendance synced." : "Some offline attendance actions still need syncing.");
      getEmployeeToday(session.accessToken)
        .then((response) => {
          setToday(response);
          cacheToday(session.employeeId, response);
        })
        .catch(() => undefined);
    }
  }

  return (
    <main className="brand-surface min-h-screen px-4 py-5 pb-[calc(1.25rem+env(safe-area-inset-bottom))] pt-[calc(1.25rem+env(safe-area-inset-top))]">
      <div className="mx-auto flex max-w-md flex-col gap-5">
        <header className="sticky top-0 z-10 -mx-4 flex items-center justify-between bg-background/80 px-4 py-2 backdrop-blur sm:static sm:mx-0 sm:bg-transparent sm:px-0 sm:py-0">
          <BrandMark compact />
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-2 rounded-full border border-border bg-card px-3 py-2 text-xs text-muted-foreground">
              {online ? <Wifi size={15} /> : <WifiOff size={15} />}
              {online ? "Online" : "Offline"}
            </div>
            <ThemeToggle />
          </div>
        </header>

        <section>
          <p className="text-sm text-muted-foreground">Welcome</p>
          <h1 className="mt-1 text-2xl font-semibold">{employeeName}</h1>
          {queuedCount > 0 && (
            <p className="mt-2 rounded-md bg-primary/15 px-3 py-2 text-sm text-primary">
              {queuedCount} offline attendance action{queuedCount === 1 ? "" : "s"} pending sync.
            </p>
          )}
          <div className="mt-3">
            <PwaInstallPrompt />
          </div>
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

          <div className="mt-5 grid gap-3 sm:grid-cols-2">
            <Button
              className="h-14 text-base sm:h-16"
              disabled={!today?.checkInOpen || actionLoading !== null}
              onClick={() => void submitAttendance("CHECK_IN")}
            >
              {actionLoading === "CHECK_IN" ? "Recording..." : "Check In"}
            </Button>
            <Button
              className="h-14 text-base sm:h-16"
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

function cacheToday(employeeId: string | null, today: EmployeeToday) {
  if (!employeeId) {
    return;
  }
  localStorage.setItem(`artdecor.today.${employeeId}`, JSON.stringify(today));
}

function getCachedToday(employeeId: string | null) {
  if (!employeeId) {
    return null;
  }

  const raw = localStorage.getItem(`artdecor.today.${employeeId}`);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as EmployeeToday;
  } catch {
    return null;
  }
}
