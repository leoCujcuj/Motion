package com.example.motion.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

object MotionIcons {

    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "Play",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(8.2f, 5.6f)
                lineTo(18.6f, 12.0f)
                lineTo(8.2f, 18.4f)
                close()
            }
        }.build()
    }

    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pause",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero
            ) {
                // Barra izquierda redondeada
                moveTo(7.5f, 5.5f)
                lineTo(9.8f, 5.5f)
                quadTo(10.6f, 5.5f, 10.6f, 6.3f)
                lineTo(10.6f, 17.7f)
                quadTo(10.6f, 18.5f, 9.8f, 18.5f)
                lineTo(7.5f, 18.5f)
                quadTo(6.7f, 18.5f, 6.7f, 17.7f)
                lineTo(6.7f, 6.3f)
                quadTo(6.7f, 5.5f, 7.5f, 5.5f)
                close()

                // Barra derecha redondeada
                moveTo(14.2f, 5.5f)
                lineTo(16.5f, 5.5f)
                quadTo(17.3f, 5.5f, 17.3f, 6.3f)
                lineTo(17.3f, 17.7f)
                quadTo(17.3f, 18.5f, 16.5f, 18.5f)
                lineTo(14.2f, 18.5f)
                quadTo(13.4f, 18.5f, 13.4f, 17.7f)
                lineTo(13.4f, 6.3f)
                quadTo(13.4f, 5.5f, 14.2f, 5.5f)
                close()
            }
        }.build()
    }

    val Gear: ImageVector by lazy {
        ImageVector.Builder(
            name = "Gear",
            defaultWidth = 22.dp,
            defaultHeight = 22.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Anillo exterior de engranaje con 8 dientes precisos
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.85f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                val cx = 12f
                val cy = 12f
                val rInner = 6.9f
                val rOuter = 9.2f
                val teeth = 8
                val step = (2.0 * Math.PI / teeth).toFloat()
                val toothHalf = step * 0.19f
                val slope = step * 0.08f

                for (i in 0 until teeth) {
                    val base = i * step
                    val a1 = base - toothHalf - slope
                    val a2 = base - toothHalf
                    val a3 = base + toothHalf
                    val a4 = base + toothHalf + slope
                    val nextA1 = (i + 1) * step - toothHalf - slope

                    if (i == 0) {
                        moveTo(cx + rInner * cos(a1), cy + rInner * sin(a1))
                    }
                    lineTo(cx + rOuter * cos(a2), cy + rOuter * sin(a2))
                    lineTo(cx + rOuter * cos(a3), cy + rOuter * sin(a3))
                    lineTo(cx + rInner * cos(a4), cy + rInner * sin(a4))
                    lineTo(cx + rInner * cos(nextA1), cy + rInner * sin(nextA1))
                }
                close()
            }
            // Círculo central
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.85f
            ) {
                moveTo(15.1f, 12f)
                arcTo(3.1f, 3.1f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8.9f, 12f)
                arcTo(3.1f, 3.1f, 0f, isMoreThanHalf = true, isPositiveArc = true, 15.1f, 12f)
                close()
            }
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Check",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5.5f, 12.6f)
                lineTo(10.0f, 17.1f)
                lineTo(18.8f, 7.4f)
            }
        }.build()
    }

    val QuickTile: ImageVector by lazy {
        ImageVector.Builder(
            name = "QuickTile",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                // Cuadrantes superiores e inferiores tipo Quick Settings
                moveTo(4f, 5.5f)
                lineTo(10f, 5.5f)
                lineTo(10f, 10.5f)
                lineTo(4f, 10.5f)
                close()

                moveTo(14f, 5.5f)
                lineTo(20f, 5.5f)
                lineTo(20f, 10.5f)
                lineTo(14f, 10.5f)
                close()

                moveTo(4f, 14f)
                lineTo(10f, 14f)
                lineTo(10f, 19f)
                lineTo(4f, 19f)
                close()

                // Signo + en el cuarto cuadrante
                moveTo(17f, 13.8f)
                lineTo(17f, 19.2f)
                moveTo(14.3f, 16.5f)
                lineTo(19.7f, 16.5f)
            }
        }.build()
    }
}
