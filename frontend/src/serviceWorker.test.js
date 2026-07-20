import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

describe("production service worker update policy", () => {
  it("uses an explicit release cache and avoids forced takeover during active sessions", () => {
    const workerPath = fileURLToPath(new URL("../public/service-worker.js", import.meta.url));
    const mainPath = fileURLToPath(new URL("./main.tsx", import.meta.url));
    const worker = readFileSync(workerPath, "utf8");
    const main = readFileSync(mainPath, "utf8");

    expect(worker).toContain('const CACHE_VERSION = "v6-production-hardening"');
    expect(worker).not.toMatch(/self\.skipWaiting\(\)/);
    expect(main).not.toMatch(/SKIP_WAITING/);
    expect(main).not.toMatch(/controllerchange/);
  });
});
