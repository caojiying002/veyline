package com.veyline.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = Primary,
    onPrimary = TextOnButton,

    // Background and surface colors
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,

    // Other important colors
    error = Error,
    outline = Outline,
)

private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = PrimaryDark,
    onPrimary = TextOnButtonDark,

    // Background and surface colors
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,

    // Other important colors
    error = ErrorDark,
    outline = OutlineDark,
)

@Composable
fun VeylineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 关闭 Material You 动态颜色（Android 12+）
    // dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val veylineColors = if (darkTheme) darkVeylineColors else lightVeylineColors

    CompositionLocalProvider(LocalVeylineColors provides veylineColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            //shapes = Shapes,         // 可自定义 Shape.kt
            content = content
        )
    }
}

/**
 * Veyline 主题令牌的统一访问入口。
 *
 * 业务 UI 应通过此对象访问主题颜色，避免直接依赖
 * [LocalVeylineColors] 或具体的亮色、暗色实现。
 *
 * 仅可在 VeylineTheme 主题作用域内使用。
 */
object VeylineTheme {
    val colors: VeylineColors
        @Composable
        @ReadOnlyComposable
        get() = LocalVeylineColors.current
}

/**
 * 应用统一的 OutlinedTextField 颜色配置
 */
@Composable
fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = VeylineTheme.colors.primary,
    unfocusedBorderColor = VeylineTheme.colors.outline,
    errorBorderColor = VeylineTheme.colors.error,
    focusedTrailingIconColor = VeylineTheme.colors.primary,
    unfocusedTrailingIconColor = VeylineTheme.colors.outline,
    errorTrailingIconColor = VeylineTheme.colors.error,
    focusedLabelColor = VeylineTheme.colors.primary,
    unfocusedLabelColor = VeylineTheme.colors.outline
)
