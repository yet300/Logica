# Prompt: research and audit Logica as a MiniApp super-app

Copy the block below into a research-capable coding agent. It must finish the
research and evidence report before proposing implementation changes.

```text
You are working in the Logica — Block Puzzle repository, a Kotlin
Multiplatform super-app for Android and iOS. One host application contains a
catalog of short, local-first MiniApp sessions. First conduct external
research, then perform a complete evidence-based audit from product model and
architecture through build, release, and CI.

Rules:

1. Do not change code, dependencies, the allowlist, CI, or resources until the
   report is complete and the proposed plan is separately approved.
2. Research at least Plato, Offline Games Hub, and two additional one-app game
   hubs. Compare catalog/discoverability, package size, offline/low-bandwidth
   behavior, session lifecycle, monetization, accessibility, privacy/analytics,
   and release cadence. Use primary sources, include the verification date,
   separate facts from inferences, and do not copy code, branding, or UI.
3. Research QuiverAI and ComposeGears/Valkyrie, plus current open-source
   text/image-to-SVG projects (for example OmniSVG, StarVector, LLM4SVG, HiVG,
   and newer candidates). Record code/weights/data licenses, hardware/runtime
   requirements, SVG validity, reproducibility, maintenance, and supply-chain
   risks. Do not add a model or MCP to production/runtime without a separate
   decision.
4. App and game icons are a final-stage task after rules, persistence,
   lifecycle, UI, accessibility, and CI are stable. The workflow is: icon brief
   → QuiverAI SVG → human review → Valkyrie SVG/XML conversion → provenance
   (source, license, tool/version, brief, date, and hash). Gemini Flash-class
   tooling may assist only the bounded SVG-to-Android-XML conversion; human
   review and Valkyrie validation are still required. A missing icon must not
   block scaffolding or engine work.
5. Follow AGENTS.md: the host owns Catalog/Running, Back, Settings, visibility,
   safe areas, and ads; MiniApps own rules, state, persistence, components, and
   UI; production shipping is controlled only by the root miniApps allowlist;
   runtime plugin loading is forbidden.

Order of work:

A. Product research — build a table for four or more hubs, cite sources, and
   identify recurring principles and anti-patterns relevant to Logica.
B. Architecture — build a dependency map for composeApp/core/feature/game/
   miniapp; inspect ownership, Metro child graphs, Decompose navigation,
   lifecycle, visibility/audio/storage contracts, persistence, diagnostics,
   analytics, and host/game coupling. Find cycles, leaky abstractions,
   duplicate policy, unbounded work, unsafe concurrency, stale callbacks, and
   allowlist bypasses.
C. Quality — inspect commonTest, Android host tests, iOS simulator tests,
   integration tests, contract tests, persistence/recovery, accessibility,
   deterministic engine/audio assertions, and test isolation. Map tests to
   public invariants and list uncovered failure paths. A declarative assertion
   is not sufficient proof for PCM, lifecycle, or persistence guarantees.
D. Build and CI/CD — inspect convention plugins, settings discovery, generated
   bundle expectations, dependency-boundary validation, configuration cache,
   reproducibility, JDK/AGP/Kotlin/Compose compatibility, caching, matrix jobs,
   simulator availability, secrets, permissions, artifact retention, flaky-test
   policy, dependency updates, static analysis, provenance/license gates, and
   signing separation. Compare CI jobs with local verification commands and
   identify gates that can be skipped.
E. Security and operations — inspect credential boundaries, consent,
   Crashlytics context, telemetry minimization, storage reset, supply-chain
   pinning, generated files, malicious MiniApp/resource input, external SVG
   services, model weights, MCP servers, and SVG/XML parser risks.

Result format:

1. Executive summary: 5–10 findings with confidence.
2. Research table: URL, verification date, fact/inference, and applicability.
3. Architecture map with ownership and data/control flows.
4. Findings table: ID, severity, evidence (file:line, job output, or URL),
   impact, affected spec/invariant, minimal fix, and regression test.
5. CI/release matrix: gate, owner, command/job, missing coverage, and proposed
   enforcement.
6. Prioritized P0/P1/P2 plan with dependencies, size, rollback, and readiness
   criteria. Do not mix icon work with engine or persistence tasks.
7. Do-not-change list for stable APIs, the production allowlist, credentials,
   generated outputs, and boundaries that are already correct.
8. Exact commands run, failures, blocked-by-environment checks, and open
   questions.

Finish with no more than three safe next-step options. Change code or CI only
after the selected option is approved.
```

## Baseline sources

- [Plato](https://platoapp.com/en/download)
- [Offline Games Hub](https://apps.apple.com/us/app/offline-games-hub-mini-games/id6730120934)
- [QuiverAI](https://app.quiver.ai/) and [API docs](https://docs.quiver.ai/)
- [ComposeGears/Valkyrie](https://github.com/ComposeGears/Valkyrie)
- [OmniSVG](https://github.com/OmniSVG/OmniSVG)
- [StarVector](https://github.com/joanrod/star-vector)
- [LLM4SVG](https://github.com/ximinng/LLM4SVG)
- [HiVG-3B-Base](https://huggingface.co/xingxm/HiVG-3B-Base)
