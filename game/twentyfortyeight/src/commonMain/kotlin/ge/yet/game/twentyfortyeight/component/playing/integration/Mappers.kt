package ge.yet.game.twentyfortyeight.component.playing.integration

import ge.yet.game.miniapp.api.MiniAppVisibility
import ge.yet.game.twentyfortyeight.component.playing.PlayingComponent
import ge.yet.game.twentyfortyeight.component.playing.store.BootstrapState
import ge.yet.game.twentyfortyeight.component.playing.store.TwentyFortyEightStore
import ge.yet.game.twentyfortyeight.engine.GamePhase

internal val stateToModel: (TwentyFortyEightStore.State) -> PlayingComponent.Model = { state ->
    val game = state.game
    PlayingComponent.Model(
        board = game?.board,
        transition = state.activeTransition,
        score = game?.score ?: 0L,
        bestScore = game?.bestScore ?: 0L,
        bestImprovedInRun = game?.facts?.bestImprovedInRun == true,
        gesturesEnabled = state.bootstrap == BootstrapState.Ready &&
                state.visibility == MiniAppVisibility.ACTIVE &&
                game?.phase == GamePhase.Playing &&
                state.overlay == null,
        undoEnabled = state.bootstrap == BootstrapState.Ready &&
                state.visibility == MiniAppVisibility.ACTIVE &&
                game?.phase == GamePhase.Playing &&
                game.undo != null &&
                state.activeTransition == null &&
                state.overlay == null,
        tutorialVisible = state.bootstrap == BootstrapState.Ready && !state.tutorialSeen,
        overlay = state.overlay,
        persistenceStatus = when {
            state.persistenceDirty -> PlayingComponent.PersistenceStatus.Dirty
            state.requestedRevision > state.durableRevision -> PlayingComponent.PersistenceStatus.Saving
            else -> PlayingComponent.PersistenceStatus.Clean
        },
    )
}