# Logica catalog and contributor governance design

## Decision

Logica is a single Kotlin Multiplatform host for a curated collection of
small, local-first MiniApps. The launcher card is intentionally compact: it
shows the MiniApp icon, title, and Play action. `MiniAppManifest.description`
remains part of the manifest contract for metadata, tests, accessibility and
future detail surfaces, but is not rendered in the launcher card.

Game and app icons are a final-stage product asset. A MiniApp may ship without
an icon while rules, persistence, lifecycle, UI and CI are being built. Once a
game is functionally and visually stable, an approved icon brief is sent to
QuiverAI (or a later approved equivalent), the SVG is reviewed, and Valkyrie
is used for Compose/Android vector conversion. A Gemini Flash-class model may
assist a bounded SVG-to-Android-XML conversion, followed by human review and
Valkyrie validation. Every committed asset records source, license, tool,
prompt/brief, date and a content hash in provenance.

## Research-informed constraints

Existing one-app game hubs commonly optimize for a curated catalog, short
sessions, low friction and offline/low-bandwidth use. Logica therefore keeps
the host-owned catalog and lifecycle boundary, avoids runtime plugin loading,
and treats each game module as independently testable and allowlisted.

Potential open SVG-generation models are research inputs, not runtime
dependencies. OmniSVG, StarVector, LLM4SVG and HiVG must be evaluated for
license, reproducibility, resource requirements, SVG validity and icon quality
before any tool integration is proposed.

## Implementation scope

1. Remove only the card's supporting description content.
2. Keep manifest/API and contract tests unchanged except for UI assertions
   that explicitly describe the compact card.
3. Update README and contributor guidance to describe Logica's super-app model,
   the allowlist boundary, the final-stage icon workflow and provenance.
4. Add a reusable prompt for an agent to research comparable super-apps first,
   then audit architecture through CI/CD with evidence, risks and a phased
   remediation plan.

## Non-goals

- Removing `description` from `MiniAppManifest`.
- Adding Quiver credentials, an SVG-generation MCP, or a model to the build.
- Generating or replacing product/game icons in this change.
- Changing production API, module dependencies or the shipping allowlist.
