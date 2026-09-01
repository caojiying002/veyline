package com.veyline.app.data.network.adapter

import com.squareup.moshi.Moshi
import com.veyline.app.data.network.model.NoData
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.junit.Test

/**
 * 验证 [NoDataJsonAdapter] 对无业务含义占位值的宽容解析契约。
 *
 * 测试覆盖常见占位形式、复杂 JSON 值的完整消费以及固定的序列化结果。这里不验证 HTTP
 * 空响应体；[NoData] 只表示 `data` 字段没有业务含义可以被客户端忽略。
 *
 * 测试方法统一使用“`fromJson`/`toJson` + `with` 条件 + 预期行为”的命名格式。
 */
class NoDataJsonAdapterTest {

    private val moshi = Moshi.Builder()
        .add(NoDataJsonAdapter())
        .build()

    private val adapter = moshi.adapter(NoData::class.java)

    /** 验证 JSON `null` 被转换为唯一的 [NoData] 实例。 */
    @Test
    fun fromJson_withJsonNull_returnsNoData() {
        val result = adapter.fromJson("null")

        assertSame(NoData, result)
    }

    /** 验证服务端使用空字符串占位时仍能得到 [NoData]。 */
    @Test
    fun fromJson_withEmptyString_returnsNoData() {
        val result = adapter.fromJson("\"\"")

        assertSame(NoData, result)
    }

    /** 验证服务端使用空对象占位时仍能得到 [NoData]。 */
    @Test
    fun fromJson_withEmptyObject_returnsNoData() {
        val result = adapter.fromJson("{}")

        assertSame(NoData, result)
    }

    /** 验证 Adapter 能完整消费包含多种 Token 的嵌套 JSON 值。 */
    @Test
    fun fromJson_withNestedArray_returnsNoData() {
        val result = adapter.fromJson(
            """[null, "", 1, true, {"key": "value"}]""",
        )

        assertSame(NoData, result)
    }

    /** 验证 [NoData] 始终序列化为 JSON `null`。 */
    @Test
    fun toJson_withNoData_writesJsonNull() {
        val result = adapter.toJson(NoData)

        assertEquals("null", result)
    }
}
