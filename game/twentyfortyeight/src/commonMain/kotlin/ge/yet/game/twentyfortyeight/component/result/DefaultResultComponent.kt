package ge.yet.game.twentyfortyeight.component.result

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import dev.zacsweers.metro.Inject
import ge.yet.game.twentyfortyeight.domain.model.ResultSnapshot

internal class DefaultResultComponent(
    snapshot: ResultSnapshot,
    private val onNewGame: () -> Unit,
) : ResultComponent {
    // Detached snapshot of the finished run, mirroring BlockBlastResultSnapshot:
    // the result screen never observes the live store.
    override val model: Value<ResultComponent.Model> = MutableValue(
        ResultComponent.Model(
            score = snapshot.score,
            bestScore = snapshot.bestScore,
            highestTile = snapshot.highestTile,
        ),
    )

    override fun onNewGameRequested() = onNewGame()
}

@Inject
internal class DefaultResultComponentFactory : ResultComponent.Factory {
    override fun create(
        snapshot: ResultSnapshot,
        onNewGame: () -> Unit,
    ): DefaultResultComponent = DefaultResultComponent(
        snapshot = snapshot,
        onNewGame = onNewGame,
    )
}
