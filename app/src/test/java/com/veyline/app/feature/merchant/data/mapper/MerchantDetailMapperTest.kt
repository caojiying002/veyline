package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.image.ImageUrlResolver
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantDetailDto
import com.veyline.app.feature.merchant.domain.model.MerchantContactAccess
import com.veyline.app.feature.merchant.domain.model.MerchantDetail
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MerchantDetailMapperTest {

    private val imageUrlResolver = ImageUrlResolver(
        baseUrl = "https://example.test/images/",
    )

    private val mapper = MerchantDetailMapper(
        imageUrlResolver = imageUrlResolver,
    )

    /** 验证字段完整的 DTO 被清洗并转换为商家详情领域模型。 */
    @Test
    fun map_withValidVipDto_returnsCleanedDomainModel() {
        val detailDto = MerchantDetailDto(
            id = " merchant-a ",
            name = " 商家甲 ",
            cityCode = " province-a ",
            picture = " first.jpg, ,second.jpg, /first.jpg ",
            desc = " 商家详情 ",
            vipProfileStatus = 1,
            contact = " 联系方式 ",
        )
        val expected = MerchantDetail(
            id = "merchant-a",
            name = "商家甲",
            provinceCode = "province-a",
            imageUrls = listOf(
                "https://example.test/images/first.jpg",
                "https://example.test/images/second.jpg",
            ),
            description = "商家详情",
            contactAccess = MerchantContactAccess.Available("联系方式"),
        )

        val result = mapper.map(detailDto)
        assertEquals(expected, result)
    }

    /** 验证可选字段缺失或为空白时被转换为约定的空值。 */
    @Test
    fun map_withMissingOptionalFields_returnsEmptyValuesAndUnavailableContact() {
        val detailDto = MerchantDetailDto(
            id = "merchant-a",
            name = "商家甲",
            cityCode = "province-a",
            picture = null,
            desc = "   ",
            vipProfileStatus = null,
            contact = null,
        )
        val expected = MerchantDetail(
            id = "merchant-a",
            name = "商家甲",
            provinceCode = "province-a",
            imageUrls = emptyList(),
            description = "",
            contactAccess = MerchantContactAccess.Unavailable,
        )

        val result = mapper.map(detailDto)
        assertEquals(expected, result)
    }

    /** 验证任一必要字段缺失或为空白时抛出无效数据异常。 */
    @Test
    fun map_withMissingRequiredFields_throwsInvalidApiDataException() {
        val validDto = MerchantDetailDto(
            id = "merchant-a",
            name = "商家甲",
            cityCode = "province-a",
            picture = null,
            desc = "商家详情",
            vipProfileStatus = 1,
            contact = null,
        )
        val invalidDtos = listOf(
            validDto.copy(id = null),
            validDto.copy(name = "   "),
            validDto.copy(cityCode = null),
        )

        invalidDtos.forEach { detailDto ->
            assertFailsWith<InvalidApiDataException> {
                mapper.map(detailDto)
            }
        }
    }

    /** 验证未登录状态即使包含联系方式，也只允许引导用户登录。 */
    @Test
    fun map_withGuestStatus_returnsLoginRequired() {
        val detailDto = createDtoForContactAccess(
            vipProfileStatus = 2,   // 未登录
            contact = "不应展示的联系方式",
        )

        val result = mapper.map(detailDto)
        assertEquals(
            MerchantContactAccess.LoginRequired,
            result.contactAccess,
        )
    }

    /** 验证普通会员状态即使包含联系方式，也只允许引导用户升级 VIP。 */
    @Test
    fun map_withMemberStatus_returnsVipRequired() {
        val detailDto = createDtoForContactAccess(
            vipProfileStatus = 3,   // 普通会员
            contact = "不应展示的联系方式",
        )

        val result = mapper.map(detailDto)
        assertEquals(
            MerchantContactAccess.VipRequired,
            result.contactAccess,
        )
    }

    /** 验证 VIP 状态缺少有效联系方式时不展示联系方式区域。 */
    @Test
    fun map_withVipStatusAndBlankContact_returnsUnavailable() {
        val detailDto = createDtoForContactAccess(
            vipProfileStatus = 1, // VIP
            contact = "   ",    // 正常情况下应有联系方式，这里用空白值模拟数据缺失
        )

        val result = mapper.map(detailDto)
        assertEquals(
            MerchantContactAccess.Unavailable,
            result.contactAccess,
        )
    }

    /** 验证无法识别的权限状态即使包含联系方式也不予展示。 */
    @Test
    fun map_withUnknownStatus_returnsUnavailable() {
        val detailDto = createDtoForContactAccess(
            vipProfileStatus = 99,
            contact = "不应展示的联系方式",
        )

        val result = mapper.map(detailDto)
        assertEquals(
            MerchantContactAccess.Unavailable,
            result.contactAccess,
        )
    }

    private fun createDtoForContactAccess(
        vipProfileStatus: Int?,
        contact: String?,
    ): MerchantDetailDto =
        MerchantDetailDto(
            id = "merchant-a",
            name = "商家甲",
            cityCode = "province-a",
            picture = null,
            desc = "商家详情",
            vipProfileStatus = vipProfileStatus,
            contact = contact,
        )
}
