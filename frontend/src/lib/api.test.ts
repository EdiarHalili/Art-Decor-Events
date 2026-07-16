import { describe, expect, it, vi } from "vitest";
import { deleteEmployee, forceDeleteEmployee } from "./api";

describe("admin employee API", () => {
  it("sends authenticated delete requests for employees", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await deleteEmployee("access-token", "employee-123");

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toMatch(/\/api\/v1\/admin\/employees\/employee-123$/);
    expect(init.method).toBe("DELETE");
    expect(init.cache).toBe("no-store");
    expect(new Headers(init.headers).get("Authorization")).toBe("Bearer access-token");
  });

  it("sends authenticated force-delete requests to the separate endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);

    await forceDeleteEmployee("access-token", "employee-123");

    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toMatch(/\/api\/v1\/admin\/employees\/employee-123\/force$/);
    expect(init.method).toBe("DELETE");
    expect(new Headers(init.headers).get("Authorization")).toBe("Bearer access-token");
  });
});
