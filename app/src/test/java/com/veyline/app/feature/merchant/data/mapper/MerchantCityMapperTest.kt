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
    fun `toDomainModels with valid cities returns domain models`() {
        val cityDtos = listOf(
            MerchantCityDto(code = "code-a", name = "城市甲"),
            MerchantCityDto(code = "code-b", name = "城市乙"),
        )
        val expected = listOf(
            MerchantCity(code = "code-a", name = "城市甲"),
            MerchantCity(code = "code-b", name = "城市乙"),
        )

        val result = cityDtos.toDomainModels()

        assertEquals(expected, result)
    }

    /** 验证城市 code 和 name 的首尾空白不会进入领域模型。 */
    @Test
    fun `toDomainModels with surrounding whitespace trims fields`() {
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

        val result = cityDtos.toDomainModels()

        assertEquals(expected, result)
    }

    /** 验证空的网络模型列表被转换为空的领域模型列表。 */
    @Test
    fun `toDomainModels with empty list returns empty list`() {
        val cityDtos = emptyList<MerchantCityDto>()

        val result = cityDtos.toDomainModels()

        assertEquals(emptyList(), result)
    }

    /** 验证城市 code 缺失时抛出数据无效异常。 */
    @Test
    fun `toDomainModels with null code throws invalid data exception`() {
        val cityDtos = listOf(
            MerchantCityDto(code = null, name = "城市甲"),
        )

        val exception = assertFailsWith<InvalidApiDataException> {
            cityDtos.toDomainModels()
        }

        assertEquals(
            "Merchant city at index 0 has a missing or blank code",
            exception.message,
        )
    }

    /** 验证城市 code 只有空白字符时抛出数据无效异常。 */
    @Test
    fun `toDomainModels with blank code throws invalid data exception`() {
        val cityDtos = listOf(
            MerchantCityDto(code = "   ", name = "城市甲"),
        )

        val exception = assertFailsWith<InvalidApiDataException> {
            cityDtos.toDomainModels()
        }

        assertEquals(
            "Merchant city at index 0 has a missing or blank code",
            exception.message,
        )
    }

    /** 验证城市 name 缺失时抛出数据无效异常。 */
    @Test
    fun `toDomainModels with null name throws invalid data exception`() {
        val cityDtos = listOf(
            MerchantCityDto(code = "code-a", name = null),
        )

        val exception = assertFailsWith<InvalidApiDataException> {
            cityDtos.toDomainModels()
        }

        assertEquals(
            "Merchant city at index 0 has a missing or blank name",
            exception.message,
        )
    }

    /** 验证城市 name 只有空白字符时抛出数据无效异常。 */
    @Test
    fun `toDomainModels with blank name throws invalid data exception`() {
        val cityDtos = listOf(
            MerchantCityDto(code = "code-a", name = "   "),
        )

        val exception = assertFailsWith<InvalidApiDataException> {
            cityDtos.toDomainModels()
        }

        assertEquals(
            "Merchant city at index 0 has a missing or blank name",
            exception.message,
        )
    }

    /** 验证规范化后 code 重复时保留接口中第一次出现的城市。 */
    @Test
    fun `toDomainModels with duplicate codes keeps first city`() {
        val cityDtos = listOf(
            MerchantCityDto(code = "code-a", name = "城市甲"),
            MerchantCityDto(code = "  code-a  ", name = "重复城市"),
            MerchantCityDto(code = "code-b", name = "城市乙"),
        )
        val expected = listOf(
            MerchantCity(code = "code-a", name = "城市甲"),
            MerchantCity(code = "code-b", name = "城市乙"),
        )

        val result = cityDtos.toDomainModels()

        assertEquals(expected, result)
    }
}
