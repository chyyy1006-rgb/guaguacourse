package com.example.npucourse.launcher

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.example.npucourse.data.settings.AppIconStyle

/**
 * 通过 activity-alias 切换桌面启动图标。
 *
 * v4.9.1 使用四套瓜瓜课程表品牌图标。旧的彩色课表 alias 仍保留在 Manifest 中，
 * 只用于已有安装升级时保证桌面入口不断裂；应用启动后会迁移并关闭旧 alias。
 */
object LauncherIconManager {

    private const val WATERMELON_ALIAS =
        "com.example.npucourse.launcher.IconWatermelonAlias"
    private const val COURSE_ALIAS =
        "com.example.npucourse.launcher.IconCourseAlias"
    private const val PLANNER_ALIAS =
        "com.example.npucourse.launcher.IconPlannerAlias"
    private const val MASCOT_ALIAS =
        "com.example.npucourse.launcher.IconMascotAlias"

    private val aliasByStyle = linkedMapOf(
        AppIconStyle.WATERMELON to WATERMELON_ALIAS,
        AppIconStyle.COURSE to COURSE_ALIAS,
        AppIconStyle.PLANNER to PLANNER_ALIAS,
        AppIconStyle.MASCOT to MASCOT_ALIAS
    )

    /** v4.8/v4.9 的旧组件名，只做升级兼容。 */
    private val legacyAliasToStyle = linkedMapOf(
        "com.example.npucourse.launcher.IconClassicAlias" to AppIconStyle.WATERMELON,
        "com.example.npucourse.launcher.IconIndigoAlias" to AppIconStyle.COURSE,
        "com.example.npucourse.launcher.IconBlueAlias" to AppIconStyle.PLANNER,
        "com.example.npucourse.launcher.IconObsidianAlias" to AppIconStyle.MASCOT,
        "com.example.npucourse.launcher.IconRoseAlias" to AppIconStyle.WATERMELON,
        "com.example.npucourse.launcher.IconSunsetAlias" to AppIconStyle.WATERMELON
    )

    private val allAliases = aliasByStyle.values + legacyAliasToStyle.keys

    fun applyIcon(
        context: Context,
        requestedStyle: String
    ): Boolean {
        val style = AppIconStyle.normalize(requestedStyle)
        val selectedAlias = aliasByStyle.getValue(style)
        val packageManager = context.packageManager

        return runCatching {
            // 先启用目标入口，再关闭其余入口，避免短时间没有 Launcher 入口。
            setAliasState(
                context = context,
                packageManager = packageManager,
                aliasClassName = selectedAlias,
                enabled = true
            )

            allAliases
                .filter { it != selectedAlias }
                .forEach { alias ->
                    setAliasState(
                        context = context,
                        packageManager = packageManager,
                        aliasClassName = alias,
                        enabled = false
                    )
                }
            true
        }.getOrDefault(false)
    }

    /**
     * 根据 PackageManager 当前组件状态推断图标。
     * 若用户从旧版本升级且旧 alias 仍启用，也会映射到新的品牌图标样式。
     */
    fun currentStyle(context: Context): String {
        val packageManager = context.packageManager

        aliasByStyle.entries
            .firstOrNull { (style, alias) ->
                val state = packageManager.getComponentEnabledSetting(
                    componentName(context, alias)
                )
                state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT &&
                        style == AppIconStyle.WATERMELON)
            }
            ?.let { return it.key }

        legacyAliasToStyle.entries
            .firstOrNull { (alias, _) ->
                packageManager.getComponentEnabledSetting(
                    componentName(context, alias)
                ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }
            ?.let { return it.value }

        return AppIconStyle.WATERMELON
    }

    private fun setAliasState(
        context: Context,
        packageManager: PackageManager,
        aliasClassName: String,
        enabled: Boolean
    ) {
        val component = componentName(context, aliasClassName)
        val targetState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        if (packageManager.getComponentEnabledSetting(component) == targetState) {
            return
        }

        packageManager.setComponentEnabledSetting(
            component,
            targetState,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun componentName(
        context: Context,
        aliasClassName: String
    ): ComponentName =
        ComponentName(context.packageName, aliasClassName)
}
