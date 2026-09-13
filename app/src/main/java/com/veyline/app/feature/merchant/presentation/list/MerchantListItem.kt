package com.veyline.app.feature.merchant.presentation.list

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.veyline.app.R
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import com.veyline.app.ui.components.AppCard
import com.veyline.app.ui.theme.DividerHeight
import com.veyline.app.ui.theme.SpacingMedium
import com.veyline.app.ui.theme.SpacingSmall
import com.veyline.app.ui.theme.ThumbnailCornerRadius
import com.veyline.app.ui.theme.VeylineTextStyles
import com.veyline.app.ui.theme.VeylineTheme

/** 列表项最小总高度：封面图 120dp + 上下 7dp 内边距，字体放大导致内容变高时允许超过该值 */
private val MerchantItemMinHeight = 134.dp
private val MerchantCoverWidth = 96.dp
private val MerchantCoverHeight = 120.dp

/**
 * 商家列表项，展示商家名称、简介、地区标签及可选的封面图。
 *
 * 左侧文字内容与右侧封面图是同一个 [Row] 的两个直接子项，通过
 * `verticalAlignment = Alignment.CenterVertically` 把两者作为整体纵向居中：字号正常时
 * 以封面图的固定高度为主导，字号放大导致文字内容变高时，Row 整体随之撑高，封面图仍在
 * 其中保持居中，不会出现重叠或裁切。
 *
 * @param merchant 商家列表项数据
 * @param onClick 点击整个列表项时的回调
 * @param modifier 应用于列表项根容器的 [Modifier]
 */
@Composable
fun MerchantListItem(
    merchant: MerchantSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                role = Role.Button,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MerchantItemMinHeight)
                .padding(
                    start = SpacingMedium,
                    top = 7.dp,
                    end = 7.dp,
                    bottom = 7.dp,
                ),
            // 与右侧封面图整体纵向居中
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MerchantTextContent(
                merchant = merchant,
                modifier = Modifier
                    .weight(1f),
            )

            // 没有封面图时不占位，文字内容独占整行宽度
            merchant.coverImageUrl?.let { imageUrl ->
                Spacer(modifier = Modifier.width(SpacingMedium))

                MerchantCover(
                    imageModel = imageUrl,
                    merchantName = merchant.name,
                )
            }
        }
    }
}

/**
 * 商家列表项左侧的文字内容：名称、简介与底部地区/标签信息。
 *
 * 保持 [Column] 自然高度，由外层 [MerchantListItem] 的 Row 统一负责纵向居中，
 * 字号变化只影响本 Column 的自然高度，与右侧封面图的对齐方式一致。
 *
 * @param merchant 商家列表项数据
 * @param modifier 应用于 [Column] 根容器的 [Modifier]
 */
@Composable
private fun MerchantTextContent(
    merchant: MerchantSummary,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // 商家名称，最多显示一行，超长时在末尾省略
        Text(
            text = merchant.name,
            style = VeylineTextStyles.ItemTitle,
            color = VeylineTheme.colors.textItemTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(SpacingSmall))

        // 商家简介，最多显示两行，超长时在末尾省略
        Text(
            text = merchant.intro,
            style = VeylineTextStyles.Body,
            color = VeylineTheme.colors.textContent,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(SpacingMedium))

        // 底部地区与商家标识
        MerchantMetadataRow(
            provinceCode = merchant.provinceCode,
        )
    }
}

/**
 * 商家列表项底部一行：地区图标、地区代码与“商家”标识。
 *
 * 地区文字使用 `Modifier.weight(1f)` 并配合单行省略：地区名称过长或系统字体放大时
 * 会被压缩省略，而不会把右侧固定内容的“商家”标识挤出这一行的可视范围。
 *
 * @param provinceCode 商家所属地区代码，暂未转换为地区名称展示
 * @param modifier 应用于 [Row] 根容器的 [Modifier]
 */
@Composable
private fun MerchantMetadataRow(
    provinceCode: String,
    modifier: Modifier = Modifier,
) {
    // 地区图标跟随系统字体大小缩放，与同一行文字保持相对比例
    val iconSize = with(LocalDensity.current) { 16.sp.toDp() }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 地区图标与地区代码
        Icon(
            painter = painterResource(R.drawable.ic_location),
            contentDescription = null,
            tint = VeylineTheme.colors.primary,
            modifier = Modifier.size(iconSize),
        )
        Spacer(modifier = Modifier.width(SpacingSmall))
        Text(
            text = provinceCode,    // TODO 转换为城市/省份名显示
            style = VeylineTextStyles.Body,
            color = VeylineTheme.colors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // 防止地区名称过长时把“商家”标识挤出这一行
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(SpacingMedium))

        // “商家”标识
        Text(
            text = stringResource(R.string.merchant_label),
            style = VeylineTextStyles.Body,
            color = VeylineTheme.colors.textOnButton,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(VeylineTheme.colors.primary)
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/**
 * 商家封面缩略图，固定尺寸裁剪显示，不随系统字体缩放。
 *
 * @param imageModel 封面图完整请求地址
 * @param merchantName 商家名称，用于生成无障碍内容描述
 * @param modifier 应用于图片的 [Modifier]
 */
@Composable
private fun MerchantCover(
    imageModel: String,
    merchantName: String,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = imageModel,
        contentScale = ContentScale.Crop,
        placeholder = painterResource(R.drawable.placeholder_image_loading),
        error = painterResource(R.drawable.placeholder_image_error),
        contentDescription = stringResource(
            R.string.merchant_cover_content_description,
            merchantName,
        ),
        modifier = modifier
            .width(MerchantCoverWidth)
            .height(MerchantCoverHeight)
            .clip(RoundedCornerShape(ThumbnailCornerRadius)),
    )
}

// ===== Preview 组件 =====

/** 预览用的示例商家数据，各预览函数按需通过 [MerchantSummary.copy] 调整个别字段 */
private val previewMerchant = MerchantSummary(
    id = "1",
    name = "示例商家名称",
    provinceCode = "310000",
    intro = "环境优雅，服务专业，欢迎光临体验。",
    coverImageUrl = "https://example.com/image.jpg",
)

@Preview(name = "商家列表项")
@Preview(name = "商家列表项 - 暗色", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "商家列表项 - 大字体", fontScale = 2f)
@Composable
private fun MerchantListItemPreview() {
    VeylineTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background),
            verticalArrangement = Arrangement.spacedBy(DividerHeight),
        ) {
            // 默认场景：有封面图
            MerchantListItem(
                merchant = previewMerchant,
                onClick = {},
            )

            // 无封面图：文字内容独占整行宽度
            MerchantListItem(
                merchant = previewMerchant.copy(coverImageUrl = null),
                onClick = {},
            )
        }
    }
}

/**
 * 长文本组合场景：验证标题单行省略、简介两行省略、地区文字省略，
 * 以及大字体下 Row 是否仍保持整体纵向居中、不出现重叠或裁切。
 */
@Preview(name = "商家列表项 - 长内容")
@Preview(name = "商家列表项 - 长内容 - 大字体", fontScale = 2f)
@Composable
private fun MerchantListItemLongContentPreview() {
    VeylineTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VeylineTheme.colors.background),
        ) {
            MerchantListItem(
                merchant = previewMerchant.copy(
                    name = "商家名称特别长的情况下会被截断显示省略号",
                    intro = "这是一段很长的商家简介，用于验证在大字体或狭窄空间下，" +
                        "简介是否能够正确显示两行并在超出部分显示省略号。",
                    provinceCode = "用于测试地区文字过长时的省略效果",
                ),
                onClick = {},
            )
        }
    }
}
