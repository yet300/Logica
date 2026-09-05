import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";
import test from "node:test";
import {
  buildExportUnits,
  parseArguments,
  shouldIgnoreConsoleError,
  validateRequest,
} from "./export-contract.mjs";

test("one complete locale produces fifty store images", () => {
  const units = buildExportUnits("ka");
  assert.equal(units.length, 50);
  assert.equal(units.filter((unit) => unit.device === "iphone").length, 28);
  assert.equal(units.filter((unit) => unit.device === "ipad").length, 14);
  assert.equal(units.filter((unit) => unit.device === "android").length, 7);
  assert.equal(units.filter((unit) => unit.device === "feature-graphic").length, 1);
  assert.equal(units[0].relativePath, "ka/iphone/1320x2868/01-hero.png");
});

test("browser favicon noise is ignored but render failures are retained", () => {
  assert.equal(shouldIgnoreConsoleError("Failed to load resource", "/favicon.ico"), true);
  assert.equal(shouldIgnoreConsoleError("Failed to load resource", "/screenshots/missing.png"), false);
});

test("unknown locale is rejected", () => {
  assert.throws(
    () => validateRequest({ locale: "xx", devices: ["iphone"] }),
    /Unsupported locale: xx/,
  );
});

test("unknown device is rejected", () => {
  assert.throws(
    () => validateRequest({ locale: "en", devices: ["watch"] }),
    /Unsupported device: watch/,
  );
});

test("command arguments have deterministic defaults", () => {
  assert.deepEqual(parseArguments(["--locale", "ja", "--output", "/tmp/out"]), {
    locale: "ja",
    output: "/tmp/out",
    baseUrl: "http://127.0.0.1:3100",
    devices: ["iphone", "ipad", "android", "feature-graphic"],
  });
});

test("manual CI workflow retains locale artifacts without publishing", () => {
  const workflowPath = path.resolve(process.cwd(), "../../.github/workflows/store-screenshots.yml");
  const workflow = fs.readFileSync(workflowPath, "utf8");
  assert.match(workflow, /workflow_dispatch:/);
  assert.match(workflow, /locale:/);
  assert.match(workflow, /max-parallel: 4/);
  assert.match(workflow, /retention-days: 14/);
  assert.match(workflow, /iphone\/1320x2868/);
  assert.match(workflow, /ipad\/2064x2752/);
  assert.match(workflow, /android\/1080x1920/);
  assert.match(workflow, /feature-graphic\/1024x500/);
  assert.doesNotMatch(workflow, /path:\s*store-assets\/ci-exports\/\$\{\{ matrix\.locale \}\}\s*$/m);
  assert.doesNotMatch(workflow, /upload_to_play_store|upload_to_app_store|PLAY_STORE_JSON_KEY/);
});
