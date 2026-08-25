package com.veyline.app.data.network.model

import com.squareup.moshi.JsonClass

/**
 * 网络层使用的通用页码分页数据结构。
 *
 * 该结构与 MyBatis Plus 常见的分页响应字段兼容，可用于承载不同业务接口返回的当前页记录
 * 和分页元数据。分页信息以服务端返回的 [current] 和 [pages] 为准，调用方不需要根据
 * [total] 和 [size] 重复计算总页数。
 *
 * 所有字段保持可空，使服务端漏传字段或返回 JSON `null` 时仍能完成解析。业务数据进入
 * PagingSource、Repository 或领域层前，调用方必须根据相应接口协议完成非空校验，不能将
 * `null` 默认为空列表或零值，否则可能把异常响应误判为正常的分页结束。`records = []`
 * 表示接口成功返回合法空页；`records` 为 JSON `null` 或字段缺失则表示分页协议异常。
 *
 * 本类型只描述未经校验的网络数据，不负责计算下一页页码、判断分页是否结束或转换列表项。
 *
 * @param T 当前页记录对应的网络模型类型。
 * @property records 服务端返回的当前页记录；不是完整数据集合。
 * @property total 符合当前查询条件的记录总数，而不是当前页的记录数量。
 * @property size 服务端采用的每页数量，最后一页实际返回的 [records] 数量可能小于该值。
 * @property current 当前页码；现有接口约定从 `1` 开始。
 * @property pages 服务端计算的总页数，可用于判断当前页是否为最后一页。
 */
@JsonClass(generateAdapter = true)
data class PagedDataDto<T>(
    val records: List<T>?,
    val total: Int?,
    val size: Int?,
    val current: Int?,
    val pages: Int?,
)
