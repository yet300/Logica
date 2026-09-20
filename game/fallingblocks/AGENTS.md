    # Fallingblocks MiniApp

    Read [the AI contributor protocol](../../docs/miniapp/AI_CONTRIBUTOR_PROTOCOL.md)
    before making changes. The human workflow is documented in
    [the MiniApp contributor guide](../../docs/CONTRIBUTING_MINIAPP.md).

    Use `MiniAppId("game.fallingblocks").storageKey(localName)` for every new persistent key. Never copy another plugin's key prefix.
    This project is discovered on the next Gradle invocation, but is not shipped until a maintainer adds it to the production allowlist.
    Verify it with `./gradlew :game:fallingblocks:verifyMiniApp`.
    The session-owned Decompose component lives in `component/root/RootComponent.kt` (`RootComponent` + `DefaultRootComponent`); session UI lives in `ui/screen/root/RootContent.kt`.
    

This profile includes a pure state/action/engine seam. Keep rules in `FallingblocksGameEngine`, keep state immutable, and keep UI side-effect free. It is a small starting point, not a universal game engine.

