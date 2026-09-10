package ge.yet.game.uikit.components.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Numbers: ImageVector
    get() {
        if (_Numbers != null) {
            return _Numbers!!
        }
        _Numbers = ImageVector.Builder(
            name = "Numbers",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveToRelative(240f, 800f)
                lineToRelative(40f, -160f)
                lineTo(120f, 640f)
                lineToRelative(20f, -80f)
                horizontalLineToRelative(160f)
                lineToRelative(40f, -160f)
                lineTo(180f, 400f)
                lineToRelative(20f, -80f)
                horizontalLineToRelative(160f)
                lineToRelative(40f, -160f)
                horizontalLineToRelative(80f)
                lineToRelative(-40f, 160f)
                horizontalLineToRelative(160f)
                lineToRelative(40f, -160f)
                horizontalLineToRelative(80f)
                lineToRelative(-40f, 160f)
                horizontalLineToRelative(160f)
                lineToRelative(-20f, 80f)
                lineTo(660f, 400f)
                lineToRelative(-40f, 160f)
                horizontalLineToRelative(160f)
                lineToRelative(-20f, 80f)
                lineTo(600f, 640f)
                lineToRelative(-40f, 160f)
                horizontalLineToRelative(-80f)
                lineToRelative(40f, -160f)
                lineTo(360f, 640f)
                lineToRelative(-40f, 160f)
                horizontalLineToRelative(-80f)
                close()
                moveTo(380f, 560f)
                horizontalLineToRelative(160f)
                lineToRelative(40f, -160f)
                lineTo(420f, 400f)
                lineToRelative(-40f, 160f)
                close()
            }
        }.build()

        return _Numbers!!
    }

@Suppress("ObjectPropertyName")
private var _Numbers: ImageVector? = null
