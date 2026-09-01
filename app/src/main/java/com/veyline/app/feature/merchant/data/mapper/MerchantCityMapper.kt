package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantCityDto
import com.veyline.app.feature.merchant.domain.model.MerchantCity
import java.util.logging.Logger

internal object MerchantCityMapper {

    fun map(merchantCityDtos: List<MerchantCityDto>): List<MerchantCity> {
        val cityCodes = mutableSetOf<String>()
        val merchantCities = ArrayList<MerchantCity>(merchantCityDtos.size)
        var invalidCount = 0
        var duplicateCount = 0

        for (cityDto in merchantCityDtos) {
            val code = cityDto.code?.trim()
            val name = cityDto.name?.trim()

            if (code.isNullOrEmpty() || name.isNullOrEmpty()) {
                invalidCount++
                continue
            }

            if (!cityCodes.add(code)) {
                duplicateCount++
                continue
            }

            merchantCities += MerchantCity(
                code = code,
                name = name,
            )
        }

        if (merchantCityDtos.isNotEmpty() && merchantCities.isEmpty()) {
            throw InvalidApiDataException(
                "Merchant city response contains no valid records",
            )
        }
        if (invalidCount > 0) {
            logger.warning(
                "Merchant city response contains $invalidCount invalid records; " +
                        "ignoring them",
            )
        }
        if (duplicateCount > 0) {
            logger.warning(
                "Merchant city response contains $duplicateCount duplicate codes; " +
                        "keeping first occurrences",
            )
        }

        return merchantCities
    }

    private val logger = Logger.getLogger("MerchantCityMapper")
}
