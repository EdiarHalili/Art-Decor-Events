import { useEffect, useState } from "react";
import { Bell, CalendarClock, LogOut, MapPin, Wifi, WifiOff } from "lucide-react";
import { BrandMark } from "../../components/BrandMark";
import { PwaInstallPrompt } from "../../components/PwaInstallPrompt";
import { ThemeToggle } from "../../components/ThemeToggle";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { checkIn, checkOut, debugGpsLog, getEmployeeToday, recordLiveLocation, type AppSettings, type AuthResponse, type EmployeeToday } from "../../lib/api";
import { getQueuedAttendanceActions, queueAttendanceAction, syncQueuedAttendanceActions } from "../../lib/offlineQueue";

type EmployeeHomeProps = {
  session: AuthResponse;
  settings: AppSettings | null;
  onLogout: () => void;
};

export function EmployeeHome({ session, settings, onLogout }: EmployeeHomeProps) {
  const [today, setToday] = useState<EmployeeToday | null>(() => getCachedToday(session.employeeId));
  const [online, setOnline] = useState(navigator.onLine);
  const [message, setMessage] = useState("");
  const [actionLoading, setActionLoading] = useState<"CHECK_IN" | "CHECK_OUT" | null>(null);
  const [queuedCount, setQueuedCount] = useState(getQueuedAttendanceActions().length);
  const [todayFresh, setTodayFresh] = useState(false);
  const [now, setNow] = useState(() => Date.now());
  const [liveTrackingActive, setLiveTrackingActive] = useState(false);
  const [checkinConfirmOpen, setCheckinConfirmOpen] = useState(false);
  const [checkoutConfirmOpen, setCheckoutConfirmOpen] = useState(false);
  const [attendanceSuccess, setAttendanceSuccess] = useState("");

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
        setTodayFresh(true);
        setNow(response.serverNow ? new Date(response.serverNow).getTime() : Date.now());
        cacheToday(session.employeeId, response);
      })
      .catch(() => {
        setTodayFresh(false);
        setMessage("Orari i sotëm nuk mund të ngarkohej. Veprimet do të ruhen pa internet nëse është e nevojshme.");
      });

    if (navigator.onLine) {
      void syncPendingActions();
    }

    return () => {
      window.removeEventListener("online", syncOnlineState);
      window.removeEventListener("offline", syncOnlineState);
    };
  }, [session.accessToken]);

  useEffect(() => {
    const timer = window.setInterval(() => setNow((current) => current + 1000), 1000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!attendanceSuccess) {
      return;
    }
    const timer = window.setTimeout(() => setAttendanceSuccess(""), 4500);
    return () => window.clearTimeout(timer);
  }, [attendanceSuccess]);

  useEffect(() => {
    const trackingEnabled = Boolean(settings?.liveLocationTrackingEnabled && today?.checkOutAvailable && online);
    if (!trackingEnabled) {
      setLiveTrackingActive(false);
      return;
    }

    let cancelled = false;
    const intervalMinutes = Math.max(5, settings?.liveLocationIntervalMinutes ?? 10);

    async function sendLiveLocation() {
      const location = await captureLiveLocation();
      if (cancelled || !location) {
        return;
      }
      try {
        await recordLiveLocation(session.accessToken, {
          ...location,
          capturedAt: new Date().toISOString(),
          device: deviceMetadata(),
        });
        if (!cancelled) {
          setLiveTrackingActive(true);
        }
      } catch {
        if (!cancelled) {
          setLiveTrackingActive(false);
        }
      }
    }

    void sendLiveLocation();
    const timer = window.setInterval(() => void sendLiveLocation(), intervalMinutes * 60 * 1000);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
      setLiveTrackingActive(false);
    };
  }, [online, session.accessToken, settings?.liveLocationIntervalMinutes, settings?.liveLocationTrackingEnabled, today?.checkOutAvailable]);

  const employeeName = today?.employeeName ?? session.fullName;

  async function captureLocation(): Promise<{ latitude?: number; longitude?: number }> {
    if (!("geolocation" in navigator)) {
      return {};
    }

    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const location = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          };
          debugGpsLog("browser attendance geolocation", {
            latitude: location.latitude,
            longitude: location.longitude,
            accuracyMeters: position.coords.accuracy,
          });
          resolve(location);
        },
        () => resolve({}),
        { enableHighAccuracy: true, timeout: 5000, maximumAge: 60000 },
      );
    });
  }

  async function captureLiveLocation(): Promise<{ latitude: number; longitude: number; accuracyMeters?: number } | null> {
    if (!("geolocation" in navigator)) {
      return null;
    }

    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const location = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracyMeters: position.coords.accuracy,
          };
          debugGpsLog("browser live geolocation", location);
          resolve(location);
        },
        () => resolve(null),
        { enableHighAccuracy: false, timeout: 10000, maximumAge: 5 * 60 * 1000 },
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
      setMessage("Regjistrimi i orarit nuk është i disponueshëm për momentin.");
      return;
    }
    if (navigator.onLine && !todayFresh) {
      setMessage("Po përditësohet gjendja e sotme. Ju lutemi provoni përsëri pas pak.");
      return;
    }

    setActionLoading(type);
    setMessage("");
    setAttendanceSuccess("");
    const location = settings?.gpsEnabled === false ? {} : await captureLocation();
    const payload = { scheduleId: today.scheduleId, ...location, device: deviceMetadata() };
    debugGpsLog(`${type.toLowerCase().replace("_", "-")} prepared payload`, {
      scheduleId: payload.scheduleId,
      latitude: payload.latitude,
      longitude: payload.longitude,
    });

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
      if (type === "CHECK_OUT") {
        setCheckoutConfirmOpen(false);
        setMessage("Dalja u ruajt pa internet dhe do të sinkronizohet kur lidhja të kthehet.");
      } else {
        setCheckinConfirmOpen(false);
        setMessage("Hyrja u ruajt pa internet dhe do të sinkronizohet kur lidhja të kthehet.");
      }
      setActionLoading(null);
      return;
    }

    try {
      const response = type === "CHECK_IN" ? await checkIn(session.accessToken, payload) : await checkOut(session.accessToken, payload);
      if (type === "CHECK_OUT") {
        setCheckoutConfirmOpen(false);
      } else {
        setCheckinConfirmOpen(false);
      }
      setToday({
        ...today,
        status: response.status === "CHECKED_OUT" ? "Dalja u regjistrua." : response.status === "LATE" ? "Hyrja u regjistrua me vonesë." : "Hyrja u regjistrua.",
        checkInOpen: false,
        checkOutAvailable: response.status !== "CHECKED_OUT",
      });
      cacheToday(session.employeeId, {
        ...today,
        status: response.status === "CHECKED_OUT" ? "Dalja u regjistrua." : response.status === "LATE" ? "Hyrja u regjistrua me vonesë." : "Hyrja u regjistrua.",
        checkInOpen: false,
        checkOutAvailable: response.status !== "CHECKED_OUT",
      });
      if (type === "CHECK_OUT") {
        setAttendanceSuccess(checkoutSuccessMessage(response.checkedOutAt, response.workedMinutes));
        setMessage("");
      } else {
        setAttendanceSuccess(checkinSuccessMessage(response.checkedInAt));
        setMessage("");
      }
    } catch (error) {
      setMessage(type === "CHECK_OUT" ? checkoutErrorMessage(error) : error instanceof Error ? error.message : "Hyrja nuk mund të regjistrohej.");
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
      setMessage(result.remaining === 0 ? "Regjistrimet pa internet u sinkronizuan." : "Disa regjistrime pa internet ende presin sinkronizimin.");
      getEmployeeToday(session.accessToken)
        .then((response) => {
          setToday(response);
          setTodayFresh(true);
          setNow(response.serverNow ? new Date(response.serverNow).getTime() : Date.now());
          cacheToday(session.employeeId, response);
        })
        .catch(() => undefined);
    }
  }

  return (
    <main className="brand-surface min-h-screen px-4 py-5 pb-[calc(1.25rem+env(safe-area-inset-bottom))] pt-[calc(1.25rem+env(safe-area-inset-top))]">
      <div className="mx-auto flex max-w-md flex-col gap-5">
        <header className="sticky top-0 z-10 -mx-4 flex items-center justify-between bg-background/80 px-4 py-2 backdrop-blur sm:static sm:mx-0 sm:bg-transparent sm:px-0 sm:py-0">
          <BrandMark compact logoUrl={settings?.logoUrl} companyName={settings?.companyName} />
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-2 rounded-full border border-border bg-card px-3 py-2 text-xs text-muted-foreground">
              {online ? <Wifi size={15} /> : <WifiOff size={15} />}
              {online ? "Online" : "Pa internet"}
            </div>
            <ThemeToggle />
          </div>
        </header>

        <section>
          <p className="text-sm text-muted-foreground">Mirë se vini</p>
          <h1 className="mt-1 text-2xl font-semibold">{employeeName}</h1>
          {queuedCount > 0 && (
            <p className="mt-2 rounded-md bg-primary/15 px-3 py-2 text-sm text-primary">
              {queuedCount} regjistrim{queuedCount === 1 ? "" : "e"} pa internet presin sinkronizimin.
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
              <h2 className="text-lg font-semibold">Orari i sotëm</h2>
              <p className="mt-1 text-sm text-muted-foreground">
                {today?.assignment ?? "Po ngarkohet orari i sotëm..."}
              </p>
            </div>
          </div>

          <div className="mt-5 rounded-md bg-muted p-4 text-sm">
            <div className="flex items-center gap-2 font-medium">
              <MapPin size={17} />
              Gjendja
            </div>
            <p className="mt-2 text-muted-foreground">{today?.status ?? "Po kontrollohet gjendja aktuale..."}</p>
            {today && <p className="mt-2 font-medium text-primary">{countdownText(today, now)}</p>}
            <p className="mt-2 text-xs text-muted-foreground">
              GPS është {settings?.gpsEnabled === false ? "i çaktivizuar për kompaninë." : "opsional dhe kërkohet vetëm kur regjistrohet hyrja ose dalja."}
            </p>
            {today?.checkOutAvailable && settings?.liveLocationTrackingEnabled && (
              <p className="mt-2 text-xs font-medium text-primary">
                {liveTrackingActive
                  ? `Gjurmimi live i lokacionit është aktiv çdo ${Math.max(5, settings.liveLocationIntervalMinutes)} minuta.`
                  : "Gjurmimi live i lokacionit funksionon vetëm gjatë orarit aktiv."}
              </p>
            )}
          </div>

          {message && <p className="mt-4 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{message}</p>}
          {attendanceSuccess && (
            <div className="mt-4 whitespace-pre-line rounded-md border border-primary/20 bg-primary/10 px-3 py-3 text-sm font-medium text-primary">
              {attendanceSuccess}
            </div>
          )}

          <div className="mt-5 grid gap-3">
            <Button
              className="h-14 text-base sm:h-16"
              disabled={!today?.checkInOpen || actionLoading !== null || (online && !todayFresh)}
              onClick={() => setCheckinConfirmOpen(true)}
            >
              {actionLoading === "CHECK_IN" ? "Duke regjistruar..." : "Hyrje"}
            </Button>
            <Button
              className="h-14 text-base sm:h-16"
              variant="secondary"
              disabled={!today?.checkOutAvailable || actionLoading !== null || (online && !todayFresh)}
              onClick={() => setCheckoutConfirmOpen(true)}
            >
              {actionLoading === "CHECK_OUT" ? "Duke regjistruar..." : "Dalje"}
            </Button>
          </div>
        </Card>

        {checkinConfirmOpen && (
          <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/45 px-4 py-4 sm:items-center" role="dialog" aria-modal="true" aria-labelledby="checkin-confirm-title">
            <div className="w-full max-w-md rounded-lg border border-border bg-card p-5 shadow-xl">
              <h2 id="checkin-confirm-title" className="text-lg font-semibold">Konfirmo Hyrjen</h2>
              <p className="mt-3 text-sm text-muted-foreground">
                A jeni i sigurt që dëshironi ta filloni orarin tuaj të punës?
              </p>
              <div className="mt-5 grid gap-2 sm:grid-cols-2">
                <Button
                  type="button"
                  variant="secondary"
                  disabled={actionLoading === "CHECK_IN"}
                  onClick={() => setCheckinConfirmOpen(false)}
                >
                  Anulo
                </Button>
                <Button
                  type="button"
                  disabled={actionLoading === "CHECK_IN"}
                  onClick={() => void submitAttendance("CHECK_IN")}
                >
                  {actionLoading === "CHECK_IN" ? "Duke regjistruar..." : "Po, filloje"}
                </Button>
              </div>
            </div>
          </div>
        )}

        {checkoutConfirmOpen && (
          <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/45 px-4 py-4 sm:items-center" role="dialog" aria-modal="true" aria-labelledby="checkout-confirm-title">
            <div className="w-full max-w-md rounded-lg border border-border bg-card p-5 shadow-xl">
              <h2 id="checkout-confirm-title" className="text-lg font-semibold">Konfirmo Daljen</h2>
              <div className="mt-3 space-y-3 text-sm text-muted-foreground">
                <p>A jeni i sigurt që dëshironi të bëni daljen?</p>
                <p>
                  Pasi të kryhet dalja, nuk do të mund të bëni hyrje (Check In) përsëri, përveç nëse kjo lejohet nga rregullat e attendance.
                </p>
              </div>
              <div className="mt-5 grid gap-2 sm:grid-cols-2">
                <Button
                  type="button"
                  variant="secondary"
                  disabled={actionLoading === "CHECK_OUT"}
                  onClick={() => setCheckoutConfirmOpen(false)}
                >
                  Anulo
                </Button>
                <Button
                  type="button"
                  disabled={actionLoading === "CHECK_OUT"}
                  onClick={() => void submitAttendance("CHECK_OUT")}
                >
                  {actionLoading === "CHECK_OUT" ? "Duke regjistruar..." : "Po, Bëj Daljen"}
                </Button>
              </div>
            </div>
          </div>
        )}

        <Card className="p-5">
          <div className="flex items-center gap-3">
            <Bell className="text-primary" size={21} />
            <h2 className="font-semibold">Njoftime</h2>
          </div>
          <div className="mt-3 space-y-2 text-sm text-muted-foreground">
            {(today?.announcements ?? ["Po ngarkohen njoftimet..."]).map((announcement) => (
              <p key={announcement}>{announcement}</p>
            ))}
          </div>
        </Card>

        <Button variant="ghost" onClick={onLogout}>
          <LogOut size={18} />
          Dil
        </Button>
      </div>
    </main>
  );
}

function countdownText(today: EmployeeToday, nowMs: number) {
  if (today.simpleOpenMode) {
    if (!today.checkInOpen && !today.checkOutAvailable) {
      return "Orari i sotëm është përfunduar.";
    }
    return today.checkOutAvailable ? "Dalja është e disponueshme në çdo kohë." : "Hyrja është e disponueshme.";
  }
  if (!today.checkInOpensAt && !today.checkInClosesAt) {
    return today.checkOutAvailable ? "Dalja është e disponueshme në çdo kohë." : "Hyrja është e disponueshme.";
  }
  if (!today.checkInOpensAt || !today.checkInClosesAt) {
    return "Nuk ka dritare hyrjeje të planifikuar.";
  }
  const opensAt = new Date(today.checkInOpensAt).getTime();
  const closesAt = new Date(today.checkInClosesAt).getTime();
  if (nowMs < opensAt) {
    return `Hyrja hapet për ${formatDuration(opensAt - nowMs)}.`;
  }
  if (nowMs <= closesAt || today.checkInOpen) {
    const remaining = Math.max(closesAt - nowMs, 0);
    return remaining > 0 ? `Hyrja mbyllet për ${formatDuration(remaining)}.` : "Hyrja është hapur manualisht.";
  }
  return "Dritarja është mbyllur.";
}

function checkinSuccessMessage(checkedInAt: string | null) {
  return `Orari juaj i punës filloi me sukses.\n\nData: ${formatAttendanceDate(checkedInAt)}\nOra e hyrjes: ${formatAttendanceTime(checkedInAt)}\n\nJu urojmë një ditë të mbarë pune!`;
}

function checkoutSuccessMessage(checkedOutAt: string | null, workedMinutes: number) {
  return `Orari juaj i punës përfundoi me sukses.\n\nOra e daljes: ${formatAttendanceTime(checkedOutAt)}\nKoha totale e punës: ${formatWorkedMinutes(workedMinutes)}\n\nFaleminderit për punën tuaj. Ju urojmë një ditë të mbarë!`;
}

function formatAttendanceDate(value: string | null) {
  if (!value) {
    return "--.--.----";
  }
  const date = new Date(value);
  return `${String(date.getDate()).padStart(2, "0")}.${String(date.getMonth() + 1).padStart(2, "0")}.${date.getFullYear()}`;
}

function formatAttendanceTime(value: string | null) {
  if (!value) {
    return "--:--";
  }
  const date = new Date(value);
  return `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function formatWorkedMinutes(minutes: number) {
  const safeMinutes = Math.max(0, minutes);
  const hours = Math.floor(safeMinutes / 60);
  const remainder = safeMinutes % 60;
  return `${hours}h ${String(remainder).padStart(2, "0")}min`;
}

function checkoutErrorMessage(error: unknown) {
  if (error instanceof Error && error.message.trim()) {
    return `Dalja nuk mund të regjistrohej. ${error.message}`;
  }
  return "Dalja nuk mund të regjistrohej. Ju lutemi provoni përsëri.";
}

function formatDuration(ms: number) {
  const totalSeconds = Math.max(0, Math.floor(ms / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  if (hours > 0) {
    return `${hours}h ${minutes}m`;
  }
  if (minutes > 0) {
    return `${minutes}m ${seconds}s`;
  }
  return `${seconds}s`;
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
