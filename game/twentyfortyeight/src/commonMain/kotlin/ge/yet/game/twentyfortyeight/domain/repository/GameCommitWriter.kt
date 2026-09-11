package ge.yet.game.twentyfortyeight.domain.repository

import ge.yet.game.miniapp.api.MiniAppStorage
import ge.yet.game.twentyfortyeight.domain.model.GameCommit

internal fun interface GameCommitWriter {
    suspend fun commit(storage: MiniAppStorage, commit: GameCommit)
}
