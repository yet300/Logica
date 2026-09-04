import fs from "node:fs";
import path from "node:path";
import process from "node:process";
import { COPY, LOCALES, localizedHeadlines, localizedLabels } from "./marketing-copy.mjs";

const story = [
  { id: "01-collection", layout: "hero", shot: "01-catalog.png" },
  { id: "02-choice", layout: "device-bottom", shot: "01-catalog.png" },
  { id: "03-blockblast", layout: "device-top", shot: "02-blockblast.png", inverted: true },
  { id: "04-2048", layout: "hero", shot: "03-2048.png" },
  { id: "05-fruitmerge", layout: "device-bottom", shot: "04-fruitmerge.png" },
  { id: "06-collection", layout: "three-devices", shot: "02-blockblast.png", secondary: "03-2048.png", tertiary: "04-fruitmerge.png", inverted: true },
  { id: "07-close", layout: "hero", shot: "01-catalog.png" },
];

const labels = localizedLabels();
const headlines = localizedHeadlines();

function buildDeck(basePath, device) {
  return story.map((item, index) => ({
    id: `${device}-${item.id}`,
    layout: item.layout,
    label: labels[index],
    headline: headlines[index],
    screenshot: `${basePath}${item.shot}`,
    ...(item.secondary ? { screenshotSecondary: `${basePath}${item.secondary}` } : {}),
    ...(item.tertiary ? { screenshotTertiary: `${basePath}${item.tertiary}` } : {}),
    ...(item.inverted ? { inverted: true } : {}),
    ...(device === "android" && item.id === "03-blockblast"
      ? {
          transforms: {
            device: { x: 252, y: -96, width: 576, height: 1248, zIndex: 3 },
          },
        }
      : {}),
  }));
}

const iphoneBase = "/screenshots/apple/iphone/en/";
const ipadBase = "/screenshots/apple/ipad/en/";
const state = {
  schemaVersion: 2,
  appName: "Logica",
  themeId: "quiet-editorial",
  connectedCanvas: true,
  locales: LOCALES,
  locale: "en",
  device: "iphone",
  orientation: "portrait",
  appIcon: "/app-icon.png",
  slidesByDevice: {
    iphone: buildDeck(iphoneBase, "iphone"),
    ipad: buildDeck(ipadBase, "ipad"),
    android: buildDeck(iphoneBase, "android"),
    "android-7": [],
    "android-10": [],
    "feature-graphic": [
      {
        id: "feature-graphic-en",
        layout: "feature-graphic",
        label: {},
        headline: Object.fromEntries(
          LOCALES.map((locale) => [locale, COPY[locale].headlines[0].replace("\n", " ")]),
        ),
        screenshot: "",
      },
    ],
  },
};

fs.writeFileSync(
  path.join(process.cwd(), "app-store-screenshots.json"),
  `${JSON.stringify(state, null, 2)}\n`,
);
