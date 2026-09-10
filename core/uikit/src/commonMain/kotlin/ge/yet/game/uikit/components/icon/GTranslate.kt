package ge.yet.game.uikit.components.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val GTranslate: ImageVector
    get() {
        if (_GTranslate != null) {
            return _GTranslate!!
        }
        _GTranslate = ImageVector.Builder(
            name = "GTranslate",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveToRelative(480f, 880f)
                lineToRelative(-40f, -120f)
                lineTo(160f, 760f)
                quadToRelative(-33f, 0f, -56.5f, -23.5f)
                reflectiveQuadTo(80f, 680f)
                verticalLineToRelative(-520f)
                quadToRelative(0f, -33f, 23.5f, -56.5f)
                reflectiveQuadTo(160f, 80f)
                horizontalLineToRelative(240f)
                lineToRelative(35f, 120f)
                horizontalLineToRelative(365f)
                quadToRelative(35f, 0f, 57.5f, 22.5f)
                reflectiveQuadTo(880f, 280f)
                verticalLineToRelative(520f)
                quadToRelative(0f, 33f, -22.5f, 56.5f)
                reflectiveQuadTo(800f, 880f)
                lineTo(480f, 880f)
                close()
                moveTo(286f, 584f)
                quadToRelative(69f, 0f, 113.5f, -44.5f)
                reflectiveQuadTo(444f, 424f)
                quadToRelative(0f, -8f, -0.5f, -14.5f)
                reflectiveQuadTo(441f, 396f)
                lineTo(283f, 396f)
                verticalLineToRelative(62f)
                horizontalLineToRelative(89f)
                quadToRelative(-8f, 28f, -30.5f, 43.5f)
                reflectiveQuadTo(287f, 517f)
                quadToRelative(-39f, 0f, -67f, -28f)
                reflectiveQuadToRelative(-28f, -69f)
                quadToRelative(0f, -41f, 28f, -69f)
                reflectiveQuadToRelative(67f, -28f)
                quadToRelative(18f, 0f, 34f, 6.5f)
                reflectiveQuadToRelative(29f, 19.5f)
                lineToRelative(49f, -47f)
                quadToRelative(-21f, -22f, -50.5f, -34f)
                reflectiveQuadTo(286f, 256f)
                quadToRelative(-67f, 0f, -114.5f, 47.5f)
                reflectiveQuadTo(124f, 420f)
                quadToRelative(0f, 69f, 47.5f, 116.5f)
                reflectiveQuadTo(286f, 584f)
                close()
                moveTo(554f, 604f)
                lineTo(576f, 583f)
                quadToRelative(-14f, -17f, -25.5f, -33f)
                reflectiveQuadTo(528f, 516f)
                lineToRelative(26f, 88f)
                close()
                moveTo(604f, 553f)
                quadToRelative(28f, -33f, 42.5f, -63f)
                reflectiveQuadToRelative(19.5f, -47f)
                lineTo(507f, 443f)
                lineToRelative(12f, 42f)
                horizontalLineToRelative(40f)
                quadToRelative(8f, 15f, 19f, 32.5f)
                reflectiveQuadToRelative(26f, 35.5f)
                close()
                moveTo(520f, 840f)
                horizontalLineToRelative(280f)
                quadToRelative(18f, 0f, 29f, -11.5f)
                reflectiveQuadToRelative(11f, -28.5f)
                verticalLineToRelative(-520f)
                quadToRelative(0f, -18f, -11f, -29f)
                reflectiveQuadToRelative(-29f, -11f)
                lineTo(447f, 240f)
                lineToRelative(47f, 162f)
                horizontalLineToRelative(79f)
                verticalLineToRelative(-42f)
                horizontalLineToRelative(41f)
                verticalLineToRelative(42f)
                horizontalLineToRelative(146f)
                verticalLineToRelative(41f)
                horizontalLineToRelative(-51f)
                quadToRelative(-10f, 38f, -30f, 74f)
                reflectiveQuadToRelative(-47f, 67f)
                lineToRelative(109f, 107f)
                lineToRelative(-29f, 29f)
                lineToRelative(-108f, -108f)
                lineToRelative(-36f, 37f)
                lineToRelative(32f, 111f)
                lineToRelative(-80f, 80f)
                close()
            }
        }.build()

        return _GTranslate!!
    }

@Suppress("ObjectPropertyName")
private var _GTranslate: ImageVector? = null
