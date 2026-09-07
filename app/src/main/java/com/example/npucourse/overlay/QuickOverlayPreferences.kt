package com.example.npucourse.overlay

import android.content.Context
import androidx.core.content.edit
import org.json.JSONObject

object OverlayGesture {
    const val DOUBLE_TAP = "DOUBLE_TAP"
    const val SWIPE_LEFT = "SWIPE_LEFT"
    const val SWIPE_RIGHT = "SWIPE_RIGHT"
    const val SWIPE_UP = "SWIPE_UP"
    const val SWIPE_DOWN = "SWIPE_DOWN"

    val all = listOf(DOUBLE_TAP, SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT)

    fun label(value: String): String = when (value) {
        SWIPE_LEFT -> "向左滑"
        SWIPE_RIGHT -> "向右滑"
        SWIPE_UP -> "向上滑"
        SWIPE_DOWN -> "向下滑"
        else -> "双击"
    }
}

object OverlayGestureClassifier {
    fun detect(
        dx: Float,
        dy: Float,
        distance: Float,
        minDistance: Float,
        speed: Float,
        minSpeed: Float
    ): String? {
        if (distance < minDistance || (speed < minSpeed && distance < minDistance * 1.48f)) return null
        return when {
            kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.45f && dx < 0 -> OverlayGesture.SWIPE_LEFT
            kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.45f && dx > 0 -> OverlayGesture.SWIPE_RIGHT
            kotlin.math.abs(dy) > kotlin.math.abs(dx) * 1.45f && dy < 0 -> OverlayGesture.SWIPE_UP
            kotlin.math.abs(dy) > kotlin.math.abs(dx) * 1.45f && dy > 0 -> OverlayGesture.SWIPE_DOWN
            else -> null
        }
    }
}

data class GestureAction(
    val gestureType: String,
    val packageName: String = "",
    val activityName: String? = null,
    val label: String = "未设置",
    val enabled: Boolean = true
) {
    val isConfigured: Boolean get() = enabled && packageName.isNotBlank()
}

data class QuickOverlayConfig(
    val enabled: Boolean = false,
    val temporarilyHidden: Boolean = false,
    val actions: Map<String, GestureAction> = OverlayGesture.all.associateWith { GestureAction(it) },
    val sizeDp: Int = 58,
    val opacity: Float = 0.88f,
    val snapToEdge: Boolean = true,
    val lockPosition: Boolean = false,
    val hapticFeedback: Boolean = true,
    val tutorialShown: Boolean = false,
    val x: Int = -1,
    val y: Int = -1
) {
    fun actionFor(gesture: String): GestureAction? = actions[gesture]?.takeIf { it.isConfigured }
    val primaryAction: GestureAction? get() = actionFor(OverlayGesture.DOUBLE_TAP)
        ?: OverlayGesture.all.asSequence().mapNotNull(::actionFor).firstOrNull()
    val hasConfiguredAction: Boolean get() = primaryAction != null
}

object QuickOverlayPreferences {
    private const val NAME = "quick_overlay"
    private const val KEY_ACTIONS = "gesture_actions_v2"
    private const val KEY_MIGRATED = "gesture_actions_migrated_v2"

    fun load(context: Context): QuickOverlayConfig {
        val p = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        if (!p.getBoolean(KEY_MIGRATED, false)) migrateLegacy(context)
        val actionsJson = p.getString(KEY_ACTIONS, null)
        val actions = OverlayGesture.all.associateWith { gesture ->
            val value = runCatching { actionsJson?.let(::JSONObject)?.optJSONObject(gesture) }.getOrNull()
            GestureAction(
                gestureType = gesture,
                packageName = value?.optString("packageName").orEmpty(),
                activityName = value?.optString("activityName")?.takeIf { it.isNotBlank() },
                label = value?.optString("label")?.takeIf { it.isNotBlank() } ?: "未设置",
                enabled = value?.optBoolean("enabled", true) ?: true
            )
        }
        return QuickOverlayConfig(
            enabled = p.getBoolean("enabled", false),
            temporarilyHidden = p.getBoolean("hidden", false),
            actions = actions,
            sizeDp = p.getInt("size_dp", 58).coerceIn(46, 82),
            opacity = p.getFloat("opacity", 0.88f).coerceIn(0.42f, 1f),
            snapToEdge = p.getBoolean("snap", true),
            lockPosition = p.getBoolean("locked", false),
            hapticFeedback = p.getBoolean("haptic", true),
            tutorialShown = p.getBoolean("tutorial_shown_v2", false),
            x = p.getInt("x", -1),
            y = p.getInt("y", -1)
        )
    }

    fun save(context: Context, value: QuickOverlayConfig) {
        val root = JSONObject()
        OverlayGesture.all.forEach { gesture ->
            val action = value.actions[gesture] ?: GestureAction(gesture)
            root.put(gesture, JSONObject()
                .put("packageName", action.packageName)
                .put("activityName", action.activityName ?: "")
                .put("label", action.label)
                .put("enabled", action.enabled))
        }
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_MIGRATED, true)
            putString(KEY_ACTIONS, root.toString())
            putBoolean("enabled", value.enabled)
            putBoolean("hidden", value.temporarilyHidden)
            putInt("size_dp", value.sizeDp.coerceIn(46, 82))
            putFloat("opacity", value.opacity.coerceIn(0.42f, 1f))
            putBoolean("snap", value.snapToEdge)
            putBoolean("locked", value.lockPosition)
            putBoolean("haptic", value.hapticFeedback)
            putBoolean("tutorial_shown_v2", value.tutorialShown)
            putInt("x", value.x)
            putInt("y", value.y)
        }
    }

    private fun migrateLegacy(context: Context) {
        val p = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        val oldPackage = p.getString("target_package", "").orEmpty()
        val oldLabel = p.getString("target_label", "未设置") ?: "未设置"
        val root = JSONObject()
        OverlayGesture.all.forEach { gesture ->
            val isDoubleTap = gesture == OverlayGesture.DOUBLE_TAP
            root.put(gesture, JSONObject()
                .put("packageName", if (isDoubleTap) oldPackage else "")
                .put("activityName", "")
                .put("label", if (isDoubleTap && oldPackage.isNotBlank()) oldLabel else "未设置")
                .put("enabled", true))
        }
        p.edit {
            putString(KEY_ACTIONS, root.toString())
            putBoolean(KEY_MIGRATED, true)
        }
    }
}
