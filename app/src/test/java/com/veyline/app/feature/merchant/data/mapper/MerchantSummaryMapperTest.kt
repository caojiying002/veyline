package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.image.ImageUrlResolver
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantSummaryDto
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MerchantSummaryMapperTest {

    private val imageUrlResolver = ImageUrlResolver(
        baseUrl = "https://example.test/images/",
    )

    private val mapper = MerchantSummaryMapper(
        imageUrlResolver = imageUrlResolver,
    )

    /** 验证字段完整的 DTO 被清洗并转换为领域模型。 */
    @Test
    fun map_withValidDto_returnsCleanedDomainModel() {
        val merchantDto = MerchantSummaryDto(
            id = " merchant-a ",
            name = " 商家甲 ",
            cityCode = " city-a ",
            intro = " 商家简介 ",
            coverPicture = " merchant-a.jpg ",
        )
        val expected = listOf(
            MerchantSummary(
                id = "merchant-a",
                name = "商家甲",
                provinceCode = "city-a",
                intro = "商家简介",
                coverImageUrl = "https://example.test/images/merchant-a.jpg",
            ),
        )

        val result = mapper.map(listOf(merchantDto))
        assertEquals(expected, result)
    }

    /** 验证空的 DTO 列表被转换为空的领域模型列表。 */
    @Test
    fun map_withEmptyList_returnsEmptyList() {
        val expected = emptyList<MerchantSummary>()

        val result = mapper.map(emptyList())
        assertEquals(expected, result)
    }

    /** 验证可选字段缺失时被转换为约定的空值。 */
    @Test
    fun map_withMissingOptionalFields_returnsEmptyOptionalValues() {
        val merchantDto = MerchantSummaryDto(
            id = "merchant-a",
            name = "商家甲",
            cityCode = "province-a",
            intro = null,
            coverPicture = "   ",
        )
        val expected = listOf(
            MerchantSummary(
                id = "merchant-a",
                name = "商家甲",
                provinceCode = "province-a",
                intro = "",
                coverImageUrl = null,
            ),
        )

        val result = mapper.map(listOf(merchantDto))
        assertEquals(expected, result)
    }

    /** 验证混合输入会过滤掉无效数据，但 ID 重复的有效数据都会保留。 */
    @Test
    fun map_withMixedDtos_filtersInvalidAndPreservesDuplicates() {
        val firstDto = MerchantSummaryDto(
            id = "merchant-a",
            name = "商家甲",
            cityCode = "province-a",
            intro = "第一条简介",
            coverPicture = null,
        )
        val duplicateDto = firstDto.copy(
            name = "商家甲副本",
            intro = "第二条简介",
        )
        val invalidDto = firstDto.copy(
            id = null,
        )

        // 过滤了 `invalidDto`，但相同ID的两个对象都被保留
        val expected = listOf(
            MerchantSummary(
                id = "merchant-a",
                name = "商家甲",
                provinceCode = "province-a",
                intro = "第一条简介",
                coverImageUrl = null,
            ),
            MerchantSummary(
                id = "merchant-a",
                name = "商家甲副本",
                provinceCode = "province-a",
                intro = "第二条简介",
                coverImageUrl = null,
            ),
        )

        val result = mapper.map(
            listOf(firstDto, invalidDto, duplicateDto),
        )
        assertEquals(expected, result)
    }

    /** 验证非空列表中没有有效 DTO 时抛出数据异常。 */
    @Test
    fun map_withNoValidDtos_throwsInvalidApiDataException() {
        val invalidDto = MerchantSummaryDto(
            id = null,
            name = "   ",
            cityCode = null,
            intro = null,
            coverPicture = null,
        )

        assertFailsWith<InvalidApiDataException> {
            mapper.map(listOf(invalidDto))
        }
    }
}
