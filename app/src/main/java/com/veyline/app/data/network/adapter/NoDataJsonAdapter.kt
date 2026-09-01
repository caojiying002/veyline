package com.veyline.app.data.network.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import com.veyline.app.data.network.model.NoData

/**
 * 将没有业务含义的 `data` 字段转换为 [NoData]。
 *
 * ## 宽容策略
 *
 * 无数据接口的 `data` 只承担占位作用，但服务端可能返回 JSON `null`、字符串、数字、
 * 布尔值、对象或数组。本 Adapter 会无条件消费任意一种合法 JSON 值，不检查其类型和内容，
 * 不保留或记录原始数据，并统一返回 [NoData]。语法损坏的 JSON 仍由 Moshi 正常抛出解析异常。
 *
 * 这种宽容行为是刻意设计的，同时也意味着被消费的数据无法用于发现协议变化。只有接口
 * 契约明确声明 `data` 没有业务含义时才能使用 `ApiResponseDto<NoData>`；不得为了省略业务模型
 * 或忽略暂时不需要的字段而使用，否则有意义的数据也会被静默丢弃。
 */
class NoDataJsonAdapter {

    @FromJson
    fun fromJson(reader: JsonReader): NoData {
        reader.skipValue()
        return NoData
    }

    @ToJson
    fun toJson(writer: JsonWriter, @Suppress("UNUSED_PARAMETER") value: NoData?) {
        writer.nullValue()
    }
}
