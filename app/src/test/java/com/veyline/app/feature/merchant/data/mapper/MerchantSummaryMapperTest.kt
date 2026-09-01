package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.image.ImageUrlResolver
import com.veyline.app.feature.merchant.data.remote.model.MerchantSummaryDto
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class MerchantSummaryMapperTest {

    private val imageUrlResolver = ImageUrlResolver(
        baseUrl = "https://example.test/images/",
    )

    private val mapper = MerchantSummaryMapper(
        imageUrlResolver = imageUrlResolver,
    )

    /** 验证字段完整的 DTO 被清洗并转换为领域模型。 */
    @Test
    fun map_withValidDto_returnsDomainModel() {
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
                cityCode = "city-a",
                intro = "商家简介",
                coverImageUrl = "https://example.test/images/merchant-a.jpg",
            ),
        )

        val result = mapper.map(listOf(merchantDto))
        assertEquals(expected, result)
    }

    /** 验证空的 DTO 列表被转换为空的领域模型列表。 */
    @Ignore("待实现")
    @Test
    fun map_withEmptyList_returnsEmptyList() {
    }

    /** 验证可选字段缺失时被转换为约定的空值。 */
    @Ignore("待实现")
    @Test
    fun map_withMissingOptionalFields_returnsEmptyOptionalValues() {
    }

    /** 验证有效和无效 DTO 混合时只忽略无效记录。 */
    @Ignore("待实现")
    @Test
    fun map_withPartiallyInvalidDtos_ignoresInvalidDtos() {
    }

    /** 验证非空列表中没有有效 DTO 时抛出数据异常。 */
    @Ignore("待实现")
    @Test
    fun map_withNoValidDtos_throwsInvalidApiDataException() {
    }

    /** 验证 Mapper 保留重复 ID，由分页层统一负责去重。 */
    @Ignore("待实现")
    @Test
    fun map_withDuplicateIds_preservesDuplicates() {
    }
}
