# Research and interview

## Evidence first

Open supplied URLs. Inspect store description, screenshots, version history, gameplay footage and relevant player reviews. Seek direct gameplay evidence for gestures, transitions and later mechanics; still images cannot establish animation timing. Record URL, access date, app version/platform when known, screenshot or video timestamp, claim, confidence and limitations.

Label each finding: observed, developer claim, player report, inference, proposed feature or unknown. Never claim to have played, watched or heard content that tools did not expose. Store copy is not a complete game specification. A few visible reviews are not a representative survey: deduplicate, discard off-topic reviews and report sample size/platform/date coverage. Do not invent percentages or consensus.

For praise and complaints, extract the gameplay consequence: precision, visibility, progression, variety, challenge, recovery, interruptions, tutorial comprehension, sound fatigue. Do not let generic advertising complaints dominate. Map useful evidence to a proposed change, its tradeoff and a test. Conflicting preferences belong in the interview.

Exact level count and original level layouts are separate unknowns. “Thousands,” highest seen level and endless generation do not establish a finite catalogue. Do not fabricate completeness, reproduce unseen levels or assume random seeds equal unique puzzles. Ask the user to choose the content target when evidence cannot establish it. Follow repository provenance rules for reference assets and distinctive expression; research can continue while an implementation rights question is unresolved.

## Present findings before asking design questions

Use a short evidence table: mechanic/experience, evidence and confidence, recommendation. Summarize the core loop, visible UI hierarchy, observed motion and unresolved mechanics. Offer specific improvements grounded in player reports, with alternatives rather than automatic scope expansion.

Example for an Arrows-like reference: if the store advertises thousands of puzzles but no exact count, explicitly leave count unknown. If sampled reviews mention accidental taps and insufficient zoom, propose testing hit targeting and fit-to-board versus manual zoom. Do not report those findings unless verified in the current research. Motion durations, collision rules and hidden modes remain unknown until observed; proposed values must be labelled as proposals.

## Interview rounds

Ask 1–3 connected questions per round using the available question tool, or one concise question at a time without it. Give a recommendation and concrete alternatives; allow free-form answers. Go deep on chosen branches. Do not dump this entire checklist into chat, repeat answered questions, or require the user to design the game unaided.

1. **Intent:** desired feeling, audience, session length, what to preserve/improve from the reference, disliked examples, fixed constraints and priorities.
2. **Rules:** exact valid actions, blocked actions, mistakes/lives, win/fail, hints/undo/retry, timers, optional modes, onboarding. Explain consequences of options such as relaxed versus punitive play.
3. **Levels:** agreed finite count or endless mode, handcrafted/generated/hybrid, target difficulty, unlocks, progression, replay, daily content only if requested. Define variety through actual decisions, not board size alone.
4. **Visuals:** user references, existing Logica screens to match, palette, light/dark preference, density, typography, board treatment, icon family, amount of persistent text. Show alternatives when words are ambiguous.
5. **Interaction and motion:** tap/drag/pan/zoom, input during animation, feedback for invalid action, speed and expressiveness, completion transition, reduced-motion behavior, haptics if available.
6. **Audio:** music versus ambience versus silence, mood, reference aesthetics, timbres to include/avoid, energy and fatigue tolerance, adaptive states, SFX character, acceptable variation. Provide original audible alternatives rather than asking for synthesis parameters.
7. **Delivery:** platforms/device classes, accessibility, persistence, languages, acceptable time/scope tradeoffs, and how the user wants to review the slice.

Convert answers into acceptance criteria. A decision is ready when it has an answer, explicit delegation, or an explicitly accepted omission. Before building, show the brief with unresolved blockers and approved choices, not a vague “shall I proceed?”.
