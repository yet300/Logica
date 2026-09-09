package ge.yet.game.miniapp.storage

import com.app.common.AppDispatchers
import com.russhwolf.settings.ExperimentalSettingsApi
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.observable.makeObservable
import ge.yet.game.domain.repository.CrashlyticsRepository
import ge.yet.game.miniapp.api.MiniAppDataResetResult
import ge.yet.game.miniapp.api.MiniAppId
import ge.yet.game.miniapp.api.MiniAppLegacyStorageKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalSettingsApi::class)
class DefaultMiniAppDataResetterTest {
    @Test
    fun clear_removes_only_requested_namespaces_and_matching_legacy_aliases() = runTest {
        val settings = MapSettings(
            "host.theme" to "dark",
            "miniapp.game.blocks.score" to 7L,
            "miniapp.game.snake.score" to 9L,
            "blockblast.game_save" to "save",
            "blockblast.best_score" to 11L,
            "blockblast.tutorial_seen" to true,
        )
        val resetter = resetter(
            settings = settings,
            legacyKeys = setOf(blockBlastLegacyKeys()),
        )

        assertEquals(
            MiniAppDataResetResult.Success,
            resetter.clear(setOf(MiniAppId("game.blocks"))),
        )

        assertEquals(
            setOf(
                "host.theme",
                "miniapp.game.snake.score",
                "blockblast.game_save",
                "blockblast.best_score",
                "blockblast.tutorial_seen",
            ),
            settings.keys,
        )

        assertEquals(
            MiniAppDataResetResult.Success,
            resetter.clear(setOf(MiniAppId("game.blockblast"))),
        )
        assertEquals(setOf("host.theme", "miniapp.game.snake.score"), settings.keys)
    }

    @Test
    fun clear_does_not_remove_a_nested_miniapp_id_namespace() = runTest {
        val settings = MapSettings(
            "miniapp.game.foo.score" to 7L,
            "miniapp.game.foo.bar.score" to 9L,
        )

        assertEquals(
            MiniAppDataResetResult.Success,
            resetter(settings).clear(setOf(MiniAppId("game.foo"))),
        )

        assertEquals(setOf("miniapp.game.foo.bar.score"), settings.keys)
    }

    @Test
    fun repeated_reset_is_successful() = runTest {
        val settings = MapSettings("miniapp.game.blocks.score" to 7L)
        val resetter = resetter(settings)
        val ids = setOf(MiniAppId("game.blocks"))

        assertEquals(MiniAppDataResetResult.Success, resetter.clear(ids))
        assertEquals(MiniAppDataResetResult.Success, resetter.clear(ids))
    }

    private fun resetter(
        settings: MapSettings,
        legacyKeys: Set<MiniAppLegacyStorageKeys> = emptySet(),
        crashlytics: RecordingCrashlytics = RecordingCrashlytics(),
    ): DefaultMiniAppDataResetter = DefaultMiniAppDataResetter(
        settings = settings.makeObservable(),
        dispatchers = AppDispatchers(
            default = Dispatchers.Unconfined,
            io = Dispatchers.Unconfined,
        ),
        legacyStorageKeys = legacyKeys,
        crashlytics = crashlytics,
    )

    private fun blockBlastLegacyKeys() = MiniAppLegacyStorageKeys(
        miniAppId = MiniAppId("game.blockblast"),
        localToPhysicalKeys = mapOf(
            "game_save" to "blockblast.game_save",
            "best_score" to "blockblast.best_score",
            "tutorial_seen" to "blockblast.tutorial_seen",
        ),
    )

    private class RecordingCrashlytics : CrashlyticsRepository {
        val failedIds = mutableListOf<String>()

        override fun setUserID(id: String) = Unit
        override fun clearUserID() = Unit
        override fun setCustomValue(key: String, value: Any) = Unit
        override fun logException(throwable: Throwable) = Unit
        override fun logMessage(message: String) {
            failedIds += message.substringAfterLast(' ')
        }
    }
}
