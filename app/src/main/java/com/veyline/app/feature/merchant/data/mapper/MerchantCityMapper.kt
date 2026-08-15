package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantCityDto
import com.veyline.app.feature.merchant.domain.model.MerchantCity
import java.util.logging.Logger

/**
 * 将商家城市网络模型列表转换为经过校验的领域模型列表。
 *
 * 必要字段缺失或内容为空时，整批转换失败；城市代码重复时，按接口原始顺序保留第一条，
 * 忽略后续重复项并记录不包含原始字段值的警告日志。
 *
 * @throws InvalidApiDataException 任一城市缺少有效的 code 或 name。
 */
internal fun List<MerchantCityDto>.toDomainModels(): List<MerchantCity> {
    val cityCodes = mutableSetOf<String>()
    val cities = ArrayList<MerchantCity>(this.size)
    var duplicateCount = 0

    for ((index, cityDto) in this.withIndex()) {
        val code = cityDto.code?.trim()
        val name = cityDto.name?.trim()

        if (code.isNullOrEmpty()) {
            throw InvalidApiDataException(
                "Merchant city at index $index has a missing or blank code",
            )
        }
        if (name.isNullOrEmpty()) {
            throw InvalidApiDataException(
                "Merchant city at index $index has a missing or blank name",
            )
        }

        if (!cityCodes.add(code)) {
            duplicateCount++
            continue
        }

        cities += MerchantCity(
            code = code,
            name = name,
        )
    }

    if (duplicateCount > 0) {
        logger.warning(
            "Merchant city response contains $duplicateCount duplicate codes; " +
                "keeping first occurrences",
        )
    }

    return cities
}

/** Merchant 城市映射日志；仅记录数据质量摘要，不输出服务端返回的城市内容。 */
private val logger = Logger.getLogger("MerchantCityMapper")
