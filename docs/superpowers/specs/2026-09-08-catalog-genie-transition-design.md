# Catalog Genie Transition Design

**Date:** 2026-09-08  
**Status:** Approved in conversation; awaiting written-spec review

## Goal

Replace the generic Catalog/Running MiniApp stack transition with a macOS-inspired Genie effect. Launching a MiniApp expands the selected catalog card into the full application viewport. Leaving the MiniApp performs the exact reverse transition and contracts the visible game into that same card.

The implementation must remain time-based and smooth on 60 Hz and 120 Hz displays, degrade safely on weaker devices, respect reduced-motion preferences, and preserve Decompose's ownership of navigation and MiniApp session lifecycles.

This is a perceptual recreation of the macOS Genie effect, not a platform-private or pixel-identical implementation.

## Scope

The effect applies only to transitions between `RootComponent.Child.Catalog` and `RootComponent.Child.RunningMiniApp`.

It does not change:

- navigation inside a MiniApp;
- Settings or Review sheets;
- MiniApp-owned themes or frame modes;
- MiniApp session creation, visibility, audio, storage, teardown, or stale-callback policies;
- catalog card content or the shipping registry.

## Chosen Approach

Compose and Decompose remain responsible for state, navigation, input gating, accessibility, and animation timing. A Compose `Canvas` renderer is used only for the nonlinear deformation that standard affine `graphicsLayer`, `SharedTransitionLayout`, and Decompose stack animators cannot express.

Before a transition, the presentation layer captures one immutable visual frame of the surface being transformed. During the transition, the renderer divides that image into a fixed set of horizontal bands. For a given normalized progress, a pure geometry function calculates the horizontal inset, vertical position, height, and corner envelope of every band. Drawing the source bands into those progressively narrowed destinations creates the curved funnel that enters the selected card.

No frame is captured inside the animation loop. No blur, live shadow, runtime bitmap scaling pass, or game recomposition is required per band.

Alternatives rejected:

1. A clipped scale/fade is cheaper but does not deform the game and therefore does not read as a Genie effect.
2. Separate native mesh or shader implementations could be more exact, but would duplicate Android/iOS work, increase old-GPU risk, and bypass the requested Compose-first architecture.

## Ownership and Boundaries

### Catalog presentation

`CatalogContent` remains a renderer for `CatalogComponent`, but gains a presentation callback that reports the selected `MiniAppId` and the card's current window-space bounds. `MiniAppListItemCard` measures itself with `onGloballyPositioned` and keeps only the latest finite, non-empty `boundsInWindow` value.

Geometry does not enter `CatalogComponent`, `RootComponent`, the MiniApp API, or domain state. Existing component callbacks still carry only domain intent.

### Root presentation

`RootContent` owns a small `GenieTransitionCoordinator`. Its state contains:

- direction: expanding or contracting;
- selected `MiniAppId`;
- source and destination rectangles in Root window coordinates;
- normalized progress;
- render mode: full Genie or reduced fallback;
- input-blocked state;
- an immutable captured frame when full Genie rendering is available.

The coordinator is UI state, not a navigation component. It cannot create or destroy sessions and does not know game-specific types.

### Decompose

Decompose remains authoritative for the child stack and lifecycle. The existing component methods remain the only operations that launch or leave a MiniApp. The custom Root transition replaces the current generic Cupertino stack animation only for Catalog/Running pairs. Other future Root child pairs retain an ordinary stack animation.

The implementation must not cause both the old stack animation and Genie animation to transform the same content.

## Transition Data Flow

### Launch

1. The Play button reports the selected ID and current card bounds to `RootContent`.
2. Root rejects the request if another transition is active.
3. Root records the target geometry and invokes the existing catalog launch callback.
4. Decompose creates the Running MiniApp child and session normally.
5. After the Running content has a valid laid-out frame, Root captures it once.
6. The catalog remains visually available below the transition overlay while the captured Running frame expands from the card to the Root viewport.
7. Root removes the overlay, exposes the live Running content, and re-enables input.

If capture or valid geometry is unavailable, step 6 uses the reduced affine fallback and navigation still completes.

### Back

1. Toolbar Back, system Back, and predictive Back enter the same Root transition request path.
2. Root rejects duplicate requests and captures the laid-out Running frame once.
3. Root makes the catalog destination available below the overlay while keeping the live session alive.
4. The captured frame contracts from the Root viewport into the recorded card bounds.
5. Only after a committed animation completes does Root invoke the existing `onBackClicked()` operation.
6. Decompose then destroys the Running child and session through its existing teardown ordering.

An aborted predictive-back gesture animates the progress back to the Running state and does not invoke `onBackClicked()`.

### Destination recovery

The last selected `MiniAppId` is retained as presentation state while Running is active. When returning, the catalog resolves that ID to its lazy-grid item. If the item is not currently laid out, the catalog requests a key-based scroll before measuring it. If it still cannot provide valid bounds—for example, after a registry change—the transition uses the reduced fallback and must not delay teardown indefinitely.

