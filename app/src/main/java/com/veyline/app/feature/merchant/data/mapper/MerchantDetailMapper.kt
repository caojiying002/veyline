package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.image.ImageUrlResolver
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantDetailDto
import com.veyline.app.feature.merchant.domain.model.MerchantContactAccess
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
 * 最终会对完整地址去重。
 *
 * `desc` 缺失或空白时转换为空字符串。联系方式不作为独立字段输出，而是结合服务端返回的
 * 会员状态 `vipProfileStatus` 转换为 [MerchantContactAccess]：
 * - VIP 且联系方式非空白：[MerchantContactAccess.Available]
 * - 未登录：[MerchantContactAccess.LoginRequired]
 * - 已登录的非 VIP：[MerchantContactAccess.VipRequired]
 * - 其余情况（会员状态缺失或无法识别，或 VIP 但没有有效的联系方式）：
 *   [MerchantContactAccess.Unavailable]
 *
 * 会员状态的取值含义因接口而异，商家详情的取值定义在本类的私有常量中，不与其他接口共用。
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

        val description = detailDto.desc?.trim().orEmpty()
        val contactAccess = mapContactAccess(
            vipProfileStatus = detailDto.vipProfileStatus,
            contact = detailDto.contact
                ?.trim()
                ?.takeIf { it.isNotEmpty() },
        )

        return MerchantDetail(
            id = id,
            name = name,
            provinceCode = provinceCode,
            imageUrls = imageUrls,
            description = description,
            contactAccess = contactAccess,
        )
    }

    private fun mapContactAccess(
        vipProfileStatus: Int?,
        contact: String?,
    ): MerchantContactAccess =
        when (vipProfileStatus) {
            // VIP 但商家没填联系方式时没有可展示的内容，和无法识别的状态一样归入 Unavailable
            STATUS_VIP ->
                if (contact == null) MerchantContactAccess.Unavailable
                else MerchantContactAccess.Available(contact)

            STATUS_GUEST -> MerchantContactAccess.LoginRequired
            STATUS_MEMBER -> MerchantContactAccess.VipRequired

            // 包括 null 和没见过的取值
            else -> MerchantContactAccess.Unavailable
        }

    private companion object {
        // 商家详情接口 vipProfileStatus 的取值，2026-09 再次确认。
        // 与信息接口的同名字段含义不同（那边 1=未登录、4=VIP），不要跟信息共用常量
        const val STATUS_VIP = 1
        const val STATUS_GUEST = 2
        const val STATUS_MEMBER = 3
    }
}
