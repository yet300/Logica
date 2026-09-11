package ge.yet.game.twentyfortyeight.domain.repository

import ge.yet.game.miniapp.api.MiniAppStorage
import ge.yet.game.twentyfortyeight.domain.model.LoadResult

internal fun interface GameSnapshotLoader {
    suspend fun load(storage: MiniAppStorage): LoadResult
}
