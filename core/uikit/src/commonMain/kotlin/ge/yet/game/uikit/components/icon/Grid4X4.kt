package ge.yet.game.uikit.components.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Grid4X4: ImageVector
    get() {
        if (_Grid4X4 != null) {
            return _Grid4X4!!
        }
        _Grid4X4 = ImageVector.Builder(
            name = "Grid4X4",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(200f, 880f)
                verticalLineToRelative(-120f)
                lineTo(80f, 760f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(120f)
                verticalLineToRelative(-160f)
                lineTo(80f, 520f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(120f)
                verticalLineToRelative(-160f)
                lineTo(80f, 280f)
                verticalLineToRelative(-80f)
                horizontalLineToRelative(120f)
                verticalLineToRelative(-120f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(120f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-120f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(120f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-120f)
                horizontalLineToRelative(80f)
                verticalLineToRelative(120f)
                horizontalLineToRelative(120f)
                verticalLineToRelative(80f)
                lineTo(760f, 280f)
                verticalLineToRelative(160f)
                horizontalLineToRelative(120f)
                verticalLineToRelative(80f)
                lineTo(760f, 520f)
                verticalLineToRelative(160f)
                horizontalLineToRelative(120f)
                verticalLineToRelative(80f)
                lineTo(760f, 760f)
                verticalLineToRelative(120f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(-120f)
                lineTo(520f, 760f)
                verticalLineToRelative(120f)
                horizontalLineToRelative(-80f)
                verticalLineToRelative(-120f)
                lineTo(280f, 760f)
                verticalLineToRelative(120f)
                horizontalLineToRelative(-80f)
                close()
                moveTo(280f, 680f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-160f)
                lineTo(280f, 520f)
                verticalLineToRelative(160f)
                close()
                moveTo(520f, 680f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-160f)
                lineTo(520f, 520f)
                verticalLineToRelative(160f)
                close()
                moveTo(280f, 440f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-160f)
                lineTo(280f, 280f)
                verticalLineToRelative(160f)
                close()
                moveTo(520f, 440f)
                horizontalLineToRelative(160f)
                verticalLineToRelative(-160f)
                lineTo(520f, 280f)
                verticalLineToRelative(160f)
                close()
            }
        }.build()

        return _Grid4X4!!
    }

@Suppress("ObjectPropertyName")
private var _Grid4X4: ImageVector? = null
