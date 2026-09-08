package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantProvinceDto
import com.veyline.app.feature.merchant.domain.model.MerchantProvince
import java.util.logging.Logger

/**
 * 将商家城市网络模型转换为经过校验、清洗和去重的领域模型。
 *
 * 缺少 code 或 name 的城市无法用于筛选，因此会被忽略；如果服务端返回了非空列表，但
 * 其中没有任何有效城市，则抛出 [InvalidApiDataException]，避免将异常数据误判为正常
 * 空列表。
 *
 * 城市接口一次返回完整数据集合，因此可以在当前输入范围内完成全量去重，并按接口原始
 * 顺序保留第一个相同 code 的城市。清洗过程只记录数据质量摘要，不输出城市内容。
 */
internal object MerchantProvinceMapper {

    /**
     * 转换商家城市数据，同时过滤无效记录并按规范化后的 code 去重。
     *
     * @throws InvalidApiDataException 原始列表非空但没有任何城市可以转换。
     */
    fun map(
        merchantProvinceDtos: List<MerchantProvinceDto>,
    ): List<MerchantProvince> {
        val provinceCodes = mutableSetOf<String>()
        val merchantCities = ArrayList<MerchantProvince>(merchantProvinceDtos.size)
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

            merchantCities += MerchantProvince(
                code = code,
                name = name,
            )
        }

        if (merchantProvinceDtos.isNotEmpty() && merchantCities.isEmpty()) {
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

        return merchantCities
    }

    private val logger = Logger.getLogger("MerchantProvinceMapper")
}
