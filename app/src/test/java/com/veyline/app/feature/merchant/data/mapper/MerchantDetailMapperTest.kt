package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.image.ImageUrlResolver
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantDetailDto
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
    fun map_withValidDto_returnsCleanedDomainModel() {
        val detailDto = MerchantDetailDto(
            id = " merchant-a ",
            name = " 商家甲 ",
            cityCode = " province-a ",
            picture = " first.jpg, ,second.jpg, /first.jpg ",
            desc = " 商家详情 ",
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
            contact = "联系方式",
        )

        val result = mapper.map(detailDto)
        assertEquals(expected, result)
    }

    /** 验证可选字段缺失或为空白时被转换为约定的空值。 */
    @Test
    fun map_withMissingOptionalFields_returnsEmptyOptionalValues() {
        val detailDto = MerchantDetailDto(
            id = "merchant-a",
            name = "商家甲",
            cityCode = "province-a",
            picture = null,
            desc = "   ",
            contact = null,
        )
        val expected = MerchantDetail(
            id = "merchant-a",
            name = "商家甲",
            provinceCode = "province-a",
            imageUrls = emptyList(),
            description = "",
            contact = null,
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
}
