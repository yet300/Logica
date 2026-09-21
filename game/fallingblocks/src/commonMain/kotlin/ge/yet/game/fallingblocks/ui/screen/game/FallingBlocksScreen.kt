package ge.yet.game.fallingblocks.ui.screen.game

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ge.yet.game.fallingblocks.component.game.FallingBlocksComponent
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.generated.resources.Res
import ge.yet.game.fallingblocks.generated.resources.board_description
import ge.yet.game.fallingblocks.ui.board.BoardGeometry
import ge.yet.game.fallingblocks.ui.board.CrtBoard
import ge.yet.game.fallingblocks.ui.board.FallingBlocksBoardEffects
import ge.yet.game.fallingblocks.ui.input.GestureEvent
import ge.yet.game.fallingblocks.ui.input.fallingBlocksGestures
import ge.yet.game.fallingblocks.ui.motion.rememberFallingBlocksMotionPolicy
import ge.yet.game.fallingblocks.ui.tutorial.TutorialOverlay
import org.jetbrains.compose.resources.stringResource

internal object FallingBlocksTestTags {
    const val Board = "falling_blocks_board"
    const val Preview = "falling_blocks_preview"
    const val PreviewPiece = "falling_blocks_preview_piece"
    const val Effects = "falling_blocks_effects"
}

@Composable
internal fun FallingBlocksScreen(
    component: FallingBlocksComponent,
    modifier: Modifier = Modifier,
) {
    val model by component.model.subscribeAsState()
    val state = model.game
    val motion = rememberFallingBlocksMotionPolicy()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (model.loading || state == null) {
            CircularProgressIndicator()
            return@BoxWithConstraints
        }

        val geometry = BoardGeometry.fit(
            viewportWidth = maxWidth.value,
            viewportHeight = maxHeight.value,
            edgeInset = 24f,
            supportReserve = if (maxHeight < 700.dp) 48f else 64f,
            maxBoardWidth = 340f,
        )
        val boardWidth = geometry.width.dp
        val boardHeight = geometry.height.dp
        val cellSize = boardWidth / Board.WIDTH
        val description = stringResource(
            Res.string.board_description,
            state.score,
            state.level,
            state.lines,
        )

        FallingBlocksBoardEffects(
            event = model.visualEvent,
            motionPolicy = motion,
            active = model.active,
            modifier = Modifier
                .size(boardWidth, boardHeight)
                .fallingBlocksGestures(
                    enabled = model.active && model.tutorialProgress?.complete != true,
                    cellSize = cellSize,
                ) { event ->
                    when (event) {
                        GestureEvent.Rotate -> component.rotate()
                        is GestureEvent.MoveHorizontal -> component.move(event.cells)
                        is GestureEvent.SoftDrop -> component.softDrop(event.cells)
                        GestureEvent.HardDrop -> component.hardDrop()
                    }
                }
                .testTag(FallingBlocksTestTags.Board)
                .semantics { contentDescription = description },
        ) {
            CrtBoard(
                state = state,
                spatialEffectsEnabled = motion.spatialMotionEnabled,
                modifier = Modifier.fillMaxSize(),
            )
        }

        val tutorialProgress = model.tutorialProgress
        if (tutorialProgress != null) {
            TutorialOverlay(
                progress = tutorialProgress,
                reducedMotion = !motion.spatialMotionEnabled,
                modifier = Modifier.size(boardWidth, boardHeight),
            )
        }
    }
}
