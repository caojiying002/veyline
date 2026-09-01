package com.veyline.app.feature.merchant.data.mapper

import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.merchant.data.remote.model.MerchantCityDto
import com.veyline.app.feature.merchant.domain.model.MerchantCity
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

/**
 * 验证商家城市网络模型到领域模型的转换规则。
 *
 * 测试覆盖正常转换、字段规范化、非法字段和重复城市代码的降级处理。
 */
class MerchantCityMapperTest {

    /** 验证合法的城市网络模型列表被转换为对应的领域模型列表。 */
    @Test
    fun map_withValidCities_returnsDomainModels() {
        val cityDtos = listOf(
            MerchantCityDto(code = "code-a", name = "城市甲"),
            MerchantCityDto(code = "code-b", name = "城市乙"),
        )
        val expected = listOf(
            MerchantCity(code = "code-a", name = "城市甲"),
            MerchantCity(code = "code-b", name = "城市乙"),
        )

        val result = MerchantCityMapper.map(cityDtos)

        assertEquals(expected, result)
    }

    /** 验证城市 code 和 name 的首尾空白不会进入领域模型。 */
    @Test
    fun map_withSurroundingWhitespace_trimsFields() {
        val cityDtos = listOf(
            MerchantCityDto(
                code = "  code-a  ",
                name = "  城市甲  ",
            ),
        )
        val expected = listOf(
            MerchantCity(
                code = "code-a",
                name = "城市甲",
            ),
        )

        val result = MerchantCityMapper.map(cityDtos)

        assertEquals(expected, result)
    }

    /** 验证空的网络模型列表被转换为空的领域模型列表。 */
    @Test
    fun map_withEmptyList_returnsEmptyList() {
        val cityDtos = emptyList<MerchantCityDto>()

        val result = MerchantCityMapper.map(cityDtos)

        assertEquals(emptyList(), result)
    }

    /** 验证规范化后 code 重复时保留接口中第一次出现的城市。 */
    @Test
    fun map_withDuplicateCodes_keepsFirstCity() {
        val cityDtos = listOf(
            MerchantCityDto(code = "code-a", name = "城市甲"),
            MerchantCityDto(code = "  code-a  ", name = "重复城市"),
            MerchantCityDto(code = "code-b", name = "城市乙"),
        )
        val expected = listOf(
            MerchantCity(code = "code-a", name = "城市甲"),
            MerchantCity(code = "code-b", name = "城市乙"),
        )

        val result = MerchantCityMapper.map(cityDtos)

        assertEquals(expected, result)
    }

    /** 验证非法城市会被忽略，且不会影响其他合法城市的转换。 */
    @Test
    fun map_withInvalidCities_ignoresThem() {
        val cityDtos = listOf(
            MerchantCityDto(code = "code-a", name = "城市甲"),
            MerchantCityDto(code = null, name = "缺少代码"),
            MerchantCityDto(code = "   ", name = "代码为空"),
            MerchantCityDto(code = "code-b", name = null),
            MerchantCityDto(code = "code-c", name = "   "),
            MerchantCityDto(code = "code-b", name = "城市乙"),
        )
        val expected = listOf(
            MerchantCity(code = "code-a", name = "城市甲"),
            MerchantCity(code = "code-b", name = "城市乙"),
        )

        val result = MerchantCityMapper.map(cityDtos)

        assertEquals(expected, result)
    }

    /** 验证非空输入中没有任何有效城市时抛出数据无效异常。 */
    @Test
    fun map_withNoValidCities_throwsInvalidDataException() {
        val cityDtos = listOf(
            MerchantCityDto(
                code = null,
                name = "城市甲",
            ),
            MerchantCityDto(
                code = "   ",
                name = "城市乙",
            ),
            MerchantCityDto(
                code = "code-c",
                name = null,
            ),
            MerchantCityDto(
                code = "code-d",
                name = "   ",
            ),
        )

        assertFailsWith<InvalidApiDataException> {
            MerchantCityMapper.map(cityDtos)
        }
    }
}
