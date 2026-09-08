package com.veyline.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veyline.app.R
import com.veyline.app.ui.theme.SpacingLarge
import com.veyline.app.ui.theme.SpacingMedium
import com.veyline.app.ui.theme.VeylineTextStyles
import com.veyline.app.ui.theme.VeylineTheme

/**
 * 分页列表底部的加载提示。
 *
 * 加载指示器和文字居中排列，使用应用统一的次要文字色。
 * 指示器跟随系统字体大小缩放，组件高度由内容和内边距决定。
 *
 * 组件只负责展示，不处理分页状态、背景或系统栏 Insets，
 * 由调用方决定何时显示。
 *
 * @param modifier 应用于整个底部提示区域的 Modifier
 */
@Composable
fun AppPagingLoadingFooter(
    modifier: Modifier = Modifier,
) {
    val indicatorSize = with(LocalDensity.current) { 20.sp.toDp() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(SpacingLarge)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(
            space = 12.dp,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(indicatorSize),
            color = VeylineTheme.colors.textLight,
            strokeWidth = 2.dp,
        )

        Text(
            text = stringResource(R.string.state_loading),
            style = VeylineTextStyles.ItemTitle,
            color = VeylineTheme.colors.textLight,
        )
    }
}

/**
 * 分页列表底部的加载失败提示。
 *
 * 错误提示使用次要文字色，重试提示使用主色，整个底部区域都可点击。
 * 文字居中显示，空间不足时允许换行，高度随内容增长。
 *
 * 固定显示“加载失败，点击重试”，不支持自定义文案，避免详细错误信息挤占列表底部空间。
 * 如需解释具体原因，由页面选择合适的方式另行提示。
 *
 * 组件只接收点击回调，不负责异常分类或分页重试逻辑，
 * 也不处理背景和系统栏 Insets。
 *
 * @param onRetryClick 点击底部提示区域时的重试回调
 * @param modifier 应用于整个底部提示区域的 Modifier
 */
@Composable
fun AppPagingErrorFooter(
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val retryHint = stringResource(R.string.paging_retry_hint)
    val fullMessage = stringResource(
        R.string.paging_error_with_retry,
        retryHint,
    )

    // 重试提示位于文案末尾，只突出高亮这部分文字
    val annotatedText = buildAnnotatedString {
        append(fullMessage)
        addStyle(
            style = SpanStyle(color = VeylineTheme.colors.primary),
            start = fullMessage.length - retryHint.length,
            end = fullMessage.length,
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = stringResource(R.string.action_retry),
                onClick = onRetryClick,
            )
            .padding(SpacingLarge),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = annotatedText,
            style = VeylineTextStyles.ItemTitle,
            color = VeylineTheme.colors.textLight,
            textAlign = TextAlign.Center,
        )
    }
}

// 后续如果需要在列表底部显示“没有更多了”之类提示，可在这里新增 AppPagingNoMoreFooter
// 是否显示由调用方根据分页状态决定，组件只负责展示提示

// ===== Preview 组件 =====
@Preview(name = "分页加载")
@Preview(name = "分页加载 - 暗色", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "分页加载 - 大字体", fontScale = 2f)
@Composable
private fun AppPagingLoadingFooterPreview() {
    VeylineTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background),
        ) {
            AppPagingLoadingFooter()
        }
    }
}

@Preview(name = "分页加载失败")
@Preview(name = "分页加载失败 - 暗色", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "分页加载失败 - 大字体", fontScale = 2f)
@Composable
private fun AppPagingErrorFooterPreview() {
    VeylineTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background),
        ) {
            AppPagingErrorFooter(
                onRetryClick = {},
            )
        }
    }
}
