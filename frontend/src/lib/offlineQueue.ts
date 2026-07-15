import { checkIn, checkOut, debugGpsLog } from "./api";

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
  if (queued.some((queuedAction) => samePendingAction(queuedAction, action))) {
    return false;
  }
  setQueuedAttendanceActions([...queued, action]);
  return true;
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

export async function syncQueuedAttendanceActions(accessToken: string): Promise<{
  synced: number;
  remaining: number;
}> {
  if (!navigator.onLine) {
    return { synced: 0, remaining: getQueuedAttendanceActions().length };
  }

  const queued = getQueuedAttendanceActions();
  const remaining: OfflineAttendanceAction[] = [];
  let synced = 0;

  for (const action of queued) {
    try {
      const payload = {
        scheduleId: action.scheduleId,
        latitude: action.latitude,
        longitude: action.longitude,
        capturedAt: action.capturedAt,
        device: {
          ...action.device,
          offlineCapturedAt: action.capturedAt,
          offlineActionId: action.id,
        },
      };
      debugGpsLog("offline attendance sync payload", {
        type: action.type,
        scheduleId: payload.scheduleId,
        latitude: payload.latitude,
        longitude: payload.longitude,
      });
      if (action.type === "CHECK_IN") {
        await checkIn(accessToken, payload);
      } else {
        await checkOut(accessToken, payload);
      }
      synced += 1;
    } catch {
      remaining.push(action);
    }
  }

  setQueuedAttendanceActions(remaining);
  return { synced, remaining: remaining.length };
}

function samePendingAction(left: OfflineAttendanceAction, right: OfflineAttendanceAction) {
  return left.id === right.id
    || (left.type === right.type && left.employeeId === right.employeeId && left.scheduleId === right.scheduleId);
}

function setQueuedAttendanceActions(actions: OfflineAttendanceAction[]) {
  if (actions.length === 0) {
    clearQueuedAttendanceActions();
    return;
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(actions));
}
