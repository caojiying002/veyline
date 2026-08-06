package com.veyline.app.data.network.exception

/**
 * HTTP 请求成功，但缺少当前接口契约要求的响应体。
 *
 * 仅当调用方要求解析响应体时使用本异常。对于 `204 No Content`，以及其他明确约定成功时
 * 不返回响应体的接口，空响应体属于合法结果，不应抛出本异常。
 */
class EmptyResponseBodyException(
    message: String = "Response body is empty",
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
