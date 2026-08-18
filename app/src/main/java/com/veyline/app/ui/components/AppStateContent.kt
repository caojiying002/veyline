package com.veyline.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.veyline.app.R
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
