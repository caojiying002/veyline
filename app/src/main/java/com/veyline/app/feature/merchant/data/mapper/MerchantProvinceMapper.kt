package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantProvinceDto
import com.veyline.app.feature.merchant.domain.model.MerchantProvince
import java.util.logging.Logger

/**
 * 将商家省份网络模型转换为经过校验、清洗和去重的领域模型。
 *
 * 缺少 code 或 name 的省份无法用于筛选，会被忽略；如果服务端返回了非空列表，却没有
 * 任何有效省份，则抛出 [InvalidApiDataException]，避免把异常数据误判为正常的空列表。
 *
 * 省份接口一次性返回完整集合，因此可以对整份输入做全量去重，并按接口原始顺序保留
 * 同一 code 的第一条记录。清洗过程只记录数据质量摘要，不写出具体省份内容。
 */
internal object MerchantProvinceMapper {

    /**
     * 转换商家省份数据，过滤无效记录并按规范化后的 code 去重。
     *
     * @throws InvalidApiDataException 原始列表非空，但没有任何省份可以转换。
     */
    fun map(
        merchantProvinceDtos: List<MerchantProvinceDto>,
    ): List<MerchantProvince> {
        val provinceCodes = mutableSetOf<String>()
        val merchantProvinces = ArrayList<MerchantProvince>(merchantProvinceDtos.size)
        var invalidCount = 0
        var duplicateCount = 0

        for (provinceDto in merchantProvinceDtos) {
            val code = provinceDto.code?.trim()
            val name = provinceDto.name?.trim()

            if (code.isNullOrEmpty() || name.isNullOrEmpty()) {
                invalidCount++
                continue
            }

            if (!provinceCodes.add(code)) {
                duplicateCount++
                continue
            }

            merchantProvinces += MerchantProvince(
                code = code,
                name = name,
            )
        }

        if (merchantProvinceDtos.isNotEmpty() && merchantProvinces.isEmpty()) {
            throw InvalidApiDataException(
                "Merchant province response contains no valid records",
            )
        }
        if (invalidCount > 0) {
            logger.warning(
                "Merchant province response contains $invalidCount invalid records; " +
                        "ignoring them",
            )
        }
        if (duplicateCount > 0) {
            logger.warning(
                "Merchant province response contains $duplicateCount duplicate codes; " +
                        "keeping first occurrences",
            )
        }

        return merchantProvinces
    }

    private val logger = Logger.getLogger("MerchantProvinceMapper")
}
