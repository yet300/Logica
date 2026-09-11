package ge.yet.game.twentyfortyeight.component.result

import com.arkivanov.decompose.value.Value
import ge.yet.game.twentyfortyeight.engine.ResultSnapshot

internal interface ResultComponent {
    val model: Value<Model>

    fun onNewGameRequested()

    data class Model(
        val score: Long,
        val bestScore: Long,
        val highestTile: Long,
    )

    fun interface Factory {
        fun create(
            snapshot: ResultSnapshot,
            onNewGame: () -> Unit,
        ): ResultComponent
    }
}
