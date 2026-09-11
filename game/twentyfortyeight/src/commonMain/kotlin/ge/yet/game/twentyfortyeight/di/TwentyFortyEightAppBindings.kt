package ge.yet.game.twentyfortyeight.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import ge.yet.game.twentyfortyeight.component.playing.store.NewGameSeedSource
import ge.yet.game.twentyfortyeight.diagnostics.CrashlyticsTwentyFortyEightDiagnostics
import ge.yet.game.twentyfortyeight.diagnostics.TwentyFortyEightDiagnostics
import kotlin.random.Random

@ContributesTo(AppScope::class)
@BindingContainer
abstract class TwentyFortyEightAppBindings {
    @Binds
    internal abstract val CrashlyticsTwentyFortyEightDiagnostics.bindDiagnostics:
        TwentyFortyEightDiagnostics

    companion object {
        @Provides
        @SingleIn(AppScope::class)
        internal fun provideNewGameSeedSource(): NewGameSeedSource =
            NewGameSeedSource { Random.Default.nextLong() }
    }
}
