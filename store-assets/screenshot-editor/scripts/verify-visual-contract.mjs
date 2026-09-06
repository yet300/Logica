import fs from "node:fs";
import path from "node:path";
import process from "node:process";

const root = process.cwd();
const read = (file) => fs.readFileSync(path.join(root, file), "utf8");
const frames = read("src/components/editor/device-frames.tsx");
const canvas = read("src/components/editor/slide-canvas.tsx");
const state = JSON.parse(read("app-store-screenshots.json"));

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

const androidPhone = frames.slice(
  frames.indexOf("export function AndroidPhone"),
  frames.indexOf("export function AndroidTabletP"),
);

assert(canvas.includes('"SF Pro Display"'), "marketing canvas must use SF Pro Display");
assert(canvas.includes("fontWeight: 900"), "marketing headline weight must be 900");
assert(
  canvas.includes("marketingHeadlineScale(headline)"),
  "localized headlines must shrink to preserve the two-line composition",
);
assert(canvas.includes("#862B18") && canvas.includes("#C84E25"), "feature graphic needs the approved orange gradient");
assert(canvas.includes('color: "#FFFFFF"'), "feature graphic text must be opaque white");
assert(androidPhone.includes('data-device-shell="camera-free"'), "Android phone shell must be camera-free");
assert(!androidPhone.includes('transform: "translateX(-50%)"'), "Android phone shell must not render a centered camera dot");

const androidSlide = state.slidesByDevice.android.find(
  (slide) => slide.id === "android-03-blockblast",
);
assert(androidSlide, "Android Block Blast slide is missing");
assert(
  JSON.stringify(androidSlide.transforms) ===
    JSON.stringify({
      device: { x: 252, y: -96, width: 576, height: 1248, zIndex: 3 },
    }),
  "Android screen 3 must use the approved device transform",
);

for (const device of ["iphone", "ipad"]) {
  const slide = state.slidesByDevice[device].find((item) =>
    item.id.endsWith("03-blockblast"),
  );
  assert(!slide.transforms, `${device} screen 3 must keep its original geometry`);
}

console.log("Verified orange feature graphic, SF typography, camera-free Android frame, and screen-3 spacing.");
