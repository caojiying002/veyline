package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantProvinceDto
import com.veyline.app.feature.merchant.domain.model.MerchantProvince
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

/**
 * 验证商家地区网络模型到领域模型的转换规则。
 *
 * 测试覆盖正常转换、字段规范化、非法字段和重复省份代码的降级处理。
 */
class MerchantProvinceMapperTest {

    /** 验证合法的省份网络模型列表被转换为对应的领域模型列表。 */
    @Test
    fun map_withValidProvinces_returnsDomainModels() {
        val provinceDtos = listOf(
            MerchantProvinceDto(code = "code-a", name = "省份甲"),
            MerchantProvinceDto(code = "code-b", name = "省份乙"),
        )
        val expected = listOf(
            MerchantProvince(code = "code-a", name = "省份甲"),
            MerchantProvince(code = "code-b", name = "省份乙"),
        )

        val result = MerchantProvinceMapper.map(provinceDtos)

        assertEquals(expected, result)
    }

    /** 验证省份 code 和 name 的首尾空白不会进入领域模型。 */
    @Test
    fun map_withSurroundingWhitespace_trimsFields() {
        val provinceDtos = listOf(
            MerchantProvinceDto(
                code = "  code-a  ",
                name = "  省份甲  ",
            ),
        )
        val expected = listOf(
            MerchantProvince(
                code = "code-a",
                name = "省份甲",
            ),
        )

        val result = MerchantProvinceMapper.map(provinceDtos)

        assertEquals(expected, result)
    }

    /** 验证空的网络模型列表被转换为空的领域模型列表。 */
    @Test
    fun map_withEmptyList_returnsEmptyList() {
        val provinceDtos = emptyList<MerchantProvinceDto>()

        val result = MerchantProvinceMapper.map(provinceDtos)

        assertEquals(emptyList(), result)
    }

    /** 验证规范化后 code 重复时保留接口中第一次出现的省份。 */
    @Test
    fun map_withDuplicateCodes_keepsFirstProvince() {
        val provinceDtos = listOf(
            MerchantProvinceDto(code = "code-a", name = "省份甲"),
            MerchantProvinceDto(code = "  code-a  ", name = "重复省份"),
            MerchantProvinceDto(code = "code-b", name = "省份乙"),
        )
        val expected = listOf(
            MerchantProvince(code = "code-a", name = "省份甲"),
            MerchantProvince(code = "code-b", name = "省份乙"),
        )

        val result = MerchantProvinceMapper.map(provinceDtos)

        assertEquals(expected, result)
    }

    /** 验证非法省份会被忽略，且不会影响其他合法省份的转换。 */
    @Test
    fun map_withInvalidProvinces_ignoresThem() {
        val provinceDtos = listOf(
            MerchantProvinceDto(code = "code-a", name = "省份甲"),
            MerchantProvinceDto(code = null, name = "缺少代码"),
            MerchantProvinceDto(code = "   ", name = "代码为空"),
            MerchantProvinceDto(code = "code-b", name = null),
            MerchantProvinceDto(code = "code-c", name = "   "),
            MerchantProvinceDto(code = "code-b", name = "省份乙"),
        )
        val expected = listOf(
            MerchantProvince(code = "code-a", name = "省份甲"),
            MerchantProvince(code = "code-b", name = "省份乙"),
        )

        val result = MerchantProvinceMapper.map(provinceDtos)

        assertEquals(expected, result)
    }

    /** 验证非空输入中没有任何有效省份时抛出数据无效异常。 */
    @Test
    fun map_withNoValidProvinces_throwsInvalidDataException() {
        val provinceDtos = listOf(
            MerchantProvinceDto(
                code = null,
                name = "省份甲",
            ),
            MerchantProvinceDto(
                code = "   ",
                name = "省份乙",
            ),
            MerchantProvinceDto(
                code = "code-c",
                name = null,
            ),
            MerchantProvinceDto(
                code = "code-d",
                name = "   ",
            ),
        )

        assertFailsWith<InvalidApiDataException> {
            MerchantProvinceMapper.map(provinceDtos)
        }
    }
}
