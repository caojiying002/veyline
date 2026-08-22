package com.veyline.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.veyline.app.ui.theme.DefaultHorizontalSpace
import com.veyline.app.ui.theme.DefaultVerticalSpace
import com.veyline.app.ui.theme.VeylineTheme
import com.veyline.app.ui.theme.withLineHeightFix

// 文件内私有实现属性遵循 Kotlin 的 lowerCamelCase 命名，以区别于对外提供的主题样式令牌。
private val itemTextStyle = TextStyle(
    fontSize = 15.sp,
    lineHeight = 19.sp,
    fontWeight = FontWeight.Normal,
).withLineHeightFix()

private val dividerThickness = 0.75.dp

@Composable
fun CitySelectionListItem(
    cityName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(VeylineTheme.colors.surface)
            .padding(
                horizontal = DefaultHorizontalSpace,
                vertical = DefaultVerticalSpace,
            ),
    ) {
        Text(
            text = cityName,
            style = itemTextStyle,
            color = VeylineTheme.colors.textContent,
        )
    }
}

/**
 * 城市选择列表中相邻 Item 之间的细分隔线。
 *
 * 该组件服务于城市选择列表当前的视觉样式，不属于全局通用分隔组件。
 * 如果以后其他类型的页面采用相同样式，应将其抽取为通用组件并重新命名。
 *
 * 分隔线应放在相邻 Item 之间，不应显示在列表末尾。
 */
@Composable
fun CitySelectionListDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = dividerThickness,
        color = VeylineTheme.colors.thinDivider,
    )
}

// ===== Preview 组件 =====
@Preview(name = "城市选择列表项", widthDp = 360)
@Preview(name = "城市选择列表项 - 暗色", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CitySelectionListItemPreview() {
    VeylineTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background),
        ) {
            CitySelectionListItem(cityName = "北京市")
            CitySelectionListDivider()
            CitySelectionListItem(cityName = "新疆维吾尔自治区")
        }
    }
}
