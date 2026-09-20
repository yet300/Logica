package ge.yet.game.fallingblocks.component.game.mapper

import ge.yet.game.fallingblocks.component.game.FallingBlocksComponent
import ge.yet.game.fallingblocks.component.game.store.FallingBlocksStore

internal val stateToModel: (FallingBlocksStore.State) -> FallingBlocksComponent.Model = { state ->
    FallingBlocksComponent.Model(
        game = state.game,
        loading = state.loading,
        tutorialSeen = state.tutorialSeen,
        bestScore = state.bestScore,
        active = state.active,
    )
}
