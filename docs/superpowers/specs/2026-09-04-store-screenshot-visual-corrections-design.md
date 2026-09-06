# Store Screenshot Visual Corrections

## Goal

Correct four visual issues in the existing English Quiet Editorial store assets without changing the approved seven-screen narrative or production gameplay captures.

## Approved Direction

- Use a darker, distinctly orange Feature Graphic gradient from `#862B18` to `#C84E25`.
- Render the Feature Graphic app name and tagline in fully opaque white for stronger contrast.
- Replace Inter in exported marketing canvases with a system display stack led by `SF Pro Display`, using `-apple-system` and `BlinkMacSystemFont` fallbacks.
- Increase headline weight to `900`, while keeping labels lighter so the hierarchy remains editorial rather than uniformly heavy.
- Replace the Android phone shell with a thin matte graphite frame without a camera, punch-hole, notch, speaker, or decorative hardware.
- Correct Android screen 3 locally: reduce the default device width by 15% and position it so the frame ends at least 32 canvas pixels above the label.

## Scope

The typography change applies to exported screenshot and Feature Graphic canvases. Editor controls keep their existing UI font. The camera-less shell applies to Android phone marketing frames only; iPhone and iPad frames remain unchanged. The screen-3 transform is stored only in the Android deck so iPhone and iPad compositions do not move.

## Export and README

Re-export the Android 1080×1920 deck and Google Play Feature Graphic through the editor. Flatten the regenerated PNGs to opaque RGB, rerun the 50-file export verifier, inspect the corrected files at full resolution, and rebuild the contact sheet. README paths already point at canonical exports, so no further README markup change is required.

## Acceptance Criteria

- White Feature Graphic text is clearly readable against orange at thumbnail size.
- Android frames contain no visible camera element.
- Android screen 3 has no overlap between the device frame, label, or headline.
- Headlines use the heavier SF system display stack without clipping.
- All 50 canonical exports retain the required dimensions, ordering, and opaque RGB mode.
- Screenshot editor verification and production build pass.
