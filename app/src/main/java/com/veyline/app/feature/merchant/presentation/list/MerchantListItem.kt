package com.veyline.app.feature.merchant.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.veyline.app.R
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import com.veyline.app.ui.components.AppCard
import com.veyline.app.ui.theme.SpacingMedium
import com.veyline.app.ui.theme.SpacingSmall
import com.veyline.app.ui.theme.ThumbnailCornerRadius
import com.veyline.app.ui.theme.VeylineTextStyles
import com.veyline.app.ui.theme.VeylineTheme

private val MerchantItemMinHeight = 134.dp
private val MerchantCoverWidth = 96.dp
private val MerchantCoverHeight = 120.dp

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
        ) {
            MerchantTextContent(
                merchant = merchant,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MerchantCoverHeight),
            )

            //
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

@Composable
private fun MerchantTextContent(
    merchant: MerchantSummary,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        //
        Text(
            text = merchant.name,
            style = VeylineTextStyles.ItemTitle,
            color = VeylineTheme.colors.textItemTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        //
        if (merchant.intro.isNotEmpty()) {
            Spacer(modifier = Modifier.height(SpacingSmall))

            Text(
                text = merchant.intro,
                style = VeylineTextStyles.Body,
                color = VeylineTheme.colors.textContent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        //
        MerchantMetadataRow(
            cityCode = merchant.cityCode,
        )
    }
}

@Composable
private fun MerchantMetadataRow(
    cityCode: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 地区图标与 cityCode
        Icon(
            painter = painterResource(R.drawable.ic_location),
            contentDescription = null,
            tint = VeylineTheme.colors.primary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(SpacingSmall))
        Text(
            text = cityCode,    // TODO 转换为城市/省份名显示
            style = VeylineTextStyles.Body,
            color = VeylineTheme.colors.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(SpacingMedium))

        // “商家”标识
        Text(
            text = stringResource(R.string.merchant_label),
            style = VeylineTextStyles.Body,
            color = VeylineTheme.colors.textLight,
            maxLines = 1,
        )
    }
}

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
