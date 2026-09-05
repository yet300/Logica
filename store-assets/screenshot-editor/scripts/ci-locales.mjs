import fs from "node:fs";
import process from "node:process";
import { LOCALES } from "./marketing-copy.mjs";

const requested = (process.env.INPUT_LOCALE || "all").trim().toLowerCase();
const locales = requested === "all" ? LOCALES : [requested];
if (requested !== "all" && !LOCALES.includes(requested)) {
  throw new Error(`Unsupported locale: ${requested}. Expected one of: ${LOCALES.join(", ")}, all`);
}
const line = `locales=${JSON.stringify(locales)}\n`;
if (process.env.GITHUB_OUTPUT) fs.appendFileSync(process.env.GITHUB_OUTPUT, line);
else process.stdout.write(line);
