package ge.yet.game.fruitmerge.component.game.integration

import ge.yet.game.fruitmerge.component.game.FruitMergeComponent
import ge.yet.game.fruitmerge.component.game.TutorialStep
import ge.yet.game.fruitmerge.component.game.store.FruitMergeStore

internal fun FruitMergeStore.State.toModel(
    visible: Boolean,
    tutorialReady: Boolean,
    tutorialStep: TutorialStep?,
): FruitMergeComponent.Model = FruitMergeComponent.Model(
    game = game,
    initialized = initialized,
    visible = visible,
    tutorialReady = tutorialReady,
    tutorialStep = tutorialStep,
)
