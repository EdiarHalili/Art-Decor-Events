import { useEffect, useRef, useState } from "react";
import { Bell, CalendarClock, FileText, LogOut, MapPin, Wifi, WifiOff } from "lucide-react";
import { BrandMark } from "../../components/BrandMark";
import { PwaInstallPrompt } from "../../components/PwaInstallPrompt";
import { ThemeToggle } from "../../components/ThemeToggle";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import {
  checkIn,
  checkOut,
  debugGpsLog,
  exportMyAttendance,
  getEmployeeToday,
  getMyAttendanceHistory,
  isTemporaryNetworkError,
  recordLiveLocation,
  type AppSettings,
  type AttendanceReportRow,
  type AuthResponse,
  type EmployeeToday,
} from "../../lib/api";
import { getQueuedAttendanceActions, queueAttendanceAction, syncQueuedAttendanceActions } from "../../lib/offlineQueue";

type EmployeeHomeProps = {
  session: AuthResponse;
  settings: AppSettings | null;
  onLogout: () => void;
};

type HistoryPreset = "today" | "this-week" | "this-month" | "last-month" | "custom";
type GpsLocation = { latitude: number; longitude: number; accuracyMeters?: number; capturedAt?: string };

export function EmployeeHome({ session, settings, onLogout }: EmployeeHomeProps) {
  const [today, setToday] = useState<EmployeeToday | null>(() => getCachedToday(session.employeeId));
  const [online, setOnline] = useState(navigator.onLine);
  const [message, setMessage] = useState("");
  const [actionLoading, setActionLoading] = useState<"CHECK_IN" | "CHECK_OUT" | null>(null);
  const [queuedCount, setQueuedCount] = useState(getQueuedAttendanceActions().length);
  const [queuedActions, setQueuedActions] = useState(() => getQueuedAttendanceActions());
  const [todayFresh, setTodayFresh] = useState(false);
  const [now, setNow] = useState(() => Date.now());
  const [liveTrackingActive, setLiveTrackingActive] = useState(false);
  const [checkinConfirmOpen, setCheckinConfirmOpen] = useState(false);
  const [checkoutConfirmOpen, setCheckoutConfirmOpen] = useState(false);
  const [attendanceSuccess, setAttendanceSuccess] = useState("");
  const defaultRange = rangeForPreset("this-month");
  const [historyPreset, setHistoryPreset] = useState<HistoryPreset>("this-month");
  const [historyFrom, setHistoryFrom] = useState(defaultRange.from);
  const [historyTo, setHistoryTo] = useState(defaultRange.to);
  const [historyRows, setHistoryRows] = useState<AttendanceReportRow[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyMessage, setHistoryMessage] = useState("");
  const [exporting, setExporting] = useState<"pdf" | null>(null);
  const liveLocationInFlight = useRef(false);
  const lastValidLiveLocation = useRef<GpsLocation | null>(readPendingLiveLocation());

  useEffect(() => {
    async function syncOnlineState() {
      const nextOnline = navigator.onLine;
      setOnline(nextOnline);
      refreshQueuedActions();
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
    const trackingEnabled = Boolean(settings?.liveLocationTrackingEnabled && today?.checkOutAvailable);
    if (!trackingEnabled) {
      if (!today?.checkOutAvailable) {
        clearPendingLiveLocation();
      }
      setLiveTrackingActive(false);
      return;
    }

    let cancelled = false;
    let watchId: number | null = null;
    const intervalMinutes = Math.max(5, settings?.liveLocationIntervalMinutes ?? 10);

    async function sendLiveLocation() {
      if (liveLocationInFlight.current) {
        return;
      }
      liveLocationInFlight.current = true;
      const location = readPendingLiveLocation() ?? lastValidLiveLocation.current ?? await captureLiveLocation();
      if (cancelled || !location) {
        liveLocationInFlight.current = false;
        return;
      }
      if (!navigator.onLine) {
        storePendingLiveLocation(location);
        setLiveTrackingActive(true);
        liveLocationInFlight.current = false;
        return;
      }
      try {
        await recordLiveLocation(session.accessToken, {
          ...location,
          capturedAt: location.capturedAt ?? new Date().toISOString(),
          device: deviceMetadata(),
        });
        clearPendingLiveLocation();
        if (!cancelled) {
          setLiveTrackingActive(true);
        }
      } catch {
        storePendingLiveLocation(location);
        if (!cancelled) {
          setLiveTrackingActive(Boolean(lastValidLiveLocation.current));
        }
      } finally {
        liveLocationInFlight.current = false;
      }
    }

    if ("geolocation" in navigator) {
      watchId = navigator.geolocation.watchPosition(
        (position) => {
          const location = gpsFromPosition(position);
          if (!location) {
            return;
          }
          lastValidLiveLocation.current = location;
          debugGpsLog("browser live watch geolocation", location);
        },
        (error) => {
          debugGpsLog("browser live watch geolocation failed", { code: error.code, message: error.message });
        },
        { enableHighAccuracy: false, timeout: 15000, maximumAge: 5 * 60 * 1000 },
      );
    }

    void sendLiveLocation();
    const timer = window.setInterval(() => void sendLiveLocation(), intervalMinutes * 60 * 1000);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
      if (watchId != null) {
        navigator.geolocation.clearWatch(watchId);
      }
      liveLocationInFlight.current = false;
      setLiveTrackingActive(false);
    };
  }, [online, session.accessToken, settings?.liveLocationIntervalMinutes, settings?.liveLocationTrackingEnabled, today?.checkOutAvailable]);

  const employeeName = today?.employeeName ?? session.fullName;
  const historyTotals = summarizeHistory(historyRows);
  const pendingCheckIn = queuedActions.some((action) =>
    action.type === "CHECK_IN" && action.employeeId === session.employeeId && action.scheduleId === today?.scheduleId,
  );
  const pendingCheckOut = queuedActions.some((action) =>
    action.type === "CHECK_OUT" && action.employeeId === session.employeeId && action.scheduleId === today?.scheduleId,
  );

  useEffect(() => {
    void loadHistory();
  }, [session.accessToken, historyFrom, historyTo]);

  async function captureLocation(): Promise<{ latitude?: number; longitude?: number }> {
    const location = await captureGpsLocation({
      attempts: 3,
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 30000,
      stage: "browser attendance geolocation",
    });
    return location ? { latitude: location.latitude, longitude: location.longitude } : {};
  }

  async function captureLiveLocation(): Promise<GpsLocation | null> {
    return captureGpsLocation({
      attempts: 2,
      enableHighAccuracy: false,
      timeout: 12000,
      maximumAge: 5 * 60 * 1000,
      stage: "browser live geolocation",
    });
  }

  function deviceMetadata() {
    return {
      userAgent: navigator.userAgent,
      platform: navigator.platform,
      language: navigator.language,
    };
  }

  function refreshQueuedActions() {
    const actions = getQueuedAttendanceActions();
    setQueuedActions(actions);
    setQueuedCount(actions.length);
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
    const capturedAt = new Date().toISOString();
    const payload = { scheduleId: today.scheduleId, ...location, capturedAt, device: deviceMetadata() };
    debugGpsLog(`${type.toLowerCase().replace("_", "-")} prepared payload`, {
      scheduleId: payload.scheduleId,
      latitude: payload.latitude,
      longitude: payload.longitude,
    });

    if (!navigator.onLine) {
      queuePendingAttendance(type, payload);
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
      if (isTemporaryNetworkError(error)) {
        queuePendingAttendance(type, payload);
        return;
      }
      setMessage(type === "CHECK_OUT" ? checkoutErrorMessage(error) : error instanceof Error ? error.message : "Hyrja nuk mund të regjistrohej.");
    } finally {
      setActionLoading(null);
    }
  }

  function queuePendingAttendance(
    type: "CHECK_IN" | "CHECK_OUT",
    payload: { scheduleId: string; latitude?: number; longitude?: number; capturedAt: string; device: Record<string, string> },
  ) {
    const queued = queueAttendanceAction({
      id: crypto.randomUUID(),
      type,
      employeeId: session.employeeId!,
      scheduleId: payload.scheduleId,
      capturedAt: payload.capturedAt,
      latitude: payload.latitude,
      longitude: payload.longitude,
      device: payload.device,
    });
    refreshQueuedActions();
    if (type === "CHECK_OUT") {
      setCheckoutConfirmOpen(false);
      setToday({ ...today!, status: "Dalja është në pritje për sinkronizim.", checkInOpen: false, checkOutAvailable: false });
      setMessage(queued
        ? "Dalja u ruajt lokalisht. Do të dërgohet automatikisht kur lidhja të rikthehet."
        : "Dalja është tashmë në pritje dhe do të dërgohet automatikisht kur lidhja të rikthehet.");
      return;
    }

    setCheckinConfirmOpen(false);
    setToday({ ...today!, status: "Hyrja është në pritje për sinkronizim.", checkInOpen: false, checkOutAvailable: true });
    setMessage(queued
      ? "Hyrja u ruajt lokalisht. Do të dërgohet automatikisht kur lidhja të rikthehet."
      : "Hyrja është tashmë në pritje dhe do të dërgohet automatikisht kur lidhja të rikthehet.");
  }

  async function syncPendingActions() {
    const before = getQueuedAttendanceActions().length;
    if (before === 0) {
      refreshQueuedActions();
      return;
    }

    const result = await syncQueuedAttendanceActions(session.accessToken);
    refreshQueuedActions();
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

  async function loadHistory() {
    if (!historyFrom || !historyTo || historyTo < historyFrom) {
      setHistoryMessage("Zgjidhni një interval të vlefshëm.");
      return;
    }
    setHistoryLoading(true);
    setHistoryMessage("");
    try {
      setHistoryRows(await getMyAttendanceHistory(session.accessToken, { from: historyFrom, to: historyTo }));
    } catch (error) {
      setHistoryMessage(error instanceof Error ? error.message : "Historia e punës nuk mund të ngarkohej.");
    } finally {
      setHistoryLoading(false);
    }
  }

  async function downloadHistory() {
    setExporting("pdf");
    setHistoryMessage("");
    try {
      const blob = await exportMyAttendance(session.accessToken, { from: historyFrom, to: historyTo, format: "pdf" });
      const url = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = `Historia_Punes_${safeFilenamePart(session.employeeCode ?? "Punetori")}_${historyFrom.slice(0, 7)}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
      setHistoryMessage("Eksporti u shkarkua me sukses.");
    } catch (error) {
      setHistoryMessage(error instanceof Error ? error.message : "Eksporti nuk mund të shkarkohej.");
    } finally {
      setExporting(null);
    }
  }

  return (
    <main className="brand-surface min-h-screen overflow-x-hidden px-3 py-4 pb-[calc(1.25rem+env(safe-area-inset-bottom))] pt-[calc(1.25rem+env(safe-area-inset-top))] sm:px-4 sm:py-5">
      <div className="mx-auto flex w-full max-w-md flex-col gap-4 sm:gap-5">
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
              disabled={!today?.checkInOpen || pendingCheckIn || actionLoading !== null || (online && !todayFresh)}
              onClick={() => setCheckinConfirmOpen(true)}
            >
              {pendingCheckIn ? "Hyrja është në pritje..." : actionLoading === "CHECK_IN" ? "Duke regjistruar..." : "Hyrje"}
            </Button>
            <Button
              className="h-14 text-base sm:h-16"
              variant="secondary"
              disabled={!today?.checkOutAvailable || pendingCheckOut || actionLoading !== null || (online && !todayFresh)}
              onClick={() => setCheckoutConfirmOpen(true)}
            >
              {pendingCheckOut ? "Dalja është në pritje..." : actionLoading === "CHECK_OUT" ? "Duke regjistruar..." : "Dalje"}
            </Button>
          </div>
        </Card>

        <Card className="p-4 sm:p-5">
          <div className="flex items-center gap-3">
            <FileText className="text-primary" size={21} />
            <h2 className="font-semibold">Raporti mujor</h2>
          </div>
          <p className="mt-2 text-sm text-muted-foreground">
            Shkarkoni raportin PDF te punes per muajin aktual.
          </p>
          <Button type="button" className="mt-4 w-full" disabled={Boolean(exporting)} onClick={() => void downloadHistory()}>
            <FileText size={17} />
            {exporting === "pdf" ? "Duke shkarkuar..." : "Shkarko raportin PDF"}
          </Button>
          {historyMessage && <p className="mt-3 rounded-md bg-muted px-3 py-2 text-sm text-muted-foreground">{historyMessage}</p>}
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
              <h2 id="checkout-confirm-title" className="text-lg font-semibold">Konfirmo daljen</h2>
              <p className="mt-3 text-sm text-muted-foreground">A jeni i sigurt që dëshironi të bëni daljen?</p>
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
                  {actionLoading === "CHECK_OUT" ? "Duke regjistruar..." : "Po, bëj daljen"}
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
    return today.checkOutAvailable ? "Dalja është e disponueshme në çdo kohë." : "Mënyra e hapur është aktive.";
  }
  if (!today.checkInOpensAt && !today.checkInClosesAt) {
    return today.checkOutAvailable ? "Dalja është e disponueshme në çdo kohë." : "Mënyra e hapur është aktive.";
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

async function captureGpsLocation({
  attempts,
  enableHighAccuracy,
  timeout,
  maximumAge,
  stage,
}: {
  attempts: number;
  enableHighAccuracy: boolean;
  timeout: number;
  maximumAge: number;
  stage: string;
}): Promise<GpsLocation | null> {
  if (!("geolocation" in navigator)) {
    debugGpsLog(`${stage} unavailable`, { reason: "Geolocation API is not supported" });
    return null;
  }

  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    const location = await getCurrentGpsLocation({ enableHighAccuracy, timeout, maximumAge, stage, attempt });
    if (location) {
      return location;
    }
    if (attempt < attempts) {
      await wait(700);
    }
  }
  return null;
}

function getCurrentGpsLocation({
  enableHighAccuracy,
  timeout,
  maximumAge,
  stage,
  attempt,
}: {
  enableHighAccuracy: boolean;
  timeout: number;
  maximumAge: number;
  stage: string;
  attempt: number;
}): Promise<GpsLocation | null> {
  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const location = gpsFromPosition(position);
        debugGpsLog(stage, {
          attempt,
          latitude: location?.latitude,
          longitude: location?.longitude,
          accuracyMeters: location?.accuracyMeters,
          valid: Boolean(location),
        });
        resolve(location);
      },
      (error) => {
        debugGpsLog(`${stage} failed`, { attempt, code: error.code, message: error.message });
        resolve(null);
      },
      { enableHighAccuracy, timeout, maximumAge },
    );
  });
}

function gpsFromPosition(position: GeolocationPosition): GpsLocation | null {
  const latitude = position.coords.latitude;
  const longitude = position.coords.longitude;
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude) || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
    debugGpsLog("browser geolocation rejected invalid coordinates", { latitude, longitude });
    return null;
  }
  return {
    latitude,
    longitude,
    accuracyMeters: Number.isFinite(position.coords.accuracy) ? position.coords.accuracy : undefined,
    capturedAt: new Date(position.timestamp || Date.now()).toISOString(),
  };
}

function wait(ms: number) {
  return new Promise((resolve) => window.setTimeout(resolve, ms));
}

function storePendingLiveLocation(location: GpsLocation) {
  try {
    localStorage.setItem("artdecor.pendingLiveLocation", JSON.stringify(location));
  } catch {
    // Best effort only; the next interval can capture a fresh location.
  }
}

function readPendingLiveLocation(): GpsLocation | null {
  try {
    const raw = localStorage.getItem("artdecor.pendingLiveLocation");
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as GpsLocation;
    if (!Number.isFinite(parsed.latitude) || !Number.isFinite(parsed.longitude)) {
      return null;
    }
    return parsed;
  } catch {
    return null;
  }
}

function clearPendingLiveLocation() {
  localStorage.removeItem("artdecor.pendingLiveLocation");
}

function checkinSuccessMessage(checkedInAt: string | null) {
  return `Orari juaj i punës filloi me sukses.\n\nData: ${formatAttendanceDate(checkedInAt)}\nOra e hyrjes: ${formatAttendanceTime(checkedInAt)}\n\nJu urojmë një ditë të mbarë pune!`;
}

function checkoutSuccessMessage(checkedOutAt: string | null, _workedMinutes: number) {
  return `Dalja u regjistrua me sukses!\nOra e daljes: ${formatAttendanceTime(checkedOutAt)}`;
}

function SummaryCell({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md bg-muted p-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="mt-1 break-words font-semibold">{value}</p>
    </div>
  );
}

function summarizeHistory(rows: AttendanceReportRow[]) {
  return rows.reduce(
    (totals, row) => {
      if (row.checkedInAt || row.workedMinutes > 0) {
        totals.days += 1;
      }
      const normalMinutes = Math.min(row.workedMinutes, 8 * 60);
      const overtimeMinutes = Math.max(row.workedMinutes - 8 * 60, row.overtimeMinutes, 0);
      totals.normalMinutes += normalMinutes;
      totals.overtimeMinutes += overtimeMinutes;
      totals.totalMinutes += normalMinutes + overtimeMinutes;
      return totals;
    },
    { days: 0, normalMinutes: 0, overtimeMinutes: 0, totalMinutes: 0 },
  );
}

function rangeForPreset(preset: HistoryPreset) {
  const now = new Date();
  if (preset === "today") {
    const today = dateOnly(now);
    return { from: today, to: today };
  }
  if (preset === "this-week") {
    const day = now.getDay() || 7;
    const from = new Date(now);
    from.setDate(now.getDate() - day + 1);
    return { from: dateOnly(from), to: dateOnly(now) };
  }
  if (preset === "last-month") {
    return {
      from: dateOnly(new Date(now.getFullYear(), now.getMonth() - 1, 1)),
      to: dateOnly(new Date(now.getFullYear(), now.getMonth(), 0)),
    };
  }
  return {
    from: dateOnly(new Date(now.getFullYear(), now.getMonth(), 1)),
    to: dateOnly(new Date(now.getFullYear(), now.getMonth() + 1, 0)),
  };
}

function dateOnly(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function safeFilenamePart(value: string) {
  return value.trim().replace(/[^A-Za-z0-9_-]+/g, "_").replace(/^_+|_+$/g, "") || "Punetori";
}

function formatDateSq(value: string) {
  const [year, month, day] = value.split("-");
  return `${day}.${month}.${year}`;
}

function historyStatus(row: AttendanceReportRow) {
  if (!row.checkedInAt) {
    return "-";
  }
  return row.checkedOutAt ? "Dalë" : "Në punë";
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
