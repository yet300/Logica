# Contributing a MiniApp to Logica

Logica is a single Kotlin Multiplatform super-app with a curated catalog of
short, local-first MiniApp sessions. Contributions are independent Kotlin
Multiplatform modules. The scaffold creates reviewable source; it does not
authorize a game to ship. Discovery, review, merge and production allowlisting
are separate steps.

The stable policy is recorded in
[ADR-0001](docs/adr/0001-miniapp-contribution-and-shipping.md). AI agents must
also follow the [AI contributor protocol](docs/miniapp/AI_CONTRIBUTOR_PROTOCOL.md).

## Create a puzzle game with an AI agent

Start with the repository's
[`puzzle-game-creation` skill](.agents/skills/puzzle-game-creation/SKILL.md).
Give the agent an idea or a reference-game URL; a complete specification is
not required to begin research and the interview. For example:

> Use $puzzle-game-creation to create a puzzle game for Logica based on this
> reference: [paste the game URL]. First research its mechanics, design,
> animations and player feedback. Interview me in detail about the rules,
> levels, visual direction and procedural music/SFX. Show concrete visual,
> motion and audio samples before implementing the full game. Use sourced
> interface icons rather than inventing them.

The skill guides the agent through research, small rounds of questions, an
agreed brief, experience samples, a complete playable loop and validated
levels. Decisions are retained in `docs/miniapp/proposals/<game-id>/brief.md`;
checks and evidence go in the adjacent `acceptance.md`. It connects to the
existing procedural-audio skill and the contributor workflow below.

Agents reading this guide for a puzzle-creation request should proactively
introduce this skill and explain the next research/interview step. Do not wait
for the user to know its name. If skill invocation is unavailable, read the
linked `SKILL.md` directly and follow its references. Preserve answers and
approvals already given; do not restart the interview. Missing submission
fields can be collected through this process before source generation.

Reference evidence may be incomplete: do not invent an exact level count or
unobserved mechanics. Visual and audible review remain necessary; the skill
does not guarantee a flawless first result or authorize production shipping.

## 1. Describe the game

Before creating source, prepare the fields from
[`submission.schema.json`](docs/miniapp/submission.schema.json):

- a unique lowercase MiniApp ID such as `game.snake`;
- display name, category, authors and a short description;
- rules, controls and the session flow;
- supported device classes and accessibility behavior;
- visual and audio style, including references;
- storage values and requested host capabilities;
- code, art, audio, font, license and AI provenance;
- deterministic acceptance scenarios and known limitations.

Keep the catalog-facing description short and useful even though the launcher
card currently renders only the icon, title and Play action. The manifest
description remains required metadata and may be used by accessibility or a
future details surface.

Original mechanics are welcome. Existing or licensed intellectual property
requires an approved proposal issue and verifiable rights evidence before
implementation begins. A claim that an asset is AI-generated is not, by itself,
proof that it is original or distributable.

## 2. Generate the module

Use the repository scaffold from the root:

```bash
./gradlew createMiniApp \
  -PminiAppId=game.snake \
  -PminiAppName=Snake
```

For a game, use the optional game profile:

```bash
./gradlew createMiniApp \
  -PminiAppId=game.snake \
  -PminiAppName=Snake \
  -PminiAppProfile=game
```

The game profile adds immutable `GameState`, typed `GameAction`, a pure
`GameEngine` reducer seam and focused engine/component tests. Its `Tick`
action is deliberately a placeholder: contributors must define the actual
rules instead of inheriting mechanics that may not fit their game. Omitting
the profile keeps the smaller basic scaffold.

Only direct `:game:<name>` and `:miniapp:samples:<name>` projects are accepted.
The command must not overwrite an existing project.

The generated module contains the plugin, retained session, Metro child graph,
minimal Compose content, resources and a contract test. Keep the generated
framework wiring unless the game requires a documented extension.

For lifecycle and contract tests, use the shared
`miniapp:testkit` `withMiniAppSession` helper. It supplies a test context,
recording host, mutable visibility source, storage and guaranteed teardown.

## 3. Implement within the boundaries

Game code owns rules, game-specific state, persistence policy, components and
UI. The host owns catalog navigation, Back, Settings, toolbar, banners, safe
areas and session visibility.

Use:

- `MiniAppSessionContext.storage` for persistence;
- local snake-case storage names, never physical keys;
- `MiniAppSessionContext.audio` and public audio presets for new audio;
- `AdaptiveGameScaffold` where the game fits its layout model;
- Compose resources for user-visible text and localized strings.

Do not depend on feature modules, application modules, another game/sample,
native ad adapters, platform audio APIs or raw Multiplatform Settings. Do not
add catalog cards, host controls, Replay actions or a second navigation host.

### Defer final icon work

The game icon is a final-stage asset, not a scaffold prerequisite. First make
the game functional, adaptive, accessible, persistent and verifiable on CI.
When the UI is stable, prepare an icon brief and use the approved
[QuiverAI](https://app.quiver.ai/) SVG workflow. Review the SVG and convert it
with [Valkyrie](https://github.com/ComposeGears/Valkyrie). A Gemini Flash-class
model may help with a bounded SVG-to-Android-XML conversion, but the output
still needs human review and Valkyrie validation. Do not add a new generation
model, MCP integration or runtime dependency as part of a normal MiniApp
contribution. Record prompt/brief, source URL, license, tool/version, date and
hash in provenance. Open-source generators are allowed only after a separate
maintainer evaluation of output quality, license and reproducibility.

## 4. Verify before review

Run the generated module gate:

```bash
./gradlew :game:snake:verifyMiniApp
```

It checks the dependency boundary, tests, Android compilation and iOS
Simulator compilation. Also run the focused tests for the engine, persistence,
lifecycle, accessibility and acceptance scenarios. Run `git diff --check`.

Before requesting review, confirm:

- the game has no borrowed code, assets, music, names or distinctive branding;
- all persistent values are namespaced through the MiniApp storage facade;
- session creation, backgrounding, destruction and recreation are safe;
- UI works on compact and wide layouts;
- important actions have accessibility semantics;
- visible text and resources have a localization path;
- provenance and license evidence are included;
- icon work is either explicitly deferred or includes the complete Quiver/
  Valkyrie provenance record;
- the change is explicitly marked **NOT ALLOWLISTED**.

## 5. Shipping decision

Contributors and automated agents must not edit the root `miniApps` allowlist.
Only a maintainer may add a reviewed module to
[`settings.gradle.kts`](settings.gradle.kts). Being discoverable or merged does
not imply that the game is included in a release.
