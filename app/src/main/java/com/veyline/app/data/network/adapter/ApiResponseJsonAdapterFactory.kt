package com.veyline.app.data.network.adapter

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.veyline.app.data.network.model.ApiResponse
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * 为 [ApiResponse] 创建能够根据业务状态码解析多态 `data` 字段的 Moshi Adapter。
 *
 * Adapter 先通过 [JsonReader.peekJson] 读取 [ApiResponse.code]，不移动原始 Reader；随后再
 * 使用原始 Reader 完成流式解析。该方式允许 `code` 出现在 `data` 之后，同时避免把完整
 * JSON 转成通用 Map 后产生类型信息损失和额外对象分配。
 */
class ApiResponseJsonAdapterFactory : JsonAdapter.Factory {

    override fun create(
        type: Type,
        annotations: Set<Annotation>,
        moshi: Moshi,
    ): JsonAdapter<*>? {
        // 只接管未携带 JsonQualifier 的 ApiResponse<T>；限定类型应交给对应的专用 Adapter。
        if (Types.getRawType(type) != ApiResponse::class.java
            || annotations.isNotEmpty()
        ) {
            return null
        }

        require(type is ParameterizedType) {
            "ApiResponse must declare a type parameter"
        }

        val dataType = type.actualTypeArguments.single()
        val dataAdapter = moshi.adapter<Any>(dataType)

        val fieldErrorType = Types.newParameterizedType(
            Map::class.java,
            String::class.java,
            String::class.java,
        )
        val fieldErrorsAdapter = moshi.adapter<Map<String, String>>(fieldErrorType)

        return ApiResponseJsonAdapter(
            dataAdapter = dataAdapter,
            fieldErrorsAdapter = fieldErrorsAdapter,
        )
    }
}

/**
 * [ApiResponse] 的实际 JSON Adapter。
 *
 * 业务成功时使用 [dataAdapter] 解析 `data`；表单验证失败时使用 [fieldErrorsAdapter] 将
 * `data` 解析为字段错误；其他业务失败则跳过 `data`，避免错误响应中的不同结构触发与
 * 成功数据类型无关的解析异常。
 */
private class ApiResponseJsonAdapter<T>(
    private val dataAdapter: JsonAdapter<T>,
    private val fieldErrorsAdapter: JsonAdapter<Map<String, String>>,
) : JsonAdapter<ApiResponse<T>>() {
    override fun fromJson(reader: JsonReader): ApiResponse<T> {
        val code = readRequiredCode(reader)
        var msg: String? = null
        var data: T? = null
        var fieldErrors: Map<String, String>? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                FIELD_CODE -> reader.skipValue()
                FIELD_MSG -> msg = reader.nextNullableString()
                FIELD_DATA -> when (code) {
                    ApiResponse.CODE_SUCCESS -> data = dataAdapter.fromJson(reader)
                    ApiResponse.CODE_VALIDATION_ERROR -> {
                        if (reader.peek() == JsonReader.Token.NULL) {
                            reader.skipValue()
                        } else {
                            fieldErrors = fieldErrorsAdapter.fromJson(reader)
                        }
                    }
                    else -> reader.skipValue()
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        // 将验证错误响应中缺失和显式为 null 的 data 统一为空 Map，简化上层处理。
        if (code == ApiResponse.CODE_VALIDATION_ERROR && fieldErrors == null) {
            fieldErrors = emptyMap()
        }

        return ApiResponse(
            code = code,
            msg = msg,
            data = data,
            fieldErrors = fieldErrors,
        )
    }

    override fun toJson(writer: JsonWriter, value: ApiResponse<T>?) {
        if (value == null) {
            writer.nullValue()
            return
        }

        writer.beginObject()
        writer.name(FIELD_CODE).value(value.code.toLong())
        writer.name(FIELD_MSG).value(value.msg)

        writer.name(FIELD_DATA)
        when (value.code) {
            ApiResponse.CODE_SUCCESS -> dataAdapter.toJson(writer, value.data)
            ApiResponse.CODE_VALIDATION_ERROR -> {
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
                    "Expected ApiResponse object at ${peekedReader.path}",
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
