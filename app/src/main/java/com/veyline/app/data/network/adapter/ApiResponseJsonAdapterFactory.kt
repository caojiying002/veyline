package com.veyline.app.data.network.adapter

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.veyline.app.data.network.model.ApiResponseDto
import com.veyline.app.data.network.model.NoData
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * 为 [ApiResponseDto] 创建能够根据业务状态码解析多态 `data` 字段的 Moshi Adapter。
 *
 * Adapter 先通过 [JsonReader.peekJson] 读取 [ApiResponseDto.code]，不移动原始 Reader；随后再
 * 使用原始 Reader 完成流式解析。该方式允许 `code` 出现在 `data` 之后，同时避免把完整
 * JSON 转成通用 Map 后产生类型信息损失和额外对象分配。
 */
class ApiResponseJsonAdapterFactory : JsonAdapter.Factory {

    override fun create(
        type: Type,
        annotations: Set<Annotation>,
        moshi: Moshi,
    ): JsonAdapter<*>? {
        // 只接管未携带 JsonQualifier 的 ApiResponseDto<T>；限定类型应交给对应的专用 Adapter。
        if (Types.getRawType(type) != ApiResponseDto::class.java
            || annotations.isNotEmpty()
        ) {
            return null
        }

        require(type is ParameterizedType) {
            "ApiResponseDto must declare a type parameter"
        }

        val dataType = type.actualTypeArguments.single()
        val dataAdapter = moshi.adapter<Any>(dataType)
        // 特殊处理：ApiResponseDto<NoData>类型允许`data`字段缺失或JSON null
        val defaultDataWhenAbsent =
            if (dataType == NoData::class.java) NoData else null

        val fieldErrorType = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            String::class.java,
        )
        val fieldErrorsAdapter = moshi.adapter<Map<String, String>>(fieldErrorType)

        return ApiResponseJsonAdapter(
            dataAdapter = dataAdapter,
            fieldErrorsAdapter = fieldErrorsAdapter,
            defaultDataWhenAbsent = defaultDataWhenAbsent,
        )
    }
}

/**
 * 根据 `code` 字段的值区分解析 [ApiResponseDto] 的 JSON Adapter。
 *
 * 业务成功时，使用响应声明的泛型 Adapter 解析 `data`；表单验证失败时，
 * 将 `data` 解析为字段错误 Map；其他业务失败则跳过 `data`，避免错误响应中的
 * 非标准数据结构触发与成功数据类型无关的解析异常。
 *
 * @param dataAdapter 解析和序列化业务成功响应中 `data` 字段的 Adapter。
 * @param fieldErrorsAdapter 解析和序列化表单验证错误中字段错误 Map 的 Adapter。
 * @param defaultDataWhenAbsent 业务成功但 JSON 完全缺少 `data` 字段时使用的默认值。
 * 当响应类型为 `ApiResponseDto<NoData>` 时传入 [NoData]；其他类型传入 `null`，保留数据缺失状态
 * 供上层转换为响应协议异常。
 */
private class ApiResponseJsonAdapter<T>(
    private val dataAdapter: JsonAdapter<T>,
    private val fieldErrorsAdapter: JsonAdapter<Map<String, String>>,
    private val defaultDataWhenAbsent: T?,
) : JsonAdapter<ApiResponseDto<T>>() {
    override fun fromJson(reader: JsonReader): ApiResponseDto<T> {
        val code = readRequiredCode(reader)
        var msg: String? = null
        /* 明确记录 `data` 字段是否出现 */
        var hasDataField = false
        var data: T? = null
        var fieldErrors: Map<String, String>? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                FIELD_CODE -> reader.skipValue()
                FIELD_MSG -> msg = reader.nextNullableString()
                FIELD_DATA -> {
                    hasDataField = true
                    when (code) {
                        ApiResponseDto.CODE_SUCCESS -> {
                            data = dataAdapter.fromJson(reader)
                        }
                        ApiResponseDto.CODE_VALIDATION_ERROR -> {
                            if (reader.peek() == JsonReader.Token.NULL) {
                                reader.skipValue()
                            } else {
                                fieldErrors = fieldErrorsAdapter.fromJson(reader)
                            }
                        }
                        else -> reader.skipValue()
                    }
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        // ApiResponseDto<NoData>类型允许缺失 `data` 字段；其他响应类型仍保持严格校验。
        if (code == ApiResponseDto.CODE_SUCCESS && !hasDataField) {
            data = defaultDataWhenAbsent
        }

        // 将验证错误响应中缺失和显式为 null 的 data 统一为空 Map，简化上层处理。
        if (code == ApiResponseDto.CODE_VALIDATION_ERROR && fieldErrors == null) {
            fieldErrors = emptyMap()
        }

        return ApiResponseDto(
            code = code,
            msg = msg,
            data = data,
            fieldErrors = fieldErrors,
        )
    }

    override fun toJson(writer: JsonWriter, value: ApiResponseDto<T>?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        writer.beginObject()
        writer.name(FIELD_CODE).value(value.code.toLong())
        writer.name(FIELD_MSG).value(value.msg)

        writer.name(FIELD_DATA)
        when (value.code) {
            ApiResponseDto.CODE_SUCCESS -> dataAdapter.toJson(writer, value.data)
            ApiResponseDto.CODE_VALIDATION_ERROR -> {
                fieldErrorsAdapter.toJson(writer, value.fieldErrors.orEmpty())
            }
            else -> writer.nullValue()
        }
        writer.endObject()
    }

    private fun readRequiredCode(reader: JsonReader): Int {
        val peekedReader = reader.peekJson()
        try {
            if (peekedReader.peek() != JsonReader.Token.BEGIN_OBJECT) {
                throw JsonDataException(
                    "Expected ApiResponseDto object at ${peekedReader.path}",
                )
            }

            peekedReader.beginObject()
            while (peekedReader.hasNext()) {
                if (peekedReader.nextName() == FIELD_CODE) {
                    if (peekedReader.peek() == JsonReader.Token.NULL) {
                        throw JsonDataException(
                            "Required field 'code' was null at ${peekedReader.path}",
                        )
                    }
                    // nextInt() 同时校验 code 必须是可解析的整数。
                    return peekedReader.nextInt()
                }
                peekedReader.skipValue()
            }

            throw JsonDataException(
                "Required field 'code' was missing at ${reader.path}",
            )
        } finally {
            peekedReader.close()
        }
    }

    private companion object {
        const val FIELD_CODE = "code"
        const val FIELD_MSG = "msg"
        const val FIELD_DATA = "data"
    }
}

private fun JsonReader.nextNullableString(): String? =
    if (peek() == JsonReader.Token.NULL) nextNull() else nextString()
