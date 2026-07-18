import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { describe, expect, it } from "vitest";

describe("mobile form control typography", () => {
  it("keeps editable controls at 16px on mobile to prevent iOS focus zoom", () => {
    const stylesPath = fileURLToPath(new URL("./styles.css", import.meta.url));
    const styles = readFileSync(stylesPath, "utf8").replace(/\s+/g, " ");

    expect(styles).toContain("@media (max-width: 768px)");
    expect(styles).toMatch(/input, select, textarea \{ font-size: 16px !important; \}/);
  });
});
