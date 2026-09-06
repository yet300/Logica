import fs from "node:fs";
import path from "node:path";
import { ExportCanvas } from "@/components/editor/export-canvas";
import { getExportSizes } from "@/lib/constants";
import { isStoreLocale } from "@/lib/store-locales";
import type { Device, ProjectState } from "@/lib/types";

export const dynamic = "force-dynamic";

const devices = new Set<Device>(["iphone", "ipad", "android", "feature-graphic"]);

function required(value: string | string[] | undefined, name: string): string {
  if (typeof value !== "string" || !value) throw new Error(`${name} is required`);
  return value;
}

export default async function ExportPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const query = await searchParams;
  const locale = required(query.locale, "locale");
  const device = required(query.device, "device") as Device;
  const slideIndex = Number(required(query.slide, "slide"));
  const width = Number(required(query.width, "width"));
  const height = Number(required(query.height, "height"));

  if (!isStoreLocale(locale)) throw new Error(`Unsupported locale: ${locale}`);
  if (!devices.has(device)) throw new Error(`Unsupported device: ${device}`);
  if (!Number.isInteger(slideIndex) || slideIndex < 0) throw new Error("Invalid slide index");
  if (!getExportSizes(device, "portrait").some((size) => size.w === width && size.h === height)) {
    throw new Error(`Unsupported size: ${width}x${height}`);
  }

  const state = JSON.parse(
    fs.readFileSync(path.join(process.cwd(), "app-store-screenshots.json"), "utf8"),
  ) as ProjectState;
  if (slideIndex >= (state.slidesByDevice[device] || []).length) {
    throw new Error(`Slide index ${slideIndex} is out of range`);
  }

  return (
    <main style={{ margin: 0, width, height, overflow: "hidden" }}>
      <ExportCanvas
        state={state}
        device={device}
        locale={locale}
        slideIndex={slideIndex}
        width={width}
        height={height}
      />
    </main>
  );
}
