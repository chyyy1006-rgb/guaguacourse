package com.example.npucourse.overlay

import android.content.Context
import androidx.core.content.edit

object OverlayGesture {
    const val DOUBLE_TAP = "DOUBLE_TAP"
    const val SWIPE_LEFT = "SWIPE_LEFT"
    const val SWIPE_RIGHT = "SWIPE_RIGHT"
    const val SWIPE_UP = "SWIPE_UP"
    const val SWIPE_DOWN = "SWIPE_DOWN"

    val all = listOf(DOUBLE_TAP, SWIPE_LEFT, SWIPE_RIGHT, SWIPE_UP, SWIPE_DOWN)

    fun label(value: String): String = when (value) {
        SWIPE_LEFT -> "向左滑"
        SWIPE_RIGHT -> "向右滑"
        SWIPE_UP -> "向上滑"
        SWIPE_DOWN -> "向下滑"
        else -> "双击"
    }
}

data class QuickOverlayConfig(
    val enabled: Boolean = false,
    val temporarilyHidden: Boolean = false,
    val targetPackage: String = "",
    val targetLabel: String = "未选择",
    val gesture: String = OverlayGesture.DOUBLE_TAP,
    val sizeDp: Int = 58,
    val opacity: Float = 0.88f,
    val snapToEdge: Boolean = true,
    val lockPosition: Boolean = false,
    val x: Int = -1,
    val y: Int = -1
)

object QuickOverlayPreferences {
    private const val NAME = "quick_overlay"

    fun load(context: Context): QuickOverlayConfig {
        val p = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        return QuickOverlayConfig(
            enabled = p.getBoolean("enabled", false),
            temporarilyHidden = p.getBoolean("hidden", false),
            targetPackage = p.getString("target_package", "").orEmpty(),
            targetLabel = p.getString("target_label", "未选择") ?: "未选择",
            gesture = p.getString("gesture", OverlayGesture.DOUBLE_TAP) ?: OverlayGesture.DOUBLE_TAP,
            sizeDp = p.getInt("size_dp", 58).coerceIn(46, 82),
            opacity = p.getFloat("opacity", 0.88f).coerceIn(0.42f, 1f),
            snapToEdge = p.getBoolean("snap", true),
            lockPosition = p.getBoolean("locked", false),
            x = p.getInt("x", -1),
            y = p.getInt("y", -1)
        )
    }

    fun save(context: Context, value: QuickOverlayConfig) {
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit {
            putBoolean("enabled", value.enabled)
            putBoolean("hidden", value.temporarilyHidden)
            putString("target_package", value.targetPackage)
            putString("target_label", value.targetLabel)
            putString("gesture", value.gesture)
            putInt("size_dp", value.sizeDp.coerceIn(46, 82))
            putFloat("opacity", value.opacity.coerceIn(0.42f, 1f))
            putBoolean("snap", value.snapToEdge)
            putBoolean("locked", value.lockPosition)
            putInt("x", value.x)
            putInt("y", value.y)
        }
    }
}
