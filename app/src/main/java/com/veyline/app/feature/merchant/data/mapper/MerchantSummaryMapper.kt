package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.image.ImageUrlResolver
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantSummaryDto
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import java.util.logging.Logger
import javax.inject.Inject

class MerchantSummaryMapper @Inject constructor(
    private val imageUrlResolver: ImageUrlResolver,
) {

    fun map(merchantSummaryDtos: List<MerchantSummaryDto>): List<MerchantSummary> {
        val merchants = ArrayList<MerchantSummary>(merchantSummaryDtos.size)
        var invalidCount = 0

        for (merchantDto in merchantSummaryDtos) {
            val id = merchantDto.id?.trim()
            val name = merchantDto.name?.trim()
            val cityCode = merchantDto.cityCode?.trim()

            if (id.isNullOrEmpty() || name.isNullOrEmpty() || cityCode.isNullOrEmpty()) {
                invalidCount++
                continue
            }

            // 写注释
            val coverImagePath = merchantDto.coverPicture
                ?.trim()
                ?.takeIf { it.isNotEmpty() }

            merchants += MerchantSummary(
                id = id,
                name = name,
                cityCode = cityCode,
                intro = merchantDto.intro?.trim().orEmpty(),
                coverImageUrl = coverImagePath?.let {
                    imageUrlResolver.resolve(it)
                },
            )
        }

        if (merchantSummaryDtos.isNotEmpty() && merchants.isEmpty()) {
            throw InvalidApiDataException(
                "Merchant summary response contains no valid records",
            )
        }

        if (invalidCount > 0) {
            logger.warning(
                "Merchant summary response contains $invalidCount invalid records; ignoring them",
            )
        }

        return merchants
    }

    private companion object {
        private val logger = Logger.getLogger("MerchantSummaryMapper")
    }
}
