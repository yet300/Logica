package ge.yet.game.fallingblocks.ui.tutorial

import ge.yet.game.fallingblocks.domain.model.GameFact

internal enum class TutorialStep {
    ROTATE,
    MOVE,
    SOFT_DROP,
    HARD_DROP,
}

internal data class TutorialProgress(
    val step: TutorialStep,
    val complete: Boolean = false,
) {
    fun accept(facts: List<GameFact>): TutorialProgress {
        if (complete || facts.none(::matches)) return this
        return when (step) {
            TutorialStep.ROTATE -> copy(step = TutorialStep.MOVE)
            TutorialStep.MOVE -> copy(step = TutorialStep.SOFT_DROP)
            TutorialStep.SOFT_DROP -> copy(step = TutorialStep.HARD_DROP)
            TutorialStep.HARD_DROP -> copy(complete = true)
        }
    }

    private fun matches(fact: GameFact): Boolean = when (step) {
        TutorialStep.ROTATE -> fact == GameFact.Rotated
        TutorialStep.MOVE -> fact is GameFact.Moved && fact.horizontalCells != 0
        TutorialStep.SOFT_DROP -> fact is GameFact.Moved && fact.downwardCells > 0
        TutorialStep.HARD_DROP -> fact is GameFact.HardDropped
    }

    companion object {
        fun initial(): TutorialProgress = TutorialProgress(TutorialStep.ROTATE)
    }
}
