import { beforeEach, describe, expect, it, vi } from "vitest";
import { checkIn, checkOut } from "./api";
import {
  clearQueuedAttendanceActions,
  getQueuedAttendanceActions,
  queueAttendanceAction,
  syncQueuedAttendanceActions,
} from "./offlineQueue";

vi.mock("./api", () => ({
  checkIn: vi.fn(),
  checkOut: vi.fn(),
  debugGpsLog: vi.fn(),
}));

class MemoryStorage {
  private values = new Map<string, string>();

  getItem(key: string) {
    return this.values.get(key) ?? null;
  }

  setItem(key: string, value: string) {
    this.values.set(key, value);
  }

  removeItem(key: string) {
    this.values.delete(key);
  }

  clear() {
    this.values.clear();
  }
}

function setOnline(value: boolean) {
  Object.defineProperty(globalThis.navigator, "onLine", {
    configurable: true,
    value,
  });
}

describe("offline attendance queue", () => {
  beforeEach(() => {
    Object.defineProperty(globalThis, "localStorage", {
      configurable: true,
      value: new MemoryStorage(),
    });
    Object.defineProperty(globalThis, "navigator", {
      configurable: true,
      value: {},
    });
    setOnline(true);
    vi.mocked(checkIn).mockReset();
    vi.mocked(checkOut).mockReset();
    clearQueuedAttendanceActions();
  });

  it("deduplicates the same pending attendance action", () => {
    const action = {
      id: "action-1",
      type: "CHECK_IN" as const,
      employeeId: "employee-1",
      scheduleId: "schedule-1",
      capturedAt: "2026-07-15T06:50:00.000Z",
      latitude: 42.30413,
      longitude: 21.64894,
      device: { platform: "phone" },
    };

    expect(queueAttendanceAction(action)).toBe(true);
    expect(queueAttendanceAction({ ...action, id: "action-2" })).toBe(false);

    expect(getQueuedAttendanceActions()).toHaveLength(1);
    expect(getQueuedAttendanceActions()[0]).toMatchObject({
      capturedAt: "2026-07-15T06:50:00.000Z",
      latitude: 42.30413,
      longitude: 21.64894,
    });
  });

  it("retries queued actions with the original timestamp and GPS", async () => {
    vi.mocked(checkIn).mockResolvedValue({} as never);
    queueAttendanceAction({
      id: "action-1",
      type: "CHECK_IN",
      employeeId: "employee-1",
      scheduleId: "schedule-1",
      capturedAt: "2026-07-15T06:50:00.000Z",
      latitude: 42.30413,
      longitude: 21.64894,
      device: { platform: "phone" },
    });

    const result = await syncQueuedAttendanceActions("token");

    expect(result).toEqual({ synced: 1, remaining: 0 });
    expect(checkIn).toHaveBeenCalledWith("token", {
      scheduleId: "schedule-1",
      latitude: 42.30413,
      longitude: 21.64894,
      capturedAt: "2026-07-15T06:50:00.000Z",
      device: {
        platform: "phone",
        offlineCapturedAt: "2026-07-15T06:50:00.000Z",
        offlineActionId: "action-1",
      },
    });
    expect(getQueuedAttendanceActions()).toHaveLength(0);
  });

  it("keeps the action queued while offline or when retry fails", async () => {
    queueAttendanceAction({
      id: "action-2",
      type: "CHECK_OUT",
      employeeId: "employee-1",
      scheduleId: "schedule-1",
      capturedAt: "2026-07-15T15:10:00.000Z",
      latitude: 42.30413,
      longitude: 21.64894,
      device: { platform: "phone" },
    });

    setOnline(false);
    expect(await syncQueuedAttendanceActions("token")).toEqual({ synced: 0, remaining: 1 });
    expect(checkOut).not.toHaveBeenCalled();

    setOnline(true);
    vi.mocked(checkOut).mockRejectedValue(new Error("temporary network failure"));
    expect(await syncQueuedAttendanceActions("token")).toEqual({ synced: 0, remaining: 1 });
    expect(getQueuedAttendanceActions()).toHaveLength(1);
  });
});
