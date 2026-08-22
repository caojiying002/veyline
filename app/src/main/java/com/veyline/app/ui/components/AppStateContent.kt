package com.veyline.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.veyline.app.R
import com.veyline.app.ui.theme.SpacingLarge
import com.veyline.app.ui.theme.VeylineTextStyles
import com.veyline.app.ui.theme.VeylineTheme

/**
 * 占满可用内容区域并居中展示加载指示器。
 *
 * 组件不绘制页面背景，也不处理系统栏 Insets，这两项职责由所在页面承担。
 *
 * @param modifier 应用于内容区域根容器的 [Modifier]。
 */
@Composable
fun AppLoadingContent(
    modifier: Modifier = Modifier,
) {
    val loadingDescription = stringResource(R.string.state_loading)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.semantics {
                contentDescription = loadingDescription
            },
            color = VeylineTheme.colors.primary,
        )
    }
}

/**
 * 占满可用内容区域并居中展示错误提示和重试操作。
 *
 * 组件只负责展示调用方提供的用户可见文案，不接收或解释 `UiError`、异常和网络错误模型。
 * 页面背景与系统栏 Insets 仍由所在页面负责。
 *
 * @param message 向用户展示的错误提示。
 * @param onRetryClick 点击重试按钮时执行的操作。
 * @param modifier 应用于内容区域根容器的 [Modifier]。
 */
@Composable
fun AppErrorContent(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SpacingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            style = VeylineTextStyles.Body,
            color = VeylineTheme.colors.textContent,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(SpacingLarge))

        AppButton(
            text = stringResource(R.string.action_retry),
            onClick = onRetryClick,
            modifier = Modifier.widthIn(min = 120.dp),
        )
    }
}

/**
 * 占满可用内容区域并居中展示空状态提示。
 *
 * 组件只负责展示调用方提供的用户可见文案，不添加默认图标、操作按钮或页面背景，
 * 系统栏 Insets 仍由所在页面负责。
 *
 * @param message 向用户展示的空状态提示。
 * @param modifier 应用于内容区域根容器的 [Modifier]。
 */
@Composable
fun AppEmptyContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = SpacingLarge),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = VeylineTextStyles.Body,
            color = VeylineTheme.colors.textLight,
            textAlign = TextAlign.Center,
        )
    }
}

// ===== Preview 组件 =====
@Preview(name = "加载状态", widthDp = 360, heightDp = 240)
@Preview(name = "加载状态 - 暗色", widthDp = 360, heightDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppLoadingContentPreview() {
    VeylineTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background),
        ) {
            AppLoadingContent()
        }
    }
}

@Preview(name = "错误状态", widthDp = 360, heightDp = 240)
@Preview(name = "错误状态 - 暗色", widthDp = 360, heightDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppErrorContentPreview() {
    VeylineTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background),
        ) {
            AppErrorContent(
                message = "网络连接失败，请检查网络后重试",
                onRetryClick = {},
            )
        }
    }
}

@Preview(name = "空状态", widthDp = 360, heightDp = 240)
@Preview(name = "空状态 - 暗色", widthDp = 360, heightDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppEmptyContentPreview() {
    VeylineTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background),
        ) {
            AppEmptyContent(
                message = "暂无数据",
            )
        }
    }
}
