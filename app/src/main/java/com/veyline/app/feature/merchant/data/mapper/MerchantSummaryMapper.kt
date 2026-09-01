package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.image.ImageUrlResolver
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantSummaryDto
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import java.util.logging.Logger
import javax.inject.Inject

/**
 * 将商家列表网络模型转换为经过校验和清洗的领域模型。
 *
 * 缺少 id、name 或 cityCode 的记录无法支持列表展示、去重和导航，因此会被忽略；如果
 * 服务端返回了非空列表，但其中没有任何有效记录，则抛出 [InvalidApiDataException]，
 * 避免将异常数据误判为正常空列表。
 *
 * 有效的封面图片相对路径会通过 [ImageUrlResolver] 转换成完整请求地址，使领域模型和
 * UI 不需要了解图片域名及路径拼接规则。空白图片路径统一转换为 `null`。
 *
 * 本 Mapper 刻意不处理重复 ID。商家列表采用分页加载，单次映射无法识别跨页重复，
 * 因此统一交由能够观察已加载页面范围的 PagingSource 处理。清洗过程只记录数据质量摘要，
 * 不输出服务端返回的业务内容。
 *
 * @property imageUrlResolver 图片相对路径到完整请求地址的解析器。
 */
class MerchantSummaryMapper @Inject constructor(
    private val imageUrlResolver: ImageUrlResolver,
) {

    /**
     * 转换商家列表数据，同时过滤无法形成有效领域模型的记录。
     *
     * @throws InvalidApiDataException 原始列表非空但没有任何记录可以转换。
     */
    fun map(
        merchantSummaryDtos: List<MerchantSummaryDto>,
    ): List<MerchantSummary> {
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

            // 空图片路径统一转换为 null，只有有效路径才解析为完整请求地址
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
