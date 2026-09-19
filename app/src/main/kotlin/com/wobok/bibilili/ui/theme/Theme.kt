package com.wobok.bibilili.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/** 用户可选的外观。一期不给切换入口，默认跟随系统，但数据层已经支持。 */
enum class ThemeMode { FollowSystem, Light, Dark }

/**
 * Material 的 ColorScheme 覆盖不到素笺自己的语义（纸感描边、播放器专用色等），
 * 所以额外挂一份扩展色，通过 [LocalPaperScheme] 取用。
 */
data class PaperScheme(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val outline: Color,
    val outline2: Color,
    val primary: Color,
    val primaryContainer: Color,
    val accent: Color,
    val onBg: Color,
    val onBg2: Color,
    val onBg3: Color,
    val isDark: Boolean,
) {
    /** 播放器无论日夜都是纯黑，强调色也另算。 */
    val playerBg: Color get() = PaperColors.PlayerBg
    val playerAccent: Color get() = PaperColors.PlayerAccent
}

private val LightPaper = PaperScheme(
    bg = PaperColors.LightBg,
    surface = PaperColors.LightSurface,
    surface2 = PaperColors.LightSurface2,
    outline = PaperColors.LightOutline,
    outline2 = PaperColors.LightOutline2,
    primary = PaperColors.LightPrimary,
    primaryContainer = PaperColors.LightPrimaryContainer,
    accent = PaperColors.LightAccent,
    onBg = PaperColors.LightOnBg,
    onBg2 = PaperColors.LightOnBg2,
    onBg3 = PaperColors.LightOnBg3,
    isDark = false,
)

private val DarkPaper = PaperScheme(
    bg = PaperColors.DarkBg,
    surface = PaperColors.DarkSurface,
    surface2 = PaperColors.DarkSurface2,
    outline = PaperColors.DarkOutline,
    outline2 = PaperColors.DarkOutline2,
    primary = PaperColors.DarkPrimary,
    primaryContainer = PaperColors.DarkPrimaryContainer,
    accent = PaperColors.DarkAccent,
    onBg = PaperColors.DarkOnBg,
    onBg2 = PaperColors.DarkOnBg2,
    onBg3 = PaperColors.DarkOnBg3,
    isDark = true,
)

val LocalPaperScheme: ProvidableCompositionLocal<PaperScheme> =
    compositionLocalOf { LightPaper }

object PaperTheme {
    val colors: PaperScheme
        @Composable @ReadOnlyComposable get() = LocalPaperScheme.current
}

@Composable
fun BibiliTheme(
    mode: ThemeMode = ThemeMode.FollowSystem,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.FollowSystem -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val paper = if (dark) DarkPaper else LightPaper

    val material = remember(dark) {
        if (dark) {
            darkColorScheme(
                primary = DarkPaper.primary,
                onPrimary = PaperColors.DarkBg,
                primaryContainer = DarkPaper.primaryContainer,
                secondary = DarkPaper.accent,
                background = DarkPaper.bg,
                onBackground = DarkPaper.onBg,
                surface = DarkPaper.surface,
                onSurface = DarkPaper.onBg,
                surfaceVariant = DarkPaper.surface2,
                onSurfaceVariant = DarkPaper.onBg2,
                outline = DarkPaper.outline2,
                error = DarkPaper.accent,
            )
        } else {
            lightColorScheme(
                primary = LightPaper.primary,
                onPrimary = LightPaper.bg,
                primaryContainer = LightPaper.primaryContainer,
                secondary = LightPaper.accent,
                background = LightPaper.bg,
                onBackground = LightPaper.onBg,
                surface = LightPaper.surface,
                onSurface = LightPaper.onBg,
                surfaceVariant = LightPaper.surface2,
                onSurfaceVariant = LightPaper.onBg2,
                outline = LightPaper.outline2,
                error = LightPaper.accent,
            )
        }
    }

    CompositionLocalProvider(LocalPaperScheme provides paper) {
        MaterialTheme(
            colorScheme = material,
            typography = PaperTypography,
            shapes = PaperShapes,
            content = content,
        )
    }
}
