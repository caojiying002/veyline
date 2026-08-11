package com.veyline.app.data.network.adapter

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.veyline.app.data.network.model.ApiResponse
import com.veyline.app.data.network.model.NoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * 验证 [ApiResponseJsonAdapterFactory] 对通用响应壳及多态 `data` 字段的解析契约。
 *
 * 测试覆盖成功、表单验证失败、普通业务失败、字段乱序、未知字段、必需状态码异常以及
 * 序列化分支。泛型使用 `String` 是为了隔离并验证 Factory 的类型分派行为，不表示 Factory
 * 只支持字符串；具体业务 DTO 的字段映射由其各自的 Moshi Adapter 负责。
 *
 * 测试方法统一使用“`fromJson`/`toJson` + `with`/`without` 条件 + 预期行为”的命名格式。
 */
class ApiResponseJsonAdapterFactoryTest {

    private val moshi = Moshi.Builder()
        .add(ApiResponseJsonAdapterFactory())
        .add(NoDataJsonAdapter())
        .build()

    private val stringResponseType = Types.newParameterizedType(
        ApiResponse::class.java,
        String::class.java,
    )
    private val stringAdapter: JsonAdapter<ApiResponse<String>> =
        moshi.adapter(stringResponseType)

    private val noDataResponseType = Types.newParameterizedType(
        ApiResponse::class.java,
        NoData::class.java,
    )
    private val noDataAdapter: JsonAdapter<ApiResponse<NoData>> =
        moshi.adapter(noDataResponseType)

    /** 验证业务成功时按照声明的泛型解析 `data`。 */
    @Test
    fun `fromJson with successful response parses String data`() {
        val result = stringAdapter.fromJson(
            """{"code":0,"msg":"success","data":"value"}""",
        )

        requireNotNull(result)
        assertEquals(ApiResponse.CODE_SUCCESS, result.code)
        assertEquals("success", result.msg)
        assertEquals("value", result.data)
        assertNull(result.fieldErrors)
    }

    /** 验证普通成功响应缺少 `data` 字段时保留数据缺失状态。 */
    @Test
    fun `fromJson with successful String response without data returns null data`() {
        val result = stringAdapter.fromJson(
            """{"code":0,"msg":"success"}""",
        )

        requireNotNull(result)
        assertEquals(ApiResponse.CODE_SUCCESS, result.code)
        assertEquals("success", result.msg)
        assertNull(result.data)
        assertNull(result.fieldErrors)
    }

    /** 验证无业务数据的成功响应缺少 `data` 字段时补充 [NoData]。 */
    @Test
    fun `fromJson with successful NoData response without data returns NoData`() {
        val result = noDataAdapter.fromJson(
            """{"code":0,"msg":"success"}""",
        )

        requireNotNull(result)
        assertEquals(ApiResponse.CODE_SUCCESS, result.code)
        assertEquals("success", result.msg)
        assertSame(NoData, result.data)
        assertNull(result.fieldErrors)
    }

    /** 验证 `code` 位于 `data` 之后时，两阶段解析仍能正确选择数据 Adapter。 */
    @Test
    fun `fromJson with data before code parses successfully`() {
        val result = stringAdapter.fromJson(
            """{"data":"value","msg":"success","code":0}""",
        )

        requireNotNull(result)
        assertEquals(ApiResponse.CODE_SUCCESS, result.code)
        assertEquals("success", result.msg)
        assertEquals("value", result.data)
        assertNull(result.fieldErrors)
    }

    /** 验证表单验证失败时将原始 `data` 解析为字段错误。 */
    @Test
    fun `fromJson with validation error parses field errors`() {
        val result = stringAdapter.fromJson(
            """
            {
              "code": -1,
              "msg": "validation failed",
              "data": {
                "username": "required",
                "password": "too short"
              }
            }
            """.trimIndent(),
        )

        requireNotNull(result)
        assertEquals(ApiResponse.CODE_VALIDATION_ERROR, result.code)
        assertEquals("validation failed", result.msg)
        assertNull(result.data)
        assertEquals(
            mapOf(
                "username" to "required",
                "password" to "too short",
            ),
            result.fieldErrors,
        )
    }

