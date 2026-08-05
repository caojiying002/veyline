package com.veyline.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.veyline.app.ui.theme.*

/**
 * 通用按钮组件
 * 对应View版本的@style/ButtonStyle样式
 *
 * UI风格说明：
 * - 使用 Foundation 组件（Box + clickable）替代 Material 的 Surface
 * - 移除 elevation 阴影效果，符合国内APP扁平化设计
 * - 移除水波纹效果（indication = null）
 * - 保留按下状态的颜色变化反馈
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shape = RoundedCornerShape(ButtonCornerRadius)
    val backgroundColor = when {
        !enabled -> VeylineTheme.colors.buttonDisabled
        isPressed -> VeylineTheme.colors.buttonPressed
        else -> VeylineTheme.colors.buttonDefault
    }

    Box(
        modifier = modifier
            .height(ButtonHeight) // 48dp，与View版本按钮高度保持一致
            .clip(shape)
            .background(backgroundColor)
            .clickable(
                enabled = enabled,
                indication = null,  // 移除水波纹，符合国内APP风格
                interactionSource = interactionSource,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = VeylineTextStyles.Button,
            color = VeylineTheme.colors.textOnButton,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 次要按钮组件（边框+透明背景）
 * 对应View版本的@style/SecondaryButtonStyle样式
 *
 * UI风格说明：
 * - 透明背景 + 边框设计
 * - 按下时有浅色背景反馈
 * - 移除水波纹效果
 * - 常用于对话框取消等次要操作
 */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shape = RoundedCornerShape(ButtonCornerRadius)
    val backgroundColor = when {
        !enabled -> Color.Transparent
        isPressed -> VeylineTheme.colors.buttonPressedSecondary
        else -> Color.Transparent
    }
    val borderColor = when {
        !enabled -> VeylineTheme.colors.buttonDisabled
        else -> VeylineTheme.colors.primary
    }

    val textColor = when {
        !enabled -> VeylineTheme.colors.buttonDisabled
        else -> VeylineTheme.colors.primary
    }

    Box(
        modifier = modifier
            .height(ButtonHeight)
            .clip(shape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = interactionSource,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = VeylineTextStyles.Button,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

// ===== Preview 组件 =====
@Preview(name = "默认按钮 - 亮色")
@Preview(name = "默认按钮 - 暗色", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppButtonPreview() {
    VeylineTheme {
        Column(
            modifier = Modifier
                .background(VeylineTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppButton(
                text = "确定",
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            AppButton(
                text = "取消",
                onClick = {},
                modifier = Modifier.width(120.dp)
            )

            AppButton(
                text = "禁用状态",
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "次要按钮 - 亮色")
@Preview(name = "次要按钮 - 暗色", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppSecondaryButtonPreview() {
    VeylineTheme {
        Column(
            modifier = Modifier
                .background(VeylineTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppSecondaryButton(
                text = "取消",
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )

            AppSecondaryButton(
                text = "返回",
                onClick = {},
                modifier = Modifier.width(120.dp)
            )

            AppSecondaryButton(
                text = "禁用状态",
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "按钮组合 - 亮色")
@Preview(name = "按钮组合 - 暗色", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppButtonCombinedPreview() {
    VeylineTheme {
        Column(
            modifier = Modifier
                .background(VeylineTheme.colors.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 对话框按钮组合示例
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                AppSecondaryButton(
                    text = "取消",
                    onClick = {},
                    modifier = Modifier.padding(end = 12.dp)
                )
                AppButton(
                    text = "确定",
                    onClick = {}
                )
            }
        }
    }
}
