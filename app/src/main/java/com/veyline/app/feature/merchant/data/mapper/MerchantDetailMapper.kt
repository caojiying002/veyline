package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.image.ImageUrlResolver
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantDetailDto
import com.veyline.app.feature.merchant.domain.model.MerchantDetail
import javax.inject.Inject

/**
 * 将商家详情网络模型转换为经过校验和清洗的领域模型。
 *
 * 缺少 id、name 或 cityCode 时说明响应本身不完整，无法支持详情页展示，因此直接抛出
 * [InvalidApiDataException]；这三项与 [MerchantSummaryMapper] 校验的必要字段一致。
 *
 * 服务端使用统一的 `cityCode` 字段表示地区筛选代码，但商家详情中该字段实际表示省级
 * 行政区代码，转换到 [MerchantDetail] 时使用 `provinceCode`，与 [MerchantSummaryMapper]
 * 的命名转换保持一致。
 *
 * `picture` 以英文逗号分隔多张图片的相对路径，逐个清理后通过 [ImageUrlResolver] 转换成
 * 完整请求地址；不同的原始路径解析后可能指向同一张图片（例如是否带开头斜杠），因此
 * 最终会对完整地址去重。`desc`、`contact` 等可选字段的空白内容统一转换为 `null`。
 *
 * @property imageUrlResolver 图片相对路径到完整请求地址的解析器
 */
class MerchantDetailMapper @Inject constructor(
    private val imageUrlResolver: ImageUrlResolver,
) {

    /**
     * 转换商家详情数据。
     *
     * @throws InvalidApiDataException 响应缺少 id、name 或 cityCode 等必要字段
     */
    fun map(detailDto: MerchantDetailDto): MerchantDetail {
        val id = detailDto.id?.trim()
        val name = detailDto.name?.trim()
        val provinceCode = detailDto.cityCode?.trim()

        if (id.isNullOrEmpty() || name.isNullOrEmpty() || provinceCode.isNullOrEmpty()) {
            throw InvalidApiDataException(
                "Merchant detail response is missing required fields",
            )
        }

        val imageUrls = detailDto.picture
            .orEmpty()
            .split(',')
            .mapNotNull { imagePath ->
                imagePath.trim().takeIf { it.isNotEmpty() }
            }
            .map { imagePath ->
                imageUrlResolver.resolve(imagePath)
            }
            .distinct()

        val description = detailDto.desc?.trim()?.takeIf { it.isNotEmpty() }
        val contact = detailDto.contact?.trim()?.takeIf { it.isNotEmpty() }

        return MerchantDetail(
            id = id,
            name = name,
            provinceCode = provinceCode,
            imageUrls = imageUrls,
            description = description,
            contact = contact,
        )
    }
}
