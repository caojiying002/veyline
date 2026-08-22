package com.veyline.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class VeylineColors(
    val isDark: Boolean,
    // 基础色
    val primary: Color,
    val background: Color,
    val surface: Color,
    val onSurface: Color,
    val onBackground: Color,
    val outline: Color,
    val error: Color,
    // 文本色
    val textMainTab: Color,
    val textItemTitle: Color,
    val textTitle: Color,
    val textContent: Color,
    val textLight: Color,
    val textPrice: Color,
    val textWarningYellow: Color,
    val textOnButton: Color,
    // 按钮色
    val buttonDefault: Color,
    val buttonPressed: Color,
    val buttonDisabled: Color,
    val buttonPressedSecondary: Color,

    /**
     * 简单列表中相邻 Item 之间的细分隔线颜色。
     *
     * 目前用于城市选择列表。信息流列表通过页面背景和 Item 间距形成宽分隔线效果，
     * 不应使用该颜色。
     */
    val thinDivider: Color,
)

internal val lightVeylineColors = VeylineColors(
    isDark = false,
    primary = Primary,
    background = Background,
    surface = Surface,
    onSurface = OnSurface,
    onBackground = OnBackground,
    outline = Outline,
    error = Error,
    textMainTab = TextMainTab,
    textItemTitle = TextItemTitle,
    textTitle = TextTitle,
    textContent = TextContent,
    textLight = TextLight,
    textPrice = TextPrice,
    textWarningYellow = TextWarningYellow,
    textOnButton = TextOnButton,
    buttonDefault = Primary,
    buttonPressed = ButtonPressed,
    buttonDisabled = ButtonDisabled,
    buttonPressedSecondary = ButtonPressedSecondary,
    thinDivider = ThinDivider,
)

internal val darkVeylineColors = VeylineColors(
    isDark = true,
    primary = PrimaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    onBackground = OnBackgroundDark,
    outline = OutlineDark,
    error = ErrorDark,
    textMainTab = TextMainTabDark,
    textItemTitle = TextItemTitleDark,
    textTitle = TextTitleDark,
    textContent = TextContentDark,
    textLight = TextLightDark,
    textPrice = TextPriceDark,
    textWarningYellow = TextWarningYellowDark,
    textOnButton = TextOnButtonDark,
    buttonDefault = PrimaryDark,
    buttonPressed = ButtonPressedDark,
    buttonDisabled = ButtonDisabledDark,
    buttonPressedSecondary = ButtonPressedSecondaryDark,
    thinDivider = ThinDividerDark,
)

internal val LocalVeylineColors = staticCompositionLocalOf<VeylineColors> {
    // fail-fast
    val name = "VeylineColors"
    error("CompositionLocal $name not present")
}
