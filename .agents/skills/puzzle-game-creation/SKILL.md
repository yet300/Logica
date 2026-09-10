---
name: puzzle-game-creation
description: Use when creating or substantially redesigning a puzzle MiniApp in Logica, including reference-game requests, unclear mechanics, prototype-looking UI, missing procedural music or repetitive SFX.
---

# Puzzle game creation

Turn a reference or idea into an agreed, playable product. Research before inventing facts; ask before deciding product preferences; demonstrate visual and audible quality before scaling content. Never promise a flawless first attempt.

## 1. Establish the brief

Read repository `AGENTS.md`, `CONTRIBUTING_MINIAPP.md` and `docs/miniapp/AI_CONTRIBUTOR_PROTOCOL.md`. For research and interview, read [discovery.md](references/discovery.md). Speak the user's language.

Default to a detailed interview in small rounds. Ask unresolved, consequential questions; do not ask the user to supply facts you can research. Preserve prior answers and explicit delegated decisions. An unanswered question is not approval. Continue independent research while answers are pending. If the user explicitly requests autonomy, record reasonable assumptions and proceed within that authorization.

Keep `docs/miniapp/proposals/<game-id>/brief.md` as the durable record: user decisions, source evidence, uncertainties, scope, exclusions and acceptance criteria. Record the current phase and next unresolved decision so another agent can resume without repeating the interview. Research-only requests stop at findings and recommendations; they do not authorize game implementation.

## 2. Make choices reviewable

Read [experience-quality.md](references/experience-quality.md). Produce a concrete rules/level specification, compact and wide screen samples, a motion sample and playable procedural audio samples. Include normal, blocked/error and completion states. Present a recommended direction with meaningful alternatives for unresolved preferences.

For the default collaborative workflow, obtain agreement on the brief and the actual experience samples before implementing the full game or bulk levels. Research, mockups and a bounded experience prototype are allowed to make those decisions concrete. Reuse existing approvals; do not add ceremonial approval rounds. If a required sample cannot be produced, report the missing evidence and ask about the specific fallback rather than silently replacing it with prose.

## 3. Build a complete playable slice, then content

Use the repository game scaffold and current module build configuration. Read relevant Kotlin/Compose/Decompose/Metro skills only for the implementation being changed. Always load `miniapp-procedural-audio` and its routed API, music, SFX and review references when audio is included. Music and SFX are separate required brief decisions; neither may be silently omitted. An explicit user choice for silence is valid.

Implement one complete loop with rules, input, persistence, motion and the agreed audio (music, ambience, semantic SFX or explicit silence) before generating the agreed content volume. Host Back, Settings, toolbar, visibility and audio lifecycle remain host-owned. Use public session audio/storage APIs. Do not add production allowlisting as a side effect.

## 4. Verify experience and scope

Read [acceptance.md](references/acceptance.md). Maintain evidence for every acceptance criterion. Code compilation cannot prove attractive UI, pleasant audio or interesting levels. Fix demonstrated deficiencies and recheck affected behavior. Report unavailable evidence honestly; do not call an unviewed/unheard game polished. Deliver exact commands, artifacts, level counts, limitations and `NOT ALLOWLISTED` status as required by the contributor protocol.

## Frequent failure modes

| Shortcut | Required correction |
|---|---|
| “Thousands of levels” becomes an exact number | Record unknown; agree a concrete original content target. |
| All sounds differ only in pitch or seed | Compare audible roles, envelopes, rhythm and timbre. |
| Placeholder UI will be polished later | Review a real screen and interaction before scaling. |
| A website design skill claims to be the app identity | Verify current host components and actual app screenshots. |
| Missing library icon leads to invented SVG/emoji | Use an approved sourced alternative or raise the asset gap. |
| Deadline pressure removes interview or audio | Explain the tradeoff and use the user's priorities; do not silently reduce scope. |
