package com.veyline.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veyline.app.R
import com.veyline.app.ui.theme.SpacingLarge
import com.veyline.app.ui.theme.SpacingMedium
import com.veyline.app.ui.theme.ToolbarHeight
import com.veyline.app.ui.theme.VeylineTextStyles
import com.veyline.app.ui.theme.VeylineTheme

/**
 * 首页各 Tab 共用的城市选择标题栏。
 *
 * 城市名称和下拉箭头靠左排列，未选择城市时显示“选择地区”。点击区域仅包含文字、
 * 箭头及其周围的内边距，右侧剩余空白不响应点击；点击时不显示水波纹。
 *
 * 组件内部处理并消费状态栏 Insets，调用方不应重复添加顶部状态栏间距。
 * 底部系统栏由首页容器负责处理。组件不绘制背景，状态栏和标题栏区域使用调用方的背景。
 *
 * 标题栏内容的最小高度为 [ToolbarHeight]，系统字体放大时允许随内容增高。
 * 城市名称最多显示一行，超长时在末尾省略，并为下拉箭头保留空间。
 *
 * @param cityName 当前城市名称；未选择时传 null，非空名称由调用方保证有效
 * @param onCitySelectionClick 点击城市选择区域时的回调，由调用方决定打开哪个选择页面
 * @param modifier 应用于整个标题栏的 [Modifier]
 */
@Composable
fun CitySelectionTopBar(
    cityName: String?,
    onCitySelectionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 将 sp 转为布局尺寸，让箭头跟随系统字体大小缩放
    val iconSize = with(LocalDensity.current) { 16.sp.toDp() }

    // 外层占满页面宽度并处理状态栏间距，不承担点击操作
    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        contentAlignment = Alignment.CenterStart,
    ) {
        // 不设置 fillMaxWidth，让点击区域随文字和箭头的实际宽度收缩
        Row(
            modifier = Modifier
                .heightIn(min = ToolbarHeight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.city_select_hint),
                    onClick = onCitySelectionClick,
                )
                // 内边距也属于点击区域，方便触摸文字和箭头周围的位置
                .padding(
                    horizontal = SpacingLarge,
                    vertical = SpacingMedium,
                ),
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = cityName ?: stringResource(R.string.city_select_hint),
                style = VeylineTextStyles.Title,
                color = VeylineTheme.colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, false),  // 让长名称仍然给箭头留出空间，短名称按实际宽度显示
            )

            Icon(
                painter = painterResource(R.drawable.ic_expand_down),
                // 箭头仅作视觉提示，整组内容通过 clickable 提供按钮语义和操作标签
                contentDescription = null,
                tint = VeylineTheme.colors.primary,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

// ===== Preview 组件 =====
// 集中对比不同名称、主题和字号；实际状态栏间距需在单个标题栏的页面中确认
@Preview(name = "城市选择标题栏")
@Preview(name = "城市选择标题栏 - 暗色", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "城市选择标题栏 - 大字体", fontScale = 2f)
@Composable
private fun CitySelectionTopBarPreview() {
    VeylineTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background),
        ) {
            CitySelectionTopBar(
                cityName = null,
                onCitySelectionClick = {},
            )

            Spacer(modifier = Modifier.height(8.dp))

            CitySelectionTopBar(
                cityName = "上海市",
                onCitySelectionClick = {},
            )

            Spacer(modifier = Modifier.height(8.dp))

            CitySelectionTopBar(
                cityName = "用于检查标题省略效果的较长地区名称",
                onCitySelectionClick = {},
            )
        }
    }
}
