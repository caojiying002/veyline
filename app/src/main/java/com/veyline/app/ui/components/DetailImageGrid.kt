package com.veyline.app.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.veyline.app.R
import com.veyline.app.ui.theme.SpacingMedium
import com.veyline.app.ui.theme.SpacingSmall
import com.veyline.app.ui.theme.ThumbnailCornerRadius
import com.veyline.app.ui.theme.VeylineTheme

/** 网格最多展示的图片数量，属于展示层的限制，不代表数据本身的图片数量上限 */
private const val MAX_VISIBLE_IMAGE_COUNT = 4

/**
 * 详情页的缩略图网格，在一行内最多展示 [MAX_VISIBLE_IMAGE_COUNT] 张图片。
 *
 * 网格固定按 4 等分排布：图片不足 4 张时，空位保持留白，已有图片仍只占 1/4 宽度，
 * 不会被拉伸铺满整行；传入的图片超过 4 张时，只展示前 4 张。
 *
 * 图片列表为空时不渲染任何内容，调用方手动添加的配套间距需要自行一并隐藏。
 *
 * 图片是否可点击、点击后做什么完全由 [onImageClick] 决定。例如只有 VIP 用户才能打开
 * 大图的场景，由调用方在回调里判断，组件本身不感知用户权限。
 *
 * @param imageUrls 可直接请求的图片完整地址，图片域名拼接应在数据层完成
 * @param onImageClick 点击图片时的回调，参数为该图片在 [imageUrls] 中的索引
 * @param modifier 应用于网格卡片根容器的 [Modifier]
 */
@Composable
fun DetailImageGrid(
    imageUrls: List<String>,
    onImageClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 无图片时组件不渲染任何内容；
    // 如果调用方在组件前后手动加了固定间距，需要自行判断是否要一并隐藏，避免有图/无图两种情况间距不一致。
    if (imageUrls.isEmpty()) {
        return
    }

    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 实际展示的图片数量，用于无障碍描述里的“第 N/M 张”
            val imageCount = minOf(imageUrls.size, MAX_VISIBLE_IMAGE_COUNT)

            // 按固定的位置数遍历而不是按 imageUrls 遍历，保证每个位置都占 1/4 宽度
            for (index in 0 until MAX_VISIBLE_IMAGE_COUNT) {
                val imageUrl = imageUrls.getOrNull(index)

                when (imageUrl) {
                    null -> {
                        // 没有图片的位置用 Spacer 占位，保持 1/4 宽度
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    else -> {
                        // 有图片的位置用 DetailImageGridItem 展示，通过 weight(1f) 同样占 1/4 宽度
                        DetailImageGridItem(
                            imageUrl = imageUrl,
                            imageIndex = index,
                            imageCount = imageCount,
                            onClick = { onImageClick(index) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 网格中的单张图片，固定 2:3 的宽高比，并裁剪为圆角缩略图。
 *
 * 宽高比和圆角由组件自己决定，调用方传入的 [modifier] 只负责在父布局中的排布（如 `weight`）。
 * 点击时不显示水波纹，符合项目的扁平化风格，并通过 [Role.Image] 向无障碍服务表明这是
 * 可点击的图片。
 *
 * @param imageUrl 图片完整地址
 * @param imageIndex 图片在网格中的位置，从 0 开始
 * @param imageCount 网格中实际展示的图片数量，与 [imageIndex] 一起生成“第 N/M 张图片”的无障碍描述
 * @param onClick 点击图片时的回调
 * @param modifier 应用于图片的 [Modifier]
 */
@Composable
private fun DetailImageGridItem(
    imageUrl: String,
    imageIndex: Int,
    imageCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = imageUrl,
        contentScale = ContentScale.Crop,
        placeholder = painterResource(R.drawable.placeholder_image_loading),
        error = painterResource(R.drawable.placeholder_image_error),
        contentDescription = stringResource(
            R.string.detail_image_content_description,
            imageIndex + 1,
            imageCount,
        ),
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(ThumbnailCornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Image,
                onClick = onClick,
            ),
    )
}

// ===== Preview 组件 =====

private val previewImageUrls = List(4) { index ->
    "https://example.com/image-${index + 1}.jpg"
}

// 依次展示 1 张、2 张、4 张图片的网格，检查不足 4 张时空位是否正确留白
@Preview(name = "详情图片网格")
@Preview(name = "详情图片网格 - 暗色", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetailImageGridPreview() {
    VeylineTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background)
                .padding(SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(SpacingMedium),
        ) {
            DetailImageGrid(
                imageUrls = previewImageUrls.take(1),
                onImageClick = {},
            )
            DetailImageGrid(
                imageUrls = previewImageUrls.take(2),
                onImageClick = {},
            )
            DetailImageGrid(
                imageUrls = previewImageUrls,
                onImageClick = {},
            )
        }
    }
}
