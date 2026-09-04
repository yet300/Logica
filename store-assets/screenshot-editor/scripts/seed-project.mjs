import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const story = [
  { id: "01-collection", layout: "hero", label: "LOGICA", headline: "A growing world\nof puzzles.", shot: "01-catalog.png" },
  { id: "02-choice", layout: "device-bottom", label: "CHOOSE YOUR GAME", headline: "Choose your\nnext challenge.", shot: "01-catalog.png" },
  { id: "03-blockblast", layout: "device-top", label: "BLOCK BLAST", headline: "Clear lines.\nFind your flow.", shot: "02-blockblast.png", inverted: true },
  { id: "04-2048", layout: "hero", label: "2048", headline: "Merge numbers.\nThink ahead.", shot: "03-2048.png" },
  { id: "05-fruitmerge", layout: "device-bottom", label: "FRUIT MERGE", headline: "Drop fruit.\nGrow bigger.", shot: "04-fruitmerge.png" },
  { id: "06-collection", layout: "three-devices", label: "GROWING COLLECTION", headline: "Different games.\nOne thoughtful home.", shot: "02-blockblast.png", secondary: "03-2048.png", tertiary: "04-fruitmerge.png", inverted: true },
  { id: "07-close", layout: "hero", label: "LOGICA", headline: "Your next\npuzzle awaits.", shot: "01-catalog.png" },
];

function localize(value) {
  return { en: value };
}

function buildDeck(basePath, device) {
  return story.map((item) => ({
    id: `${device}-${item.id}`,
    layout: item.layout,
    label: localize(item.label),
    headline: localize(item.headline),
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

const iphoneBase = "/screenshots/apple/iphone/{locale}/";
const ipadBase = "/screenshots/apple/ipad/{locale}/";
const state = {
  schemaVersion: 2,
  appName: "Logica",
  themeId: "quiet-editorial",
  connectedCanvas: true,
  locales: ["en"],
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
        headline: localize("A growing world of puzzles."),
        screenshot: "",
      },
    ],
  },
};

fs.writeFileSync(
  path.join(process.cwd(), "app-store-screenshots.json"),
  `${JSON.stringify(state, null, 2)}\n`,
);
