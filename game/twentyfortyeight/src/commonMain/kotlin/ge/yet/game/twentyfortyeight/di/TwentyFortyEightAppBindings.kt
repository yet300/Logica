package ge.yet.game.twentyfortyeight.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import ge.yet.game.twentyfortyeight.diagnostics.CrashlyticsTwentyFortyEightDiagnostics
import ge.yet.game.twentyfortyeight.diagnostics.TwentyFortyEightDiagnostics

@ContributesTo(AppScope::class)
@BindingContainer
abstract class TwentyFortyEightAppBindings {
    @Binds
    internal abstract val CrashlyticsTwentyFortyEightDiagnostics.bindDiagnostics:
        TwentyFortyEightDiagnostics
}
