import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const root = process.cwd();
const state = JSON.parse(fs.readFileSync(path.join(root, "app-store-screenshots.json"), "utf8"));
const expectedLocales = [
  "ar", "az", "be", "bn", "da", "de", "el", "en", "es", "fi",
  "fr", "he", "hi", "hu", "hy", "id", "it", "ja", "ka", "kk",
  "ko", "ky", "nb", "nl", "pl", "pt", "ro", "ru", "sv", "th",
  "tr", "uk", "vi", "zh",
];
const expectedHeadlines = [
  "A growing world\nof puzzles.",
  "Choose your\nnext challenge.",
  "Clear lines.\nFind your flow.",
  "Merge numbers.\nThink ahead.",
  "Drop fruit.\nGrow bigger.",
  "Different games.\nOne thoughtful home.",
  "Your next\npuzzle awaits.",
];
const expectedLayouts = [
  "hero",
  "device-bottom",
  "device-top",
  "hero",
  "device-bottom",
  "three-devices",
  "hero",
];
const imageDecks = ["iphone", "ipad", "android"];

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

assert(state.schemaVersion === 2, "schemaVersion must be 2");
assert(state.appName === "Logica", "appName must be Logica");
assert(state.themeId === "quiet-editorial", "Quiet Editorial theme must be active");
assert(state.connectedCanvas === true, "new decks must use connected canvas");
assert(JSON.stringify(state.locales) === JSON.stringify(expectedLocales), "all store-supported locales must be in scope");
assert(state.locale === "en", "active locale must be en");
assert(state.appIcon === "/app-icon.png", "production icon path is required");

for (const device of imageDecks) {
  const slides = state.slidesByDevice[device];
  assert(Array.isArray(slides) && slides.length === 7, `${device} must have seven slides`);
  assert(
    JSON.stringify(slides.map((slide) => slide.layout)) === JSON.stringify(expectedLayouts),
    `${device} layout rhythm is wrong`,
  );
  assert(
    JSON.stringify(slides.map((slide) => slide.headline.en)) === JSON.stringify(expectedHeadlines),
    `${device} narrative copy is wrong`,
  );
  const mosaic = slides[5];
  assert(mosaic.screenshot && mosaic.screenshotSecondary && mosaic.screenshotTertiary, `${device} mosaic needs three captures`);
}

const feature = state.slidesByDevice["feature-graphic"];
assert(Array.isArray(feature) && feature.length === 1, "feature graphic deck must have one slide");
assert(feature[0].layout === "feature-graphic", "feature graphic layout is required");
assert(feature[0].headline.en === "A growing world of puzzles.", "feature graphic copy is wrong");

const referenced = new Set();
for (const device of imageDecks) {
  for (const slide of state.slidesByDevice[device]) {
    for (const key of ["screenshot", "screenshotSecondary", "screenshotTertiary"]) {
      const value = slide[key];
      if (value) referenced.add(value.replace("{locale}", "en"));
    }
  }
}
for (const publicPath of referenced) {
  const diskPath = path.join(root, "public", publicPath.replace(/^\//, ""));
  assert(fs.existsSync(diskPath), `missing referenced screenshot: ${publicPath}`);
}

console.log(`Verified ${imageDecks.length} seven-slide decks and ${referenced.size} source paths.`);
