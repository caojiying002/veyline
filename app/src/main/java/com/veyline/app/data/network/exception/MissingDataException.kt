package com.veyline.app.data.network.exception

/**
 * API 响应表示业务成功，但缺少调用方要求的业务数据。
 *
 * 该异常与 [EmptyResponseBodyException] 不同：后者表示整个 HTTP 响应体为空；本异常表示
 * 已取得 API 响应结构，但其中的 `data` 违反“成功响应必须包含数据”的客户端约定。
 * 对于明确约定成功时不返回业务数据的接口，应使用专门的无数据成功类型，而不是抛出本异常。
 */
class MissingDataException(
    message: String = "Required data is missing from a successful API response",
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
