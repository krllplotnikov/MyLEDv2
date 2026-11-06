package com.example.myled.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import com.example.myled.ui.theme.NeuTheme
import com.example.myled.ui.theme.Philosopher
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private val TrackThickness = 12.dp
private val ThumbSize = 26.dp

/**
 * Неоморфный ползунок 0..100. Вдавленная дорожка, оранжевое заполнение,
 * выпуклый бегунок с оранжевой точкой. В вертикальном режиме 100 — сверху.
 */
@Composable
fun NeuSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    label: String? = null,
) {
    val c = NeuTheme.colors
    val onChange by rememberUpdatedState(onValueChange)
    BoxWithConstraints(
        modifier = modifier
            .then(if (vertical) Modifier.width(ThumbSize + 8.dp) else Modifier.height(ThumbSize + 8.dp))
            .semantics {
                if (label != null) contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..100f)
                setProgress { onChange(it.roundToInt().coerceIn(0, 100)); true }
            }
            .pointerInput(vertical) {
                val thumbPx = ThumbSize.toPx()
                fun valueAt(pos: Offset): Int {
                    val length = (if (vertical) size.height else size.width) - thumbPx
                    val p = if (vertical) size.height - pos.y - thumbPx / 2 else pos.x - thumbPx / 2
                    return (p / length * 100f).roundToInt().coerceIn(0, 100)
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    onChange(valueAt(down.position))
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (change.positionChange() != Offset.Zero) {
                            change.consume()
                            onChange(valueAt(change.position))
                        }
                    }
                }
            },
    ) {
        val length = if (vertical) maxHeight else maxWidth
        val fraction = value.coerceIn(0, 100) / 100f
        val thumbPos: Dp = (length - ThumbSize) * fraction
        val fillLen: Dp = thumbPos + ThumbSize / 2
        val trackShape = RoundedCornerShape(50)

        if (vertical) {
            Box(
                Modifier.align(Alignment.Center).width(TrackThickness).fillMaxHeight()
                    .neuPressed(trackShape, 2.dp),
            )
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp).width(TrackThickness - 4.dp)
                    .height((fillLen - 2.dp).coerceAtLeast(0.dp)).clip(trackShape).background(c.accent),
            )
            Thumb(Modifier.align(Alignment.BottomCenter).offset(y = -thumbPos))
        } else {
            Box(
                Modifier.align(Alignment.CenterStart).height(TrackThickness).fillMaxWidth()
                    .neuPressed(trackShape, 2.dp),
            )
            Box(
                Modifier.align(Alignment.CenterStart).padding(start = 2.dp).height(TrackThickness - 4.dp)
                    .width((fillLen - 2.dp).coerceAtLeast(0.dp)).clip(trackShape).background(c.accent),
            )
            Thumb(Modifier.align(Alignment.CenterStart).offset(x = thumbPos))
        }
    }
}

@Composable
private fun Thumb(modifier: Modifier) {
    Box(modifier.size(ThumbSize).neuRaised(CircleShape, 3.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(NeuTheme.colors.accent))
    }
}

/** Подпись + горизонтальный ползунок, как в макете. */
@Composable
fun LabeledSlider(label: String, value: Int, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        NeuSlider(value, onValueChange, Modifier.fillMaxWidth(), label = label)
    }
}

// ---------------------------------------------------------------- color wheel

/**
 * Цветовой круг: угол — оттенок (красный справа, против часовой стрелки),
 * расстояние от центра — насыщенность.
 */
@Composable
fun ColorWheel(
    hue: Float,
    saturation: Int,
    onChange: (hue: Float, saturation: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = NeuTheme.colors
    val onChangeState by rememberUpdatedState(onChange)
    // Порядок по часовой стрелке от 3 часов: 0°, 300°, 240°, ... — т.е. оттенок растёт против часовой.
    val sweep = remember {
        listOf(0f, 300f, 240f, 180f, 120f, 60f, 0f).map { Color.hsv(it, 1f, 1f) }
    }
    Box(modifier.neuRaised(CircleShape, 10.dp)) {
        Canvas(
            Modifier.fillMaxSize().padding(4.dp)
                .pointerInput(Unit) {
                    fun emit(pos: Offset) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = pos.x - cx
                        val dy = cy - pos.y
                        var deg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                        if (deg < 0) deg += 360f
                        val r = minOf(cx, cy)
                        val sat = (hypot(dx, dy) / r * 100f).roundToInt().coerceIn(0, 100)
                        onChangeState(deg, sat)
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        emit(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            change.consume()
                            emit(change.position)
                        }
                    }
                },
        ) {
            val r = size.minDimension / 2f
            drawCircle(Brush.sweepGradient(sweep, center))
            drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), center, r))

            val rad = Math.toRadians(hue.toDouble())
            val dist = r * saturation / 100f
            val p = Offset(center.x + (cos(rad) * dist).toFloat(), center.y - (sin(rad) * dist).toFloat())
            drawCircle(Color.hsv(hue % 360f, saturation / 100f, 1f), 6.dp.toPx(), p)
            drawCircle(c.text, 6.dp.toPx(), p, style = Stroke(1.5.dp.toPx()))
        }
    }
}

// ---------------------------------------------------------------- icons

@Composable
fun PowerIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.minDimension
        val stroke = w * 0.09f
        val pad = w * 0.18f
        drawArc(
            color, startAngle = -60f, sweepAngle = 300f, useCenter = false,
            topLeft = Offset(pad, pad), size = Size(w - 2 * pad, w - 2 * pad),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        drawLine(color, Offset(w / 2, w * 0.08f), Offset(w / 2, w * 0.45f), stroke, StrokeCap.Round)
    }
}

@Composable
fun CloseIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.minDimension
        val s = 1.5.dp.toPx()
        drawLine(color, Offset(w * 0.25f, w * 0.25f), Offset(w * 0.75f, w * 0.75f), s, StrokeCap.Round)
        drawLine(color, Offset(w * 0.75f, w * 0.25f), Offset(w * 0.25f, w * 0.75f), s, StrokeCap.Round)
    }
}

// ---------------------------------------------------------------- logo

/** Горизонтальный логотип «MY▣LED». */
@Composable
fun MyLedLogo(modifier: Modifier = Modifier) {
    val c = NeuTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("MY", color = c.textMuted, fontSize = 22.sp, fontFamily = Philosopher)
        Box(
            Modifier.padding(horizontal = 2.dp).size(17.dp).clip(RoundedCornerShape(2.dp)).background(c.textMuted),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(c.background))
        }
        Text("LED", color = c.accent, fontSize = 22.sp, fontFamily = Philosopher)
    }
}

/** Квадратный логотип для окна «О приложении». */
@Composable
fun MyLedMainLogo(modifier: Modifier = Modifier) {
    val c = NeuTheme.colors
    Box(modifier.size(110.dp).background(Color(0xFF898D91)), contentAlignment = Alignment.Center) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(Color(0xFFE5E8E9)), contentAlignment = Alignment.Center) {
            Text(
                "MY\nLED", color = c.accent, fontSize = 30.sp, lineHeight = 30.sp, textAlign = TextAlign.Center,
                fontFamily = Philosopher, fontWeight = FontWeight.Normal,
            )
        }
    }
}
