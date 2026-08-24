package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantSummaryDto
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import java.util.logging.Logger

/**
 * 将商家摘要网络模型转换为经过校验和清洗的领域模型。
 *
 * 缺少 id、name 或 cityCode 的记录无法支持列表展示、去重和导航，因此会被忽略；如果
 * 服务端返回了非空列表，但其中没有任何有效记录，则抛出 [InvalidApiDataException]，
 * 避免将异常数据误判为正常空列表。
 *
 * 本 Mapper 刻意不处理重复 ID。商家摘要列表采用分页加载，单次映射只能看到当前页，
 * 无法可靠识别跨页重复；如果在这里进行页内去重，能够观察完整已加载范围的上层仍需再次
 * 处理跨页重复，反而会形成两套规则。因此，所有重复 ID 统一交由该上层处理。
 *
 * 清洗过程只记录数据质量摘要，不输出服务端返回的业务内容。
 *
 * @throws InvalidApiDataException 原始列表非空但没有任何记录可以转换。
 */
internal fun List<MerchantSummaryDto>.toDomainModels(): List<MerchantSummary> {
    val merchants = ArrayList<MerchantSummary>(size)
    var invalidCount = 0

    for (merchantDto in this) {
        val id = merchantDto.id?.trim()
        val name = merchantDto.name?.trim()
        val cityCode = merchantDto.cityCode?.trim()

        if (id.isNullOrEmpty() || name.isNullOrEmpty() || cityCode.isNullOrEmpty()) {
            invalidCount++
            continue
        }

        merchants += MerchantSummary(
            id = id,
            name = name,
            cityCode = cityCode,
            intro = merchantDto.intro?.trim().orEmpty(),
            coverImagePath = merchantDto.coverPicture
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        )
    }

    if (this.isNotEmpty() && merchants.isEmpty()) {
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

/** Merchant 摘要映射日志；仅记录数据质量摘要，不输出服务端返回的业务内容。 */
private val logger = Logger.getLogger("MerchantSummaryMapper")
