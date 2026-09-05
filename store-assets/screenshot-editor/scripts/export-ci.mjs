import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import { execFileSync } from "node:child_process";
import { chromium } from "playwright";
import { buildExportUnits, parseArguments, shouldIgnoreConsoleError } from "./export-contract.mjs";

const options = parseArguments(process.argv.slice(2));
const units = buildExportUnits(options.locale, options.devices);
const startedAt = Date.now();
const deviceTimings = {};
const browser = await chromium.launch({
  headless: true,
  ...(process.env.SCREENSHOT_BROWSER_CHANNEL
    ? { channel: process.env.SCREENSHOT_BROWSER_CHANNEL }
    : {}),
});
const page = await browser.newPage({ deviceScaleFactor: 1 });
const errors = [];

page.on("pageerror", (error) => errors.push(error.message));
page.on("response", (response) => {
  if (response.status() < 400) return;
  const message = `Failed to load resource: HTTP ${response.status()}`;
  if (!shouldIgnoreConsoleError(message, response.url())) errors.push(`${message} ${response.url()}`);
});

try {
  for (const unit of units) {
    const deviceStartedAt = deviceTimings[unit.device]?.startedAt ?? Date.now();
    deviceTimings[unit.device] = { startedAt: deviceStartedAt, seconds: 0 };
    await page.setViewportSize({ width: unit.width, height: unit.height });
    const query = new URLSearchParams({
      locale: unit.locale,
      device: unit.device,
      slide: String(unit.slideIndex),
      width: String(unit.width),
      height: String(unit.height),
    });
    const response = await page.goto(`${options.baseUrl}/export?${query}`, {
      waitUntil: "networkidle",
      timeout: 60_000,
    });
    if (!response?.ok()) throw new Error(`Export page returned HTTP ${response?.status()}`);
    await page.locator('[data-export-ready="true"]').waitFor({ timeout: 60_000 });
    const exportError = await page.locator("#export-frame").getAttribute("data-export-error");
    if (exportError) throw new Error(exportError);
    const destination = path.join(options.output, unit.relativePath);
    fs.mkdirSync(path.dirname(destination), { recursive: true });
    await page.locator("#export-frame").screenshot({ path: destination, animations: "disabled" });
    deviceTimings[unit.device].seconds = (Date.now() - deviceStartedAt) / 1000;
    if (errors.length) throw new Error(`Browser render error: ${errors.shift()}`);
  }
} finally {
  await browser.close();
}

const commit = process.env.GITHUB_SHA || (() => {
  try {
    return execFileSync("git", ["rev-parse", "HEAD"], { encoding: "utf8" }).trim();
  } catch {
    return "unknown";
  }
})();
const manifest = {
  locale: options.locale,
  commit,
  generatedAt: new Date().toISOString(),
  imageCount: units.length,
  totalSeconds: (Date.now() - startedAt) / 1000,
  deviceSeconds: Object.fromEntries(
    Object.entries(deviceTimings).map(([device, timing]) => [device, timing.seconds]),
  ),
  files: units.map(({ relativePath, width, height, device }) => ({ relativePath, width, height, device })),
};
const manifestPath = path.join(options.output, options.locale, "manifest.json");
fs.mkdirSync(path.dirname(manifestPath), { recursive: true });
fs.writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
console.log(`Exported ${units.length} PNGs for ${options.locale} in ${manifest.totalSeconds.toFixed(1)}s.`);
