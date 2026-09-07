package com.example.npucourse.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class GlassTokens(
    val fillAlpha: Float,
    val tintAlpha: Float,
    val borderAlpha: Float,
    val highlightAlpha: Float,
    val shadowElevation: Dp,
    val cardRadius: Dp,
    val dialogRadius: Dp,
    val motion: AnimationSpec<Float>
)

object GlassDefaults {
    val light = GlassTokens(.62f, .045f, .14f, .22f, 3.dp, 24.dp, 28.dp,
        spring(dampingRatio = .76f, stiffness = 430f))
    val dark = GlassTokens(.50f, .08f, .22f, .12f, 4.dp, 24.dp, 28.dp,
        spring(dampingRatio = .78f, stiffness = 400f))

    @Composable
    fun tokens(): GlassTokens = if (isSystemInDarkTheme()) dark else light
}

/**
 * 普通内容卡片使用不透明的柔和色面。磨砂只保留给底部导航等确实有
 * 背景内容需要模糊的悬浮层，避免“半透明外壳 + 白色矩形内容层”。
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(GlassDefaults.tokens().cardRadius),
    elevation: Dp = GlassDefaults.tokens().shadowElevation,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    Surface(
        modifier = modifier,
        shape = shape,
        color = base,
        tonalElevation = 1.dp,
        shadowElevation = elevation.coerceAtMost(1.dp),
        border = BorderStroke(1.dp, outline.copy(alpha = 0.10f))
    ) {
        Column(content = content)
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = LiquidGlassSurface(modifier = modifier, content = content)

@Composable
fun LiquidGlassDialogContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = LiquidGlassSurface(
    modifier = modifier,
    shape = RoundedCornerShape(GlassDefaults.tokens().dialogRadius),
    elevation = 14.dp,
    content = content
)

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) = LiquidGlassSurface(
    modifier = modifier.clickable(onClick = onClick),
    shape = RoundedCornerShape(18.dp),
    elevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = androidx.compose.ui.Alignment.Center,
            content = content
        )
    }
