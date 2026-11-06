package com.example.myled.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.myled.ui.theme.NeuTheme

val NeuShapeSmall = RoundedCornerShape(10.dp)
val NeuShapeMedium = RoundedCornerShape(14.dp)

/** Выпуклая неоморфная поверхность: светлая тень сверху-слева, тёмная снизу-справа. */
fun Modifier.neuRaised(shape: Shape = NeuShapeMedium, elevation: Dp = 6.dp): Modifier = composed {
    val c = NeuTheme.colors
    this
        .dropShadow(shape, Shadow(radius = elevation * 2, color = c.darkShadow, offset = DpOffset(elevation, elevation)))
        .dropShadow(shape, Shadow(radius = elevation * 2, color = c.lightShadow, offset = DpOffset(-elevation, -elevation)))
        .clip(shape)
        .background(c.background)
}

/** Вдавленная неоморфная поверхность. */
fun Modifier.neuPressed(shape: Shape = NeuShapeMedium, depth: Dp = 4.dp): Modifier = composed {
    val c = NeuTheme.colors
    this
        .clip(shape)
        .background(c.background)
        .innerShadow(shape, Shadow(radius = depth * 2, color = c.darkShadow, offset = DpOffset(depth, depth)))
        .innerShadow(shape, Shadow(radius = depth * 2, color = c.lightShadow, offset = DpOffset(-depth, -depth)))
}

fun Modifier.neu(pressed: Boolean, shape: Shape = NeuShapeMedium, elevation: Dp = 6.dp): Modifier =
    if (pressed) neuPressed(shape, elevation * 0.66f) else neuRaised(shape, elevation)

/**
 * Неоморфная кнопка. [selected] — «включённое» состояние (вдавлена), при нажатии кнопка
 * тоже вдавливается.
 */
@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    shape: Shape = NeuShapeMedium,
    elevation: Dp = 5.dp,
    contentDescription: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .neu(pressed = selected || isPressed, shape = shape, elevation = elevation)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
