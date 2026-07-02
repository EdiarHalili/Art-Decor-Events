export type OfflineAttendanceAction = {
  id: string;
  type: "CHECK_IN" | "CHECK_OUT";
  employeeId: string;
  scheduleId: string;
  capturedAt: string;
  latitude?: number;
  longitude?: number;
  device: Record<string, string>;
};

const STORAGE_KEY = "artdecor.offlineAttendanceQueue";

export function queueAttendanceAction(action: OfflineAttendanceAction) {
  const queued = getQueuedAttendanceActions();
  localStorage.setItem(STORAGE_KEY, JSON.stringify([...queued, action]));
}

export function getQueuedAttendanceActions(): OfflineAttendanceAction[] {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return [];
  }

  try {
    return JSON.parse(raw) as OfflineAttendanceAction[];
  } catch {
    return [];
  }
}

export function clearQueuedAttendanceActions() {
  localStorage.removeItem(STORAGE_KEY);
}

