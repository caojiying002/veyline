package com.veyline.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.veyline.app.R
import com.veyline.app.ui.theme.*

/**
 * 带返回操作的 Veyline 页面标题栏。
 *
 * 延续 View 版本 `TitleBarBack` 的交互与视觉设计：标题不居中，返回区域中的文字同时
 * 承担页面标题。默认显示“返回”，页面也可以传入更具体的标题。
 *
 * 组件通过 `statusBarsPadding` 自行处理并消费状态栏 Insets，调用方不应再为它重复添加
 * 顶部系统栏间距。组件总高度为 56dp 的标题栏高度（[ToolbarHeight]）加上当前设备的
 * 状态栏 Insets。
 *
 * 组件本身不绘制背景，状态栏和标题栏区域会透出调用方提供的背景。返回区域占满标题栏
 * 高度，包含图标、间距和标题文字；点击时不显示水波纹，通过无障碍点击标签表达返回操作。
 * 过长标题限制为单行并在末尾省略。
 *
 * @param onBackClick 点击返回区域时执行的操作。
 * @param modifier 应用于整个标题栏的 [Modifier]。
 * @param title 返回区域显示的页面标题，默认使用通用“返回”文案。
 */
@Composable
fun AppBackTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.action_back),
) {
    val interactionSource = remember { MutableInteractionSource() }

    // 标题栏内容 - 对应原layout中的FrameLayout
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(ToolbarHeight)
    ) {
        // 返回按钮 - 对应原layout中的TextView，同时承担标题功能
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .clickable(
                    indication = null,  // 移除水波纹效果，符合国内APP风格
                    interactionSource = interactionSource,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.action_back),
                    onClick = onBackClick,
                )
                .padding(horizontal = SpacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_back),
                contentDescription = null,
                tint = VeylineTheme.colors.primary,
                modifier = Modifier.size(18.dp),
            )

            Spacer(modifier = Modifier.width(SpacingMedium))

            Text(
                text = title,
                style = VeylineTextStyles.Title,
                color = VeylineTheme.colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ===== Preview 组件 =====
@Preview(name = "默认返回按钮")
@Preview(name = "默认返回按钮 - 暗色", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppBackTopBarDefaultPreview() {
    VeylineTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background),
        ) {
            AppBackTopBar(
                onBackClick = {}
            )
        }
    }
}

@Preview(name = "自定义返回文字")
@Preview(name = "自定义返回文字 - 暗色", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppBackTopBarCustomPreview() {
    VeylineTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background),
        ) {
            AppBackTopBar(
                title = "页面标题",
                onBackClick = {}
            )

            Spacer(modifier = Modifier.height(8.dp))

            AppBackTopBar(
                title = "个人资料",
                onBackClick = {}
            )

            Spacer(modifier = Modifier.height(8.dp))

            AppBackTopBar(
                title = "设置",
                onBackClick = {}
            )
        }
    }
}
