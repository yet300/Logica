package ge.yet.game.fallingblocks.ui.screen.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ge.yet.game.fallingblocks.component.game.FallingBlocksComponent
import ge.yet.game.fallingblocks.domain.engine.cells
import ge.yet.game.fallingblocks.domain.model.ActivePiece
import ge.yet.game.fallingblocks.domain.model.Board
import ge.yet.game.fallingblocks.domain.model.Cell
import ge.yet.game.fallingblocks.domain.model.Rotation
import ge.yet.game.fallingblocks.domain.model.Tetromino
import ge.yet.game.fallingblocks.generated.resources.Res
import ge.yet.game.fallingblocks.generated.resources.board_description
import ge.yet.game.fallingblocks.generated.resources.level_label
import ge.yet.game.fallingblocks.generated.resources.lines_label
import ge.yet.game.fallingblocks.generated.resources.next_label
import ge.yet.game.fallingblocks.generated.resources.score_label
import ge.yet.game.fallingblocks.ui.board.BoardGeometry
import ge.yet.game.fallingblocks.ui.board.CrtBoard
import ge.yet.game.fallingblocks.ui.board.colors
import ge.yet.game.fallingblocks.ui.input.GestureEvent
import ge.yet.game.fallingblocks.ui.input.fallingBlocksGestures
import ge.yet.game.fallingblocks.ui.motion.rememberFallingBlocksMotionPolicy
import ge.yet.game.fallingblocks.ui.tutorial.TutorialOverlay
import org.jetbrains.compose.resources.stringResource

internal object FallingBlocksTestTags {
    const val Board = "falling_blocks_board"
    const val Preview = "falling_blocks_preview"
    const val Level = "falling_blocks_level"
    const val Lines = "falling_blocks_lines"
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

        val geometry = BoardGeometry.fit(maxWidth.value, maxHeight.value, edgeInset = 12f)
        val boardWidth = geometry.width.dp
        val boardHeight = geometry.height.dp
        val cellSize = boardWidth / Board.WIDTH
        val description = stringResource(
            Res.string.board_description,
            state.score,
            state.level,
            state.lines,
        )

        CrtBoard(
            state = state,
            spatialEffectsEnabled = motion.spatialMotionEnabled,
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
        )

        val tutorialProgress = model.tutorialProgress
        if (tutorialProgress != null) {
            TutorialOverlay(
                progress = tutorialProgress,
                reducedMotion = !motion.spatialMotionEnabled,
                modifier = Modifier.size(boardWidth, boardHeight),
            )
        } else {
            Hud(
                score = state.score,
                level = state.level,
                lines = state.lines,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
            )
            Preview(
                pieces = state.preview,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun Hud(score: Long, level: Int, lines: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f),
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Metric(stringResource(Res.string.score_label), score.toString())
        Metric(
            stringResource(Res.string.level_label),
            level.toString(),
            Modifier.testTag(FallingBlocksTestTags.Level),
        )
        Metric(
            stringResource(Res.string.lines_label),
            lines.toString(),
            Modifier.testTag(FallingBlocksTestTags.Lines),
        )
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Preview(pieces: List<Tetromino>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .background(
                scheme.surfaceContainer.copy(alpha = 0.88f),
                RoundedCornerShape(12.dp),
            )
            .padding(8.dp)
            .testTag(FallingBlocksTestTags.Preview),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(Res.string.next_label),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        pieces.take(5).forEach { type ->
            Canvas(Modifier.width(48.dp).height(28.dp)) {
                val cells = ActivePiece(type, Rotation.SPAWN, Cell(0, 0)).cells()
                val minX = cells.minOf { it.x }
                val maxX = cells.maxOf { it.x }
                val minY = cells.minOf { it.y }
                val maxY = cells.maxOf { it.y }
                val cell = minOf(size.width / (maxX - minX + 1), size.height / (maxY - minY + 1))
                val pieceWidth = (maxX - minX + 1) * cell
                val pieceHeight = (maxY - minY + 1) * cell
                val origin = Offset((size.width - pieceWidth) / 2f, (size.height - pieceHeight) / 2f)
                val colors = type.colors(scheme)
                cells.forEach { block ->
                    val topLeft = origin + Offset((block.x - minX) * cell, (block.y - minY) * cell)
                    val inset = cell * 0.08f
                    drawRoundRect(
                        color = colors.fill,
                        topLeft = topLeft + Offset(inset, inset),
                        size = Size(cell - inset * 2f, cell - inset * 2f),
                        cornerRadius = CornerRadius(cell * 0.12f),
                    )
                    drawRoundRect(
                        color = colors.outline.copy(alpha = 0.66f),
                        topLeft = topLeft + Offset(inset, inset),
                        size = Size(cell - inset * 2f, cell - inset * 2f),
                        cornerRadius = CornerRadius(cell * 0.12f),
                        style = Stroke(maxOf(1f, cell * 0.045f)),
                    )
                }
            }
        }
    }
}
