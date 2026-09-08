package com.example.npucourse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild

/**
 * A physics-driven glass navigation lens. Tab centres are measured from the real
 * layout so this remains correct when labels, font scale or the item count changes.
 */
@Suppress("DEPRECATION")
@Composable
fun LiquidGlassBottomBar(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val centres = remember(items) { mutableStateMapOf<String, Float>() }
    val lensX = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var gestureVelocity by remember { mutableFloatStateOf(0f) }
    var lastCrossed by remember { mutableStateOf<String?>(null) }
    val lensHeight = 48.dp
    val fallbackLensWidthPx = with(density) { 66.dp.toPx() }

    fun lensWidthPx(): Float {
        val sorted = items.mapNotNull(centres::get).sorted()
        val spacing = sorted.zipWithNext { a, b -> b - a }.minOrNull()
        return ((spacing ?: fallbackLensWidthPx) * 0.88f)
            .coerceIn(with(density) { 54.dp.toPx() }, with(density) { 84.dp.toPx() })
    }

    LaunchedEffect(selectedItem, centres.size) {
        val target = centres[selectedItem] ?: return@LaunchedEffect
        if (lensX.value == 0f) {
            lensX.snapTo(target)
        } else if (!dragging) {
            lensX.animateTo(
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.70f, stiffness = 430f),
                initialVelocity = lensX.velocity
            )
        }
    }

    val glassTokens = GlassDefaults.tokens()
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val barShape = RoundedCornerShape(31.dp)
    val hazeStyle = HazeStyle(
        backgroundColor = surface,
        tints = listOf(
            HazeTint(surface.copy(alpha = 0.42f)),
            HazeTint(primary.copy(alpha = 0.045f))
        ),
        blurRadius = 24.dp,
        noiseFactor = 0.055f,
        fallbackTint = HazeTint(surface.copy(alpha = 0.90f))
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .height(62.dp)
            .graphicsLayer {
                shadowElevation = 4.dp.toPx()
                shape = barShape
                clip = false
            }
            // 把圆角形状直接交给 Haze。仅在外层 clip 无法约束 Haze 自己绘制的
            // fallback/模糊层，部分设备上会因此露出一整块矩形底色。
            .hazeChild(state = hazeState, shape = barShape, style = hazeStyle)
            .drawWithCache {
                val radius = size.height / 2f
                val body = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = glassTokens.highlightAlpha * 0.30f),
                        Color.Transparent,
                        primary.copy(alpha = glassTokens.tintAlpha * 0.48f)
                    )
                )
                onDrawBehind {
                    drawRoundRect(body, cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius))
                    drawRoundRect(
                        color = outline.copy(alpha = glassTokens.borderAlpha),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx())
                    )
                    drawLine(
                        color = Color.White.copy(alpha = glassTokens.highlightAlpha),
                        start = Offset(radius * 0.72f, 1.5.dp.toPx()),
                        end = Offset(size.width - radius * 0.72f, 1.5.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            // selectedItem is a key: the gesture coroutine must never retain the
            // initial "今天" selection while the visual state moves elsewhere.
            .pointerInput(items, centres.size, selectedItem) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = lensWidthPx()
                    if (abs(down.position.x - lensX.value) > width * 0.64f) return@awaitEachGesture

                    dragging = true
                    dragX = lensX.value
                    lastCrossed = selectedItem
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var pointer = down
                    while (pointer.pressed) {
                        val event = awaitPointerEvent()
                        pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        tracker.addPosition(pointer.uptimeMillis, pointer.position)
                        val min = centres.values.minOrNull() ?: width / 2f
                        val max = centres.values.maxOrNull() ?: size.width - width / 2f
                        val next = pointer.position.x.coerceIn(min, max)
                        dragX = next
                        gestureVelocity = tracker.calculateVelocity().x

                        val crossed = centres.minByOrNull { abs(it.value - next) }?.key
                        if (crossed != null && crossed != lastCrossed) {
                            lastCrossed = crossed
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        if (pointer.position != pointer.previousPosition) pointer.consume()
                    }

                    val sorted = items.mapNotNull { item -> centres[item]?.let { item to it } }
                        .sortedBy { it.second }
                    if (sorted.isNotEmpty()) {
                        val nearestIndex = sorted.indices.minByOrNull { abs(sorted[it].second - dragX) } ?: 0
                        val flingThreshold = with(density) { 900.dp.toPx() }
                        val targetIndex = when {
                            gestureVelocity > flingThreshold -> (nearestIndex + 1).coerceAtMost(sorted.lastIndex)
                            gestureVelocity < -flingThreshold -> (nearestIndex - 1).coerceAtLeast(0)
                            else -> nearestIndex
                        }
                        val target = sorted[targetIndex]
                        val releaseX = dragX
                        val releaseVelocity = gestureVelocity.coerceIn(-4200f, 4200f)
                        if (target.first != selectedItem) onItemSelected(target.first)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        scope.launch {
                            lensX.snapTo(releaseX)
                            lensX.animateTo(
                                targetValue = target.second,
                                animationSpec = spring(dampingRatio = 0.68f, stiffness = 410f),
                                initialVelocity = releaseVelocity
                            )
                        }
                    }
                    dragging = false
                    gestureVelocity = 0f
                }
            }
    ) {
        val widthPx = lensWidthPx()
        val visualX = if (dragging) dragX else lensX.value
        val motion = (abs(if (dragging) gestureVelocity else lensX.velocity) / 4200f).coerceIn(0f, 1f)
        val widthDp = with(density) { widthPx.toDp() }

        if (visualX > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        translationX = visualX - widthPx / 2f
                        scaleX = 1f + motion * 0.075f
                        scaleY = 1f - motion * 0.025f
                        shadowElevation = 4.dp.toPx()
                        shape = RoundedCornerShape(24.dp)
                        clip = true
                    }
                    .background(Color.Transparent)
                    .height(lensHeight)
                    .width(widthDp)
                    .graphicsLayer { }
                    .drawWithCache {
                        val r = size.height / 2f
                        val glass = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = glassTokens.highlightAlpha * .85f),
                                primary.copy(alpha = 0.13f),
                                surface.copy(alpha = glassTokens.fillAlpha * .52f)
                            ),
                            center = Offset(size.width * 0.42f, size.height * 0.30f),
                            radius = size.maxDimension * 0.78f
                        )
                        onDrawBehind {
                            drawRoundRect(glass, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
                            drawRoundRect(
                                Color.White.copy(alpha = glassTokens.highlightAlpha * 1.25f),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(1.15.dp.toPx())
                            )
                            drawArc(
                                color = Color.White.copy(alpha = glassTokens.highlightAlpha * 1.35f),
                                startAngle = 198f,
                                sweepAngle = 112f,
                                useCenter = false,
                                topLeft = Offset(3.dp.toPx(), 2.dp.toPx()),
                                size = Size(size.width - 6.dp.toPx(), size.height - 4.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(1.4.dp.toPx())
                            )
                            // Restrained edge dispersion gives curvature without a visible RGB fringe.
                            drawArc(Color(0xFF75C8FF).copy(alpha = 0.12f), 88f, 94f, false,
                                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()), size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(0.7.dp.toPx()))
                            drawArc(Color(0xFFFF8DB2).copy(alpha = 0.09f), -88f, 94f, false,
                                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()), size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(0.7.dp.toPx()))
                        }
                    }
                    .graphicsLayer { clip = true }
                    .run { this.then(Modifier) }
            )
        }

        Row(modifier = Modifier.fillMaxWidth().align(Alignment.Center)) {
            items.forEach { item ->
                val interactionSource = remember(item) { MutableInteractionSource() }
                val center = centres[item] ?: 0f
                val proximity = if (widthPx > 0f && visualX > 0f) {
                    (1f - abs(center - visualX) / widthPx).coerceIn(0f, 1f)
                } else if (item == selectedItem) 1f else 0f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .onGloballyPositioned { coordinates ->
                            centres[item] = coordinates.positionInParent().x + coordinates.size.width / 2f
                        }
                        .graphicsLayer {
                            scaleX = 1f + proximity * 0.07f
                            scaleY = 1f + proximity * 0.07f
                        }
                        .selectable(
                            selected = item == selectedItem,
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab,
                            onClick = { onItemSelected(item) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        fontSize = (13.5f + proximity * 0.7f).sp,
                        fontWeight = if (proximity > 0.55f) FontWeight.Bold else FontWeight.Medium,
                        color = lerp(onSurfaceVariant, if (proximity > 0.4f) primary else onSurface, proximity)
                    )
                }
            }
        }
    }
}
