package ge.yet.game.fruitmerge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ge.yet.game.fruitmerge.component.result.FruitMergeResultSnapshot
import ge.yet.game.fruitmerge.domain.model.FruitMergeState
import ge.yet.game.fruitmerge.domain.model.RunPhase
import ge.yet.game.fruitmerge.generated.resources.Res
import ge.yet.game.fruitmerge.generated.resources.best_score
import ge.yet.game.fruitmerge.generated.resources.board_description
import ge.yet.game.fruitmerge.generated.resources.danger_line
import ge.yet.game.fruitmerge.generated.resources.game_over
import ge.yet.game.fruitmerge.generated.resources.game_over_supporting
import ge.yet.game.fruitmerge.generated.resources.new_game
import ge.yet.game.fruitmerge.generated.resources.score
import ge.yet.game.uikit.components.score.compactScore
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun FruitMergeResultContent(
    snapshot: FruitMergeResultSnapshot,
    faceTimeSeconds: Float,
    reducedMotion: Boolean,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scoreState = scoreCardState(snapshot.bestScore, snapshot.bestImprovedInRun)
    val palette = rememberFruitMergePalette()
    // Frozen terminal board: physics already settled, the crate keeps the
    // exact pile that crossed the danger line so the loss reason is visible.
    val frozenBoard = FruitMergeState(
        bodies = snapshot.bodies,
        score = snapshot.score,
        bestScore = snapshot.bestScore,
        bestImprovedInRun = snapshot.bestImprovedInRun,
        dangerSeconds = snapshot.dangerSeconds,
        runOrdinal = snapshot.runOrdinal,
        phase = RunPhase.RESULT,
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .semantics { testTag = FruitMergeTestTags.Result },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.game_over),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(Res.string.game_over_supporting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        FruitMergeBoard(
            game = frozenBoard,
            faceTimeSeconds = faceTimeSeconds,
            reducedMotion = reducedMotion,
            boardDescription = stringResource(
                Res.string.board_description,
                snapshot.score,
                snapshot.bodies.size,
            ),
            dangerDescription = stringResource(Res.string.danger_line),
            onClearTarget = {},
            showPreview = false,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .semantics { testTag = FruitMergeTestTags.Board },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                24.dp,
                Alignment.CenterHorizontally,
            ),
        ) {
            if (scoreState != ScoreCardState.BEST_ONLY) {
                ResultValue(
                    label = stringResource(Res.string.score),
                    value = snapshot.score,
                    tag = FruitMergeTestTags.ResultScore,
                )
            }
            if (scoreState != ScoreCardState.SCORE_ONLY) {
                ResultValue(
                    label = if (scoreState == ScoreCardState.BEST_ONLY) {
                        "♛ " + stringResource(Res.string.best_score)
                    } else {
                        stringResource(Res.string.best_score)
                    },
                    value = snapshot.bestScore,
                    tag = FruitMergeTestTags.ResultBest,
                )
            }
        }
        Button(
            onClick = onNewGame,
            modifier = Modifier
                .widthIn(min = 200.dp)
                .padding(top = 12.dp, bottom = 4.dp)
                .semantics { testTag = FruitMergeTestTags.NewGame },
            colors = ButtonDefaults.buttonColors(
                containerColor = palette.coral,
                contentColor = Color.White,
            ),
        ) {
            Text(stringResource(Res.string.new_game), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ResultValue(label: String, value: Long, tag: String) {
    Column(
        modifier = Modifier.semantics {
            testTag = tag
            contentDescription = "$label $value"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall)
        Text(compactScore(value), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
    }
}
