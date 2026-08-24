package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantCityDto
import com.veyline.app.feature.merchant.domain.model.MerchantCity
import java.util.logging.Logger

/**
 * 将商家城市网络模型列表转换为经过校验和清洗的领域模型列表。
 *
 * 缺少 code 或 name 的城市无法用于筛选，因此会被忽略；如果服务端返回了非空列表，但
 * 其中没有任何有效城市，则抛出 [InvalidApiDataException]，避免将异常数据误判为正常
 * 空列表。
 *
 * 城市接口一次返回完整的数据集合，因此本 Mapper 可以在输入范围内完成全量去重，并按
 * 接口原始顺序保留第一个相同 code 的城市。项目中大部分业务列表采用分页加载，单次映射
 * 只能看到当前页，无法可靠识别跨页重复；这类数据应由能够观察完整已加载范围的上层统一
 * 去重。
 *
 * 清洗过程只记录数据质量摘要，不输出服务端返回的城市内容。
 *
 * @throws InvalidApiDataException 原始列表非空但没有任何城市可以转换。
 */
internal fun List<MerchantCityDto>.toDomainModels(): List<MerchantCity> {
    val cityCodes = mutableSetOf<String>()
    val cities = ArrayList<MerchantCity>(this.size)
    var invalidCount = 0
    var duplicateCount = 0

    for (cityDto in this) {
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

        cities += MerchantCity(
            code = code,
            name = name,
        )
    }

    if (isNotEmpty() && cities.isEmpty()) {
        throw InvalidApiDataException(
            "Merchant city response contains no valid records",
        )
    }

    if (invalidCount > 0) {
        logger.warning(
            "Merchant city response contains $invalidCount invalid records; ignoring them",
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
