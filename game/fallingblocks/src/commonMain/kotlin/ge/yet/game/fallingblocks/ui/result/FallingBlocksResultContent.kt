package ge.yet.game.fallingblocks.ui.result

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ge.yet.game.fallingblocks.component.result.ResultComponent
import ge.yet.game.fallingblocks.generated.resources.Res
import ge.yet.game.fallingblocks.generated.resources.advertisement_suffix
import ge.yet.game.fallingblocks.generated.resources.best_label
import ge.yet.game.fallingblocks.generated.resources.game_over_title
import ge.yet.game.fallingblocks.generated.resources.new_game_action
import ge.yet.game.fallingblocks.generated.resources.result_board_description
import ge.yet.game.fallingblocks.generated.resources.score_label
import ge.yet.game.fallingblocks.generated.resources.try_again_action
import ge.yet.game.fallingblocks.ui.board.CrtBoard
import ge.yet.game.miniapp.compose.MiniAppAdGate
import org.jetbrains.compose.resources.stringResource

internal object FallingBlocksResultTags {
    const val Root = "falling_blocks_result"
    const val Board = "falling_blocks_result_board"
    const val Primary = "falling_blocks_result_primary"
}

@Composable
internal fun FallingBlocksResultContent(
    component: ResultComponent,
    interstitialGate: MiniAppAdGate,
    modifier: Modifier = Modifier,
) {
    val model by component.model.subscribeAsState()
    FallingBlocksResultContent(
        model = model,
        advertisementExpected = interstitialGate.willShowAd,
        onPrimary = { component.onPrimaryClicked(interstitialGate.request) },
        modifier = modifier,
    )
}

@Composable
internal fun FallingBlocksResultContent(
    model: ResultComponent.Model,
    advertisementExpected: Boolean,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().testTag(FallingBlocksResultTags.Root),
    ) {
        val budget = resultLayoutBudget(maxWidth.value, maxHeight.value)
        if (budget.usesTwoPanes) {
            LandscapeResultLayout(model, budget, advertisementExpected, onPrimary)
        } else {
            PortraitResultLayout(model, budget, advertisementExpected, onPrimary)
        }
    }
}

@Composable
private fun PortraitResultLayout(
    model: ResultComponent.Model,
    budget: ResultLayoutBudget,
    advertisementExpected: Boolean,
    onPrimary: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(
            horizontal = budget.policy.horizontalPaddingDp.dp,
            vertical = budget.policy.verticalPaddingDp.dp,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ResultTitle(budget.policy)
        Spacer(Modifier.height(budget.policy.sectionSpacingDp.dp))
        ResultBoard(model, budget)
        Spacer(Modifier.height(budget.policy.sectionSpacingDp.dp))
        ResultActions(model, budget.policy, advertisementExpected, onPrimary)
    }
}

@Composable
private fun LandscapeResultLayout(
    model: ResultComponent.Model,
    budget: ResultLayoutBudget,
    advertisementExpected: Boolean,
    onPrimary: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(
            horizontal = budget.policy.horizontalPaddingDp.dp,
            vertical = budget.policy.verticalPaddingDp.dp,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            ResultBoard(model, budget)
        }
        Spacer(Modifier.width(budget.policy.sectionSpacingDp.dp))
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ResultTitle(budget.policy)
            Spacer(Modifier.height(budget.policy.sectionSpacingDp.dp))
            ResultActions(model, budget.policy, advertisementExpected, onPrimary)
        }
    }
}

@Composable
private fun ResultTitle(policy: ResultLayoutPolicy) {
    Text(
        text = stringResource(Res.string.game_over_title),
        style = when {
            policy.isUltraCompact -> MaterialTheme.typography.headlineSmall
            policy.isCompact -> MaterialTheme.typography.headlineMedium
            else -> MaterialTheme.typography.headlineLarge
        },
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ResultBoard(model: ResultComponent.Model, budget: ResultLayoutBudget) {
    val occupied = model.snapshot.boardCells.count { it != null }
    val boardDescription = stringResource(Res.string.result_board_description, occupied)
    CrtBoard(
        board = model.snapshot.board(),
        active = model.snapshot.activePiece(),
        spatialEffectsEnabled = false,
        showGhost = false,
        modifier = Modifier
            .size(budget.boardWidthDp.dp, budget.boardHeightDp.dp)
            .testTag(FallingBlocksResultTags.Board)
            .semantics {
                contentDescription = boardDescription
            },
    )
}

@Composable
private fun ResultActions(
    model: ResultComponent.Model,
    policy: ResultLayoutPolicy,
    advertisementExpected: Boolean,
    onPrimary: () -> Unit,
) {
    ResultCard(
        model = model,
        scoreLabel = stringResource(Res.string.score_label),
        bestLabel = stringResource(Res.string.best_label),
        continueLabel = stringResource(Res.string.try_again_action),
        newGameLabel = stringResource(Res.string.new_game_action),
        advertisementLabel = if (advertisementExpected) {
            stringResource(Res.string.advertisement_suffix)
        } else {
            null
        },
        layoutPolicy = policy,
        onPrimary = onPrimary,
        modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
    )
}
