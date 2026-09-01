package com.veyline.app.data.network.adapter

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.veyline.app.data.network.model.ApiResponseDto
import com.veyline.app.data.network.model.NoData
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * 验证 [ApiResponseJsonAdapterFactory] 对 [ApiResponse] 及其多态 `data` 字段的解析契约。
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
        ApiResponseDto::class.java,
        String::class.java,
    )
    private val stringAdapter: JsonAdapter<ApiResponseDto<String>> =
        moshi.adapter(stringResponseType)

    private val noDataResponseType = Types.newParameterizedType(
        ApiResponseDto::class.java,
        NoData::class.java,
    )
    private val noDataAdapter: JsonAdapter<ApiResponseDto<NoData>> =
        moshi.adapter(noDataResponseType)

    /** 验证业务成功时按照声明的泛型解析 `data`。 */
    @Test
    fun fromJson_withSuccessfulStringResponse_parsesData() {
        val result = stringAdapter.fromJson(
            """{"code":0,"msg":"success","data":"value"}""",
        )

        assertNotNull(result)
        assertEquals(ApiResponseDto.CODE_SUCCESS, result.code)
        assertEquals("success", result.msg)
        assertEquals("value", result.data)
        assertNull(result.fieldErrors)
    }

    /** 验证普通成功响应缺少 `data` 字段时保留数据缺失状态。 */
    @Test
    fun fromJson_withSuccessfulStringResponseWithoutData_returnsNullData() {
        val result = stringAdapter.fromJson(
            """{"code":0,"msg":"success"}""",
        )

        assertNotNull(result)
        assertEquals(ApiResponseDto.CODE_SUCCESS, result.code)
        assertEquals("success", result.msg)
        assertNull(result.data)
        assertNull(result.fieldErrors)
    }

    /** 验证无业务数据的成功响应缺少 `data` 字段时补充 [NoData]。 */
    @Test
    fun fromJson_withSuccessfulNoDataResponseWithoutData_returnsNoData() {
        val result = noDataAdapter.fromJson(
            """{"code":0,"msg":"success"}""",
        )

        assertNotNull(result)
        assertEquals(ApiResponseDto.CODE_SUCCESS, result.code)
        assertEquals("success", result.msg)
        assertSame(NoData, result.data)
        assertNull(result.fieldErrors)
    }

    /** 验证 `code` 位于 `data` 之后时，两阶段解析仍能正确选择数据 Adapter。 */
    @Test
    fun fromJson_withDataBeforeCode_parsesSuccessfully() {
        val result = stringAdapter.fromJson(
            """{"data":"value","msg":"success","code":0}""",
        )

        assertNotNull(result)
        assertEquals(ApiResponseDto.CODE_SUCCESS, result.code)
        assertEquals("success", result.msg)
        assertEquals("value", result.data)
        assertNull(result.fieldErrors)
    }

    /** 验证表单验证失败时将原始 `data` 解析为字段错误。 */
    @Test
    fun fromJson_withValidationError_parsesFieldErrors() {
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

        assertNotNull(result)
        assertEquals(ApiResponseDto.CODE_VALIDATION_ERROR, result.code)
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
    fun fromJson_withNullValidationData_returnsEmptyFieldErrors() {
        val result = stringAdapter.fromJson(
            """
            {
              "code": -1,
              "msg": "validation failed",
              "data": null
            }
            """.trimIndent(),
        )

        assertNotNull(result)
        assertEquals(ApiResponseDto.CODE_VALIDATION_ERROR, result.code)
        assertEquals("validation failed", result.msg)
        assertNull(result.data)
        assertEquals(
            emptyMap<String, String>(),
            result.fieldErrors,
        )
    }

    /** 验证验证错误缺少 `data` 字段时仍返回空字段错误 Map。 */
    @Test
    fun fromJson_withValidationErrorWithoutData_returnsEmptyFieldErrors() {
        val result = stringAdapter.fromJson(
            """
            {
              "code": -1,
              "msg": "validation failed"
            }
            """.trimIndent(),
        )

        assertNotNull(result)
        assertEquals(ApiResponseDto.CODE_VALIDATION_ERROR, result.code)
        assertEquals("validation failed", result.msg)
        assertNull(result.data)
        assertEquals(
            emptyMap<String, String>(),
            result.fieldErrors,
        )
    }

    /** 验证普通业务错误会跳过与成功类型不兼容的 `data`。 */
    @Test
    fun fromJson_withBusinessError_ignoresIncompatibleData() {
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

        assertNotNull(result)
        assertEquals(1000, result.code)
        assertEquals("business failed", result.msg)
        assertNull(result.data)
        assertNull(result.fieldErrors)
    }

    /** 验证缺少必需的 `code` 字段时解析失败。 */
    @Test
    fun fromJson_withoutCode_throwsJsonDataException() {
        assertFailsWith<JsonDataException> {
            stringAdapter.fromJson(
                """{"msg":"failed","data":null}""",
            )
        }
    }

    /** 验证必需的 `code` 显式为 JSON `null` 时解析失败。 */
    @Test
    fun fromJson_withNullCode_throwsJsonDataException() {
        assertFailsWith<JsonDataException> {
            stringAdapter.fromJson(
                """{"code":null,"msg":"failed","data":null}""",
            )
        }
    }

    /** 验证 `code` 不是整数类型时解析失败。 */
    @Test
    fun fromJson_withInvalidCodeType_throwsJsonDataException() {
        assertFailsWith<JsonDataException> {
            stringAdapter.fromJson(
                """{"code":true,"msg":"failed","data":null}""",
            )
        }
    }

    /** 验证新增的未知字段不会影响既有响应字段解析。 */
    @Test
    fun fromJson_withUnknownField_parsesKnownFields() {
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

        assertNotNull(result)
        assertEquals(ApiResponseDto.CODE_SUCCESS, result.code)
        assertEquals("success", result.msg)
        assertEquals("value", result.data)
        assertNull(result.fieldErrors)
    }

    /** 验证业务成功响应将泛型数据写入协议的 `data` 字段。 */
    @Test
    fun toJson_withSuccessfulStringResponse_writesData() {
        val response = ApiResponseDto(
            code = ApiResponseDto.CODE_SUCCESS,
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
    fun toJson_withValidationError_writesFieldErrors() {
        val response = ApiResponseDto<String>(
            code = ApiResponseDto.CODE_VALIDATION_ERROR,
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
