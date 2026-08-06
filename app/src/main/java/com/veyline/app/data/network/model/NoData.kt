package com.veyline.app.data.network.model

/**
 * 表示 API 调用成功，但响应中没有需要使用的业务数据。
 *
 * 对于仍然返回 [ApiResponse] 响应壳、但 `data` 只是 `null`、空字符串或其他无意义占位值
 * 的接口，将响应类型声明为 `ApiResponse<NoData>`。自定义 JsonAdapter 会忽略 `data` 的
 * 具体 JSON 值并将其解析为 [NoData]，调用方因此不需要为无意义的字段创建业务模型。
 *
 * 只有接口契约明确说明 `data` 没有业务含义时才能使用 `ApiResponse<NoData>`，不能用它
 * 忽略暂时不需要的真实业务数据。本类型也不表示 HTTP Body 为空；`204 No Content` 等接口
 * 应在 Retrofit 与 `apiCall()` 层使用无响应体调用契约。
 */
data object NoData
