---
name: metro-di
description: Implement, review, and refactor Metro dependency injection in Logica, including platform graphs, MiniApp aggregation, and session graph extensions.
---

# Metro DI in Logica

Use this skill for changes involving Metro annotations, graph composition, scopes, or generated graph factories. Verify behavior against the installed Metro version and repository sources; do not rely on older Tetris-era examples.

## Current architecture

- Metro version is declared in `gradle/libs.versions.toml` (currently `1.4.2`).
- `composeApp/src/androidMain/.../AndroidAppGraph.kt` and `composeApp/src/iosMain/.../NativeAppGraph.kt` are the final platform graphs.
- App-wide bindings use `AppScope`; final graphs explicitly list their app binding containers.
- `:miniapp:metro` owns `MiniAppMetroBindings`, the immutable registry, empty-capable plugin/expectation multibindings, `MiniAppSessionScope`, and the graph-retaining session wrapper.
- Every MiniApp contributes one `MiniAppPlugin` into the `AppScope` set.
- Every active plugin creates one `@GraphExtension(MiniAppSessionScope::class)` through a namespaced contributed factory.
- Runtime session inputs arrive as one `@Provides MiniAppSessionContext` factory parameter. `MiniAppSessionContextBindings` projects that object into `ComponentContext`, visibility, host, storage, and audio contracts.
- Game-owned session providers live in a game-owned `@BindingContainer` included explicitly by that game's `@GraphExtension(bindingContainers = [...])`. Do not contribute game-specific containers globally to `MiniAppSessionScope`; sibling games must not share one aggregation namespace.

## Rules

1. Keep `core/domain` and `:miniapp:api` free of Metro.
2. Keep native SDK bindings in platform source sets and app graphs.
3. Use `@SingleIn(AppScope::class)` only for app-lifetime objects. Use `@SingleIn(MiniAppSessionScope::class)` for mutable/session-owned components and reducers.
4. Give every `@Provides` declaration an explicit return type.
5. Namespace `@GraphExtension.Factory` methods from the complete MiniApp ID, such as `createGameSnakeSessionGraph`. Kotlin cannot aggregate sibling factories distinguished only by return type.
6. Expose a concrete session type from each child graph. Do not bind every game to one qualified `MiniAppSession` key.
7. Return the graph-retaining session wrapper from `MiniAppPlugin.createSession`; otherwise the child graph can be collected while its session remains visible.
8. Bind runtime resources through `MiniAppSessionContext`. A game must not inject the host, Settings, platform audio, or native ad implementations directly.
9. Prefer constructor injection for simple concrete classes and small explicit binding containers for factories or aliases. Avoid provider methods that only hide a trivial constructor without improving graph ownership.
10. Use `@GraphPrivate` only where a binding must be hidden from child graphs. Use `@Multibinds(allowEmpty = true)` when an empty aggregate is valid.

## MiniApp graph pattern

```kotlin
@BindingContainer
abstract class SnakeSessionBindings {
    companion object {
        @Provides
        @SingleIn(MiniAppSessionScope::class)
        fun provideSession(component: SnakeComponent): SnakeSession = SnakeSession(component)
    }
}

@GraphExtension(
    scope = MiniAppSessionScope::class,
    bindingContainers = [SnakeSessionBindings::class],
)
interface SnakeSessionGraph {
    val session: SnakeSession

    @ContributesTo(AppScope::class)
    @GraphExtension.Factory
    fun interface Factory {
        fun createGameSnakeSessionGraph(
            @Provides context: MiniAppSessionContext,
        ): SnakeSessionGraph
    }
}
```

Inline providers on the graph are also acceptable for a very small MiniApp. Extract a binding container when it clarifies ownership or the graph has several providers.

## Review workflow

1. Use the codebase knowledge graph to find the graph, factory, contribution, and consumers.
2. Read the affected module's `build.gradle.kts`, plus `settings.gradle.kts` and `gradle/libs.versions.toml` when plugin or module wiring changes.
3. Confirm scope compatibility and whether bindings belong to the app graph or one MiniApp session graph.
4. Compile the final Android and iOS graphs because cross-module Metro errors often surface only at final graph generation.
5. For MiniApp work, run that game's `allTests`, `validateMiniAppDependencies`, `compileAndroidMain`, and `compileKotlinIosSimulatorArm64`. Run `:miniapp:bundle:verifyMiniAppBundle` when shipping aggregation changes.

Reject changes that place game-specific session bindings in a shared global contribution, use string qualifiers to distinguish sibling sessions, create graphs from Compose UI, or add a second game-specific scope on top of `MiniAppSessionScope` without a real additional lifecycle.
