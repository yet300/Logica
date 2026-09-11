# Fruit Merge MiniApp Agent Guide

Read [the AI contributor protocol](../../docs/miniapp/AI_CONTRIBUTOR_PROTOCOL.md)
before making changes. The human workflow is documented in
[the MiniApp contributor guide](../../docs/CONTRIBUTING_MINIAPP.md).

Use `MiniAppId("game.fruitmerge").storageKey(localName)` for every new persistent key. Never copy another plugin's key prefix.
This project is discovered on the next Gradle invocation, but is not shipped until a maintainer adds it to the production allowlist.
Verify it with `./gradlew :game:fruitmerge:verifyMiniApp`.

The pure engine owns rules, physics transitions, deterministic RNG and score. It performs no persistence, navigation, analytics, audio, host
callbacks, Compose work, or delays. UI receives immutable models and actions; business mutation stays outside composables.

## Boundaries

- Keep engine, component, session and UI implementation details
  `internal`. `FruitMergeSession` is public only as the graph's unique
  Metro binding type; its constructor and state remain internal.
- Create session-owned components through `FruitMergeSessionGraph`;
  every level (`Session`, `Game`) exposes a `Factory` interface with a
  `Default` implementation. Factories and aliases are unscoped `@Binds`;
  `@SingleIn(MiniAppSessionScope)` lives on stateful classes (engine,
  physics, audio adapter) and on the retained component/store/session
  providers. The stateless persistence stays unscoped and plays all
  three repository roles.
- The MVI store is session-retained and game-owned (`session/store`);
  the game component receives the retained store from the session
  factory. Pure step-label and result-reached decisions live in
  `FruitMergeTransitionPlanner` with focused unit tests; the executor
  keeps orchestration and side effects only.
- Persistence splits into `domain/repository` contracts (snapshot
  loading, commit writing, tutorial flag) plus the `data/`
  implementation and schemas. Keys and schemas are frozen.
- Replay remains a future host action and is not part of the public API.

Every runtime session uses only the `MiniAppSessionContext.storage` and
`MiniAppSessionContext.audio` facades supplied to its retained child graph.
Persistent names are local snake-case names under the host-owned namespace;
never construct physical keys.
