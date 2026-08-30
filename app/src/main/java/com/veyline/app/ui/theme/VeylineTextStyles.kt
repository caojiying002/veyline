package com.veyline.app.ui.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Veyline 业务 UI 的常用文字样式。
 *
 * 这里仅收录具有稳定语义且跨页面复用的样式。临时或页面专属的文字样式可在使用处
 * 直接创建 [TextStyle] 并调用 [withLineHeightFix]；当相同组合稳定复用后，再提升到此处。
 *
 * 使用时仍可通过 `Text` 的参数覆盖字重、颜色等局部属性：
 * ```kotlin
 * Text(
 *     text = "标题",
 *     style = VeylineTextStyles.Title,
 *     color = VeylineTheme.colors.textTitle,
 * )
 * ```
 */
object VeylineTextStyles {
    /** 页面标题、卡片标题等标题文本，字号 18sp，行高 22sp。 */
    val Title = TextStyle(
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    ).withLineHeightFix()

    /** 列表项标题等次级标题文本，字号 16sp，行高 20sp。 */
    val ItemTitle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ).withLineHeightFix()

    /** 正文、描述等常规文本，字号 14sp，行高 18sp。 */
    val Body = TextStyle(
        fontSize = 14.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
    ).withLineHeightFix()

    /** 时间、浏览量、发布者等辅助信息文本，字号 11sp，行高 14sp。 */
    val Meta = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Normal,
    ).withLineHeightFix()

    /** 按钮文字，字号 15sp，行高 20sp。 */
    val Button = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
    ).withLineHeightFix()
}

/**
 * 应用 Veyline 统一的 Android 文本垂直排版策略。
 *
 * 从 View 体系迁移到 Compose 时，只设置 `fontSize` 往往无法得到设计稿预期的文字高度：
 * 字体自身的度量信息、可能存在的字体额外内边距，以及 `lineHeight` 在首行和末行的
 * 空间分配，都会影响文字最终占用的视觉高度。即使字号相同，`Text` 也可能比原来的
 * `TextView` 看起来更高，或在容器中出现不符合预期的上下留白。
 *
 * 本项目通过以下配置统一这类差异：
 * - `includeFontPadding = false`：不计入 Android 字体的额外内边距；
 * - `Alignment.Center`：将指定行高产生的额外空间在文字上下居中分配；
 * - `Trim.Both`：裁剪首行上方和末行下方的行高空白。
 *
 * 这是一项项目级排版约定，不依赖 Compose 某个版本的默认值。无论调用方的
 * [TextStyle] 来自手动创建、Material Typography 还是第三方组件，都可以显式应用
 * 此扩展，以保持不同页面的文字垂直节奏一致。
 *
 * 调用方仍需显式指定合适的 `lineHeight`。对于特殊字体、Emoji、混合语言或系统大字体
 * 场景，应通过 Preview 或真机确认文字没有被裁切。
 *
 * @return 合并统一垂直排版配置后的新 [TextStyle]，不会修改原对象。
 */
fun TextStyle.withLineHeightFix(): TextStyle = merge(
    TextStyle(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
    )
)
