package com.veyline.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.veyline.app.ui.theme.*

/**
 * 通用卡片容器组件
 *
 * UI风格说明：
 * - 使用 Foundation 的 Box + background 替代 Material Card
 * - 移除 elevation 阴影效果，符合国内APP扁平化设计
 * - 使用简洁的圆角和背景色
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(CardContentPadding),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = VeylineTheme.colors.surface,
                shape = RoundedCornerShape(CardCornerRadius)
            )
            .padding(contentPadding)
    ) {
        content()
    }
}

// ===== Preview 组件 =====
@Preview(name = "默认卡片")
@Preview(name = "默认卡片 - 暗色", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppCardPreview() {
    VeylineTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppCard {
                Text(
                    text = "这是一个默认卡片",
                    style = VeylineTextStyles.Body,
                    color = VeylineTheme.colors.textContent
                )
            }

            AppCard(
                contentPadding = PaddingValues(16.dp)
            ) {
                Column {
                    Text(
                        text = "卡片标题",
                        style = VeylineTextStyles.Title,
                        color = VeylineTheme.colors.textTitle
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "这是卡片内容，可以包含多行文字和其他组件。",
                        style = VeylineTextStyles.Body,
                        color = VeylineTheme.colors.textContent
                    )
                }
            }

            AppCard(
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "无内边距的卡片",
                    style = VeylineTextStyles.Body,
                    color = VeylineTheme.colors.textContent,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
