package ge.yet.game.fruitmerge.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import ge.yet.game.fruitmerge.domain.model.FruitLevel
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FruitRendererTest {
    @Test
    fun `face stays upright while the body takes the tilt`() {
        runComposeUiTest {
            setContent {
                FruitCanvas(angleRadians = 0f, tag = "fruit_zero")
                FruitCanvas(angleRadians = PI.toFloat() / 2f, tag = "fruit_quarter_turn")
            }

            val zero = onNodeWithTag("fruit_zero").captureToImage()
            val turned = onNodeWithTag("fruit_quarter_turn").captureToImage()

            // APPLE has open PROUD eyes at (±0.28R, -0.02R). A 90° body tilt must
            // not drag them along: both eyes stay dark in both renders.
            assertTrue(eyeIsDark(zero, left = true), "Left eye must be drawn at rest")
            assertTrue(eyeIsDark(zero, left = false), "Right eye must be drawn at rest")
            assertTrue(eyeIsDark(turned, left = true), "Left eye must stay put on a 90° tilt")
            assertTrue(eyeIsDark(turned, left = false), "Right eye must stay put on a 90° tilt")
        }
    }
}

@Composable
private fun FruitCanvas(
    angleRadians: Float,
    tag: String,
) {
    Canvas(Modifier.size(144.dp).testTag(tag)) {
        drawFruit(
            level = FruitLevel.APPLE,
            center = center,
            radius = size.minDimension * 0.30f,
            angleRadians = angleRadians,
            verticalVelocity = 0f,
            impact = 0f,
            facePhase = 1f,
            danger = DangerVisual(intensity = 0f, crying = false),
            alpha = 1f,
        )
    }
}

/** Samples a small patch around the expected eye position; the white eye-shine dot sits near the eye center, so the darkest pixel decides. Face ink (0xFF471111) is dark, body (0xE53935) is bright red. */
private fun eyeIsDark(image: ImageBitmap, left: Boolean): Boolean {
    val pixels = image.toPixelMap()
    val cx = pixels.width / 2f
    val cy = pixels.height / 2f
    val radius = minOf(pixels.width, pixels.height) * 0.30f
    val eyeX = cx + (if (left) -0.28f else 0.28f) * radius
    val eyeY = cy - 0.02f * radius
    val step = radius * 0.05f
    var minRed = Float.POSITIVE_INFINITY
    var minGreen = Float.POSITIVE_INFINITY
    for (dx in listOf(-step, 0f, step)) {
        for (dy in listOf(-step, 0f, step)) {
            val x = (eyeX + dx).toInt().coerceIn(0, pixels.width - 1)
            val y = (eyeY + dy).toInt().coerceIn(0, pixels.height - 1)
            val color = pixels[x, y]
            minRed = minOf(minRed, color.red)
            minGreen = minOf(minGreen, color.green)
        }
    }
    return minRed < 0.5f && minGreen < 0.35f
}
