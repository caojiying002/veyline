package com.veyline.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.king.ultraswiperefresh.UltraSwipeHeaderState
import com.king.ultraswiperefresh.UltraSwipeRefreshState
import com.king.ultraswiperefresh.indicator.classic.ClassicRefreshHeader
import com.king.ultraswiperefresh.rememberUltraSwipeRefreshState
import com.veyline.app.R
import com.veyline.app.ui.theme.VeylineTextStyles
import com.veyline.app.ui.theme.VeylineTheme

/**
 * 应用统一的经典下拉刷新头。
 *
 * 复用 UltraSwipeRefresh 的箭头、加载动画和结束阶段，统一文字样式与图标颜色，
 * 不显示上次刷新时间。结束时隐藏图标并保留其占位，避免提示文字左右跳动。
 *
 * 刷新结果由调用方提供，仅在结束阶段用于区分成功和失败。组件不发起请求，
 * 不修改刷新状态，也不依赖 Paging。背景和系统栏 Insets 由宿主处理。
 *
 * @param state 刷新容器提供的状态，与 UltraSwipeRefresh 使用同一个实例
 * @param isRefreshSuccess 本次刷新是否成功，调用方应在结束刷新前更新，并保留到结束阶段完成
 * @param modifier 应用于刷新头的 Modifier
 */
@Composable
fun AppRefreshHeader(
    state: UltraSwipeRefreshState,
    isRefreshSuccess: Boolean,
    modifier: Modifier = Modifier,
) {
    val contentColor = VeylineTheme.colors.textLight
    val iconSize = with(LocalDensity.current) { 24.sp.toDp() }

    val tipRes = when {
        // 结束阶段优先展示请求结果，不再显示“正在刷新”
        state.isFinishing -> {
            if (isRefreshSuccess) R.string.refresh_success else R.string.refresh_failed
        }

        else -> when(state.headerState) {
            UltraSwipeHeaderState.PullDownToRefresh -> R.string.refresh_pull_down
            UltraSwipeHeaderState.ReleaseToRefresh -> R.string.refresh_release
            UltraSwipeHeaderState.Refreshing -> R.string.refresh_loading

            // 当前不启用“二级”内容，这两个状态只做防御性兜底
            UltraSwipeHeaderState.ReleaseToSecondary,
            UltraSwipeHeaderState.Secondary -> R.string.refresh_pull_down
        }
    }

    ClassicRefreshHeader(
        state = state,
        tipContent = { stringResource(tipRes) },
        tipContentStyle = VeylineTextStyles.Body.copy(color = contentColor),
        tipTime = { "" },
        tipTimeVisible = false,
        paddingValues = PaddingValues(12.dp),
        iconSize = iconSize,
        iconColorFilter = ColorFilter.tint(
            color = if (state.isFinishing) Color.Transparent else contentColor,
        ),
        modifier = modifier,
    )
}

// ===== Preview 组件 =====
// 静态预览检查默认提示的样式，手势与结束反馈在接入刷新容器后验证
@Preview(name = "经典刷新头")
@Preview(name = "经典刷新头 - 暗色", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "经典刷新头 - 大字体", fontScale = 2f)
@Composable
private fun AppRefreshHeaderPreview() {
    VeylineTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background),
        ) {
            AppRefreshHeader(
                state = rememberUltraSwipeRefreshState(),
                isRefreshSuccess = true,
            )
        }
    }
}
