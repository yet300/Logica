import project from "../app-store-screenshots.json" with { type: "json" };

export const SUPPORTED_DEVICES = ["iphone", "ipad", "android", "feature-graphic"];

export function shouldIgnoreConsoleError(message, url = "") {
  return message.includes("Failed to load resource") && url.endsWith("/favicon.ico");
}

const SIZES = {
  iphone: [[1320, 2868], [1284, 2778], [1206, 2622], [1125, 2436]],
  ipad: [[2064, 2752], [2048, 2732]],
  android: [[1080, 1920]],
  "feature-graphic": [[1024, 500]],
};

export function validateRequest({ locale, devices }) {
  if (!project.locales.includes(locale)) throw new Error(`Unsupported locale: ${locale}`);
  for (const device of devices) {
    if (!SUPPORTED_DEVICES.includes(device)) throw new Error(`Unsupported device: ${device}`);
  }
}

export function buildExportUnits(locale, devices = SUPPORTED_DEVICES) {
  validateRequest({ locale, devices });
  const units = [];
  for (const device of devices) {
    const slides = project.slidesByDevice[device] || [];
    for (const [width, height] of SIZES[device]) {
      slides.forEach((slide, slideIndex) => {
        const filename = `${String(slideIndex + 1).padStart(2, "0")}-${slide.layout}.png`;
        units.push({
          locale,
          device,
          slideIndex,
          width,
          height,
          filename,
          relativePath: `${locale}/${device}/${width}x${height}/${filename}`,
        });
      });
    }
  }
  return units;
}

export function parseArguments(argv) {
  const values = {
    locale: "",
    output: "",
    baseUrl: "http://127.0.0.1:3100",
    devices: [...SUPPORTED_DEVICES],
  };
  for (let index = 0; index < argv.length; index += 2) {
    const flag = argv[index];
    const value = argv[index + 1];
    if (!value) throw new Error(`Missing value for ${flag}`);
    if (flag === "--locale") values.locale = value;
    else if (flag === "--output") values.output = value;
    else if (flag === "--base-url") values.baseUrl = value.replace(/\/$/, "");
    else if (flag === "--devices") values.devices = value.split(",").filter(Boolean);
    else throw new Error(`Unknown argument: ${flag}`);
  }
  if (!values.locale) throw new Error("--locale is required");
  if (!values.output) throw new Error("--output is required");
  validateRequest(values);
  return values;
}
