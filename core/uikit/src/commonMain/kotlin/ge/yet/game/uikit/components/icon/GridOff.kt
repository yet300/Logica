package ge.yet.game.uikit.components.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GridOff: ImageVector
    get() {
        if (_GridOff != null) {
            return _GridOff!!
        }
        _GridOff = ImageVector.Builder(
            name = "GridOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(333f, 760f)
                verticalLineToRelative(-133f)
                lineTo(200f, 627f)
                verticalLineToRelative(133f)
                horizontalLineToRelative(133f)
                close()
                moveTo(547f, 760f)
                verticalLineToRelative(-100f)
                lineToRelative(-33f, -33f)
                lineTo(413f, 627f)
                verticalLineToRelative(133f)
                horizontalLineToRelative(134f)
                close()
                moveTo(627f, 760f)
                close()
                moveTo(743f, 627f)
                close()
                moveTo(333f, 547f)
                verticalLineToRelative(-101f)
                lineToRelative(-33f, -33f)
                lineTo(200f, 413f)
                verticalLineToRelative(134f)
                horizontalLineToRelative(133f)
                close()
                moveTo(413f, 547f)
                close()
                moveTo(760f, 547f)
                verticalLineToRelative(-134f)
                lineTo(627f, 413f)
                verticalLineToRelative(99f)
                lineToRelative(35f, 35f)
                horizontalLineToRelative(98f)
                close()
                moveTo(529f, 413f)
                close()
                moveTo(200f, 333f)
                close()
                moveTo(547f, 333f)
                verticalLineToRelative(-133f)
                lineTo(413f, 200f)
                verticalLineToRelative(98f)
                lineToRelative(35f, 35f)
                horizontalLineToRelative(99f)
                close()
                moveTo(760f, 333f)
                verticalLineToRelative(-133f)
                lineTo(627f, 200f)
                verticalLineToRelative(133f)
                horizontalLineToRelative(133f)
                close()
                moveTo(316f, 200f)
                close()
                moveTo(840f, 725f)
                lineTo(235f, 120f)
                horizontalLineToRelative(525f)
                quadToRelative(33f, 0f, 56.5f, 23.5f)
                reflectiveQuadTo(840f, 200f)
                verticalLineToRelative(525f)
                close()
                moveTo(200f, 840f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(120f, 760f)
                verticalLineToRelative(-640f)
                lineToRelative(720f, 720f)
                lineTo(200f, 840f)
                close()
                moveTo(819f, 932f)
                lineTo(28f, 140f)
                lineToRelative(56f, -56f)
                lineTo(876f, 876f)
                lineToRelative(-57f, 56f)
                close()
            }
        }.build()

        return _GridOff!!
    }

@Suppress("ObjectPropertyName")
private var _GridOff: ImageVector? = null
