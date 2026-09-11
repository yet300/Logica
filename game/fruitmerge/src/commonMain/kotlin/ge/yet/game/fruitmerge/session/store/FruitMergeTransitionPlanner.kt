package ge.yet.game.fruitmerge.session.store

import ge.yet.game.fruitmerge.domain.engine.FruitMergeEngine
import ge.yet.game.fruitmerge.domain.model.FruitBody
import ge.yet.game.fruitmerge.domain.model.FruitLevel
import ge.yet.game.fruitmerge.domain.model.FruitMergeState
import ge.yet.game.fruitmerge.domain.model.RunPhase

// Pure transition decisions: no dispatch, publish, scope or persistence.
// The executor owns orchestration and side effects.
internal object FruitMergeTransitionPlanner {
    fun stepLabels(before: FruitMergeState, after: FruitMergeState): List<FruitMergeStore.Label> {
        val labels = ArrayList<FruitMergeStore.Label>(4)
        labels += landingLabels(before, after)
        labels += mergeLabels(before, after)
        shakePulse(before)?.let(labels::add)
        if (before.dangerSeconds == 0f && after.dangerSeconds > 0f) {
            labels += FruitMergeStore.Label.DangerEntered
        }
        return labels
    }

    fun resultReached(before: FruitMergeState, after: FruitMergeState): Boolean =
        before.phase == RunPhase.PLAYING && after.phase == RunPhase.RESULT

    private fun landingLabels(
        before: FruitMergeState,
        after: FruitMergeState,
    ): List<FruitMergeStore.Label> {
        if (before.bodies.none { body -> !body.hasJoinedPile }) return emptyList()
        val afterById = after.bodies.associateBy { body -> body.id }
        return before.bodies
            .asSequence()
            .filterNot { body -> body.hasJoinedPile }
            .mapNotNull { body -> afterById[body.id] }
            .filter(FruitBody::hasJoinedPile)
            .sortedBy { body -> body.id }
            .map { body -> FruitMergeStore.Label.FruitLanded(body.level, body.position) }
            .toList()
    }

    private fun mergeLabels(
        before: FruitMergeState,
        after: FruitMergeState,
    ): List<FruitMergeStore.Label> {
        if (before.score == after.score) return emptyList()
        val labels = ArrayList<FruitMergeStore.Label>()
        val previousIds = before.bodies.asSequence().map { body -> body.id }.toHashSet()
        after.bodies
            .asSequence()
            .filterNot { body -> body.id in previousIds }
            .sortedBy { body -> body.id }
            .mapTo(labels) { body -> FruitMergeStore.Label.MergeResolved(body.level, body.position) }

        val survivingIds = after.bodies.asSequence().map { body -> body.id }.toHashSet()
        val removedMelons = before.bodies.filter { body ->
            body.level == FruitLevel.WATERMELON && body.id !in survivingIds
        }.sortedBy { body -> body.id }
        removedMelons.chunked(2).forEach { pair ->
            if (pair.size == 2) {
                labels += FruitMergeStore.Label.MergeResolved(
                    level = FruitLevel.WATERMELON,
                    position = (pair[0].position + pair[1].position) * 0.5f,
                )
            }
        }
        return labels
    }

    private fun shakePulse(before: FruitMergeState): FruitMergeStore.Label.ShakePulse? {
        if (before.shakeStepsRemaining <= 0) return null
        if (
            (FruitMergeEngine.SHAKE_DURATION_STEPS - before.shakeStepsRemaining) %
            FruitMergeEngine.SHAKE_IMPULSE_INTERVAL_STEPS != 0
        ) {
            return null
        }
        val pulseIndex =
            (FruitMergeEngine.SHAKE_DURATION_STEPS - before.shakeStepsRemaining) /
                FruitMergeEngine.SHAKE_IMPULSE_INTERVAL_STEPS
        return FruitMergeStore.Label.ShakePulse(pulseIndex)
    }
}