    /** 验证验证错误中的 `data: null` 被归一为空字段错误 Map。 */
    @Test
    fun `fromJson with null validation data returns empty field errors`() {
        val result = stringAdapter.fromJson(
            """
            {
              "code": -1,
              "msg": "validation failed",
              "data": null
            }
            """.trimIndent(),
        )

        requireNotNull(result)
        assertEquals(ApiResponse.CODE_VALIDATION_ERROR, result.code)
        assertEquals("validation failed", result.msg)
        assertNull(result.data)
        assertEquals(
            emptyMap<String, String>(),
            result.fieldErrors,
        )
    }

    /** 验证验证错误缺少 `data` 字段时仍返回空字段错误 Map。 */
    @Test
    fun `fromJson with validation error without data returns empty field errors`() {
        val result = stringAdapter.fromJson(
            """
            {
              "code": -1,
              "msg": "validation failed"
            }
            """.trimIndent(),
        )

        requireNotNull(result)
        assertEquals(ApiResponse.CODE_VALIDATION_ERROR, result.code)
        assertEquals("validation failed", result.msg)
        assertNull(result.data)
        assertEquals(
            emptyMap<String, String>(),
            result.fieldErrors,
        )
    }

    /** 验证普通业务错误会跳过与成功类型不兼容的 `data`。 */
    @Test
    fun `fromJson with business error ignores incompatible data`() {
        val result = stringAdapter.fromJson(
            """
            {
              "code": 1000,
              "msg": "business failed",
              "data": {
                "unexpected": "value"
              }
            }
            """.trimIndent(),
        )

        requireNotNull(result)
        assertEquals(1000, result.code)
        assertEquals("business failed", result.msg)
        assertNull(result.data)
        assertNull(result.fieldErrors)
    }

    /** 验证缺少必需的 `code` 字段时解析失败。 */
    @Test
    fun `fromJson without code throws JsonDataException`() {
        assertThrows(JsonDataException::class.java) {
            stringAdapter.fromJson(
                """{"msg":"failed","data":null}""",
            )
        }
    }

    /** 验证必需的 `code` 显式为 JSON `null` 时解析失败。 */
    @Test
    fun `fromJson with null code throws JsonDataException`() {
        assertThrows(JsonDataException::class.java) {
            stringAdapter.fromJson(
                """{"code":null,"msg":"failed","data":null}""",
            )
        }
    }

    /** 验证 `code` 不是整数类型时解析失败。 */
    @Test
    fun `fromJson with invalid code type throws JsonDataException`() {
        assertThrows(JsonDataException::class.java) {
            stringAdapter.fromJson(
                """{"code":true,"msg":"failed","data":null}""",
            )
        }
    }

    /** 验证新增的未知字段不会影响既有响应字段解析。 */
    @Test
    fun `fromJson with unknown field parses known fields`() {
        val result = stringAdapter.fromJson(
            """
            {
              "code": 0,
              "unknown": {
                "nested": true
              },
              "msg": "success",
              "data": "value"
            }
            """.trimIndent(),
        )

        requireNotNull(result)
        assertEquals(ApiResponse.CODE_SUCCESS, result.code)
        assertEquals("success", result.msg)
        assertEquals("value", result.data)
        assertNull(result.fieldErrors)
    }

    /** 验证业务成功响应将泛型数据写入协议的 `data` 字段。 */
    @Test
    fun `toJson with successful response writes String data`() {
        val response = ApiResponse(
            code = ApiResponse.CODE_SUCCESS,
            msg = "success",
            data = "value",
        )

        val result = stringAdapter.toJson(response)

        assertEquals(
            """{"code":0,"msg":"success","data":"value"}""",
            result,
        )
    }

    /** 验证表单字段错误序列化到协议的 `data` 字段，而不是模型属性名。 */
    @Test
    fun `toJson with validation error writes field errors`() {
        val response = ApiResponse<String>(
            code = ApiResponse.CODE_VALIDATION_ERROR,
            msg = "validation failed",
            data = null,
            fieldErrors = mapOf(
                "username" to "required",
                "password" to "too short",
            ),
        )

        val result = stringAdapter.toJson(response)

        assertEquals(
            """{"code":-1,"msg":"validation failed","data":{"username":"required","password":"too short"}}""",
            result,
        )
    }
}
