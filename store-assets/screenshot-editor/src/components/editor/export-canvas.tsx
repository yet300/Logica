"use client";

import * as React from "react";
import { themeById } from "@/lib/constants";
import { didFail, preloadImages } from "@/lib/image-cache";
import { resolveScreenshot } from "@/lib/locale";
import type { Device, ProjectState } from "@/lib/types";
import { DeckCanvas, getCanvas } from "./slide-canvas";

export function ExportCanvas({
  state,
  device,
  locale,
  slideIndex,
  width,
  height,
}: {
  state: ProjectState;
  device: Device;
  locale: string;
  slideIndex: number;
  width: number;
  height: number;
}) {
  const [ready, setReady] = React.useState(false);
  const [exportError, setExportError] = React.useState("");
  const slides = state.slidesByDevice[device] || [];
  const { cW, cH } = getCanvas(device, "portrait");

  React.useEffect(() => {
    const paths = new Set<string>();
    if (state.appIcon) paths.add(state.appIcon);
    for (const slide of slides) {
      for (const source of [slide.screenshot, slide.screenshotSecondary, slide.screenshotTertiary]) {
        if (source) paths.add(resolveScreenshot(source, locale));
      }
    }
    Promise.all([
      preloadImages(Array.from(paths), { retryFailed: true }),
      document.fonts?.ready ?? Promise.resolve(),
    ]).then(() => {
      const missing = Array.from(paths).filter((source) => didFail(source));
      if (missing.length) setExportError(`Missing export assets: ${missing.join(", ")}`);
      requestAnimationFrame(() => requestAnimationFrame(() => setReady(true)));
    });
  }, [locale, slides, state.appIcon]);

  return (
    <div
      id="export-frame"
      data-export-ready={ready ? "true" : "false"}
      data-export-error={exportError || undefined}
      style={{ width, height, position: "relative", overflow: "hidden", background: "#fff" }}
    >
      <div
        style={{
          width: cW,
          height: cH,
          position: "absolute",
          left: 0,
          top: 0,
          overflow: "hidden",
          transform: `scale(${width / cW}, ${height / cH})`,
          transformOrigin: "top left",
        }}
      >
        <div style={{ position: "absolute", left: -slideIndex * cW, top: 0 }}>
          <DeckCanvas
            slides={slides}
            device={device}
            orientation="portrait"
            theme={themeById(state.themeId)}
            locale={locale}
            appName={state.appName}
            appIcon={state.appIcon}
            connectedCanvas={state.connectedCanvas}
          />
        </div>
      </div>
    </div>
  );
}