## Motion Model

Opening and closing use one reversible pure transform. Reversal, rather than a separately tuned approximation, guarantees matching endpoints.

- Expansion duration: 440 ms.
- Contraction duration: 380 ms.
- Reduced/failure fallback duration: 160 ms.
- Progress is elapsed-time based and independent of refresh rate.
- The easing is monotonic and bounded; it must not overshoot outside the viewport or produce negative sizes.
- The distant edge begins narrowing first. The card-facing edge follows later, creating a curved throat rather than uniform scaling.
- The outer envelope interpolates between the viewport corners and the catalog card shape.
- At progress 0 and 1, geometry exactly matches the corresponding endpoint rectangles.

These constants remain centralized in one motion specification rather than repeated through composables. Changing them requires updating the motion tests and this specification.

## Performance Strategy

The initial implementation uses a small, fixed band count in the approximate 16–24 range. The exact default is selected by benchmark and visual inspection, not by display refresh rate. A 120 Hz display receives more time samples, not more bands or more work per frame.

The render loop must:

- reuse primitive arrays or stable band objects;
- avoid collection construction and lambda capture per band;
- read animation progress once per frame;
- avoid bitmap capture, blur, shadow generation, layout, and navigation work;
- avoid observing unrelated MiniApp state;
- draw only the transition overlay while the live source is visually suppressed.

The coordinator never attempts unreliable device-class detection. Reduced motion explicitly selects the lightweight path. Capture failure, invalid geometry, or renderer unavailability also selects it. The full effect is deliberately bounded enough to remain the normal path on supported devices.

The implementation will use trace-based or allocation-oriented inspection where available and manual profiling on representative Android hardware. Debug-build visual smoothness alone is not sufficient evidence of release performance.

## Accessibility and Interaction

System reduced-motion or a zero motion-duration scale selects the short affine transition without nonlinear deformation. It remains spatially tied to the selected card so navigation context is preserved.

All launch/back input is disabled while a non-interactive transition is active. Semantics expose only one active screen; the captured image and overlay are invisible to accessibility services. Focus moves according to the final Decompose child, not to the temporary overlay.

Predictive Back may control transition progress interactively. Cancelling restores the Running screen and its input state. Committing finishes contraction before navigation teardown.

## Failure Handling

Every failure mode completes navigation:

- missing or empty bounds: reduced fallback;
- card not laid out after bounded recovery: reduced fallback;
- frame capture failure: reduced fallback;
- interrupted composition or disposal: clear transition state and reconcile with the current Decompose child;
- duplicate launch/back input: ignore while the first request is active;
- target ID no longer in the catalog: reduced fallback targeting a zero-size rectangle at the horizontal center of the visible catalog viewport and its vertical center line.

There is no unbounded wait for layout, capture, scroll, or an animation completion callback.

## Verification

### Pure geometry tests

- Both endpoints equal their exact input rectangles.
- Expansion is the mathematical reverse of contraction.
- All coordinates and dimensions remain finite and non-negative for progress in `[0, 1]`.
- Progress and the funnel envelope remain bounded and monotonic.
- Phone, tablet, portrait, landscape, and off-center card rectangles produce valid bands.

### Compose UI tests

- A card reports the correct `MiniAppId` with valid window bounds.
- Repeated Play and Back input is blocked during transition.
- The selected ID, rather than list position, identifies the return target.
- The overlay is excluded from semantics.
- Reduced motion and invalid bounds select the fallback.
- The final live child is visible and interactive after completion or cancellation.

### Root and lifecycle tests

- Launch creates a session exactly once.
- Committed Back destroys it exactly once and only after the transition completes.
- Cancelled predictive Back does not destroy the session.
- Existing teardown-before-clear, visibility, audio, and stale-callback guarantees remain intact.

### Build and manual verification

Run the narrow Root, Catalog, and Compose tests first, followed by Android and iOS compilation. Validate the release build on at least one 60 Hz device/emulator and one 120 Hz Android device when available. Confirm:

- no first-frame flash;
- exact visual entry into the selected card;
- consistent wall-clock duration at both refresh rates;
- no stuck input or navigation after interruption;
- acceptable frame timing during both directions;
- correct fallback under reduced motion.

## Acceptance Criteria

1. Launch visibly grows from the exact selected catalog card.
2. Back visibly contracts into that same card with a recognizable curved Genie funnel.
3. Decompose remains the sole navigation and session-lifecycle authority.
4. The animation is time-based, reversible, interrupt-safe, and does not allocate or capture frames in its render loop.
5. Reduced motion and all capture/geometry failures finish through a lightweight transition.
6. Root, Catalog, lifecycle, Android compilation, and iOS simulator compilation checks pass.
