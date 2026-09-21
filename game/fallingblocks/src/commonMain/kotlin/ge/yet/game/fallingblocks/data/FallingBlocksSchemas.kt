package ge.yet.game.fallingblocks.data

import ge.yet.game.fallingblocks.domain.engine.BagState
import ge.yet.game.fallingblocks.domain.engine.LOCK_DELAY_MILLIS
import ge.yet.game.fallingblocks.domain.engine.MAX_LOCK_RESETS
import ge.yet.game.fallingblocks.domain.engine.RandomState
import ge.yet.game.fallingblocks.domain.engine.canOccupy
import ge.yet.game.fallingblocks.domain.engine.gravityMillis
import ge.yet.game.fallingblocks.domain.model.ActivePiece
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.FallingBlocksState
import ge.yet.game.fallingblocks.domain.model.GamePhase
import ge.yet.game.fallingblocks.domain.model.Rotation
import ge.yet.game.fallingblocks.domain.model.Tetromino
import ge.yet.game.miniapp.api.MiniAppSnapshotSpec
import kotlinx.serialization.Serializable

@Serializable
internal data class GameSnapshotV1(
    val version: Int = VERSION,
    val cells: List<Int>,
    val activeType: Int,
    val activeRotation: Int,
    val activeX: Int,
    val activeY: Int,
    val preview: List<Int>,
    val bag: List<Int>,
    val randomBits: Long,
    val score: Long,
    val level: Int,
    val lines: Int,
    val combo: Int,
    val backToBack: Boolean,
    val gravityRemainingMillis: Int,
    val lockRemainingMillis: Int,
    val lockResetCount: Int,
    val lastActionWasRotation: Boolean,
    val revivesUsed: Int,
    val runId: Long,
    val phase: Int,
) {
    companion object {
        const val VERSION: Int = 1

        fun from(state: FallingBlocksState): GameSnapshotV1 = GameSnapshotV1(
            cells = state.board.cells.map { piece -> piece?.let(::encodeTetromino) ?: EMPTY_CELL },
            activeType = encodeTetromino(state.active.type),
            activeRotation = encodeRotation(state.active.rotation),
            activeX = state.active.origin.x,
            activeY = state.active.origin.y,
            preview = state.preview.map(::encodeTetromino),
            bag = state.bag.remaining.map(::encodeTetromino),
            randomBits = state.bag.random.bits,
            score = state.score,
            level = state.level,
            lines = state.lines,
            combo = state.combo,
            backToBack = state.backToBack,
            gravityRemainingMillis = state.gravityRemainingMillis,
            lockRemainingMillis = state.lockRemainingMillis,
            lockResetCount = state.lockResetCount,
            lastActionWasRotation = state.lastActionWasRotation,
            revivesUsed = state.revivesUsed,
            runId = state.runId,
            phase = encodePhase(state.phase),
        )
    }
}

internal val FallingBlocksSnapshotSpec = MiniAppSnapshotSpec(
    serializer = GameSnapshotV1.serializer(),
    currentVersion = GameSnapshotV1.VERSION,
)

internal object FallingBlocksSchemas {
    fun toDomain(payload: GameSnapshotV1): Result<FallingBlocksState> = runCatching {
        require(payload.version == GameSnapshotV1.VERSION)
        require(payload.cells.size == Board.WIDTH * Board.TOTAL_HEIGHT)
        require(payload.preview.size == PREVIEW_SIZE)
        require(payload.bag.size <= Tetromino.entries.size)
        require(payload.bag.distinct().size == payload.bag.size)
        require(payload.score >= 0)
        require(payload.level >= 1)
        require(payload.lines >= 0)
        require(payload.level == payload.lines / 10 + 1)
        require(payload.combo >= -1)
        require(payload.gravityRemainingMillis in 0..gravityMillis(payload.level))
        require(payload.lockRemainingMillis in 0..LOCK_DELAY_MILLIS)
        require(payload.lockResetCount in 0..MAX_LOCK_RESETS)
        require(payload.revivesUsed in 0..1)
        require(payload.runId >= 0)

        val board = Board(payload.cells.map(::decodeCell))
        val active = ActivePiece(
            type = decodeTetromino(payload.activeType),
            rotation = decodeRotation(payload.activeRotation),
            origin = Cell(payload.activeX, payload.activeY),
        )
        val phase = decodePhase(payload.phase)
        val state = FallingBlocksState(
            board = board,
            active = active,
            preview = payload.preview.map(::decodeTetromino),
            bag = BagState(payload.bag.map(::decodeTetromino), RandomState(payload.randomBits)),
            score = payload.score,
            level = payload.level,
            lines = payload.lines,
            combo = payload.combo,
            backToBack = payload.backToBack,
            gravityRemainingMillis = payload.gravityRemainingMillis,
            lockRemainingMillis = payload.lockRemainingMillis,
            lockResetCount = payload.lockResetCount,
            lastActionWasRotation = payload.lastActionWasRotation,
            revivesUsed = payload.revivesUsed,
            runId = payload.runId,
            phase = phase,
        )
        require(phase == GamePhase.TERMINAL || canOccupy(active, board))
        state
    }
}

private fun encodeTetromino(value: Tetromino): Int = when (value) {
    Tetromino.I -> 0
    Tetromino.J -> 1
    Tetromino.L -> 2
    Tetromino.O -> 3
    Tetromino.S -> 4
    Tetromino.T -> 5
    Tetromino.Z -> 6
}

private fun decodeTetromino(value: Int): Tetromino = when (value) {
    0 -> Tetromino.I
    1 -> Tetromino.J
    2 -> Tetromino.L
    3 -> Tetromino.O
    4 -> Tetromino.S
    5 -> Tetromino.T
    6 -> Tetromino.Z
    else -> throw IllegalArgumentException("Unsupported tetromino id: $value")
}

private fun decodeCell(value: Int): Tetromino? = if (value == EMPTY_CELL) null else decodeTetromino(value)

private fun encodeRotation(value: Rotation): Int = when (value) {
    Rotation.SPAWN -> 0
    Rotation.RIGHT -> 1
    Rotation.REVERSE -> 2
    Rotation.LEFT -> 3
}

private fun decodeRotation(value: Int): Rotation = when (value) {
    0 -> Rotation.SPAWN
    1 -> Rotation.RIGHT
    2 -> Rotation.REVERSE
    3 -> Rotation.LEFT
    else -> throw IllegalArgumentException("Unsupported rotation id: $value")
}

private fun encodePhase(value: GamePhase): Int = when (value) {
    GamePhase.PLAYING -> 0
    GamePhase.TERMINAL -> 1
}

private fun decodePhase(value: Int): GamePhase = when (value) {
    0 -> GamePhase.PLAYING
    1 -> GamePhase.TERMINAL
    else -> throw IllegalArgumentException("Unsupported phase id: $value")
}

private const val EMPTY_CELL: Int = -1
private const val PREVIEW_SIZE: Int = 5
