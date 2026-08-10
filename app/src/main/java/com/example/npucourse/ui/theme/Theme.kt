package com.example.npucourse.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.npucourse.data.settings.AccentStyle
import com.example.npucourse.data.settings.ThemeMode

private data class AccentPalette(
    val lightPrimary: Color,
    val lightPrimaryContainer: Color,
    val darkPrimary: Color,
    val darkPrimaryContainer: Color
)

private fun accentPalette(style: String): AccentPalette =
    when (style) {
        AccentStyle.BLUE -> AccentPalette(
            lightPrimary = Color(0xFF3768D8),
            lightPrimaryContainer = Color(0xFFDCE6FF),
            darkPrimary = Color(0xFFADC6FF),
            darkPrimaryContainer = Color(0xFF1E438F)
        )
        AccentStyle.GREEN -> AccentPalette(
            lightPrimary = Color(0xFF26765E),
            lightPrimaryContainer = Color(0xFFB9F1D9),
            darkPrimary = Color(0xFF8CD7BA),
            darkPrimaryContainer = Color(0xFF07513E)
        )
        AccentStyle.ROSE -> AccentPalette(
            lightPrimary = Color(0xFF9A405F),
            lightPrimaryContainer = Color(0xFFFFD9E3),
            darkPrimary = Color(0xFFFFB1C6),
            darkPrimaryContainer = Color(0xFF792847)
        )
        AccentStyle.ORANGE -> AccentPalette(
            lightPrimary = Color(0xFF9A4F16),
            lightPrimaryContainer = Color(0xFFFFDBC7),
            darkPrimary = Color(0xFFFFB785),
            darkPrimaryContainer = Color(0xFF713500)
        )
        else -> AccentPalette(
            lightPrimary = Color(0xFF5B5BD6),
            lightPrimaryContainer = Color(0xFFE4E1FF),
            darkPrimary = Color(0xFFC4C0FF),
            darkPrimaryContainer = Color(0xFF4040A8)
        )
    }

@Composable
fun NPUcourseTheme(
    themeMode: String = ThemeMode.SYSTEM,
    accentStyle: String = AccentStyle.INDIGO,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        else -> systemDark
    }

    val context = LocalContext.current
    val view = LocalView.current
    val palette = accentPalette(accentStyle)

    if (!view.isInEditMode) {
        SideEffect {
            val activity = context as? Activity
            if (activity != null) {
                val controller = WindowCompat.getInsetsController(
                    activity.window,
                    view
                )
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> darkColorScheme(
            primary = palette.darkPrimary,
            onPrimary = Color(0xFF14131C),
            primaryContainer = palette.darkPrimaryContainer,
            onPrimaryContainer = Color(0xFFF3F0FF),
            background = Color(0xFF111216),
            onBackground = Color(0xFFE8E8ED),
            surface = Color(0xFF191A1F),
            onSurface = Color(0xFFE8E8ED),
            surfaceVariant = Color(0xFF25262D),
            onSurfaceVariant = Color(0xFFBFC0CA),
            outline = Color(0xFF858691)
        )

        else -> lightColorScheme(
            primary = palette.lightPrimary,
            onPrimary = Color.White,
            primaryContainer = palette.lightPrimaryContainer,
            onPrimaryContainer = Color(0xFF191733),
            background = Color(0xFFF7F7FA),
            onBackground = Color(0xFF1B1B20),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1B1B20),
            surfaceVariant = Color(0xFFF0F0F5),
            onSurfaceVariant = Color(0xFF666770),
            outline = Color(0xFF8B8B94)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
